package com.github.mangaloid.client.screens.manga_search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.model.data.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.ui.widget.manga.FullSizeTextWidget
import com.github.mangaloid.client.ui.widget.manga.ErrorTextWidget
import com.github.mangaloid.client.ui.widget.manga.MangaItemListWidget
import com.github.mangaloid.client.ui.widget.manga.CircularProgressIndicatorWidget
import kotlinx.coroutines.flow.collect

@Composable
fun MangaSearchScreen(
  extensionId: ExtensionId,
  searchQuery: String,
  onMangaClicked: (Manga) -> Unit
) {
  val mangaSearchScreenViewModel = viewModel<MangaSearchScreenViewModel>()

  if (searchQuery.length < AppConstants.minSearchQueryLength) {
    return
  }

  val state by produceState<AsyncData<List<Manga>>>(
    initialValue = AsyncData.NotInitialized(),
    key1 = searchQuery,
    producer = {
      mangaSearchScreenViewModel.search(extensionId, searchQuery)
        .collect { mangaListAsync -> value = mangaListAsync }
    })

  when (val asyncData = state) {
    is AsyncData.NotInitialized -> return
    is AsyncData.Loading -> CircularProgressIndicatorWidget()
    is AsyncData.Error -> ErrorTextWidget(asyncData.throwable)
    is AsyncData.Data -> {
      val foundManga = asyncData.data
      if (foundManga.isEmpty()) {
        FullSizeTextWidget(stringResource(R.string.no_manga_found_by_query, searchQuery))
      } else {
        MangaItemListWidget(foundManga, searchQuery, onMangaClicked)
      }
    }
  }
}