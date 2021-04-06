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
  val title: String,
  val group: String,
  val date: DateTime,
  val pages: Int,
  val mangaChapterMeta: MangaChapterMeta,
  val chapterPagesUrl: HttpUrl
) {

  fun formatDate(): String {
    return "Date: ${MANGA_CHAPTER_DATE_FORMATTER.print(date)}"
  }

  fun formatGroup(): String {
    return "TL Group: ${group}"
  }

  fun formatPages(): String {
    return "Pages: ${pages}"
  }

  fun chapterCoverUrl(pageExtension: String = AppConstants.preferredPageImageExtension): HttpUrl {
    return chapterPagesUrl.newBuilder()
      .addEncodedPathSegment(mangaChapterIpfsId.cid)
      // TODO(hardcoded): 3/29/2021: For now it's impossible to know page's image extension and
      //  there are no chapter covers.
      .addEncodedPathSegment("1.$pageExtension")
      .build()
  }

  fun mangaChapterPageUrl(
    mangaPage: Int,
    pageExtension: String = AppConstants.preferredPageImageExtension
  ): DownloadableMangaPageUrl {
    // mangaPage starts from 1, not 0 because that's how pages are enumerated on the backend
    val url = "https://ipfs.io/ipfs/${mangaChapterIpfsId.cid}/${mangaPage}.$pageExtension".toHttpUrl()
    val actualPageIndex = mangaPage - 1

    return DownloadableMangaPageUrl(
      extensionId = extensionId,
      mangaId = ownerMangaId,
      chapterId = chapterId,
      url = url,
      currentPage = actualPageIndex,
      totalPages = pages,
      nextChapterId = nextChapterId
    )
  }

  companion object {
    private val MANGA_CHAPTER_DATE_FORMATTER = DateTimeFormat.fullDate()
  }

}