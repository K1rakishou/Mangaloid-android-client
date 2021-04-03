package com.github.mangaloid.client.screens.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.setPreferredBitmapConfig
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.MangaloidCoroutineScope
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaPageUrl
import com.github.mangaloid.client.ui.widget.LoadingBar
import com.github.mangaloid.client.util.AndroidUtils
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.errorMessageOrClassName
import com.github.mangaloid.client.util.setVisibilityFast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ReaderScreenMangaPageView(
  context: Context,
  private val readerScreenViewModel: ReaderScreenViewModel,
  private val onTap: () -> Unit
) : FrameLayout(context) {
  private var mangaPageUrl: MangaPageUrl? = null

  private val coroutineScope = MangaloidCoroutineScope()
  private val mangaPageImageViewer: SubsamplingScaleImageView
  private val pageLoadingErrorViewContainer: FrameLayout
  private val loadingBar: LoadingBar

  private val gestureDetector = GestureDetector(
    context,
    TapListener { onTap() }
  )

  init {
    setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
    inflate(context, R.layout.reader_screen_manga_page_view, this)

    mangaPageImageViewer = findViewById(R.id.manga_page_image_view)
    mangaPageImageViewer.isZoomEnabled = true

    mangaPageImageViewer.setOnTouchListener { _, event ->
      return@setOnTouchListener gestureDetector.onTouchEvent(event)
    }

    loadingBar = findViewById(R.id.loading_bar)
    pageLoadingErrorViewContainer = findViewById(R.id.page_loading_error_view_container)
  }

  fun bindMangaPage(mangaChapter: MangaChapter, currentMangaPageIndex: Int) {
    this.mangaPageUrl = mangaChapter.mangaChapterPageUrl(currentMangaPageIndex)
    Logger.d(TAG, "bindMangaPage(${mangaPageUrl!!.url})")

    coroutineScope.launch {
      readerScreenViewModel.loadImage(mangaPageUrl!!)
        .collect { mangaPageLoadingStatus ->
          when (mangaPageLoadingStatus) {
            MangaPageLoader.MangaPageLoadingStatus.Start -> {
              loadingBar.setVisibilityFast(VISIBLE)
              pageLoadingErrorViewContainer.setVisibilityFast(GONE)
              Logger.d(TAG, "MangaPageLoader.MangaPageLoadingStatus.Start")
            }
            is MangaPageLoader.MangaPageLoadingStatus.Loading -> {
              Logger.d(TAG, "MangaPageLoader.MangaPageLoadingStatus.Loading, " +
                "progress=${mangaPageLoadingStatus.progress}")

              if (mangaPageLoadingStatus.progress != null) {
                loadingBar.setVisibilityFast(VISIBLE)
                loadingBar.setProgress(mangaPageLoadingStatus.progress)
              } else {
                loadingBar.setVisibilityFast(GONE)
              }
            }
            is MangaPageLoader.MangaPageLoadingStatus.Success -> {
              Logger.d(TAG, "MangaPageLoader.MangaPageLoadingStatus.Success, " +
                "file=${mangaPageLoadingStatus.mangaPageFile.absolutePath}")

              onMangaPageLoadSuccess(mangaPageLoadingStatus)
              onMangaPageLoadEnd()
            }
            MangaPageLoader.MangaPageLoadingStatus.Canceled -> {
              Logger.e(TAG, "MangaPageLoader.MangaPageLoadingStatus.Canceled")
              onMangaPageLoadError(CancellationException())
              onMangaPageLoadEnd()
            }
            is MangaPageLoader.MangaPageLoadingStatus.Error -> {
              val errorMessage = mangaPageLoadingStatus.throwable.errorMessageOrClassName()
              Logger.e(TAG, "MangaPageLoader.MangaPageLoadingStatus.Error ${errorMessage}")
              onMangaPageLoadError(mangaPageLoadingStatus.throwable)
              onMangaPageLoadEnd()
            }
          }
        }
    }
  }

  private fun onMangaPageLoadError(error: Throwable) {
    pageLoadingErrorViewContainer.setVisibilityFast(VISIBLE)
    pageLoadingErrorViewContainer.removeAllViews()

    val pageLoadingErrorView = PageLoadingErrorView(
      context = context,
      error = error,
      onRetryClicked = {
        mangaPageUrl?.let { mpUrl ->
          coroutineScope.launch { readerScreenViewModel.loadImage(mpUrl) }
        }
      }
    )

    pageLoadingErrorViewContainer.addView(
      pageLoadingErrorView,
      AndroidUtils.mpMpLayoutParams
    )
  }

  private fun onMangaPageLoadEnd() {
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

  class TapListener(
    private val onTap: () -> Unit
  ) : GestureDetector.SimpleOnGestureListener() {

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
      onTap()
      return true
    }

  }

  companion object {
    private const val TAG = "ReaderScreenMangaPageView"
  }

}