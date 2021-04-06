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
import com.github.mangaloid.client.model.data.ViewableMangaChapter
import com.github.mangaloid.client.model.data.ViewablePage
import com.github.mangaloid.client.util.setVisibilityFast

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ReaderScreenNextMangaChapterView(
  context: Context,
  private val readerScreenViewModel: ReaderScreenViewModel,
  private val onTap: () -> Unit
) : FrameLayout(context), ReaderScreenMangaPageViewContract<ViewablePage.NextChapterPage> {
  private val nextChapterText: TextView
  private val loadNextChapterButton: AppCompatButton

  private val gestureDetector = GestureDetector(
    context,
    SimpleTapListener { onTap() }
  )

  init {
    inflate(context, R.layout.reader_screen_next_manga_chapter_view, this)
    nextChapterText = findViewById(R.id.next_chapter_text)
    loadNextChapterButton = findViewById(R.id.load_next_chapter)

    setOnTouchListener { v, event -> gestureDetector.onTouchEvent(event) }
  }

  override fun view(): View {
    return this
  }

  override fun bind(viewableMangaChapter: ViewableMangaChapter, viewablePage: ViewablePage.NextChapterPage) {
    if (viewablePage.mangaChapterId == null) {
      nextChapterText.text = context.getString(R.string.no_next_chapter)
      loadNextChapterButton.setVisibilityFast(View.GONE)
      return
    }

    nextChapterText.text = context.getString(R.string.next_chapter, viewablePage.mangaChapterId.id)

    loadNextChapterButton.setVisibilityFast(View.VISIBLE)
    loadNextChapterButton.setOnClickListener {
      readerScreenViewModel.switchMangaChapter(viewablePage.mangaChapterId)
    }
  }

  override fun unbind() {
    loadNextChapterButton.setOnClickListener(null)
  }
}