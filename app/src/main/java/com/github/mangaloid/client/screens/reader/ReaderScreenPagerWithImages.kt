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
import com.github.mangaloid.client.model.data.MangaChapter

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
  private val closeReaderButtonContainer: CardView

  init {
    inflate(context, R.layout.reader_screen_pager_with_images, this)
    viewPager = findViewById(R.id.view_pager)
    pageIndicator = findViewById(R.id.page_indicator)
    rootContainer = findViewById(R.id.root_container)
    closeReaderButton = findViewById(R.id.close_reader_button)
    closeReaderButtonContainer = findViewById(R.id.close_reader_button_container)

    // TODO: 4/1/2021: This is probably not the best idea but for now it should be enough. In the
    //  future MangaPageLoader should do the page preloading.
    viewPager.offscreenPageLimit = AppConstants.preloadImagesCount

    viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
      override fun onPageSelected(position: Int) {
        val mangaChapter = (viewPager.adapter as? ViewPagerAdapter)?.mangaChapter
          ?: return

        mangaChapter.mangaChapterMeta.lastViewedPageIndex = position
        updatePageIndicator(position + 1, mangaChapter.pages)
      }
    })

    closeReaderButton.setOnClickListener { readerActivityCallbacks?.closeReader() }

    ViewCompat.setOnApplyWindowInsetsListener(pageIndicator) { v, insets ->
      v.updateLayoutParams<MarginLayoutParams> { bottomMargin = insets.systemWindowInsets.bottom }
      return@setOnApplyWindowInsetsListener insets
    }

    ViewCompat.setOnApplyWindowInsetsListener(closeReaderButtonContainer) { v, insets ->
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
    initialMangaPageIndex: Int,
    mangaChapter: MangaChapter,
    readerScreenViewModel: ReaderScreenViewModel
  ) {
    updatePageIndicator(initialMangaPageIndex + 1, mangaChapter.pages)

    val viewPagerAdapter = ViewPagerAdapter(mangaChapter) { position, parent ->
      val readerScreenMangaPageView = ReaderScreenMangaPageView(
        context = parent.context,
        readerScreenViewModel = readerScreenViewModel,
        onTap = { readerActivityCallbacks?.toggleFullScreenMode() }
      )

      val currentMangaPageIndex = position + 1
      readerScreenMangaPageView.bindMangaPage(mangaChapter, currentMangaPageIndex)

      return@ViewPagerAdapter readerScreenMangaPageView
    }

    viewPager.adapter = viewPagerAdapter
    viewPager.currentItem = initialMangaPageIndex
  }

  private fun updatePageIndicator(currentPage: Int, totalPages: Int) {
    pageIndicator.text = context.getString(R.string.page_indicator_template, currentPage, totalPages)
  }

  interface ReaderActivityCallbacks {
    fun toggleFullScreenMode()
    fun closeReader()
  }

  class ViewPagerAdapter(
    val mangaChapter: MangaChapter,
    private val getView: (position: Int, parent: ViewGroup) -> ReaderScreenMangaPageView
  ) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
      val view = getView(position, container)
      container.addView(view)
      return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
      val readerScreenMangaPageView = obj as ReaderScreenMangaPageView
      readerScreenMangaPageView.unbindMangaPage()
      container.removeView(readerScreenMangaPageView)
    }

    override fun getCount(): Int = mangaChapter.pages

    override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj
  }

}