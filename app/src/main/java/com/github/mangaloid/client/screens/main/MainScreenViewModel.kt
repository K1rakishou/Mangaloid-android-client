package com.github.mangaloid.client.screens.main

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.cache.MangaUpdates
import com.github.mangaloid.client.model.data.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.Logger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class MainScreenViewModel(
  private val extensionId: ExtensionId,
  private val mangaUpdates: MangaUpdates = DependenciesGraph.mangaUpdates,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModelWithState<MainScreenViewModel.MainScreenState>(MainScreenState()) {

  init {
    viewModelScope.launch {
      mangaUpdates.mangaBookmarkUpdates
        .debounce(250L)
        .collect { reloadLibrary() }
    }

    viewModelScope.launch {
      updateState { copy(initialLoadState = AsyncData.Loading()) }
      reloadLibrary()
    }
  }

  private suspend fun reloadLibrary() {
    when (val result = mangaRepository.loadLibrary(extensionId)) {
      is ModularResult.Error -> {
        Logger.e(TAG, "mangaRepository.loadMangaFromServer() error", result.error)
        updateState { copy(initialLoadState = AsyncData.Error(result.error)) }
      }
      is ModularResult.Value -> {
        updateState { copy(initialLoadState = AsyncData.Data(result.value)) }
      }
    }
  }

  data class MainScreenState(
    val initialLoadState: AsyncData<List<Manga>> = AsyncData.NotInitialized()
  )

  companion object {
    private const val TAG = "MainScreenViewModel"
  }

}