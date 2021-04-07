package com.github.mangaloid.client.screens.chapters

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.model.repository.MangaRepository
import kotlinx.coroutines.launch

class ChaptersScreenViewModel(
  private val extensionId: ExtensionId,
  private val mangaId: MangaId,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModelWithState<ChaptersScreenViewModel.ChaptersScreenState>(ChaptersScreenState()) {

  init {
    viewModelScope.launch {
      updateState { copy(currentMangaAsync = AsyncData.Loading()) }

      val mangaResult = mangaRepository.getMangaByMangaId(extensionId, mangaId)
      if (mangaResult is ModularResult.Error) {
        updateState { copy(currentMangaAsync = AsyncData.Error(mangaResult.error)) }
        return@launch
      }

      val mangaWithChapters = (mangaResult as ModularResult.Value).value
      if (mangaWithChapters == null) {
        updateState {
          val error = MangaRepository.MangaNotFound(extensionId, mangaId)
          copy(currentMangaAsync = AsyncData.Error(error))
        }

        return@launch
      }

      if (!mangaWithChapters.hasChapters()) {
        updateState {
          val error = MangaRepository.MangaHasNoChapters(extensionId, mangaId)
          copy(currentMangaAsync = AsyncData.Error(error))
        }

        return@launch
      }

      updateState { copy(currentMangaAsync = AsyncData.Data(mangaWithChapters)) }
    }
  }

  data class ChaptersScreenState(val currentMangaAsync: AsyncData<Manga> = AsyncData.NotInitialized())
}