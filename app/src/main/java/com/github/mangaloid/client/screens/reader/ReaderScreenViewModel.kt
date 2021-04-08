package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.coroutine_executor.SerializedCoroutineExecutor
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
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
  private val initialMangaChapterDescriptor: MangaChapterDescriptor,
  private val appSettings: AppSettings = DependenciesGraph.appSettings,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository,
  private val mangaPageLoader: MangaPageLoader = DependenciesGraph.mangaPageLoader
) : ViewModelWithState<ReaderScreenViewModel.ReaderScreenState>(ReaderScreenState()) {
  private val updateMangaChapterExecutor = SerializedCoroutineExecutor(viewModelScope)

  init {
    viewModelScope.launch {
      getMangaChapterInternal(initialMangaChapterDescriptor)
    }
  }

  private suspend fun getMangaChapterInternal(mangaChapterDescriptor: MangaChapterDescriptor) {
    Logger.d(TAG, "getMangaChapterInternal($mangaChapterDescriptor)")
    updateState { copy(currentMangaChapterAsync = AsyncData.Loading()) }

    val mangaChapterResult = mangaRepository.getMangaChapter(mangaChapterDescriptor)

    if (mangaChapterResult is ModularResult.Error) {
      Logger.e(TAG, "getMangaChapterInternal($mangaChapterDescriptor) " +
        "getMangaChapter error", mangaChapterResult.error)

      updateState {
        copy(currentMangaChapterAsync = AsyncData.Error(mangaChapterResult.error))
      }

      return
    }

    val mangaChapter = (mangaChapterResult as ModularResult.Value).value

    val updatedMangaChapterResult = mangaRepository.refreshMangaChapterPagesIfNeeded(
      extensionId = mangaChapterDescriptor.extensionId,
      mangaChapter = mangaChapter
    )

    if (updatedMangaChapterResult is ModularResult.Error) {
      Logger.e(TAG, "getMangaChapterInternal($mangaChapterDescriptor) " +
        "refreshMangaChapterPagesIfNeeded error", updatedMangaChapterResult.error)

      updateState {
        copy(currentMangaChapterAsync = AsyncData.Error(updatedMangaChapterResult.error))
      }

      return
    }

    updateState {
      val updatedMangaChapter = (updatedMangaChapterResult as ModularResult.Value).value

      val viewableMangaChapter = ViewableMangaChapter.fromMangaChapter(
        readerSwipeDirection = appSettings.readerSwipeDirection.get(),
        prevChapterId = updatedMangaChapter.prevChapterId,
        currentChapter = updatedMangaChapter,
        nextChapterId = updatedMangaChapter.nextChapterId
      )

      Logger.d(TAG, "getMangaChapterInternal($mangaChapterDescriptor) success. " +
        "Loaded manga chapter ${updatedMangaChapter.chapterId.id} with " +
        "${viewableMangaChapter.pagesCount()} pages, prevChapterId: ${updatedMangaChapter.prevChapterId}, " +
        "nextChapterId: ${updatedMangaChapter.nextChapterId}")

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
    val newMangaChapterDescriptor = MangaChapterDescriptor(
      mangaDescriptor = initialMangaChapterDescriptor.mangaDescriptor,
      mangaChapterId = newMangaChapterId
    )

    Logger.d(TAG, "switchMangaChapter($newMangaChapterDescriptor)")
    viewModelScope.launch { getMangaChapterInternal(newMangaChapterDescriptor) }
  }

  @Suppress("FoldInitializerAndIfToElvis")
  suspend fun formatUrlsOfPagesToPreload(
    viewableMangaChapter: ViewableMangaChapter,
    downloadableMangaPage: DownloadableMangaPage,
    preloadCount: Int
  ): List<DownloadableMangaPage> {
    val currentMCD = viewableMangaChapter.mangaChapterDescriptor

    val currentChapter = mangaRepository.getMangaChapter(currentMCD)
      .peekError { error ->
        Logger.e(TAG, "Failed to get current manga chapter ${currentMCD}, error=${error.errorMessageOrClassName()}")
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

    val nextMCD = MangaChapterDescriptor(
      mangaDescriptor = currentMCD.mangaDescriptor,
      mangaChapterId = nextChapterId
    )

    val nextChapter = mangaRepository.getMangaChapter(nextMCD)
      .peekError { error ->
        Logger.e(TAG, "Failed to get next manga chapter $nextMCD, error=${error.errorMessageOrClassName()}")
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

  fun updateMangaChapterMeta(mangaChapterMeta: MangaChapterMeta) {
    updateMangaChapterExecutor.post { mangaRepository.updateMangaChapterMeta(mangaChapterMeta) }
  }

  data class ReaderScreenState(
    val currentMangaChapterAsync: AsyncData<ViewableMangaChapter> = AsyncData.NotInitialized(),
  )

  companion object {
    private const val TAG = "ReaderScreenViewModel"
  }

}