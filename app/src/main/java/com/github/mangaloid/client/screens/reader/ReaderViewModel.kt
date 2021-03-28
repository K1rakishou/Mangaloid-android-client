package com.github.mangaloid.client.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.di.Graph
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.model.Manga
import com.github.mangaloid.client.model.MangaId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReaderViewModel(
  private val mangaId: MangaId,
  private val mangaRepository: MangaRepository = Graph.mangaRepository
) : ViewModel() {
  private val _state = MutableStateFlow(ReaderState())

  val state: StateFlow<ReaderState>
    get() = _state

  init {
    viewModelScope.launch {
      val manga = mangaRepository.getMangaById(mangaId)
      _state.value = ReaderState(manga)
    }
  }

  data class ReaderState(
    val currentManga: Manga? = null
  )

}