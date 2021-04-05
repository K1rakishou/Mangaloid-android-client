package com.github.mangaloid.client.screens.reader

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.github.mangaloid.client.R
import com.github.mangaloid.client.helper.SimpleTapListener
import com.github.mangaloid.client.model.data.ViewablePage
import com.github.mangaloid.client.util.setVisibilityFast

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ReaderScreenPrevMangaChapterView(
  context: Context,
  private val readerScreenViewModel: ReaderScreenViewModel,
  private val onTap: () -> Unit
) : FrameLayout(context), ReaderScreenMangaPageViewContract<ViewablePage.PrevChapterPage> {
  private val prevChapterText: TextView
  private val loadPrevChapterButton: AppCompatButton

  private val gestureDetector = GestureDetector(
    context,
    SimpleTapListener { onTap() }
  )

  init {
    inflate(context, R.layout.reader_screen_prev_manga_chapter_view, this)
    prevChapterText = findViewById(R.id.prev_chapter_text)
    loadPrevChapterButton = findViewById(R.id.load_prev_chapter)

    setOnTouchListener { v, event -> gestureDetector.onTouchEvent(event) }
  }

  override fun view(): View {
    return this
  }

  override fun bind(viewablePage: ViewablePage.PrevChapterPage) {
    if (viewablePage.mangaChapterId == null) {
      prevChapterText.text = context.getString(R.string.no_prev_chapter)
      loadPrevChapterButton.setVisibilityFast(View.GONE)
      return
    }

    prevChapterText.text = context.getString(R.string.prev_chapter, viewablePage.mangaChapterId.id)

    loadPrevChapterButton.setVisibilityFast(View.VISIBLE)
    loadPrevChapterButton.setOnClickListener {
      readerScreenViewModel.changeMangaChapter(viewablePage.mangaChapterId)
    }
  }

  override fun unbind() {
    loadPrevChapterButton.setOnClickListener(null)
  }
}