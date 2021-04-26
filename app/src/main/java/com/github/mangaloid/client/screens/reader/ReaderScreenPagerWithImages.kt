package com.github.mangaloid.client.screens.reader

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
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
import com.github.mangaloid.client.util.CachedInsets
import com.github.mangaloid.client.util.setVisibilityFast

class ReaderScreenPagerWithImages @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : ConstraintLayout(context, attributeSet, defAttrStyle) {
  private var readerActivityCallbacks: ReaderActivityCallbacks? = null
  private lateinit var readerScreenViewModel: ReaderScreenViewModel

  private lateinit var viewPager: ViewPager
  private lateinit var readerToolbarTitle: TextView
  private lateinit var readerToolbarSubtitle: TextView
  private lateinit var rootContainer: ConstraintLayout
  private lateinit var closeReaderButton: FrameLayout
  private lateinit var readerToolbarContainer: ConstraintLayout

  private val cachedInsets = CachedInsets()
  private val asyncDataView = AsyncDataView(context)
  private val appSettings = DependenciesGraph.appSettings
  private var mangaChapterMeta: MangaChapterMeta? = null

  init {
    addView(asyncDataView, AndroidUtils.mpMpLayoutParams)
  }

  fun init(readerActivityCallbacks: ReaderActivityCallbacks, readerScreenViewModel: ReaderScreenViewModel) {
    this.readerActivityCallbacks = readerActivityCallbacks
    this.readerScreenViewModel = readerScreenViewModel
  }

  fun updateToolbarVisibility(isSystemUIHidden: Boolean) {
    if (!::readerToolbarContainer.isInitialized) {
      return
    }

    val visibility = if (isSystemUIHidden) {
      View.GONE
    } else {
      View.VISIBLE
    }

    readerToolbarContainer.setVisibilityFast(visibility)
  }

  suspend fun onMangaLoadProgress() {
    asyncDataView.changeState(AsyncDataView.State.Loading)
    asyncDataView.onTap { onViewablePageTapped() }
    asyncDataView.onErrorButtonClicked(null)
  }

  suspend fun onMangaLoadError(throwable: Throwable, onErrorButtonClicked: (() -> Unit)) {
    asyncDataView.changeState(AsyncDataView.State.Error(throwable))
    asyncDataView.onTap { onViewablePageTapped() }
    asyncDataView.onErrorButtonClicked { onErrorButtonClicked() }
  }

  suspend fun onMangaLoaded(viewableMangaChapter: ViewableMangaChapter) {
    asyncDataView.changeState(AsyncDataView.State.Success(R.layout.reader_screen_pager_with_images))
    asyncDataView.onTap(null)
    asyncDataView.onErrorButtonClicked(null)

    onMangaChapterPagesLoaded(readerScreenViewModel, viewableMangaChapter)

    this.mangaChapterMeta = readerScreenViewModel.getOrCreateMangaChapterMeta(
      viewableMangaChapter.mangaChapterDescriptor
    ).deepCopy()

    val pageIndex = viewableMangaChapter.coercePositionInActualPagesRange(
      mangaChapterMeta!!.lastViewedPageIndex.lastViewedPageIndex
    )

    val viewPagerAdapter = ViewPagerAdapter(viewableMangaChapter) { position, parent ->
      getViewInternal(position, parent, viewableMangaChapter, readerScreenViewModel)
    }

    viewPager.adapter = viewPagerAdapter
    viewPager.currentItem = viewableMangaChapter.pagesCount() - pageIndex - 1
  }

  private fun onMangaChapterPagesLoaded(
    readerScreenViewModel: ReaderScreenViewModel,
    viewableMangaChapter: ViewableMangaChapter
  ) {
    viewPager = findViewById(R.id.view_pager)
    readerToolbarTitle = findViewById(R.id.reader_toolbar_title)
    readerToolbarSubtitle = findViewById(R.id.reader_toolbar_subtitle)
    rootContainer = findViewById(R.id.root_container)
    closeReaderButton = findViewById(R.id.close_reader_button)
    readerToolbarContainer = findViewById(R.id.reader_toolbar_container)

    viewPager.offscreenPageLimit = OFFSCREEN_PAGES_COUNT
    readerToolbarTitle.text = viewableMangaChapter.mangaTitle

    viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
      override fun onPageSelected(position: Int) {
        val mangaChapterDescriptor = mangaChapterMeta?.mangaChapterDescriptor
          ?: return

        val actualPosition = viewableMangaChapter.pagesCount() - position - 1

        val updatedLastViewedPageIndex = LastViewedPageIndex(
          lastViewedPageIndex = viewableMangaChapter.coercePositionInActualPagesRange(actualPosition),
          lastReadPageIndex = viewableMangaChapter.coercePositionInActualPagesRange(actualPosition)
        )

        readerScreenViewModel.updateChapterLastViewedPageIndex(
          mangaChapterDescriptor = mangaChapterDescriptor,
          updatedLastViewedPageIndex = updatedLastViewedPageIndex
        )

        updatePageIndicator(actualPosition, viewableMangaChapter)
      }
    })

    closeReaderButton.setOnClickListener { readerActivityCallbacks?.closeReader() }

    ViewCompat.setOnApplyWindowInsetsListener(readerToolbarContainer) { v, insets ->
      cachedInsets.updateFromInsets(insets.systemWindowInsets)
      updateToolbarPaddingsByInsets()
      return@setOnApplyWindowInsetsListener insets
    }

    updateToolbarPaddingsByInsets()
  }

  private fun updateToolbarPaddingsByInsets() {
    if (!::readerToolbarContainer.isInitialized) {
      return
    }

    readerToolbarContainer.updateLayoutParams<ViewGroup.LayoutParams> {
      height = context.resources.getDimension(R.dimen.reader_toolbar_height).toInt() + cachedInsets.top
    }

    readerToolbarContainer.updatePadding(
      left = cachedInsets.left,
      right = cachedInsets.right,
      top =  cachedInsets.top
    )
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

    readerToolbarSubtitle.text = context.getString(
      R.string.reader_toolbar_subtitle_template,
      viewableMangaChapter.chapterTitle,
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