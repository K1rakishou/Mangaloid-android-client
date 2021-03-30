package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.local.MangaChapter
import com.github.mangaloid.client.model.data.local.MangaChapterId
import com.github.mangaloid.client.model.data.local.MangaId
import com.github.mangaloid.client.model.repository.MangaRepository
import kotlinx.coroutines.launch

class ReaderScreenViewModel(
  private val mangaId: MangaId,
  private val mangaChapterId: MangaChapterId,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModelWithState<ReaderScreenViewModel.ReaderScreenState>(ReaderScreenState()) {

  init {
    viewModelScope.launch {
      val mangaChapter = mangaRepository.getMangaChapterByIdFromCache(mangaId, mangaChapterId)
      updateState { copy(currentMangaChapter = mangaChapter) }
    }
  }

  data class ReaderScreenState(
    val currentMangaChapter: MangaChapter? = null
  )

}