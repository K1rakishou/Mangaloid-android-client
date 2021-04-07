package com.github.mangaloid.client.screens.chapters

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
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
      val mangaResult = mangaRepository.getMangaByMangaId(extensionId, mangaId)
      if (mangaResult is ModularResult.Error) {
        updateState {
          copy(currentMangaResult = ModularResult.error(mangaResult.error))
        }
        return@launch
      } else {
        val mangaWithChapters = (mangaResult as ModularResult.Value).value
        if (mangaWithChapters == null) {
          updateState {
            val error = MangaRepository.MangaNotFound(extensionId, mangaId)
            copy(currentMangaResult = ModularResult.error(error))
          }

          return@launch
        }

        if (!mangaWithChapters.hasChapters()) {
          updateState {
            val error = MangaRepository.MangaHasNoChapters(extensionId, mangaId)
            copy(currentMangaResult = ModularResult.error(error))
          }

          return@launch
        }

        updateState { copy(currentMangaResult = ModularResult.value(mangaWithChapters)) }
      }
    }
  }

  data class ChaptersScreenState(val currentMangaResult: ModularResult<Manga>? = null)
}