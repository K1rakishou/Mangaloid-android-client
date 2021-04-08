package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.page_loader.DownloadableMangaPage
import com.github.mangaloid.client.core.settings.enums.SwipeDirection
import com.github.mangaloid.client.util.mutableListWithCap

data class ViewableMangaChapter(
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val chapterPages: List<ViewablePage>,
  val mangaChapterMeta: MangaChapterMeta,
  val readerSwipeDirection: SwipeDirection
) {
  fun pagesCount() = chapterPages.size
  fun pagesCountForPageCounterUi() = pagesCount() - 1

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

  fun firstNonMetaPageIndex(): Int {
    return when (readerSwipeDirection) {
      SwipeDirection.LeftToRight -> {
        val index = chapterPages.indexOfFirst { viewablePage -> viewablePage is ViewablePage.MangaPage }
        check(index >= 0) { "Failed to find a non-meta page" }

        index
      }
      SwipeDirection.RightToLeft -> {
        val index = chapterPages.indexOfLast { viewablePage -> viewablePage is ViewablePage.MangaPage }
        check(index >= 0) { "Failed to find a non-meta page" }

        index
      }
    }
  }

  companion object {
    private const val META_PAGES_COUNT = 2

    fun fromMangaChapter(
      readerSwipeDirection: SwipeDirection,
      prevChapterId: MangaChapterId?,
      currentChapter: MangaChapter,
      nextChapterId: MangaChapterId?
    ): ViewableMangaChapter {
      return ViewableMangaChapter(
        mangaChapterDescriptor = currentChapter.mangaChapterDescriptor,
        chapterPages = createViewableChapterPages(
          readerSwipeDirection = readerSwipeDirection,
          prevChapterId = prevChapterId,
          currentChapter = currentChapter,
          nextChapterId = nextChapterId
        ),
        mangaChapterMeta = currentChapter.mangaChapterMeta,
        readerSwipeDirection = readerSwipeDirection
      )
    }

    private fun createViewableChapterPages(
      readerSwipeDirection: SwipeDirection,
      prevChapterId: MangaChapterId?,
      currentChapter: MangaChapter,
      nextChapterId: MangaChapterId?
    ): List<ViewablePage> {
      val resultList = mutableListWithCap<ViewablePage>(currentChapter.pageCount + META_PAGES_COUNT)

      when (readerSwipeDirection) {
        SwipeDirection.LeftToRight -> {
          resultList += ViewablePage.PrevChapterPage(prevChapterId)

          currentChapter.iterateMangaChapterPages(readerSwipeDirection) { pageIndex, downloadableMangaPage ->
            resultList += ViewablePage.MangaPage(pageIndex, downloadableMangaPage)
          }

          resultList += ViewablePage.NextChapterPage(nextChapterId)
        }
        SwipeDirection.RightToLeft -> {
          resultList += ViewablePage.NextChapterPage(nextChapterId)

          currentChapter.iterateMangaChapterPages(readerSwipeDirection) { pageIndex, downloadableMangaPage ->
            resultList += ViewablePage.MangaPage(pageIndex, downloadableMangaPage)
          }

          resultList += ViewablePage.PrevChapterPage(prevChapterId)
        }
      }

      return resultList
    }
  }
}

sealed class ViewablePage {
  data class PrevChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
  data class MangaPage(val pageIndex: Int, val downloadableMangaPage: DownloadableMangaPage) : ViewablePage()
  data class NextChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
}



