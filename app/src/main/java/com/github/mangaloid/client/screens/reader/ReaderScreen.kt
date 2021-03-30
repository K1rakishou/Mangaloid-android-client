package com.github.mangaloid.client.screens.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.model.data.local.MangaChapter
import com.github.mangaloid.client.model.data.local.MangaChapterId
import com.github.mangaloid.client.model.data.local.MangaId
import com.github.mangaloid.client.util.viewModelProviderFactoryOf
import com.google.accompanist.coil.CoilImage
import com.google.accompanist.imageloading.ImageLoadState
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import okhttp3.HttpUrl.Companion.toHttpUrl

@Composable
fun ReaderScreen(
  mangaId: MangaId,
  mangaChapterId: MangaChapterId,
  toggleFullScreenModeFunc: () -> Unit = { }
) {
  val viewModel = viewModel<ReaderScreenViewModel>(
    key = "reader_screen_${mangaChapterId.id}",
    factory = viewModelProviderFactoryOf { ReaderScreenViewModel(mangaId = mangaId, mangaChapterId = mangaChapterId) }
  )
  val viewState by viewModel.stateViewable.collectAsState()
  val currentMangaChapter = viewState.currentMangaChapter
  val color = remember { Color.Black }

  val gestureDetector = Modifier.pointerInput(Unit) {
    detectTapGestures(onTap = { toggleFullScreenModeFunc() })
  }

  Surface(modifier = Modifier.fillMaxSize().then(gestureDetector), color = color) {
    if (currentMangaChapter == null) {
      ReaderScreenEmptyContent(mangaId)
    } else {
      ReaderScreenContent(currentMangaChapter)
    }
  }

}

@Composable
private fun ReaderScreenEmptyContent(
  mangaId: MangaId
) {
  Text(
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight(),
    text = "No manga was found by id: ${mangaId.id}"
  )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun ReaderScreenContent(
  mangaChapter: MangaChapter
) {
  val pagerState = rememberPagerState(pageCount = mangaChapter.pages)

  HorizontalPager(
    state = pagerState,
    offscreenLimit = AppConstants.pagesToPreload,
    modifier = Modifier.fillMaxSize()
  ) { mangaPage ->
    val currentMangaPage = mangaPage + 1
    val imageUrl = remember {
      "https://ipfs.io/ipfs/${mangaChapter.mangaIpfsId.id}/${currentMangaPage}.jpg".toHttpUrl()
    }

    CoilImage(
      data = imageUrl,
      contentDescription = null,
      loading = { MangePageLoadingContent() },
      error = { error -> MangaPageLoadingErrorContent(error) },
      contentScale = ContentScale.Inside,
      modifier = Modifier.fillMaxSize()
    )
  }
}

@Composable
private fun MangaPageLoadingErrorContent(
  error: ImageLoadState.Error
) {
  val errorText = remember { "Failed to load page! Error=${error.throwable}" }

  Text(
    modifier = Modifier
      .fillMaxSize(),
    text = errorText
  )
}

@Composable
private fun MangePageLoadingContent() {
  Box(modifier = Modifier.fillMaxSize()) {
    CircularProgressIndicator(
      modifier = Modifier
        .align(Alignment.Center)
        .size(42.dp, 42.dp)
    )
  }
}
