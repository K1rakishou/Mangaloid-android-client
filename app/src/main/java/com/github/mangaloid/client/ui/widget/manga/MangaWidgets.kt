package com.github.mangaloid.client.ui.widget.manga

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.mangaloid.client.model.data.Manga
import com.google.accompanist.coil.CoilImage
import dev.chrisbanes.accompanist.insets.imePadding

@Composable
fun MangaProgressWidget() {
  Box(modifier = Modifier.fillMaxSize()) {
    CircularProgressIndicator(
      modifier = Modifier
        .align(Alignment.Center)
        .size(42.dp, 42.dp)
    )
  }
}

@Composable
fun MangaErrorWidget(error: Throwable) {
  Box(modifier = Modifier.fillMaxSize().imePadding().padding(8.dp)) {
    Text(
      text = error.toString(),
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Composable
fun MangaFullSizeTextWidget(text: String) {
  Box(modifier = Modifier.fillMaxSize().imePadding().padding(8.dp)) {
    Text(
      text = text,
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaItemListWidget(
  mainPageMangaList: List<Manga>,
  onMangaClicked: (Manga) -> Unit
) {
  LazyVerticalGrid(
    cells = GridCells.Adaptive(192.dp),
    modifier = Modifier.fillMaxSize().imePadding()
  ) {
    // TODO: 3/29/2021: add search bar

    items(mainPageMangaList.size) { index ->
      MangaItemWidget(mainPageMangaList[index], onMangaClicked)
    }
  }
}

@Composable
fun MangaItemWidget(manga: Manga, onMangaClicked: (Manga) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(160.dp)
      .padding(4.dp)
      .clickable { onMangaClicked(manga) }
  ) {

    CoilImage(
      data = manga.coverThumbnailUrl(),
      contentDescription = null,
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.aspectRatio(0.7f)
    )

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
      Text(text = manga.fullTitlesString, overflow = TextOverflow.Ellipsis)

      Spacer(modifier = Modifier.weight(1f))

      Text(text = "Chapters: ${manga.chaptersCount()}")
    }
  }
}