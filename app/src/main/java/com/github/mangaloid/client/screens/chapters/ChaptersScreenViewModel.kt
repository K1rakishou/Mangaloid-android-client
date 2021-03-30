package com.github.mangaloid.client.screens.chapters

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.local.Manga
import com.github.mangaloid.client.model.data.local.MangaId
import com.github.mangaloid.client.model.repository.MangaRepository
import kotlinx.coroutines.launch

class ChaptersScreenViewModel(
  private val mangaId: MangaId,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModelWithState<ChaptersScreenViewModel.ChaptersScreenState>(ChaptersScreenState()) {

  init {
    viewModelScope.launch {
      val mangaFromCache = mangaRepository.getMangaByIdFromCache(mangaId)
      updateState { copy(currentManga = mangaFromCache) }
    }
  }

  data class ChaptersScreenState(val currentManga: Manga? = null)
}