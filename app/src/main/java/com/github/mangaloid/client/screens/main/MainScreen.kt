package com.github.mangaloid.client.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.core.AsyncData
import com.github.mangaloid.client.model.data.local.Manga
import com.google.accompanist.coil.CoilImage

@Composable
fun MainScreen(onMangaClicked: (Manga) -> Unit) {
  val viewModel = viewModel<MainScreenViewModel>()
  val viewState by viewModel.stateViewable.collectAsState()

  when (val initialLoadState = viewState.initialLoadState) {
    is AsyncData.NotInitialized -> {
      // no-op
    }
    is AsyncData.Loading -> MainScreenInitialLoadingProgress()
    is AsyncData.Error -> MainScreenInitialLoadingError(initialLoadState.throwable)
    is AsyncData.Data -> {
      val mainPageMangaList = initialLoadState.data
      if (mainPageMangaList.isEmpty()) {
        MainScreenEmptyContent()
      } else {
        MainScreenContent(mainPageMangaList, onMangaClicked)
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreenContent(mainPageMangaList: List<Manga>, onMangaClicked: (Manga) -> Unit) {
  LazyVerticalGrid(
    cells = GridCells.Fixed(2),
    modifier = Modifier.fillMaxSize()
  ) {
    // TODO: 3/29/2021: add search bar

    items(mainPageMangaList.size) { index ->
      MangaItem(mainPageMangaList[index], onMangaClicked)
    }
  }
}

@Composable
private fun MangaItem(manga: Manga, onMangaClicked: (Manga) -> Unit) {
  Row(modifier = Modifier
    .fillMaxWidth()
    .height(160.dp)
    .padding(4.dp)
    .clickable { onMangaClicked(manga) }) {

    CoilImage(
      data = manga.coverUrl(),
      contentDescription = null,
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.aspectRatio(0.7f)
    )

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
      Text(text = manga.title, overflow = TextOverflow.Ellipsis)

      Spacer(modifier = Modifier.weight(1f))

      Text(text = "Chapters: ${manga.chapters.size}")
    }
  }
}

@Composable
private fun MainScreenEmptyContent() {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(
      text = "Nothing found on the server",
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Composable
private fun MainScreenInitialLoadingError(error: Throwable) {
  // TODO: 3/29/2021: add button to reload manga from the server
  Box(modifier = Modifier.fillMaxSize()) {
    Text(
      text = error.toString(),
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Composable
private fun MainScreenInitialLoadingProgress() {
  Box(modifier = Modifier.fillMaxSize()) {
    CircularProgressIndicator(
      modifier = Modifier
        .align(Alignment.Center)
        .size(42.dp, 42.dp)
    )
  }
}
