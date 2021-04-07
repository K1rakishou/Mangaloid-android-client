package com.github.mangaloid.client.core.page_loader

import android.util.LruCache
import com.github.mangaloid.client.core.cache.CacheHandler
import com.github.mangaloid.client.core.coroutine_executor.LimitingConcurrentCoroutineExecutor
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.util.BackgroundUtils
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.errorMessageOrClassName
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
  private val mangaPages = LruCache<DownloadableMangaPage, LoadableMangaPage>(256)

  private val limitingConcurrentCoroutineExecutor = LimitingConcurrentCoroutineExecutor(
    maxCoroutinesCount = appSettings.pagesToPreloadCount.getBlocking(),
    coroutineScope = appScope,
    dispatcher = Dispatchers.Main
  )

  fun loadMangaPage(downloadableMangaPage: DownloadableMangaPage): SharedFlow<MangaPageLoadingStatus> {
    BackgroundUtils.ensureMainThread()
    Logger.d(TAG, "loadMangaPage(${downloadableMangaPage.debugDownloadableMangaPageId()})")

    val existingLoadableMangaPage = mangaPages.get(downloadableMangaPage)
    val currentLoadableMangaPageStatus = existingLoadableMangaPage?.currentValue()

    val mangaPage = if (existingLoadableMangaPage != null
      && currentLoadableMangaPageStatus !is MangaPageLoadingStatus.Error) {
      // Already cached or is being loaded, no need to do anything
      return existingLoadableMangaPage.listenForLoadStatus()
    } else {
      // Haven't been loaded yet or failed to load previously, try to load again
      LoadableMangaPage(downloadableMangaPage)
    }

    mangaPages.put(downloadableMangaPage, mangaPage)

    limitingConcurrentCoroutineExecutor.post(key = downloadableMangaPage) {
      loadMangaPageInternal(downloadableMangaPage)
    }

    return mangaPage.listenForLoadStatus()
  }

  fun preloadNextPages(pagesToPreload: List<DownloadableMangaPage>) {
    BackgroundUtils.ensureMainThread()

    if (pagesToPreload.isEmpty()) {
      return
    }

    pagesToPreload.forEach { downloadableMangaPage ->
      val existingLoadableMangaPage = mangaPages.get(downloadableMangaPage)
      if (existingLoadableMangaPage != null) {
        // Already cached or is being loaded, no need to do anything
        return@forEach
      }

      // Haven't been loaded yet or failed to load previously, try to preload it
      val mangaPage =  LoadableMangaPage(downloadableMangaPage)

      Logger.d(TAG, "preloadNextPages(${downloadableMangaPage.debugDownloadableMangaPageId()})")
      mangaPages.put(downloadableMangaPage, mangaPage)

      limitingConcurrentCoroutineExecutor.post(key = downloadableMangaPage) {
        loadMangaPageInternal(downloadableMangaPage)
      }
    }
  }

  fun retryLoadMangaPage(downloadableMangaPage: DownloadableMangaPage) {
    BackgroundUtils.ensureMainThread()
    Logger.d(TAG, "retryLoadMangaPage(${downloadableMangaPage.debugDownloadableMangaPageId()})")

    limitingConcurrentCoroutineExecutor.post(key = downloadableMangaPage) {
      loadMangaPageInternal(downloadableMangaPage)
    }
  }

  fun cancelMangaPageLoading(downloadableMangaPage: DownloadableMangaPage): Boolean {
    BackgroundUtils.ensureMainThread()
    var canceled = false

    canceled = canceled or limitingConcurrentCoroutineExecutor.cancel(key = downloadableMangaPage)
    canceled = canceled or (mangaPages[downloadableMangaPage]?.cancel() ?: false)

    if (canceled) {
      Logger.d(TAG, "cancelMangaPageLoading(${downloadableMangaPage.debugDownloadableMangaPageId()})")
    }

    return canceled
  }

  private suspend fun loadMangaPageInternal(downloadableMangaPage: DownloadableMangaPage) {
    BackgroundUtils.ensureMainThread()

    val loadableMangaPage = mangaPages.get(downloadableMangaPage)
      ?: return

    if (loadableMangaPage.isCanceled()) {
      val status = MangaPageLoadingStatus.Canceled(downloadableMangaPage)
      mangaPages[downloadableMangaPage]?.emit(status)
      return
    }

    try {
      downloadPageInternal(downloadableMangaPage, loadableMangaPage)
        .flowOn(Dispatchers.Main)
        .collect { mangaPageLoadingStatus ->
          BackgroundUtils.ensureMainThread()

          mangaPages[downloadableMangaPage]?.emit(mangaPageLoadingStatus)
        }
    } catch (error: Throwable) {
      val status = MangaPageLoadingStatus.Error(downloadableMangaPage, error)
      mangaPages[downloadableMangaPage]?.emit(status)
    }
  }

  private suspend fun downloadPageInternal(
    downloadableMangaPage: DownloadableMangaPage,
    loadableMangaPage: LoadableMangaPage
  ): Flow<MangaPageLoadingStatus> {
    return flow {
      val request = Request.Builder()
        .get()
        .url(downloadableMangaPage.url)
        .build()

      val result = okHttpClient.suspendStoreIntoCacheIfNotCachedYet(
        cacheHandler = cacheHandler,
        url = downloadableMangaPage.url.toString(),
        request = request,
        cancellationCheckFunc = { loadableMangaPage.isCanceled() }
      ) { progress ->
        BackgroundUtils.ensureBackgroundThread()

        withContext(Dispatchers.Main) {
          emit(MangaPageLoadingStatus.Loading(downloadableMangaPage, progress))
        }
      }

      when (result) {
        is ModularResult.Error -> {
          if (result.error is CancellationException) {
            Logger.e(TAG, "downloadPageInternal(${downloadableMangaPage.debugDownloadableMangaPageId()}) Canceled")
            emit(MangaPageLoadingStatus.Canceled(downloadableMangaPage))
          } else {
            Logger.e(TAG, "downloadPageInternal(${downloadableMangaPage.debugDownloadableMangaPageId()}) " +
              "Error (${result.error.errorMessageOrClassName()})")
            emit(MangaPageLoadingStatus.Error(downloadableMangaPage, result.error))
          }
        }
        is ModularResult.Value -> {
          Logger.d(TAG, "downloadPageInternal(${downloadableMangaPage.debugDownloadableMangaPageId()}) Success")
          emit(MangaPageLoadingStatus.Success(downloadableMangaPage, result.value))
        }
      }
    }
  }

  class LoadableMangaPage(downloadableMangaPage: DownloadableMangaPage) {
    private val canceled: AtomicBoolean = AtomicBoolean(false)
    private val _loadStatusFlow = MutableSharedFlow<MangaPageLoadingStatus>(
      replay = 1,
      extraBufferCapacity = 32,
      onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Start(downloadableMangaPage))
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Loading(downloadableMangaPage, 0f))
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
    val downloadableMangaPage: DownloadableMangaPage
  ) {
    class Start(
      downloadableMangaPage: DownloadableMangaPage
    ) : MangaPageLoadingStatus(downloadableMangaPage)

    class Loading(
      downloadableMangaPage: DownloadableMangaPage,
      val progress: Float?
    ): MangaPageLoadingStatus(downloadableMangaPage)

    class Success(
      downloadableMangaPage: DownloadableMangaPage,
      val mangaPageFile: File
    ) : MangaPageLoadingStatus(downloadableMangaPage)

    class Error(
      downloadableMangaPage: DownloadableMangaPage,
      val throwable: Throwable
    ) : MangaPageLoadingStatus(downloadableMangaPage)

    class Canceled(
      downloadableMangaPage: DownloadableMangaPage
    ) : MangaPageLoadingStatus(downloadableMangaPage)
  }

  companion object {
    private const val TAG = "MangaPageLoader"
  }
}