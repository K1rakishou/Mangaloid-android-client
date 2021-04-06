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
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel
import com.github.mangaloid.client.ui.widget.toolbar.ToolbarButtonId
import com.github.mangaloid.client.ui.widget.toolbar.ToolbarSearchType
import com.github.mangaloid.client.util.StringSpanUtils
import com.github.mangaloid.client.util.viewModelProviderFactoryOf
import com.google.accompanist.coil.CoilImage

@Composable
fun ChaptersScreen(
  extensionId: ExtensionId,
  mangaId: MangaId,
  toolbarViewModel: MangaloidToolbarViewModel,
  onMangaChapterClicked: (MangaId, MangaChapterId) -> Unit
) {
  val chaptersScreenViewModel: ChaptersScreenViewModel = viewModel(
    key = "chapters_screen_view_model_${extensionId.rawId}_${mangaId.id}",
    factory = viewModelProviderFactoryOf {
      ChaptersScreenViewModel(
        extensionId = extensionId,
        mangaId = mangaId
      )
    }
  )
  val chaptersScreenState by chaptersScreenViewModel.stateViewable.collectAsState()

  val currentManga = chaptersScreenState.currentManga
  val toolbarState by toolbarViewModel.stateViewable.collectAsState()
  val searchQuery = toolbarState.searchInfo?.let { searchInfo ->
    if (searchInfo.toolbarSearchType != ToolbarSearchType.MangaChapterSearch) {
      return@let null
    }

    return@let searchInfo.query
  }

  if (currentManga == null) {
    // Still loading
    return
  }

  if (currentManga.chapters.isEmpty()) {
    ChaptersScreenEmptyContent(mangaId, toolbarViewModel)
    return
  }

  ChaptersScreenContent(
    manga = currentManga,
    searchQuery = searchQuery,
    toolbarViewModel = toolbarViewModel,
    onMangaChapterClicked = onMangaChapterClicked
  )
}

@Composable
private fun ChaptersScreenContent(
  manga: Manga,
  searchQuery: String?,
  toolbarViewModel: MangaloidToolbarViewModel,
  onMangaChapterClicked: (MangaId, MangaChapterId) -> Unit
) {
  if (searchQuery == null) {
    toolbarViewModel.updateToolbar { chaptersScreenToolbar(manga) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(all = 8.dp)
  ) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(manga.chapters.size) { index ->
        MangaChapterItem(
          manga = manga,
          mangaChapter = manga.chapters.get(index),
          searchQuery = searchQuery,
          onMangaChapterClicked = onMangaChapterClicked
        )
      }
    }
  }
}

@Composable
private fun ChaptersScreenEmptyContent(
  mangaId: MangaId,
  toolbarViewModel: MangaloidToolbarViewModel
) {
  toolbarViewModel.updateToolbar {
    titleWithBackButton(
      backButtonId = ToolbarButtonId.BackArrow,
      title = "No manga chapters found"
    )
  }

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
  searchQuery: String?,
  onMangaChapterClicked: (MangaId, MangaChapterId) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(128.dp)
      .padding(4.dp)
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
      val annotatedTitle = StringSpanUtils.annotateString(mangaChapter.title, searchQuery)

      Text(
        text = annotatedTitle,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )

      Text(
        text = mangaChapter.formatGroup(),
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )

      Text(
        text = mangaChapter.formatPages(),
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )

      Text(
        text = mangaChapter.formatDate(),
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )
    }
  }
}