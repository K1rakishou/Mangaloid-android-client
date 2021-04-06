package com.github.mangaloid.client.core.page_loader

import android.util.LruCache
import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.core.coroutine_executor.LimitingConcurrentCoroutineExecutor
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.cache.CacheHandler
import com.github.mangaloid.client.model.data.MangaPageUrl
import com.github.mangaloid.client.util.BackgroundUtils
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
  private val preloadImagesCount: Int = AppConstants.preloadImagesCount,
  private val appScope: CoroutineScope,
  private val cacheHandler: CacheHandler,
  private val okHttpClient: OkHttpClient
) {
  private val mangaPages = LruCache<MangaPageUrl, LoadableMangaPage>(64)

  private val limitingConcurrentCoroutineExecutor = LimitingConcurrentCoroutineExecutor(
    maxCoroutinesCount = preloadImagesCount,
    coroutineScope = appScope,
    dispatcher = Dispatchers.Main
  )

  fun loadMangaPage(mangaPageUrl: MangaPageUrl): SharedFlow<MangaPageLoadingStatus> {
    BackgroundUtils.ensureMainThread()

    val existingLoadableMangaPage = mangaPages.get(mangaPageUrl)
    val currentLoadableMangaPageStatus = existingLoadableMangaPage?.currentValue()

    val mangaPage = if (existingLoadableMangaPage != null
      && currentLoadableMangaPageStatus !is MangaPageLoadingStatus.Error) {
      // Already cached or is being loaded, no need to do anything
      return existingLoadableMangaPage.listenForLoadStatus()
    } else {
      // Haven't been loaded yet or failed to load previously, try to load again
      LoadableMangaPage()
    }

    mangaPages.put(mangaPageUrl, mangaPage)

    limitingConcurrentCoroutineExecutor.post(key = mangaPageUrl) {
      loadMangaPageInternal(mangaPageUrl)
    }

    return mangaPage.listenForLoadStatus()
  }

  fun retryLoadMangaPage(mangaPageUrl: MangaPageUrl) {
    BackgroundUtils.ensureMainThread()

    limitingConcurrentCoroutineExecutor.post(key = mangaPageUrl) {
      loadMangaPageInternal(mangaPageUrl)
    }
  }

  fun cancelMangaPageLoading(mangaPageUrl: MangaPageUrl) {
    BackgroundUtils.ensureMainThread()

    limitingConcurrentCoroutineExecutor.cancel(key = mangaPageUrl)
    mangaPages[mangaPageUrl]?.cancel()
  }

  private suspend fun loadMangaPageInternal(mangaPageUrl: MangaPageUrl) {
    BackgroundUtils.ensureMainThread()

    val loadableMangaPage = mangaPages.get(mangaPageUrl)
      ?: return

    if (loadableMangaPage.isCanceled()) {
      mangaPages[mangaPageUrl]?.emit(MangaPageLoadingStatus.Canceled)
      return
    }

    try {
      downloadPageInternal(mangaPageUrl, loadableMangaPage)
        .flowOn(Dispatchers.Main)
        .collect { mangaPageLoadingStatus ->
          BackgroundUtils.ensureMainThread()

          mangaPages[mangaPageUrl]?.emit(mangaPageLoadingStatus)
        }
    } catch (error: Throwable) {
      mangaPages[mangaPageUrl]?.emit(MangaPageLoadingStatus.Error(error))
    }
  }

  private suspend fun downloadPageInternal(
    mangaPageUrl: MangaPageUrl,
    loadableMangaPage: LoadableMangaPage
  ): Flow<MangaPageLoadingStatus> {
    return flow {
      val request = Request.Builder()
        .get()
        .url(mangaPageUrl.url)
        .build()

      val result = okHttpClient.suspendStoreIntoCacheIfNotCachedYet(
        cacheHandler = cacheHandler,
        url = mangaPageUrl.url.toString(),
        request = request,
        cancellationCheckFunc = { loadableMangaPage.isCanceled() }
      ) { progress ->
        BackgroundUtils.ensureBackgroundThread()

        withContext(Dispatchers.Main) { emit(MangaPageLoadingStatus.Loading(progress)) }
      }

      when (result) {
        is ModularResult.Error -> {
          if (result.error is CancellationException) {
            emit(MangaPageLoadingStatus.Canceled)
          } else {
            emit(MangaPageLoadingStatus.Error(result.error))
          }
        }
        is ModularResult.Value -> {
          emit(MangaPageLoadingStatus.Success(result.value))
        }
      }
    }
  }

  class LoadableMangaPage() {
    private val canceled: AtomicBoolean = AtomicBoolean(false)
    private val _loadStatusFlow = MutableSharedFlow<MangaPageLoadingStatus>(
      replay = 1,
      extraBufferCapacity = 32,
      onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Start)
      _loadStatusFlow.tryEmit(MangaPageLoadingStatus.Loading(0f))
    }

    fun isCanceled(): Boolean = canceled.get()

    fun currentValue(): MangaPageLoadingStatus = _loadStatusFlow.replayCache.first()

    fun listenForLoadStatus(): SharedFlow<MangaPageLoadingStatus> = _loadStatusFlow.asSharedFlow()

    suspend fun emit(mangaPageLoadingStatus: MangaPageLoadingStatus) {
      _loadStatusFlow.emit(mangaPageLoadingStatus)
    }

    fun cancel() {
      if (currentValue() !is MangaPageLoadingStatus.Loading) {
        return
      }

      canceled.compareAndSet(false, true)
    }

  }

  sealed class MangaPageLoadingStatus {
    object Start : MangaPageLoadingStatus()
    data class Loading(val progress: Float?): MangaPageLoadingStatus()
    data class Success(val mangaPageFile: File) : MangaPageLoadingStatus()
    data class Error(val throwable: Throwable) : MangaPageLoadingStatus()
    object Canceled : MangaPageLoadingStatus()
  }

  companion object {
    private const val TAG = "MangaPageLoader"
  }
}