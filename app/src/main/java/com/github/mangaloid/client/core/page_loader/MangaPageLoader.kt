package com.github.mangaloid.client.core.page_loader

import android.util.LruCache
import com.github.mangaloid.client.core.cache.CacheHandler
import com.github.mangaloid.client.core.coroutine_executor.LimitingConcurrentCoroutineExecutor
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.model.data.MangaChapterPage
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
  private val mangaPages = LruCache<MangaChapterPage, LoadableMangaPage>(256)

  private val limitingConcurrentCoroutineExecutor = LimitingConcurrentCoroutineExecutor(
    maxCoroutinesCount = appSettings.pagesToPreloadCount.getBlocking(),
    coroutineScope = appScope,
    dispatcher = Dispatchers.Main
  )

  fun loadMangaPage(mangaChapterPage: MangaChapterPage): SharedFlow<MangaPageLoadingStatus> {
    BackgroundUtils.ensureMainThread()
    Logger.d(TAG, "loadMangaPage(${mangaChapterPage.debugMangaPageId()})")

    val existingLoadableMangaPage = mangaPages.get(mangaChapterPage)
    val currentLoadableMangaPageStatus = existingLoadableMangaPage?.currentValue()

    val mangaPage = if (existingLoadableMangaPage != null
      && currentLoadableMangaPageStatus !is MangaPageLoadingStatus.Error) {
      // Already cached or is being loaded, no need to do anything
      return existingLoadableMangaPage.listenForLoadStatus()
    } else {
      // Haven't been loaded yet or failed to load previously, try to load again
      LoadableMangaPage(mangaChapterPage)
    }

    mangaPages.put(mangaChapterPage, mangaPage)

    limitingConcurrentCoroutineExecutor.post(key = mangaChapterPage) {
      loadMangaPageInternal(mangaChapterPage)
    }

    return mangaPage.listenForLoadStatus()
  }

  fun preloadNextPages(pagesToPreload: List<MangaChapterPage>) {
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

      Logger.d(TAG, "preloadNextPages(${downloadableMangaPage.debugMangaPageId()})")
      mangaPages.put(downloadableMangaPage, mangaPage)

      limitingConcurrentCoroutineExecutor.post(key = downloadableMangaPage) {
        loadMangaPageInternal(downloadableMangaPage)
      }
    }
  }

  fun retryLoadMangaPage(mangaChapterPage: MangaChapterPage) {
    BackgroundUtils.ensureMainThread()
    Logger.d(TAG, "retryLoadMangaPage(${mangaChapterPage.debugMangaPageId()})")

    limitingConcurrentCoroutineExecutor.post(key = mangaChapterPage) {
      loadMangaPageInternal(mangaChapterPage)
    }
  }

  fun cancelMangaPageLoading(mangaChapterPage: MangaChapterPage): Boolean {
    BackgroundUtils.ensureMainThread()
    var canceled = false

    canceled = canceled or limitingConcurrentCoroutineExecutor.cancel(key = mangaChapterPage)
    canceled = canceled or (mangaPages[mangaChapterPage]?.cancel() ?: false)

    if (canceled) {
      Logger.d(TAG, "cancelMangaPageLoading(${mangaChapterPage.debugMangaPageId()})")
    }

    return canceled
  }

  private suspend fun loadMangaPageInternal(mangaChapterPage: MangaChapterPage) {
    BackgroundUtils.ensureMainThread()

    val loadableMangaPage = mangaPages.get(mangaChapterPage)
      ?: return

    if (loadableMangaPage.isCanceled()) {
      val status = MangaPageLoadingStatus.Canceled(mangaChapterPage)
      mangaPages[mangaChapterPage]?.emit(status)
      return
    }

    try {
      downloadPageInternal(mangaChapterPage, loadableMangaPage)
        .flowOn(Dispatchers.Main)
        .collect { mangaPageLoadingStatus ->
          BackgroundUtils.ensureMainThread()

          mangaPages[mangaChapterPage]?.emit(mangaPageLoadingStatus)
        }
    } catch (error: Throwable) {
      val status = MangaPageLoadingStatus.Error(mangaChapterPage, error)
      mangaPages[mangaChapterPage]?.emit(status)
    }
  }

  private suspend fun downloadPageInternal(
    mangaChapterPage: MangaChapterPage,
    loadableMangaPage: LoadableMangaPage
  ): Flow<MangaPageLoadingStatus> {
    return flow {
      val request = Request.Builder()
        .get()
        .url(mangaChapterPage.url)
        .build()

      val result = okHttpClient.suspendStoreIntoCacheIfNotCachedYet(
        cacheHandler = cacheHandler,
        url = mangaChapterPage.url.toString(),
        request = request,
        cancellationCheckFunc = { loadableMangaPage.isCanceled() }
      ) { progress ->
        BackgroundUtils.ensureBackgroundThread()

        withContext(Dispatchers.Main) {
          emit(MangaPageLoadingStatus.Loading(mangaChapterPage, progress))
        }
      }

      when (result) {
        is ModularResult.Error -> {
          if (result.error is CancellationException) {
            Logger.e(TAG, "downloadPageInternal(${mangaChapterPage.debugMangaPageId()}) Canceled")
            emit(MangaPageLoadingStatus.Canceled(mangaChapterPage))
          } else {
            Logger.e(TAG, "downloadPageInternal(${mangaChapterPage.debugMangaPageId()}) " +
              "Error (${result.error.errorMessageOrClassName()})")
            emit(MangaPageLoadingStatus.Error(mangaChapterPage, result.error))
          }
        }
        is ModularResult.Value -> {
          Logger.d(TAG, "downloadPageInternal(${mangaChapterPage.debugMangaPageId()}) Success")
          emit(MangaPageLoadingStatus.Success(mangaChapterPage, result.value))
        }
      }
    }
  }

  class LoadableMangaPage(mangaChapterPage: MangaChapterPage) {
    private val canceled: AtomicBoolean = AtomicBoolean(false)
    private val _loadStatusFlow = MutableSharedFlow<MangaPageLoadingStatus>(
      replay = 1,
      extraBufferCapacity = 32,
      onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Start(mangaChapterPage))
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Loading(mangaChapterPage, 0f))
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
    val mangaChapterPage: MangaChapterPage
  ) {
    class Start(
      mangaChapterPage: MangaChapterPage
    ) : MangaPageLoadingStatus(mangaChapterPage)

    class Loading(
      mangaChapterPage: MangaChapterPage,
      val progress: Float?
    ): MangaPageLoadingStatus(mangaChapterPage)

    class Success(
      mangaChapterPage: MangaChapterPage,
      val mangaPageFile: File
    ) : MangaPageLoadingStatus(mangaChapterPage)

    class Error(
      mangaChapterPage: MangaChapterPage,
      val throwable: Throwable
    ) : MangaPageLoadingStatus(mangaChapterPage)

    class Canceled(
      mangaChapterPage: MangaChapterPage
    ) : MangaPageLoadingStatus(mangaChapterPage)
  }

  companion object {
    private const val TAG = "MangaPageLoader"
  }
}