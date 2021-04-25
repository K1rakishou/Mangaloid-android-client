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
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.LastViewedPageIndex
import com.github.mangaloid.client.model.data.MangaChapterMeta
import com.github.mangaloid.client.model.data.ViewableMangaChapter
import com.github.mangaloid.client.model.data.ViewablePage
import com.github.mangaloid.client.ui.widget.AsyncDataView
import com.github.mangaloid.client.util.AndroidUtils

class ReaderScreenPagerWithImages @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : ConstraintLayout(context, attributeSet, defAttrStyle) {
  private var readerActivityCallbacks: ReaderActivityCallbacks? = null
  private lateinit var readerScreenViewModel: ReaderScreenViewModel

  private val asyncDataView = AsyncDataView(context)

  private lateinit var viewPager: ViewPager
  private lateinit var pageIndicator: TextView
  private lateinit var rootContainer: ConstraintLayout
  private lateinit var closeReaderButton: FrameLayout
  private lateinit var readerButtonContainer: CardView

  private val appSettings = DependenciesGraph.appSettings
  private var mangaChapterMeta: MangaChapterMeta? = null

  init {
    addView(asyncDataView, AndroidUtils.mpMpLayoutParams)
  }

  fun init(readerActivityCallbacks: ReaderActivityCallbacks, readerScreenViewModel: ReaderScreenViewModel) {
    this.readerActivityCallbacks = readerActivityCallbacks
    this.readerScreenViewModel = readerScreenViewModel
  }

  suspend fun onMangaLoadProgress() {
    asyncDataView.changeState(AsyncDataView.State.Loading)
    asyncDataView.onTap { onViewablePageTapped() }
  }

  suspend fun onMangaLoadError(throwable: Throwable) {
    asyncDataView.changeState(AsyncDataView.State.Error(throwable))
    asyncDataView.onTap { onViewablePageTapped() }
  }

  suspend fun onMangaLoaded(viewableMangaChapter: ViewableMangaChapter) {
    asyncDataView.changeState(AsyncDataView.State.Success(R.layout.reader_screen_pager_with_images))
    asyncDataView.onTap(null)

    onMangaChapterPagesLoaded(readerScreenViewModel)

    this.mangaChapterMeta = readerScreenViewModel.getMangaChapterMeta(viewableMangaChapter.mangaChapterDescriptor)
      ?.deepCopy()
      ?: MangaChapterMeta(
        databaseId = null,
        mangaChapterDescriptor = viewableMangaChapter.mangaChapterDescriptor,
        lastViewedPageIndex = LastViewedPageIndex(
          lastViewedPageIndex = 0,
          lastReadPageIndex = 0
        )
      )

    val pageIndex = viewableMangaChapter.coercePositionInActualPagesRange(
      mangaChapterMeta!!.lastViewedPageIndex.lastViewedPageIndex
    )

    val viewPagerAdapter = ViewPagerAdapter(viewableMangaChapter) { position, parent ->
      getViewInternal(position, parent, viewableMangaChapter, readerScreenViewModel)
    }

    viewPager.adapter = viewPagerAdapter
    viewPager.currentItem = viewableMangaChapter.pagesCount() - pageIndex - 1
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

        val newMangaChapterMeta = mangaChapterMeta?.deepCopy()
          ?: return

        val actualPosition = viewableMangaChapter.pagesCount() - position - 1

        newMangaChapterMeta.lastViewedPageIndex.update(
          newLastViewedPageIndex = viewableMangaChapter.coercePositionInActualPagesRange(actualPosition),
          newLastReadPageIndex = viewableMangaChapter.coercePositionInActualPagesRange(actualPosition)
        )

        readerScreenViewModel.updateMangaChapterMeta(newMangaChapterMeta)
        updatePageIndicator(actualPosition, viewableMangaChapter)
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

  private fun updatePageIndicator(pageIndex: Int, viewableMangaChapter: ViewableMangaChapter) {
    val actualPageIndex = viewableMangaChapter.coercePositionInActualPagesRange(pageIndex)

    pageIndicator.text = context.getString(
      R.string.page_indicator_template,
      actualPageIndex,
      viewableMangaChapter.pagesCountForPageCounterUi()
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