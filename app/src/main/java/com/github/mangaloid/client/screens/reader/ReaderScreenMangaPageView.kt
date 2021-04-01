package com.github.mangaloid.client.screens.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.widget.FrameLayout
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.setPreferredBitmapConfig
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.MangaloidCoroutineScope
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.model.data.MangaPageUrl
import com.github.mangaloid.client.ui.widget.LoadingBar
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.errorMessageOrClassName
import com.github.mangaloid.client.util.setVisibilityFast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class ReaderScreenMangaPageView(
  context: Context,
  private val readerScreenViewModel: ReaderScreenViewModel
) : FrameLayout(context) {
  private var mangaPageUrl: MangaPageUrl? = null
  private val coroutineScope = MangaloidCoroutineScope()

  private val mangaPageImageViewer: SubsamplingScaleImageView
  private val loadingBar: LoadingBar

  init {
    setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
    inflate(context, R.layout.reader_screen_manga_page_view, this)

    mangaPageImageViewer = findViewById(R.id.manga_page_image_view)
    mangaPageImageViewer.isZoomEnabled = true

    loadingBar = findViewById(R.id.loading_bar)
  }

  fun bindMangaPage(mangaPageUrl: MangaPageUrl) {
    this.mangaPageUrl = mangaPageUrl
    Logger.d(TAG, "bindMangaPage(${mangaPageUrl.url})")

    coroutineScope.launch {
      readerScreenViewModel.loadImage(mangaPageUrl)
        .collect { mangaPageLoadingStatus ->
          when (mangaPageLoadingStatus) {
            MangaPageLoader.MangaPageLoadingStatus.Start -> {
              loadingBar.setVisibilityFast(VISIBLE)
              Logger.d(TAG, "MangaPageLoader.MangaPageLoadingStatus.Start")
            }
            is MangaPageLoader.MangaPageLoadingStatus.Loading -> {
              Logger.d(TAG, "MangaPageLoader.MangaPageLoadingStatus.Loading, " +
                "progress=${mangaPageLoadingStatus.progress}")

              if (mangaPageLoadingStatus.progress != null) {
                loadingBar.setProgress(mangaPageLoadingStatus.progress)
              }
            }
            is MangaPageLoader.MangaPageLoadingStatus.Success -> {
              Logger.d(TAG, "MangaPageLoader.MangaPageLoadingStatus.Success, " +
                "file=${mangaPageLoadingStatus.mangaPageFile.absolutePath}")

              onMangaPageLoadSuccess(mangaPageLoadingStatus)
              onMangaPageLoadSuccessEnd()
            }
            MangaPageLoader.MangaPageLoadingStatus.Canceled -> {
              Logger.e(TAG, "MangaPageLoader.MangaPageLoadingStatus.Canceled")
              onMangaPageLoadSuccessEnd()
            }
            is MangaPageLoader.MangaPageLoadingStatus.Error -> {
              val errorMessage = mangaPageLoadingStatus.throwable.errorMessageOrClassName()
              Logger.e(TAG, "MangaPageLoader.MangaPageLoadingStatus.Error ${errorMessage}")
              onMangaPageLoadSuccessEnd()
            }
          }
        }
    }
  }

  private fun onMangaPageLoadSuccessEnd() {
    loadingBar.setVisibilityFast(GONE)
  }

  private fun onMangaPageLoadSuccess(mangaPageLoadingStatus: MangaPageLoader.MangaPageLoadingStatus.Success) {
    mangaPageImageViewer.setImage(
      ImageSource
        .uri(mangaPageLoadingStatus.mangaPageFile.toUri())
        .tiling(true)
    )
  }

  fun unbindMangaPage() {
    Logger.d(TAG, "unbindMangaPage(${mangaPageUrl?.url})")

    mangaPageUrl?.let { readerScreenViewModel.cancelLoading(it) }
    coroutineScope.cancelChildren()
  }

  companion object {
    private const val TAG = "ReaderScreenMangaPageView"
  }

}