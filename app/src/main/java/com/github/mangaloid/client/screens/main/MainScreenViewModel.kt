package com.github.mangaloid.client.screens.main

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.AsyncData
import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.local.Manga
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.Logger
import kotlinx.coroutines.launch

class MainScreenViewModel(
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModelWithState<MainScreenViewModel.MainScreenState>(MainScreenState()) {

  init {
    viewModelScope.launch {
      updateState { copy(initialLoadState = AsyncData.Loading()) }

      when (val result = mangaRepository.loadMangaFromServer()) {
        is ModularResult.Error -> {
          Logger.e(TAG, "mangaRepository.loadMangaFromServer() error", result.error)
          updateState { copy(initialLoadState = AsyncData.Error(result.error)) }
        }
        is ModularResult.Value -> {
          updateState { copy(initialLoadState = AsyncData.Data(result.value)) }
        }
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