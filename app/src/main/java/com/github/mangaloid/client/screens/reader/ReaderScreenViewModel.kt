package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.coroutine_executor.DebouncingCoroutineExecutor
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.model.data.MangaChapterPage
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.cache.MangaCache
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.errorMessageOrClassName
import com.github.mangaloid.client.util.mutableListWithCap
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ReaderScreenViewModel(
  private val initialMangaChapterDescriptor: MangaChapterDescriptor,
  private val appSettings: AppSettings = DependenciesGraph.appSettings,
  private val mangaCache: MangaCache = DependenciesGraph.mangaCache,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository,
  private val mangaPageLoader: MangaPageLoader = DependenciesGraph.mangaPageLoader
) : ViewModelWithState<ReaderScreenViewModel.ReaderScreenState>(ReaderScreenState()) {
  private val updateMangaChapterExecutor = DebouncingCoroutineExecutor(viewModelScope)

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

      val chapterPages = createViewableChapterPages(
        mangaChapterDescriptor = updatedMangaChapter.mangaChapterDescriptor,
        prevChapterId = updatedMangaChapter.prevChapterId,
        currentChapter = updatedMangaChapter,
        nextChapterId = updatedMangaChapter.nextChapterId
      )

      val viewableMangaChapter = ViewableMangaChapter.fromMangaChapter(
        chapterPages = chapterPages,
        currentChapter = updatedMangaChapter,
      )

      Logger.d(TAG, "getMangaChapterInternal($mangaChapterDescriptor) success. " +
        "Loaded manga chapter ${updatedMangaChapter.chapterId.id} with " +
        "${viewableMangaChapter.pagesCount()} pages, prevChapterId: ${updatedMangaChapter.prevChapterId}, " +
        "nextChapterId: ${updatedMangaChapter.nextChapterId}")

      copy(currentMangaChapterAsync = AsyncData.Data(viewableMangaChapter))
    }
  }

  private suspend fun createViewableChapterPages(
    mangaChapterDescriptor: MangaChapterDescriptor,
    prevChapterId: MangaChapterId?,
    currentChapter: MangaChapter,
    nextChapterId: MangaChapterId?
  ): List<ViewablePage> {
    val resultList = mutableListWithCap<ViewablePage>(currentChapter.pageCount + ViewableMangaChapter.META_PAGES_COUNT)

    resultList += ViewablePage.NextChapterPage(nextChapterId)

    mangaCache.iterateMangaChapterPages(mangaChapterDescriptor) { pageIndex, downloadableMangaPage ->
      resultList += ViewablePage.MangaPage(pageIndex, downloadableMangaPage)
    }

    resultList += ViewablePage.PrevChapterPage(prevChapterId)

    return resultList
  }

  suspend fun loadImage(
    viewableMangaChapter: ViewableMangaChapter,
    mangaChapterPage: MangaChapterPage
  ): SharedFlow<MangaPageLoader.MangaPageLoadingStatus> {
    Logger.d(TAG, "loadImage(${mangaChapterPage.debugMangaPageId()})")
    val mangaPageLoadStatusFlow = mangaPageLoader.loadMangaPage(mangaChapterPage)

    // Launch a separate coroutine because we may have to go to server to fetch the next manga
    // chapter pages
    viewModelScope.launch {
      val pagesToPreload = formatUrlsOfPagesToPreload(
        viewableMangaChapter = viewableMangaChapter,
        mangaChapterPage = mangaChapterPage,
        preloadCount = appSettings.pagesToPreloadCount.get()
      )
      mangaPageLoader.preloadNextPages(pagesToPreload)
    }

    return mangaPageLoadStatusFlow
  }

  fun retryLoadMangaPage(mangaChapterPage: MangaChapterPage) {
    Logger.d(TAG, "retryLoadMangaPage(${mangaChapterPage.debugMangaPageId()})")
    mangaPageLoader.retryLoadMangaPage(mangaChapterPage)
  }

  fun cancelLoading(mangaChapterPage: MangaChapterPage) {
    if (mangaPageLoader.cancelMangaPageLoading(mangaChapterPage)) {
      Logger.d(TAG, "cancelLoading(${mangaChapterPage.debugMangaPageId()})")
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
    mangaChapterPage: MangaChapterPage,
    preloadCount: Int
  ): List<MangaChapterPage> {
    val currentMCD = viewableMangaChapter.mangaChapterDescriptor

    val currentChapter = mangaRepository.getMangaChapter(currentMCD)
      .peekError { error ->
        Logger.e(TAG, "Failed to get current manga chapter ${currentMCD}, error=${error.errorMessageOrClassName()}")
      }
      .valueOrNull()

    if (currentChapter == null) {
      return emptyList()
    }

    val resultPages = mutableListOf<MangaChapterPage>()
    val nextChapterId = mangaChapterPage.nextChapterId

    resultPages += mangaChapterPage.sliceNextPages(preloadCount)
      .mapNotNull { pageIndex -> currentChapter.getMangaChapterPageDescriptor(pageIndex) }
      .mapNotNull { mangaChapterPageDescriptor -> mangaCache.getMangaChapterPage(mangaChapterPageDescriptor) }

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
      .mapNotNull { pageIndex -> nextChapter.getMangaChapterPageDescriptor(pageIndex) }
      .mapNotNull { mangaChapterPageDescriptor -> mangaCache.getMangaChapterPage(mangaChapterPageDescriptor) }

    return resultPages
  }

  fun updateMangaChapterMeta(mangaChapterMeta: MangaChapterMeta) {
    updateMangaChapterExecutor.post(UPDATE_TIMEOUT_MS) {
      mangaRepository.updateMangaChapterMeta(mangaChapterMeta)
        .peekError { error -> Logger.e(TAG, "updateMangaChapterMeta($mangaChapterMeta) error", error) }
        .ignore()
    }
  }

  suspend fun getMangaChapterMeta(mangaChapterDescriptor: MangaChapterDescriptor): MangaChapterMeta? {
    return mangaCache.getMangaChapterMeta(mangaChapterDescriptor)
  }

  data class ReaderScreenState(
    val currentMangaChapterAsync: AsyncData<ViewableMangaChapter> = AsyncData.NotInitialized(),
  )

  companion object {
    private const val TAG = "ReaderScreenViewModel"
    private const val UPDATE_TIMEOUT_MS = 125L
  }

}