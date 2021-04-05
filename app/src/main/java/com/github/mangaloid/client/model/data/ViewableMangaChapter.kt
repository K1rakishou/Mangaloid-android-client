package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.util.mutableListWithCap

data class ViewableMangaChapter(
  val mangaId: MangaId,
  val mangaChapterId: MangaChapterId,
  val chapterPages: List<ViewablePage>,
  val mangaChapterMeta: MangaChapterMeta
) {
  fun pagesCount() = chapterPages.size

  fun firstNonMetaPageIndex(): Int {
    val index = chapterPages.indexOfFirst { viewablePage -> viewablePage is ViewablePage.MangaPage }
    check(index >= 0) { "Failed to find a non-meta page" }

    return index
  }

  companion object {
    private const val META_PAGES_COUNT = 2

    fun fromMangaChapter(
      prevChapterId: MangaChapterId?,
      currentChapter: MangaChapter,
      nextChapterId: MangaChapterId?
    ): ViewableMangaChapter {
      return ViewableMangaChapter(
        mangaId = currentChapter.ownerMangaId,
        mangaChapterId = currentChapter.chapterId,
        chapterPages = createViewableChapterPages(prevChapterId, currentChapter, nextChapterId),
        mangaChapterMeta = currentChapter.mangaChapterMeta
      )
    }

    private fun createViewableChapterPages(
      prevChapterId: MangaChapterId?,
      currentChapter: MangaChapter,
      nextChapterId: MangaChapterId?
    ): List<ViewablePage> {
      val resultList = mutableListWithCap<ViewablePage>(currentChapter.pages + META_PAGES_COUNT)

      resultList += ViewablePage.PrevChapterPage(prevChapterId)
      resultList += (0 until currentChapter.pages)
        // Pages start with 1 not zero so we need to use "pageIndex + 1" to get the correct page url
        .map { pageIndex -> ViewablePage.MangaPage(currentChapter.mangaChapterPageUrl(pageIndex + 1)) }
      resultList += ViewablePage.NextChapterPage(nextChapterId)

      return resultList
    }
  }
}

sealed class ViewablePage {
  data class PrevChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
  data class MangaPage(val mangaPageUrl: MangaPageUrl) : ViewablePage()
  data class NextChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
}



