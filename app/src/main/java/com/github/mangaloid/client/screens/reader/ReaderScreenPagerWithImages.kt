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
import com.github.mangaloid.client.model.data.ViewableMangaChapter
import com.github.mangaloid.client.model.data.ViewablePage

class ReaderScreenPagerWithImages @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : ConstraintLayout(context, attributeSet, defAttrStyle) {
  private var readerActivityCallbacks: ReaderActivityCallbacks? = null

  private val viewPager: ViewPager
  private val pageIndicator: TextView
  private val rootContainer: ConstraintLayout
  private val closeReaderButton: FrameLayout
  private val readerButtonContainer: CardView

  init {
    inflate(context, R.layout.reader_screen_pager_with_images, this)
    viewPager = findViewById(R.id.view_pager)
    pageIndicator = findViewById(R.id.page_indicator)
    rootContainer = findViewById(R.id.root_container)
    closeReaderButton = findViewById(R.id.close_reader_button)
    readerButtonContainer = findViewById(R.id.reader_buttons_container)

    // TODO: 4/1/2021: This is probably not the best idea but for now it should be enough. In the
    //  future MangaPageLoader should do the page preloading.
    viewPager.offscreenPageLimit = AppConstants.preloadImagesCount

    viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
      override fun onPageSelected(position: Int) {
        val viewableMangaChapter = (viewPager.adapter as? ViewPagerAdapter)?.viewableMangaChapter
          ?: return

        viewableMangaChapter.mangaChapterMeta.lastViewedPageIndex = position
        updatePageIndicator(position + 1, viewableMangaChapter.pagesCount())
      }
    })

    closeReaderButton.setOnClickListener { readerActivityCallbacks?.closeReader() }

    ViewCompat.setOnApplyWindowInsetsListener(readerButtonContainer) { v, insets ->
      v.updateLayoutParams<MarginLayoutParams> {
        topMargin = insets.systemWindowInsets.top
        rightMargin = insets.systemWindowInsets.right + 16.dp.value.toInt()
      }
      return@setOnApplyWindowInsetsListener insets
    }
  }

  fun init(readerActivityCallbacks: ReaderActivityCallbacks) {
    this.readerActivityCallbacks = readerActivityCallbacks
  }

  fun onMangaLoaded(
    lastViewedPageIndex: Int?,
    viewableMangaChapter: ViewableMangaChapter,
    readerScreenViewModel: ReaderScreenViewModel
  ) {
    val initialMangaPageIndex = lastViewedPageIndex
      ?: viewableMangaChapter.firstNonMetaPageIndex()

    updatePageIndicator(initialMangaPageIndex + 1, viewableMangaChapter.pagesCount())

    val viewPagerAdapter = ViewPagerAdapter(viewableMangaChapter) { position, parent ->
      getViewInternal(position, parent, viewableMangaChapter, readerScreenViewModel)
    }

    viewPager.adapter = viewPagerAdapter
    viewPager.currentItem = initialMangaPageIndex
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

        readerScreenPrevMangaChapterView.bind(viewablePage)
        return readerScreenPrevMangaChapterView as ReaderScreenMangaPageViewContract<ViewablePage>
      }
      is ViewablePage.MangaPage -> {
        val readerScreenMangaPageView = ReaderScreenMangaPageView(
          context = parent.context,
          readerScreenViewModel = readerScreenViewModel,
          onTap = { onViewablePageTapped() }
        )

        readerScreenMangaPageView.bind(viewablePage)
        return readerScreenMangaPageView as ReaderScreenMangaPageViewContract<ViewablePage>
      }
      is ViewablePage.NextChapterPage -> {
        val readerScreenNextMangaChapterView = ReaderScreenNextMangaChapterView(
          context = parent.context,
          readerScreenViewModel = readerScreenViewModel,
          onTap = { onViewablePageTapped() }
        )

        readerScreenNextMangaChapterView.bind(viewablePage)
        return readerScreenNextMangaChapterView as ReaderScreenMangaPageViewContract<ViewablePage>
      }
    }
  }

  private fun onViewablePageTapped() {
    readerActivityCallbacks?.toggleFullScreenMode()
  }

  private fun updatePageIndicator(currentPage: Int, totalPages: Int) {
    pageIndicator.text = context.getString(R.string.page_indicator_template, currentPage, totalPages)
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

}