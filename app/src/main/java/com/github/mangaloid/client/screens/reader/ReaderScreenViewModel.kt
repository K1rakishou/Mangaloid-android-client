package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
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
        prevChapterId = mangaChapter.prevChapterId,
        currentChapter = mangaChapter,
        nextChapterId = mangaChapter.nextChapterId
      )

      copy(currentMangaChapterResult = ModularResult.Value(viewableMangaChapter))
    }
  }

  fun loadImage(mangaPageUrl: MangaPageUrl): SharedFlow<MangaPageLoader.MangaPageLoadingStatus> {
    Logger.d(TAG, "loadImage($mangaPageUrl)")
    return mangaPageLoader.loadMangaPage(mangaPageUrl)
  }

  fun retryLoadMangaPage(mangaPageUrl: MangaPageUrl) {
    Logger.d(TAG, "retryLoadMangaPage($mangaPageUrl)")
    mangaPageLoader.retryLoadMangaPage(mangaPageUrl)
  }

  fun cancelLoading(mangaPageUrl: MangaPageUrl) {
    Logger.d(TAG, "cancelLoading($mangaPageUrl)")
    mangaPageLoader.cancelMangaPageLoading(mangaPageUrl)
  }

  fun changeMangaChapter(mangaChapterId: MangaChapterId) {
    Logger.d(TAG, "showMangaChapter($mangaChapterId)")
    viewModelScope.launch { getMangaChapterInternal(mangaChapterId = mangaChapterId) }
  }

  data class ReaderScreenState(
    val currentMangaChapterResult: ModularResult<ViewableMangaChapter>? = null,
  )

  companion object {
    private const val TAG = "ReaderScreenViewModel"
  }

}