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
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.ui.widget.manga.MangaErrorWidget
import com.github.mangaloid.client.ui.widget.manga.MangaProgressWidget
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel
import com.github.mangaloid.client.ui.widget.toolbar.ToolbarButtonId
import com.github.mangaloid.client.ui.widget.toolbar.ToolbarSearchType
import com.github.mangaloid.client.util.StringSpanUtils
import com.github.mangaloid.client.util.viewModelProviderFactoryOf
import com.google.accompanist.coil.CoilImage

@Composable
fun ChaptersScreen(
  mangaDescriptor: MangaDescriptor,
  toolbarViewModel: MangaloidToolbarViewModel,
  onMangaChapterClicked: (MangaChapterDescriptor) -> Unit
) {
  val chaptersScreenViewModel: ChaptersScreenViewModel = viewModel(
    key = "chapters_screen_view_model_${mangaDescriptor}",
    factory = viewModelProviderFactoryOf { ChaptersScreenViewModel(mangaDescriptor) }
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
    ChaptersScreenEmptyContent(mangaDescriptor, toolbarViewModel)
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
  onMangaChapterClicked: (MangaChapterDescriptor) -> Unit
) {
  if (searchQuery == null) {
    toolbarViewModel.updateToolbar { chaptersScreenToolbar(manga) }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      item("HEADER") {
        ChaptersScreenHeader(manga)
      }

      items(
        count = manga.chaptersCount(),
        key = { index ->
          val mangaChapter = manga.getChapterByIndexReversed(index)!!
          return@items "MANGA_CHAPTER_${mangaChapter.chapterId.id}"
        }
      ) { index ->
        val mangaChapter = manga.getChapterByIndexReversed(index)
          ?: return@items

        MangaChapterItem(
          mangaChapter = mangaChapter,
          searchQuery = searchQuery,
          onMangaChapterClicked = onMangaChapterClicked
        )
      }
    }
  }
}

@Composable
fun ChaptersScreenHeader(manga: Manga) {
  val heightModifier = if (manga.description != null && manga.description.isNotEmpty()) {
    Modifier.height(500.dp)
  } else {
    Modifier.wrapContentHeight()
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .then(heightModifier)
      .padding(4.dp)
  ) {
    Row(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
      CoilImage(
        data = manga.coverThumbnailUrl(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.width(96.dp).height(192.dp)
      )

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
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

    manga.description?.let { description ->
      Text(
        text = description,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ChaptersScreenEmptyContent(
  mangaDescriptor: MangaDescriptor,
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
      text = stringResource(R.string.no_chapters_found_for_manga, mangaDescriptor.mangaId.id),
      modifier = Modifier.align(Alignment.Center)
    )
  }
}

@Composable
private fun MangaChapterItem(
  mangaChapter: MangaChapter,
  searchQuery: String?,
  onMangaChapterClicked: (MangaChapterDescriptor) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(128.dp)
      .padding(4.dp)
      .clickable { onMangaChapterClicked(mangaChapter.mangaChapterDescriptor) }
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

      mangaChapter.groupId?.let { groupId ->
        Text(
          text = stringResource(id = R.string.manga_chapter_group_id, groupId),
          fontSize = 14.sp,
          modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
        )
      }

      Text(
        text = stringResource(id = R.string.manga_chapter_page_count, mangaChapter.pageCount),
        fontSize = 14.sp,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )

      Text(
        text = stringResource(id = R.string.manga_chapter_date_added, mangaChapter.formatDate()),
        fontSize = 14.sp,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
      )
    }
  }
}