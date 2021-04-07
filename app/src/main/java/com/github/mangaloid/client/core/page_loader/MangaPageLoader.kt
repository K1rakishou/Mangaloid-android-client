package com.github.mangaloid.client.core.page_loader

import android.util.LruCache
import com.github.mangaloid.client.core.cache.CacheHandler
import com.github.mangaloid.client.core.coroutine_executor.LimitingConcurrentCoroutineExecutor
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.util.BackgroundUtils
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.suspendStoreIntoCacheIfNotCachedYet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

// TODO: 4/2/2021 use partial content for faster concurrent page downloads.
//  (use https://ipfs.io/api/v0/ls/bafybeihnoou2av5w2bzmwkl6hi25scyzz6sjwdfqp4cwq2ikf6dfmev3ta
//  to get file sizes of all pages of a chapter).
class MangaPageLoader(
  private val appScope: CoroutineScope,
  private val appSettings: AppSettings,
  private val cacheHandler: CacheHandler,
  private val okHttpClient: OkHttpClient
) {
  private val mangaPages = LruCache<DownloadableMangaPageUrl, LoadableMangaPage>(256)

  private val limitingConcurrentCoroutineExecutor = LimitingConcurrentCoroutineExecutor(
    maxCoroutinesCount = appSettings.pagesToPreloadCount.getBlocking(),
    coroutineScope = appScope,
    dispatcher = Dispatchers.Main
  )

  fun loadMangaPage(downloadableMangaPageUrl: DownloadableMangaPageUrl): SharedFlow<MangaPageLoadingStatus> {
    BackgroundUtils.ensureMainThread()
    Logger.d(TAG, "loadMangaPage(${downloadableMangaPageUrl.debugDownloadableMangaPageId()})")

    val existingLoadableMangaPage = mangaPages.get(downloadableMangaPageUrl)
    val currentLoadableMangaPageStatus = existingLoadableMangaPage?.currentValue()

    val mangaPage = if (existingLoadableMangaPage != null
      && currentLoadableMangaPageStatus !is MangaPageLoadingStatus.Error) {
      // Already cached or is being loaded, no need to do anything
      return existingLoadableMangaPage.listenForLoadStatus()
    } else {
      // Haven't been loaded yet or failed to load previously, try to load again
      LoadableMangaPage(downloadableMangaPageUrl)
    }

    mangaPages.put(downloadableMangaPageUrl, mangaPage)

    limitingConcurrentCoroutineExecutor.post(key = downloadableMangaPageUrl) {
      loadMangaPageInternal(downloadableMangaPageUrl)
    }

    return mangaPage.listenForLoadStatus()
  }

  fun preloadNextPages(pagesToPreload: List<DownloadableMangaPageUrl>) {
    BackgroundUtils.ensureMainThread()

    val pageIndexesString = pagesToPreload
      .joinToString { downloadableMangaPageUrl -> downloadableMangaPageUrl.debugDownloadableMangaPageId() }
    Logger.d(TAG, "preloadNextPages($pageIndexesString)")

    if (pagesToPreload.isEmpty()) {
      return
    }

    pagesToPreload.forEach { downloadableMangaPageUrl ->
      val existingLoadableMangaPage = mangaPages.get(downloadableMangaPageUrl)

      val mangaPage = if (existingLoadableMangaPage != null) {
        // Already cached or is being loaded, no need to do anything
        return@forEach
      } else {
        // Haven't been loaded yet or failed to load previously, try to preload it
        LoadableMangaPage(downloadableMangaPageUrl)
      }

      mangaPages.put(downloadableMangaPageUrl, mangaPage)

      limitingConcurrentCoroutineExecutor.post(key = downloadableMangaPageUrl) {
        loadMangaPageInternal(downloadableMangaPageUrl)
      }
    }
  }

  fun retryLoadMangaPage(downloadableMangaPageUrl: DownloadableMangaPageUrl) {
    BackgroundUtils.ensureMainThread()
    Logger.d(TAG, "retryLoadMangaPage(${downloadableMangaPageUrl.debugDownloadableMangaPageId()})")

    limitingConcurrentCoroutineExecutor.post(key = downloadableMangaPageUrl) {
      loadMangaPageInternal(downloadableMangaPageUrl)
    }
  }

  fun cancelMangaPageLoading(downloadableMangaPageUrl: DownloadableMangaPageUrl): Boolean {
    BackgroundUtils.ensureMainThread()
    var canceled = false

    canceled = canceled or limitingConcurrentCoroutineExecutor.cancel(key = downloadableMangaPageUrl)
    canceled = canceled or (mangaPages[downloadableMangaPageUrl]?.cancel() ?: false)

    if (canceled) {
      Logger.d(TAG, "cancelMangaPageLoading(${downloadableMangaPageUrl.debugDownloadableMangaPageId()})")
    }

    return canceled
  }

  private suspend fun loadMangaPageInternal(downloadableMangaPageUrl: DownloadableMangaPageUrl) {
    BackgroundUtils.ensureMainThread()

    val loadableMangaPage = mangaPages.get(downloadableMangaPageUrl)
      ?: return

    if (loadableMangaPage.isCanceled()) {
      val status = MangaPageLoadingStatus.Canceled(downloadableMangaPageUrl)
      mangaPages[downloadableMangaPageUrl]?.emit(status)
      return
    }

    try {
      downloadPageInternal(downloadableMangaPageUrl, loadableMangaPage)
        .flowOn(Dispatchers.Main)
        .collect { mangaPageLoadingStatus ->
          BackgroundUtils.ensureMainThread()

          mangaPages[downloadableMangaPageUrl]?.emit(mangaPageLoadingStatus)
        }
    } catch (error: Throwable) {
      val status = MangaPageLoadingStatus.Error(downloadableMangaPageUrl, error)
      mangaPages[downloadableMangaPageUrl]?.emit(status)
    }
  }

  private suspend fun downloadPageInternal(
    downloadableMangaPageUrl: DownloadableMangaPageUrl,
    loadableMangaPage: LoadableMangaPage
  ): Flow<MangaPageLoadingStatus> {
    return flow {
      val request = Request.Builder()
        .get()
        .url(downloadableMangaPageUrl.url)
        .build()

      val result = okHttpClient.suspendStoreIntoCacheIfNotCachedYet(
        cacheHandler = cacheHandler,
        url = downloadableMangaPageUrl.url.toString(),
        request = request,
        cancellationCheckFunc = { loadableMangaPage.isCanceled() }
      ) { progress ->
        BackgroundUtils.ensureBackgroundThread()

        withContext(Dispatchers.Main) {
          emit(MangaPageLoadingStatus.Loading(downloadableMangaPageUrl, progress))
        }
      }

      when (result) {
        is ModularResult.Error -> {
          if (result.error is CancellationException) {
            emit(MangaPageLoadingStatus.Canceled(downloadableMangaPageUrl))
          } else {
            emit(MangaPageLoadingStatus.Error(downloadableMangaPageUrl, result.error))
          }
        }
        is ModularResult.Value -> {
          emit(MangaPageLoadingStatus.Success(downloadableMangaPageUrl, result.value))
        }
      }
    }
  }

  class LoadableMangaPage(downloadableMangaPageUrl: DownloadableMangaPageUrl) {
    private val canceled: AtomicBoolean = AtomicBoolean(false)
    private val _loadStatusFlow = MutableSharedFlow<MangaPageLoadingStatus>(
      replay = 1,
      extraBufferCapacity = 32,
      onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Start(downloadableMangaPageUrl))
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Loading(downloadableMangaPageUrl, 0f))
    }

    fun isCanceled(): Boolean = canceled.get()

    fun currentValue(): MangaPageLoadingStatus = _loadStatusFlow.replayCache.first()

    fun listenForLoadStatus(): SharedFlow<MangaPageLoadingStatus> = _loadStatusFlow.asSharedFlow()

    suspend fun emit(mangaPageLoadingStatus: MangaPageLoadingStatus) {
      _loadStatusFlow.emit(mangaPageLoadingStatus)
    }

    fun cancel(): Boolean {
      if (currentValue() !is MangaPageLoadingStatus.Loading) {
        return false
      }

      canceled.compareAndSet(false, true)
      return true
    }

  }

  sealed class MangaPageLoadingStatus(
    val downloadableMangaPageUrl: DownloadableMangaPageUrl
  ) {
    class Start(
      downloadableMangaPageUrl: DownloadableMangaPageUrl
    ) : MangaPageLoadingStatus(downloadableMangaPageUrl)

    class Loading(
      downloadableMangaPageUrl: DownloadableMangaPageUrl,
      val progress: Float?
    ): MangaPageLoadingStatus(downloadableMangaPageUrl)

    class Success(
      downloadableMangaPageUrl: DownloadableMangaPageUrl,
      val mangaPageFile: File
    ) : MangaPageLoadingStatus(downloadableMangaPageUrl)

    class Error(
      downloadableMangaPageUrl: DownloadableMangaPageUrl,
      val throwable: Throwable
    ) : MangaPageLoadingStatus(downloadableMangaPageUrl)

    class Canceled(
      downloadableMangaPageUrl: DownloadableMangaPageUrl
    ) : MangaPageLoadingStatus(downloadableMangaPageUrl)
  }

  companion object {
    private const val TAG = "MangaPageLoader"
  }
}