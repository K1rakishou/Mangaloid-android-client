package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.page_loader.DownloadableMangaPageUrl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

data class MangaChapter(
  val extensionId: ExtensionId,
  val ownerMangaId: MangaId,
  val prevChapterId: MangaChapterId?,
  val chapterId: MangaChapterId,
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

  fun formatDate(): String {
    return "Date: ${MANGA_CHAPTER_DATE_FORMATTER.print(dateAdded)}"
  }

  fun formatGroup(): String {
    return "TL Group: ${groupId}"
  }

  fun formatPages(): String {
    return "Page count: ${pageCount}"
  }

  fun mangaChapterPageUrl(
    mangaPage: Int,
    pageExtension: String = AppConstants.preferredPageImageExtension
  ): DownloadableMangaPageUrl {
    // mangaPage starts from 1, not 0 because that's how pages are enumerated on the backend
    val url = "https://ipfs.io/ipfs/${mangaChapterIpfsId.ipfsId}/${mangaPage}.$pageExtension".toHttpUrl()
    val actualPageIndex = mangaPage - 1

    return DownloadableMangaPageUrl(
      extensionId = extensionId,
      mangaId = ownerMangaId,
      chapterId = chapterId,
      url = url,
      currentPage = actualPageIndex,
      pageCount = pageCount,
      nextChapterId = nextChapterId
    )
  }

  companion object {
    private val MANGA_CHAPTER_DATE_FORMATTER = DateTimeFormat.fullDate()
  }

}