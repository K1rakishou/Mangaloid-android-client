package com.github.mangaloid.client.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.screens.manga_search.MangaSearchScreen
import com.github.mangaloid.client.ui.widget.drawer.MangaloidDrawerViewModel
import com.github.mangaloid.client.ui.widget.manga.MangaFullSizeTextWidget
import com.github.mangaloid.client.ui.widget.manga.MangaErrorWidget
import com.github.mangaloid.client.ui.widget.manga.MangaItemListWidget
import com.github.mangaloid.client.ui.widget.manga.MangaProgressWidget
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel
import com.github.mangaloid.client.ui.widget.toolbar.ToolbarSearchType
import com.github.mangaloid.client.util.viewModelProviderFactoryOf

@Composable
fun MainScreen(
  toolbarViewModel: MangaloidToolbarViewModel,
  drawerViewModel: MangaloidDrawerViewModel,
  onMangaClicked: (Manga) -> Unit
) {
  val drawerState by drawerViewModel.stateViewable.collectAsState()
  val selectedExtension = drawerState.currentExtension

  if (selectedExtension == null) {
    MangaProgressWidget()
    return
  }

  val mainScreenViewModel: MainScreenViewModel = viewModel(
    key = "main_screen_view_model_${selectedExtension.extensionId.id}",
    factory = viewModelProviderFactoryOf { MainScreenViewModel(extensionId = selectedExtension.extensionId) }
  )

  val viewState by mainScreenViewModel.stateViewable.collectAsState()
  val toolbarState by toolbarViewModel.stateViewable.collectAsState()
  val searchInfo = toolbarState.searchInfo

  if (searchInfo != null && searchInfo.toolbarSearchType == ToolbarSearchType.MangaSearch) {
    MangaSearchScreen(
      extensionId = selectedExtension.extensionId,
      searchQuery = searchInfo.query,
      onMangaClicked = onMangaClicked
    )

    return
  }

  toolbarViewModel.updateToolbar { mainScreenToolbar() }

  when (val initialLoadState = viewState.initialLoadState) {
    is AsyncData.NotInitialized -> {
      // no-op
    }
    is AsyncData.Loading -> MangaProgressWidget()
    is AsyncData.Error -> MangaErrorWidget(initialLoadState.throwable)
    is AsyncData.Data -> {
      val mainPageMangaList = initialLoadState.data
      if (mainPageMangaList.isEmpty()) {
        MangaFullSizeTextWidget("Library is empty")
      } else {
        MangaItemListWidget(
          mainPageMangaList = mainPageMangaList,
          onMangaClicked = onMangaClicked
        )
      }
    }
  }
}