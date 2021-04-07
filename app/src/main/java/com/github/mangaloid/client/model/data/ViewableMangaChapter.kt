package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.page_loader.DownloadableMangaPageUrl
import com.github.mangaloid.client.core.settings.enums.SwipeDirection
import com.github.mangaloid.client.util.mutableListWithCap

data class ViewableMangaChapter(
  val extensionId: ExtensionId,
  val mangaId: MangaId,
  val mangaChapterId: MangaChapterId,
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
      extensionId: ExtensionId,
      prevChapterId: MangaChapterId?,
      currentChapter: MangaChapter,
      nextChapterId: MangaChapterId?
    ): ViewableMangaChapter {
      return ViewableMangaChapter(
        extensionId = extensionId,
        mangaId = currentChapter.ownerMangaId,
        mangaChapterId = currentChapter.chapterId,
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
          resultList += (0 until currentChapter.pageCount)
            // Pages start with 1 not zero so we need to use "pageIndex + 1" to get the correct page url
            .map { pageIndex -> ViewablePage.MangaPage(pageIndex, currentChapter.mangaChapterPageUrl(pageIndex + 1)) }
          resultList += ViewablePage.NextChapterPage(nextChapterId)
        }
        SwipeDirection.RightToLeft -> {
          resultList += ViewablePage.NextChapterPage(nextChapterId)
          resultList += (currentChapter.pageCount downTo 0)
            // Pages start with 1 not zero so we need to use "pageIndex + 1" to get the correct page url
            .map { pageIndex -> ViewablePage.MangaPage(pageIndex, currentChapter.mangaChapterPageUrl(pageIndex + 1)) }
          resultList += ViewablePage.PrevChapterPage(prevChapterId)
        }
      }

      return resultList
    }
  }
}

sealed class ViewablePage {
  data class PrevChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
  data class MangaPage(val pageIndex: Int, val downloadableMangaPageUrl: DownloadableMangaPageUrl) : ViewablePage()
  data class NextChapterPage(val mangaChapterId: MangaChapterId?) : ViewablePage()
}



