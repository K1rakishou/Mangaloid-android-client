package com.github.mangaloid.client.screens.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaDescriptor
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.updateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChaptersScreenViewModel(
  private val mangaDescriptor: MangaDescriptor,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModel() {
  private val _chaptersScreenViewModelState = MutableStateFlow(ChaptersScreenState())

  val chaptersScreenViewModelState: StateFlow<ChaptersScreenState>
    get() = _chaptersScreenViewModelState

  init {
    viewModelScope.launch {
      _chaptersScreenViewModelState.updateState {
        copy(currentMangaAsync = AsyncData.Loading())
      }

      val mangaResult = mangaRepository.getManga(mangaDescriptor)
      if (mangaResult is ModularResult.Error) {
        _chaptersScreenViewModelState.updateState {
          copy(currentMangaAsync = AsyncData.Error(mangaResult.error))
        }
        return@launch
      }

      val mangaWithChapters = (mangaResult as ModularResult.Value).value
      if (mangaWithChapters == null) {
        _chaptersScreenViewModelState.updateState {
          val error = MangaRepository.MangaNotFound(mangaDescriptor)
          copy(currentMangaAsync = AsyncData.Error(error))
        }

        return@launch
      }

      if (!mangaWithChapters.hasChapters()) {
        _chaptersScreenViewModelState.updateState {
          val error = MangaRepository.MangaHasNoChapters(mangaDescriptor)
          copy(currentMangaAsync = AsyncData.Error(error))
        }

        return@launch
      }

      _chaptersScreenViewModelState.updateState {
        copy(currentMangaAsync = AsyncData.Data(mangaWithChapters))
      }
    }
  }

  data class ChaptersScreenState(
    val currentMangaAsync: AsyncData<Manga> = AsyncData.NotInitialized()
  )
}