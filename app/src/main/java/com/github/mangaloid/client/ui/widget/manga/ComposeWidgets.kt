package com.github.mangaloid.client.ui.widget.manga

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.mangaloid.client.R
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.util.StringSpanUtils
import com.google.accompanist.coil.CoilImage
import dev.chrisbanes.accompanist.insets.imePadding

@Composable
fun CircularProgressIndicatorWidget() {
  Box(modifier = Modifier.fillMaxSize().imePadding()) {
    CircularProgressIndicator(
      modifier = Modifier
        .align(Alignment.Center)
        .size(42.dp, 42.dp)
    )
  }
}

@Composable
fun ErrorTextWidget(error: Throwable) {
  Box(modifier = Modifier
    .fillMaxSize()
    .imePadding()
    .padding(8.dp)) {
    Text(
      text = error.toString(),
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Composable
fun FullSizeTextWidget(text: String) {
  Box(modifier = Modifier
    .fillMaxSize()
    .imePadding()
    .padding(8.dp)) {
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
  searchQuery: String?,
  onMangaClicked: (Manga) -> Unit
) {
  LazyVerticalGrid(
    cells = GridCells.Adaptive(192.dp),
    modifier = Modifier
      .fillMaxSize()
      .imePadding()
  ) {
    items(mainPageMangaList.size) { index ->
      MangaItemWidget(mainPageMangaList[index], searchQuery, onMangaClicked)
    }
  }
}

@Composable
fun MangaItemWidget(manga: Manga, searchQuery: String?, onMangaClicked: (Manga) -> Unit) {
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
      val annotatedTitle = remember(key1 = searchQuery) {
        StringSpanUtils.annotateString(manga.fullTitlesString, searchQuery)
      }

      Text(
        text = annotatedTitle,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.weight(1f))

      // We may not know how many chapters a manga has (for example, when searching for manga)
      if (manga.chaptersCount() > 0) {
        Text(
          text = stringResource(R.string.manga_chapters, manga.chaptersCount()),
          fontSize = 14.sp
        )
      }
    }
  }
}