package com.github.mangaloid.client.screens.reader

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.github.mangaloid.client.model.data.MangaChapter

class ReaderScreenPagerWithImages @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : FrameLayout(context, attributeSet, defAttrStyle) {
  private var viewPager: ViewPager

  init {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )

    viewPager = ViewPager(context)

    addView(viewPager, ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    ))

    viewPager.offscreenPageLimit = 1
  }

  fun onMangaLoaded(mangaChapter: MangaChapter, readerScreenViewModel: ReaderScreenViewModel) {
    val viewPagerAdapter = ViewPagerAdapter(mangaChapter) { position, parent ->
      val readerScreenMangaPageView = ReaderScreenMangaPageView(
        parent.context,
        readerScreenViewModel
      )

      val mangaPageIndex = position + 1
      readerScreenMangaPageView.bindMangaPage(mangaChapter.mangaChapterPageUrl(mangaPageIndex))

      return@ViewPagerAdapter readerScreenMangaPageView
    }

    viewPager.adapter = viewPagerAdapter
    viewPager.currentItem = 0
  }

  class ViewPagerAdapter(
    private val mangaChapter: MangaChapter,
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