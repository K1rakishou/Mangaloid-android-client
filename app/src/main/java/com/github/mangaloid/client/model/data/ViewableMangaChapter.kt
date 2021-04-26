package com.github.mangaloid.client.model.data


data class ViewableMangaChapter(
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val chapterPages: List<ViewablePage>,
  val mangaTitle: String,
  val chapterTitle: String
) {
  fun pagesCount() = chapterPages.size
  fun pagesCountForPageCounterUi() = chapterPages.count { viewablePage -> viewablePage is ViewablePage.MangaPage }

  // [MetaPage][Page][Page][MetaPage]
  //  0         1     2     3
  //
  // input position = 0
  // result position = 1 (Skip 1 meta page)
  //
  // input position = 3
  // result position = 2 (Drop the last meta page)
  fun coercePositionInActualPagesRange(position: Int): Int {
    val metaPagesAtTheBeginningCount = 1
    val metaPagesAtTheEndCount = 1

    return position.coerceIn(metaPagesAtTheBeginningCount, chapterPages.lastIndex - metaPagesAtTheEndCount)
  }

  companion object {
    const val META_PAGES_COUNT = 2

    fun fromMangaChapter(
      currentChapter: MangaChapter,
      chapterPages: List<ViewablePage>,
      mangaTitle: String,
      mangaChapterTitle: String
    ): ViewableMangaChapter {
      return ViewableMangaChapter(
        mangaChapterDescriptor = currentChapter.mangaChapterDescriptor,
        chapterPages = chapterPages,
        mangaTitle = mangaTitle,
        chapterTitle = mangaChapterTitle
      )
    }
  }
}

sealed class ViewablePage {
  data class PrevChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
  data class MangaPage(val pageIndex: Int, val mangaChapterPage: MangaChapterPage) : ViewablePage()
  data class NextChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
}



