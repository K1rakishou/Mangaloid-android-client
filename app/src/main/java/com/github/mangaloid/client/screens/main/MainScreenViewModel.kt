package com.github.mangaloid.client.screens.main

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.Logger
import kotlinx.coroutines.launch

class MainScreenViewModel(
  private val extensionId: ExtensionId,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModelWithState<MainScreenViewModel.MainScreenState>(MainScreenState()) {

  init {
    viewModelScope.launch {
      updateState { copy(initialLoadState = AsyncData.Loading()) }

      when (val result = mangaRepository.loadMangaFromServer(extensionId)) {
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

  private fun debugDumpLoadedMangaInfo(mangaList: List<Manga>): String {
    if (!AppConstants.isDevBuild()) {
      return "<Not a dev build>"
    }

    return buildString(128) {
      appendLine()

      mangaList.forEach { manga ->
        appendLine("--- Manga ${manga.mangaId.id} START ---")
        append(manga.toDebugString())
        appendLine("--- Manga ${manga.mangaId.id} END ---")
      }

      appendLine()
    }
  }

  data class MainScreenState(
    val initialLoadState: AsyncData<List<Manga>> = AsyncData.NotInitialized()
  )

  companion object {
    private const val TAG = "MainScreenViewModel"
  }

}