package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.page_loader.DownloadableMangaPageUrl
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.Logger
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ReaderScreenViewModel(
  private val extensionId: ExtensionId,
  private val mangaId: MangaId,
  private val initialMangaChapterId: MangaChapterId,
  private val appSettings: AppSettings = DependenciesGraph.appSettings,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository,
  private val mangaPageLoader: MangaPageLoader = DependenciesGraph.mangaPageLoader
) : ViewModelWithState<ReaderScreenViewModel.ReaderScreenState>(ReaderScreenState()) {

  init {
    viewModelScope.launch {
      getMangaChapterInternal(initialMangaChapterId)
    }
  }

  private suspend fun getMangaChapterInternal(mangaChapterId: MangaChapterId) {
    Logger.d(TAG, "getMangaChapterInternal($mangaChapterId)")

    val mangaChapter = mangaRepository.getMangaChapterByIdFromCache(
      extensionId = extensionId,
      mangaId = mangaId,
      mangaChapterId = mangaChapterId
    )

    if (mangaChapter == null) {
      updateState {
        val error = MangaRepository.MangaChapterNotFound(
          extensionId = extensionId,
          mangaId = mangaId,
          mangaChapterId = mangaChapterId
        )

        copy(currentMangaChapterResult = ModularResult.Error(error))
      }

      return
    }

    updateState {
      val viewableMangaChapter = ViewableMangaChapter.fromMangaChapter(
        readerSwipeDirection = appSettings.readerSwipeDirection.get(),
        extensionId = mangaChapter.extensionId,
        prevChapterId = mangaChapter.prevChapterId,
        currentChapter = mangaChapter,
        nextChapterId = mangaChapter.nextChapterId
      )

      copy(currentMangaChapterResult = ModularResult.Value(viewableMangaChapter))
    }
  }

  suspend fun loadImage(
    viewableMangaChapter: ViewableMangaChapter,
    downloadableMangaPageUrl: DownloadableMangaPageUrl
  ): SharedFlow<MangaPageLoader.MangaPageLoadingStatus> {
    Logger.d(TAG, "loadImage($downloadableMangaPageUrl)")
    val mangaPageLoadStatusFlow = mangaPageLoader.loadMangaPage(downloadableMangaPageUrl)

    val pagesToPreload = formatUrlsOfPagesToPreload(
      mangaRepository = mangaRepository,
      viewableMangaChapter = viewableMangaChapter,
      downloadableMangaPageUrl = downloadableMangaPageUrl,
      preloadCount = appSettings.pagesToPreloadCount.get()
    )
    mangaPageLoader.preloadNextPages(pagesToPreload)

    return mangaPageLoadStatusFlow
  }

  fun retryLoadMangaPage(downloadableMangaPageUrl: DownloadableMangaPageUrl) {
    Logger.d(TAG, "retryLoadMangaPage(${downloadableMangaPageUrl.debugDownloadableMangaPageId()})")
    mangaPageLoader.retryLoadMangaPage(downloadableMangaPageUrl)
  }

  fun cancelLoading(downloadableMangaPageUrl: DownloadableMangaPageUrl) {
    if (mangaPageLoader.cancelMangaPageLoading(downloadableMangaPageUrl)) {
      Logger.d(TAG, "cancelLoading(${downloadableMangaPageUrl.debugDownloadableMangaPageId()})")
    }
  }

  fun switchMangaChapter(newMangaChapterId: MangaChapterId) {
    Logger.d(TAG, "switchMangaChapter($newMangaChapterId)")
    viewModelScope.launch { getMangaChapterInternal(mangaChapterId = newMangaChapterId) }
  }

  private suspend fun formatUrlsOfPagesToPreload(
    mangaRepository: MangaRepository,
    viewableMangaChapter: ViewableMangaChapter,
    downloadableMangaPageUrl: DownloadableMangaPageUrl,
    preloadCount: Int
  ): List<DownloadableMangaPageUrl> {
    val resultPages = mutableListOf<DownloadableMangaPageUrl>()
    val currentChapter = mangaRepository.getMangaChapterByIdFromCache(
      extensionId = viewableMangaChapter.extensionId,
      mangaId = viewableMangaChapter.mangaId,
      mangaChapterId = viewableMangaChapter.mangaChapterId
    )
    val nextChapterId = downloadableMangaPageUrl.nextChapterId

    if (currentChapter == null) {
      return resultPages
    }

    resultPages += downloadableMangaPageUrl.sliceNextPages(preloadCount)
      .map { pageIndex -> currentChapter.mangaChapterPageUrl(pageIndex + 1) }

    if (resultPages.size == preloadCount || nextChapterId == null) {
      return resultPages
    }

    val nextChapter = mangaRepository.getMangaChapterByIdFromCache(
      extensionId = viewableMangaChapter.extensionId,
      mangaId = viewableMangaChapter.mangaId,
      mangaChapterId = nextChapterId
    )

    checkNotNull(nextChapter) {
      "Next chapter is null for some unknown reason. " +
        "viewableMangaChapter=$viewableMangaChapter, nextChapterId=$nextChapterId"
    }

    val preloadFromNextChapterCount = preloadCount - resultPages.size

    resultPages += (0 until preloadFromNextChapterCount)
      .map { pageIndex -> nextChapter.mangaChapterPageUrl(pageIndex + 1) }

    return resultPages
  }

  data class ReaderScreenState(
    val currentMangaChapterResult: ModularResult<ViewableMangaChapter>? = null,
  )

  companion object {
    private const val TAG = "ReaderScreenViewModel"
  }

}