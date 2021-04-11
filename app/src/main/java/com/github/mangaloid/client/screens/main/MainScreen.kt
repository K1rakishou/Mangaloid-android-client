package com.github.mangaloid.client.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.screens.manga_search.MangaSearchScreen
import com.github.mangaloid.client.ui.widget.drawer.MangaloidDrawerViewModel
import com.github.mangaloid.client.ui.widget.manga.FullSizeTextWidget
import com.github.mangaloid.client.ui.widget.manga.ErrorTextWidget
import com.github.mangaloid.client.ui.widget.manga.MangaItemListWidget
import com.github.mangaloid.client.ui.widget.manga.CircularProgressIndicatorWidget
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
    CircularProgressIndicatorWidget()
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
    is AsyncData.Loading -> CircularProgressIndicatorWidget()
    is AsyncData.Error -> ErrorTextWidget(initialLoadState.throwable)
    is AsyncData.Data -> {
      val mainPageMangaList = initialLoadState.data
      if (mainPageMangaList.isEmpty()) {
        FullSizeTextWidget(stringResource(R.string.manga_library_is_empty))
      } else {
        MangaItemListWidget(
          mainPageMangaList = mainPageMangaList,
          searchQuery = searchInfo?.query,
          onMangaClicked = onMangaClicked
        )
      }
    }
  }
}