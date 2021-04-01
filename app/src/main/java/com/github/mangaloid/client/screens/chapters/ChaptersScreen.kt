package com.github.mangaloid.client.screens.chapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.util.viewModelProviderFactoryOf
import com.google.accompanist.coil.CoilImage

@Composable
fun ChaptersScreen(mangaId: MangaId, onMangaChapterClicked: (MangaId, MangaChapterId) -> Unit) {
  val viewModel = viewModel<ChaptersScreenViewModel>(
    key = "chapters_screen_view_model_${mangaId.id}",
    factory = viewModelProviderFactoryOf { ChaptersScreenViewModel(mangaId = mangaId) }
  )

  val chaptersScreenState by viewModel.stateViewable.collectAsState()
  val currentManga = chaptersScreenState.currentManga

  if (currentManga == null || currentManga.chapters.isEmpty()) {
    ChaptersScreenEmptyContent(mangaId)
  } else {
    ChaptersScreenContent(currentManga, onMangaChapterClicked)
  }
}

@Composable
private fun ChaptersScreenContent(manga: Manga, onMangaChapterClicked: (MangaId, MangaChapterId) -> Unit) {
  Column(modifier = Modifier
    .fillMaxSize()
    .padding(all = 8.dp)) {

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(manga.chapters.size) { index ->
        MangaChapterItem(manga, manga.chapters.get(index), onMangaChapterClicked)
      }
    }
  }
}

@Composable
private fun ChaptersScreenEmptyContent(mangaId: MangaId) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(
      text = "No chapters found for manga with id ${mangaId.id}",
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Composable
private fun MangaChapterItem(
  manga: Manga,
  mangaChapter: MangaChapter,
  onMangaChapterClicked: (MangaId, MangaChapterId) -> Unit
) {
  Row(modifier = Modifier
    .fillMaxWidth()
    .height(128.dp)
    .clickable { onMangaChapterClicked(manga.mangaId, mangaChapter.chapterId) }
  ) {
    CoilImage(
      data = mangaChapter.chapterCoverUrl(),
      contentDescription = null,
      contentScale = ContentScale.Inside,
      modifier = Modifier.aspectRatio(0.7f)
    )

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.fillMaxSize()) {
      Text(
        text = mangaChapter.title,
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
      )

      Text(
        text = "TL Group: ${mangaChapter.group}",
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
      )

      Text(
        text = "Pages: ${mangaChapter.pages}",
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
      )

      Text(
        text = "Date: ${mangaChapter.formatDate()}",
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
      )
    }
  }
}
