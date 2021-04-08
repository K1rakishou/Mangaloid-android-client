package com.github.mangaloid.client.screens.reader

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.core.settings.enums.SwipeDirection
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.LastViewedPageIndex
import com.github.mangaloid.client.model.data.ViewableMangaChapter
import com.github.mangaloid.client.model.data.ViewablePage
import com.github.mangaloid.client.ui.widget.AsyncDataView
import com.github.mangaloid.client.util.AndroidUtils
import com.github.mangaloid.client.util.Logger

class ReaderScreenPagerWithImages @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : ConstraintLayout(context, attributeSet, defAttrStyle) {
  private var readerActivityCallbacks: ReaderActivityCallbacks? = null

  private val asyncDataView = AsyncDataView(context)

  private lateinit var viewPager: ViewPager
  private lateinit var pageIndicator: TextView
  private lateinit var rootContainer: ConstraintLayout
  private lateinit var closeReaderButton: FrameLayout
  private lateinit var readerButtonContainer: CardView

  private val appSettings = DependenciesGraph.appSettings
  private var currentSwipeDirection: SwipeDirection = SwipeDirection.LeftToRight

  init {
    addView(asyncDataView, AndroidUtils.mpMpLayoutParams)
  }

  fun init(readerActivityCallbacks: ReaderActivityCallbacks) {
    this.readerActivityCallbacks = readerActivityCallbacks
  }

  suspend fun onMangaLoadProgress() {
    asyncDataView.changeState(AsyncDataView.State.Loading)
    asyncDataView.onTap { onViewablePageTapped() }
  }

  suspend fun onMangaLoadError(throwable: Throwable) {
    asyncDataView.changeState(AsyncDataView.State.Error(throwable))
    asyncDataView.onTap { onViewablePageTapped() }
  }

  suspend fun onMangaLoaded(
    lastViewedPageIndex: LastViewedPageIndex?,
    viewableMangaChapter: ViewableMangaChapter,
    readerScreenViewModel: ReaderScreenViewModel
  ) {
    asyncDataView.changeState(AsyncDataView.State.Success(R.layout.reader_screen_pager_with_images))
    asyncDataView.onTap(null)

    onMangaChapterPagesLoaded(readerScreenViewModel)

    this.currentSwipeDirection = appSettings.readerSwipeDirection.get()
    val initialMangaPageIndex = calculateInitialMangaPageIndex(lastViewedPageIndex, viewableMangaChapter)

    updatePageIndicator(initialMangaPageIndex, viewableMangaChapter)

    val viewPagerAdapter = ViewPagerAdapter(viewableMangaChapter) { position, parent ->
      getViewInternal(position, parent, viewableMangaChapter, readerScreenViewModel)
    }

    viewPager.adapter = viewPagerAdapter
    viewPager.currentItem = initialMangaPageIndex
  }

