package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.page_loader.DownloadableMangaPage
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.errorMessageOrClassName
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
    updateState { copy(currentMangaChapterAsync = AsyncData.Loading()) }

    val mangaChapterResult = mangaRepository.getMangaChapterById(
      extensionId = extensionId,
      mangaId = mangaId,
      mangaChapterId = mangaChapterId
    )

    if (mangaChapterResult is ModularResult.Error) {
      updateState {
        copy(currentMangaChapterAsync = AsyncData.Error(mangaChapterResult.error))
      }

      return
    }

    val mangaChapter = (mangaChapterResult as ModularResult.Value).value

    val updatedMangaChapterResult = mangaRepository.refreshMangaChapterPagesIfNeeded(
      extensionId = extensionId,
      mangaChapter = mangaChapter
    )

    if (updatedMangaChapterResult is ModularResult.Error) {
      updateState {
        copy(currentMangaChapterAsync = AsyncData.Error(updatedMangaChapterResult.error))
      }

      return
    }

    val updatedMangaChapter = (updatedMangaChapterResult as ModularResult.Value).value
    if (updatedMangaChapter == null) {
      updateState {
        val error = MangaRepository.MangaChapterNotFound(
          extensionId = extensionId,
          mangaId = mangaId,
          mangaChapterId = mangaChapterId
        )

        copy(currentMangaChapterAsync = AsyncData.Error(error))
      }

      return
    }

    updateState {
      val viewableMangaChapter = ViewableMangaChapter.fromMangaChapter(
        readerSwipeDirection = appSettings.readerSwipeDirection.get(),
        extensionId = updatedMangaChapter.extensionId,
        prevChapterId = updatedMangaChapter.prevChapterId,
        currentChapter = updatedMangaChapter,
        nextChapterId = updatedMangaChapter.nextChapterId
      )

      copy(currentMangaChapterAsync = AsyncData.Data(viewableMangaChapter))
    }
  }

  suspend fun loadImage(
    viewableMangaChapter: ViewableMangaChapter,
    downloadableMangaPage: DownloadableMangaPage
  ): SharedFlow<MangaPageLoader.MangaPageLoadingStatus> {
    Logger.d(TAG, "loadImage(${downloadableMangaPage.debugDownloadableMangaPageId()})")
    val mangaPageLoadStatusFlow = mangaPageLoader.loadMangaPage(downloadableMangaPage)

    // Launch a separate coroutine because we may have to go to server to fetch the next manga
    // chapter pages
    viewModelScope.launch {
      val pagesToPreload = formatUrlsOfPagesToPreload(
        viewableMangaChapter = viewableMangaChapter,
        downloadableMangaPage = downloadableMangaPage,
        preloadCount = appSettings.pagesToPreloadCount.get()
      )
      mangaPageLoader.preloadNextPages(pagesToPreload)
    }

    return mangaPageLoadStatusFlow
  }

  fun retryLoadMangaPage(downloadableMangaPage: DownloadableMangaPage) {
    Logger.d(TAG, "retryLoadMangaPage(${downloadableMangaPage.debugDownloadableMangaPageId()})")
    mangaPageLoader.retryLoadMangaPage(downloadableMangaPage)
  }

  fun cancelLoading(downloadableMangaPage: DownloadableMangaPage) {
    if (mangaPageLoader.cancelMangaPageLoading(downloadableMangaPage)) {
      Logger.d(TAG, "cancelLoading(${downloadableMangaPage.debugDownloadableMangaPageId()})")
    }
  }

  fun switchMangaChapter(newMangaChapterId: MangaChapterId) {
    Logger.d(TAG, "switchMangaChapter($newMangaChapterId)")
    viewModelScope.launch { getMangaChapterInternal(mangaChapterId = newMangaChapterId) }
  }

  @Suppress("FoldInitializerAndIfToElvis")
  suspend fun formatUrlsOfPagesToPreload(
    viewableMangaChapter: ViewableMangaChapter,
    downloadableMangaPage: DownloadableMangaPage,
    preloadCount: Int
  ): List<DownloadableMangaPage> {
    val currentChapter = mangaRepository.getMangaChapterById(
      extensionId = viewableMangaChapter.extensionId,
      mangaId = viewableMangaChapter.mangaId,
      mangaChapterId = viewableMangaChapter.mangaChapterId
    )
      .peekError { error ->
        val extensionId = viewableMangaChapter.extensionId.id
        val mangaId = viewableMangaChapter.mangaId.id
        val mangaChapterId = viewableMangaChapter.mangaChapterId.id
        val debugInfo = "(E: ${extensionId}, M: ${mangaId}, MC: ${mangaChapterId})"

        Logger.e(TAG, "Failed to get current manga chapter $debugInfo, error=${error.errorMessageOrClassName()}")
      }
      .valueOrNull()

    if (currentChapter == null) {
      return emptyList()
    }

    val resultPages = mutableListOf<DownloadableMangaPage>()
    val nextChapterId = downloadableMangaPage.nextChapterId

    resultPages += downloadableMangaPage.sliceNextPages(preloadCount)
      .mapNotNull { pageIndex -> currentChapter.getMangaChapterPage(pageIndex) }

    if (resultPages.size == preloadCount || nextChapterId == null) {
      return resultPages
    }

    val nextChapter = mangaRepository.getMangaChapterById(
      extensionId = viewableMangaChapter.extensionId,
      mangaId = viewableMangaChapter.mangaId,
      mangaChapterId = nextChapterId
    )
      .peekError { error ->
        val extensionId = viewableMangaChapter.extensionId.id
        val mangaId = viewableMangaChapter.mangaId.id
        val mangaChapterId = nextChapterId.id
        val debugInfo = "(E: ${extensionId}, M: ${mangaId}, MC: ${mangaChapterId})"

        Logger.e(TAG, "Failed to get next manga chapter $debugInfo, error=${error.errorMessageOrClassName()}")
      }
      .valueOrNull()

    if (nextChapter == null) {
      return emptyList()
    }

    val preloadFromNextChapterCount = preloadCount - resultPages.size

    resultPages += (0 until preloadFromNextChapterCount)
      .mapNotNull { pageIndex -> nextChapter.getMangaChapterPage(pageIndex) }

    return resultPages
  }

  data class ReaderScreenState(
    val currentMangaChapterAsync: AsyncData<ViewableMangaChapter> = AsyncData.NotInitialized(),
  )

  companion object {
    private const val TAG = "ReaderScreenViewModel"
  }

}