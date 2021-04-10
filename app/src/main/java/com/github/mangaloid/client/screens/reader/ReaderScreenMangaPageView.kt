package com.github.mangaloid.client.screens.reader

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.GestureDetector
import android.view.View
import android.widget.FrameLayout
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.setPreferredBitmapConfig
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.MangaloidCoroutineScope
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.helper.SimpleTapListener
import com.github.mangaloid.client.model.data.ViewableMangaChapter
import com.github.mangaloid.client.model.data.ViewablePage
import com.github.mangaloid.client.ui.widget.LoadingBar
import com.github.mangaloid.client.util.AndroidUtils
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.setVisibilityFast
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ReaderScreenMangaPageView(
  context: Context,
  private val readerScreenViewModel: ReaderScreenViewModel,
  private val onTap: () -> Unit
) : FrameLayout(context), ReaderScreenMangaPageViewContract<ViewablePage.MangaPage> {
  private var viewableMangaChapter: ViewableMangaChapter? = null
  private var viewableMangaPage: ViewablePage.MangaPage? = null

  private val coroutineScope = MangaloidCoroutineScope()
  private val mangaPageImageViewer: SubsamplingScaleImageView
  private val pageLoadingErrorViewContainer: FrameLayout
  private val loadingBar: LoadingBar

  private val gestureDetector = GestureDetector(
    context,
    SimpleTapListener { onTap() }
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

  override fun view(): View {
    return this
  }

  override fun bind(viewableMangaChapter: ViewableMangaChapter, viewablePage: ViewablePage.MangaPage) {
    this.viewableMangaChapter = viewableMangaChapter
    this.viewableMangaPage = viewablePage

    Logger.d(TAG, "bindMangaPage(${viewableMangaPage!!.mangaChapterPage.debugMangaPageId()})")

    coroutineScope.launch {
      readerScreenViewModel.loadImage(
        viewableMangaChapter = viewableMangaChapter,
        mangaChapterPage = viewableMangaPage!!.mangaChapterPage
      )
        .collect { mangaPageLoadingStatus ->
          when (mangaPageLoadingStatus) {
            is MangaPageLoader.MangaPageLoadingStatus.Start -> {
              loadingBar.setVisibilityFast(VISIBLE)
              pageLoadingErrorViewContainer.setVisibilityFast(GONE)
            }
            is MangaPageLoader.MangaPageLoadingStatus.Loading -> {
              pageLoadingErrorViewContainer.setVisibilityFast(GONE)

              if (mangaPageLoadingStatus.progress != null) {
                loadingBar.setVisibilityFast(VISIBLE)
                loadingBar.setProgress(mangaPageLoadingStatus.progress)
              } else {
                loadingBar.setVisibilityFast(GONE)
              }
            }
            is MangaPageLoader.MangaPageLoadingStatus.Success -> {
              onMangaPageLoadSuccess(mangaPageLoadingStatus)
              onMangaPageLoadEnd()
            }
            is MangaPageLoader.MangaPageLoadingStatus.Canceled -> {
              onMangaPageLoadError(CancellationException())
              onMangaPageLoadEnd()
            }
            is MangaPageLoader.MangaPageLoadingStatus.Error -> {
              onMangaPageLoadError(mangaPageLoadingStatus.throwable)
              onMangaPageLoadEnd()
            }
          }
        }
    }
  }

  override fun unbind() {
    Logger.d(TAG, "unbindMangaPage(${viewableMangaPage?.mangaChapterPage?.debugMangaPageId()})")

    viewableMangaPage?.mangaChapterPage?.let { readerScreenViewModel.cancelLoading(it) }
    coroutineScope.cancelChildren()

    this.viewableMangaChapter = null
    this.viewableMangaPage = null
  }

  private fun onMangaPageLoadError(error: Throwable) {
    pageLoadingErrorViewContainer.setVisibilityFast(VISIBLE)
    pageLoadingErrorViewContainer.removeAllViews()

    val pageLoadingErrorView = LoadingErrorView(
      context = context,
      error = error,
      onRetryClicked = {
        viewableMangaPage?.mangaChapterPage
          ?.let { mpUrl -> readerScreenViewModel.retryLoadMangaPage(mpUrl) }
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
    pageLoadingErrorViewContainer.setVisibilityFast(GONE)

    mangaPageImageViewer.setImage(
      ImageSource
        .uri(mangaPageLoadingStatus.mangaPageFile.toUri())
        .tiling(true)
    )
  }

  companion object {
    private const val TAG = "ReaderScreenMangaPageView"
  }

}