  private fun onMangaChapterPagesLoaded(readerScreenViewModel: ReaderScreenViewModel) {
    viewPager = findViewById(R.id.view_pager)
    pageIndicator = findViewById(R.id.page_indicator)
    rootContainer = findViewById(R.id.root_container)
    closeReaderButton = findViewById(R.id.close_reader_button)
    readerButtonContainer = findViewById(R.id.reader_buttons_container)

    viewPager.offscreenPageLimit = OFFSCREEN_PAGES_COUNT

    viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
      override fun onPageSelected(position: Int) {
        val viewableMangaChapter = (viewPager.adapter as? ViewPagerAdapter)?.viewableMangaChapter
          ?: return

        val lastViewedPageIndex = LastViewedPageIndex(
          currentSwipeDirection,
          viewableMangaChapter.coercePositionInActualPagesRange(position)
        )

        viewableMangaChapter.mangaChapterMeta.lastViewedPageIndex = lastViewedPageIndex
        readerScreenViewModel.updateMangaChapterMeta(viewableMangaChapter.mangaChapterMeta)

        updatePageIndicator(position, viewableMangaChapter)
      }
    })

    closeReaderButton.setOnClickListener { readerActivityCallbacks?.closeReader() }

    ViewCompat.setOnApplyWindowInsetsListener(readerButtonContainer) { v, insets ->
      v.updateLayoutParams<MarginLayoutParams> {
        topMargin = insets.systemWindowInsets.top
        rightMargin = insets.systemWindowInsets.right + CLOSE_BUTTON_RIGHT_PADDING
      }
      return@setOnApplyWindowInsetsListener insets
    }
  }

  private fun calculateInitialMangaPageIndex(
    lastViewedPageIndex: LastViewedPageIndex?,
    viewableMangaChapter: ViewableMangaChapter
  ): Int {
    val pageIndex = when {
      lastViewedPageIndex == null -> {
        viewableMangaChapter.firstNonMetaPageIndex()
      }
      lastViewedPageIndex.swipeDirection == currentSwipeDirection -> {
        lastViewedPageIndex.pageIndex
      }
      else -> {
        when (lastViewedPageIndex.swipeDirection) {
          SwipeDirection.LeftToRight -> lastViewedPageIndex.pageIndex
          SwipeDirection.RightToLeft -> viewableMangaChapter.pagesCount() - lastViewedPageIndex.pageIndex
        }
      }
    }

    return viewableMangaChapter.coercePositionInActualPagesRange(pageIndex)
  }

  private fun getViewInternal(
    position: Int,
    parent: ViewGroup,
    viewableMangaChapter: ViewableMangaChapter,
    readerScreenViewModel: ReaderScreenViewModel
  ): ReaderScreenMangaPageViewContract<ViewablePage> {
    when (val viewablePage = viewableMangaChapter.chapterPages[position]) {
      is ViewablePage.PrevChapterPage -> {
        val readerScreenPrevMangaChapterView = ReaderScreenPrevMangaChapterView(
          context = parent.context,
          readerScreenViewModel = readerScreenViewModel,
          onTap = { onViewablePageTapped() }
        )

        readerScreenPrevMangaChapterView.bind(viewableMangaChapter, viewablePage)
        return readerScreenPrevMangaChapterView as ReaderScreenMangaPageViewContract<ViewablePage>
      }
      is ViewablePage.MangaPage -> {
        val readerScreenMangaPageView = ReaderScreenMangaPageView(
          context = parent.context,
          readerScreenViewModel = readerScreenViewModel,
          onTap = { onViewablePageTapped() }
        )

        readerScreenMangaPageView.bind(viewableMangaChapter, viewablePage)
        return readerScreenMangaPageView as ReaderScreenMangaPageViewContract<ViewablePage>
      }
      is ViewablePage.NextChapterPage -> {
        val readerScreenNextMangaChapterView = ReaderScreenNextMangaChapterView(
          context = parent.context,
          readerScreenViewModel = readerScreenViewModel,
          onTap = { onViewablePageTapped() }
        )

        readerScreenNextMangaChapterView.bind(viewableMangaChapter, viewablePage)
        return readerScreenNextMangaChapterView as ReaderScreenMangaPageViewContract<ViewablePage>
      }
    }
  }

  private fun onViewablePageTapped() {
    readerActivityCallbacks?.toggleFullScreenMode()
  }

  private fun updatePageIndicator(actualPosition: Int, viewableMangaChapter: ViewableMangaChapter) {
    val totalPages = viewableMangaChapter.pagesCountForPageCounterUi()

    val pageIndicatorIndex = when (currentSwipeDirection) {
      SwipeDirection.LeftToRight -> actualPosition
      SwipeDirection.RightToLeft -> totalPages - actualPosition
    }

    pageIndicator.text = context.getString(
      R.string.page_indicator_template,
      pageIndicatorIndex,
      totalPages
    )
  }

  interface ReaderActivityCallbacks {
    fun toggleFullScreenMode()
    fun closeReader()
  }

  class ViewPagerAdapter(
    val viewableMangaChapter: ViewableMangaChapter,
    private val getView: (position: Int, parent: ViewGroup) -> ReaderScreenMangaPageViewContract<ViewablePage>
  ) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
      val readerScreenMangaPageViewContract = getView(position, container)
      container.addView(readerScreenMangaPageViewContract.view())
      return readerScreenMangaPageViewContract
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
      val readerScreenMangaPageViewContract = obj as ReaderScreenMangaPageViewContract<ViewablePage>
      readerScreenMangaPageViewContract.unbind()
      container.removeView(readerScreenMangaPageViewContract.view())
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
      return view === (obj as ReaderScreenMangaPageViewContract<ViewablePage>).view()
    }

    override fun getCount(): Int = viewableMangaChapter.pagesCount()
  }

  companion object {
    private const val TAG = "ReaderScreenPagerWithImages"
    private const val OFFSCREEN_PAGES_COUNT = 2
    private val CLOSE_BUTTON_RIGHT_PADDING = 16.dp.value.toInt()
  }

}