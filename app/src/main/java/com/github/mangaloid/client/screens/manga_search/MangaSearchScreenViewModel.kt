package com.github.mangaloid.client.screens.manga_search

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.coroutine_executor.DebouncingCoroutineExecutor
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.repository.MangaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MangaSearchScreenViewModel(
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModelWithState<MangaSearchScreenViewModel.MangaSearchScreenState>(MangaSearchScreenState()) {
  private val debouncingCoroutineExecutor = DebouncingCoroutineExecutor(viewModelScope)
  private var searchJob: Job? = null

  fun search(extensionId: ExtensionId, searchQuery: String): Flow<AsyncData<List<Manga>>> {
    debouncingCoroutineExecutor.post(250L) {
      searchJob?.cancel()
      searchJob = null

      searchJob = viewModelScope.launch {
        updateState { copy(foundMangaAsync = AsyncData.Loading()) }

        when (val foundMangaResult = mangaRepository.searchForManga(extensionId, searchQuery)) {
          is ModularResult.Error -> updateState {
            copy(foundMangaAsync = AsyncData.Error(foundMangaResult.error))
          }
          is ModularResult.Value -> updateState {
            copy(foundMangaAsync = AsyncData.Data(foundMangaResult.value))
          }
        }
      }
    }

    return stateViewable.map { mangaSearchScreenState -> mangaSearchScreenState.foundMangaAsync }
  }

  override fun onCleared() {
    super.onCleared()

    searchJob?.cancel()
    searchJob = null
  }

  data class MangaSearchScreenState(
    val foundMangaAsync: AsyncData<List<Manga>> = AsyncData.NotInitialized()
  )

}