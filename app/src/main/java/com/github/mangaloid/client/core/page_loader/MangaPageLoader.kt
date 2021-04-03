package com.github.mangaloid.client.core.page_loader

import android.util.LruCache
import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.core.cache.CacheHandler
import com.github.mangaloid.client.model.data.MangaPageUrl
import com.github.mangaloid.client.util.BackgroundUtils
import com.github.mangaloid.client.util.suspendStoreIntoCacheIfNotCachedYet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
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
  private val cacheHandler: CacheHandler,
  private val okHttpClient: OkHttpClient
) {
  private val mangaPages = LruCache<MangaPageUrl, LoadableMangaPage>(128)

  private val mangaPageLoadActor = appScope.actor<MangaPageUrl>(
    context = Dispatchers.Main,
    capacity = 10
  ) {
    consumeEach { mangaPageUrl ->
      BackgroundUtils.ensureMainThread()

      val loadableMangaPage = mangaPages.get(mangaPageUrl)
        ?: return@consumeEach

      if (loadableMangaPage.isCanceled()) {
        mangaPages[mangaPageUrl]?.emit(MangaPageLoadingStatus.Canceled)
        return@consumeEach
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
        return@consumeEach
      }
    }
  }

  suspend fun loadMangaPage(mangaPageUrl: MangaPageUrl): SharedFlow<MangaPageLoadingStatus> {
    BackgroundUtils.ensureMainThread()
    val existingLoadableMangaPage = mangaPages.get(mangaPageUrl)

    val mangaPage = if (existingLoadableMangaPage != null) {
      if (existingLoadableMangaPage.currentValue() !is MangaPageLoadingStatus.Error) {
        // Already cached or is being loaded, no need to do anything
        return existingLoadableMangaPage.listenForLoadStatus()
      }

      existingLoadableMangaPage
    } else {
      // Use a new MutableStateFlow if the previous one ended up with an error so we can retry the
      // failed download
      LoadableMangaPage()
    }

    mangaPages.put(mangaPageUrl, mangaPage)
    mangaPageLoadActor.send(mangaPageUrl)

    return mangaPage.listenForLoadStatus()
  }

  fun cancelMangaPageLoading(mangaPageUrl: MangaPageUrl) {
    BackgroundUtils.ensureMainThread()
    mangaPages[mangaPageUrl]?.cancel()
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