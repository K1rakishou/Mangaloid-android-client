package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.model.data.MangaPageUrl
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.Logger
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ReaderScreenViewModel(
  private val extensionId: ExtensionId,
  private val mangaId: MangaId,
  private val mangaChapterId: MangaChapterId,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository,
  private val mangaPageLoader: MangaPageLoader = DependenciesGraph.mangaPageLoader
) : ViewModelWithState<ReaderScreenViewModel.ReaderScreenState>(ReaderScreenState()) {

  init {
    viewModelScope.launch {
      val mangaChapter = mangaRepository.getMangaChapterByIdFromCache(extensionId, mangaId, mangaChapterId)
      updateState { copy(currentMangaChapter = mangaChapter) }
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

  data class ReaderScreenState(
    val currentMangaChapter: MangaChapter? = null
  )

  companion object {
    private const val TAG = "ReaderScreenViewModel"
  }

}