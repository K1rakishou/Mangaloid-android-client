package com.github.mangaloid.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.model.MangaChapter
import com.github.mangaloid.client.model.MangaId
import com.github.mangaloid.client.ui.theme.MangaloidclientTheme
import com.github.mangaloid.client.util.viewModelProviderFactoryOf
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState


class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MangaloidclientTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
          ReaderScreen(MangaId(0))
        }
      }
    }
  }
}

@Composable
fun ReaderScreen(mangaId: MangaId) {
  val viewModel = viewModel<ReaderViewModel>(
    key = "reader_screen_${mangaId.id}",
    factory = viewModelProviderFactoryOf { ReaderViewModel(mangaId = mangaId) }
  )
  val viewState by viewModel.state.collectAsState()
  val currentManga = viewState.currentManga

  Surface(Modifier.fillMaxSize()) {
    if (currentManga == null) {
      ReaderScreenEmptyContent(mangaId)
    } else {
      ReaderScreenContent(currentManga.chapters.first())
    }
  }

}

@Composable
fun ReaderScreenEmptyContent(mangaId: MangaId) {
  Text(
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight(),
    text = "No manga was found by id: ${mangaId.id}"
  )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ReaderScreenContent(mangaChapter: MangaChapter) {
  val pagerState = rememberPagerState(pageCount = mangaChapter.pages)

  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize()
  ) { mangaPage ->
    MangaPage(mangaChapter.chapterTitle, mangaPage + 1, mangaChapter.pages)
  }
}

@Composable
fun MangaPage(chapterTitle: String, currentPage: Int, pages: Int) {
  val color = remember { Color.LightGray }

  Surface(modifier = Modifier.fillMaxSize(), color = color) {
    Text(text = "$chapterTitle Page $currentPage/$pages", modifier = Modifier.fillMaxSize())
  }
}
