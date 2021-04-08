package com.github.mangaloid.client.ui.widget.drawer

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.core.extension.MangaExtensionManager
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.ExtensionId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MangaloidDrawerViewModel(
  private val mangaExtensionManager: MangaExtensionManager = DependenciesGraph.mangaExtensionManager
) : ViewModelWithState<MangaloidDrawerViewModel.MangaloidDrawerState>(MangaloidDrawerState()) {
  private val shouldDrawerBeOpened = MutableStateFlow(false)

  init {
    viewModelScope.launch {
      updateState { copy(extensionsAsync = AsyncData.Loading()) }
      val allExtensions = mangaExtensionManager.preloadAllExtensions()

      updateState {
        copy(
          // TODO: 4/6/2021 persist this
          currentExtension = allExtensions.first(),
          extensionsAsync = AsyncData.Data(allExtensions)
        )
      }
    }
  }

  fun listenForDrawerOpenCloseState(): StateFlow<Boolean> = shouldDrawerBeOpened.asStateFlow()

  fun resetDrawerOpenCloseState() {
    shouldDrawerBeOpened.value = false
  }

  fun openDrawer() {
    shouldDrawerBeOpened.value = true
  }

  fun selectExtension(extensionId: ExtensionId) {
    viewModelScope.launch {
      val selectedExtension = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
      updateState { copy(currentExtension = selectedExtension) }
    }
  }

  data class MangaloidDrawerState(
    val currentExtension: AbstractMangaExtension? = null,
    val extensionsAsync: AsyncData<List<AbstractMangaExtension>> = AsyncData.NotInitialized()
  )

}