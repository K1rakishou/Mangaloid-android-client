package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.page_loader.DownloadableMangaPage
import com.github.mangaloid.client.core.settings.enums.SwipeDirection
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class MangaChapter(
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val prevChapterId: MangaChapterId?,
  val nextChapterId: MangaChapterId?,
  val mangaChapterIpfsId: MangaChapterIpfsId,
  val groupId: Int?,
  val title: String,
  val chapterPostfix: String,
  val ordinal: Int?,
  val version: Int,
  val languageId: String,
  val dateAdded: DateTime,
  val pageCount: Int,
  val mangaChapterMeta: MangaChapterMeta
) {
  private val chapterPages = mutableListOf<DownloadableMangaPage>()
  private val lastChapterPagesUpdate = AtomicLong(0)

  val extensionId: ExtensionId
    get() = mangaChapterDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaChapterDescriptor.mangaId
  val chapterId: MangaChapterId
    get() = mangaChapterDescriptor.mangaChapterId

  @Synchronized
  fun replaceChapterPages(newChapterPages: List<DownloadableMangaPage>) {
    this.chapterPages.clear()
    this.chapterPages.addAll(newChapterPages)

    lastChapterPagesUpdate.set(System.currentTimeMillis())
  }

  @Synchronized
  fun iterateMangaChapterPages(
    swipeDirection: SwipeDirection,
    iterator: (Int, DownloadableMangaPage) -> Unit
  ) {
    when (swipeDirection) {
      SwipeDirection.LeftToRight -> {
        (0 until chapterPages.size).forEach { index ->
          val downloadableMangaPage = chapterPages[index]
          iterator(index, downloadableMangaPage)
        }
      }
      SwipeDirection.RightToLeft -> {
        (chapterPages.lastIndex downTo 0).forEach { index ->
          val downloadableMangaPage = chapterPages[index]
          iterator(index, downloadableMangaPage)
        }
      }
    }
  }

  @Synchronized
  fun needChapterPagesUpdate(): Boolean {
    if (chapterPages.isEmpty()) {
      return true
    }

    return (System.currentTimeMillis() - lastChapterPagesUpdate.get()) > ONE_HOUR
  }

  @Synchronized
  fun getMangaChapterPage(pageIndex: Int): DownloadableMangaPage? {
    return chapterPages.getOrNull(pageIndex)
  }

  fun formatDate(): String {
    return MANGA_CHAPTER_DATE_FORMATTER.print(dateAdded)
  }

  companion object {
    private val MANGA_CHAPTER_DATE_FORMATTER = DateTimeFormat.fullDate()
    private val ONE_HOUR = TimeUnit.HOURS.toMillis(1)
  }

}