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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.ui.widget.manga.MangaErrorWidget
import com.github.mangaloid.client.ui.widget.manga.MangaProgressWidget
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel
import com.github.mangaloid.client.ui.widget.toolbar.ToolbarButtonId
import com.github.mangaloid.client.ui.widget.toolbar.ToolbarSearchType
import com.github.mangaloid.client.util.StringSpanUtils
import com.github.mangaloid.client.util.viewModelProviderFactoryOf
import com.google.accompanist.coil.CoilImage

private const val HEADER_INDEX = 0
private const val ADDITIONAL_ITEMS_COUNT = 1 // header

@Composable
fun ChaptersScreen(
  extensionId: ExtensionId,
  mangaId: MangaId,
  toolbarViewModel: MangaloidToolbarViewModel,
  onMangaChapterClicked: (MangaId, MangaChapterId) -> Unit
) {
  val chaptersScreenViewModel: ChaptersScreenViewModel = viewModel(
    key = "chapters_screen_view_model_${extensionId.id}_${mangaId.id}",
    factory = viewModelProviderFactoryOf { ChaptersScreenViewModel(extensionId = extensionId, mangaId = mangaId) }
  )
  val chaptersScreenState by chaptersScreenViewModel.stateViewable.collectAsState()

  val currentManga = when (val currentMangaAsync = chaptersScreenState.currentMangaAsync) {
    is AsyncData.NotInitialized -> {
      return
    }
    is AsyncData.Loading -> {
      MangaProgressWidget()
      return
    }
    is AsyncData.Error -> {
      MangaErrorWidget(error = currentMangaAsync.throwable)
      return
    }
    is AsyncData.Data -> currentMangaAsync.data
  }

  val toolbarState by toolbarViewModel.stateViewable.collectAsState()
  val searchQuery = toolbarState.searchInfo?.let { searchInfo ->
    if (searchInfo.toolbarSearchType != ToolbarSearchType.MangaChapterSearch) {
      return@let null
    }

    return@let searchInfo.query
  }

  if (!currentManga.hasChapters()) {
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

  Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(manga.chaptersCount() + ADDITIONAL_ITEMS_COUNT) { index ->
        when (index) {
          HEADER_INDEX -> {
            ChaptersScreenHeader(manga)
          }
          else -> {
            MangaChapterItem(
              manga = manga,
              mangaChapter = manga.getChapterByIndex(index),
              searchQuery = searchQuery,
              onMangaChapterClicked = onMangaChapterClicked
            )
          }
        }
      }
    }
  }
}

@Composable
fun ChaptersScreenHeader(manga: Manga) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(240.dp)
      .padding(4.dp)
  ) {

    CoilImage(
      data = manga.coverThumbnailUrl(),
      contentDescription = null,
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.aspectRatio(0.5f)
    )

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
      Text(text = manga.fullTitlesString, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
      Text(text = stringResource(R.string.manga_type, manga.mangaContentType.type), fontSize = 14.sp)
      Text(text = stringResource(R.string.manga_country, manga.countryOfOrigin), fontSize = 14.sp)
      Text(text = stringResource(R.string.manga_artist, manga.fullArtistString), overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
      Text(text = stringResource(R.string.manga_author, manga.fullAuthorsString), overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
      Text(text = stringResource(R.string.manga_genre, manga.fullGenresString), overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
      Text(text = stringResource(R.string.manga_publication_status, manga.publicationStatus), fontSize = 14.sp)
      Text(text = stringResource(R.string.manga_chapters, manga.chaptersCount()), fontSize = 14.sp)
    }
  }
}

@Composable
private fun ChaptersScreenEmptyContent(
  mangaId: MangaId,
  toolbarViewModel: MangaloidToolbarViewModel
) {
  val toolbarMessage = stringResource(R.string.no_manga_chapters_found)

  toolbarViewModel.updateToolbar {
    titleWithBackButton(
      backButtonId = ToolbarButtonId.BackArrow,
      title = toolbarMessage
    )
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Text(
      text = stringResource(R.string.no_chapters_found_for_manga, mangaId.id),
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
    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.fillMaxSize()) {
      val annotatedTitle = StringSpanUtils.annotateString(mangaChapter.title, searchQuery)

      Text(
        text = annotatedTitle,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )

      Text(
        text = mangaChapter.formatGroup(),
        fontSize = 14.sp,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )

      Text(
        text = mangaChapter.formatPages(),
        fontSize = 14.sp,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )

      Text(
        text = mangaChapter.formatDate(),
        fontSize = 14.sp,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )
    }
  }
}