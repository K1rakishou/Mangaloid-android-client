package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.AppConstants
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

data class MangaChapter(
  val chapterId: MangaChapterId,
  val mangaChapterIpfsId: MangaChapterIpfsId,
  val title: String,
  val group: String,
  val date: DateTime,
  val pages: Int
) {

  fun formatDate(): String {
    return MANGA_CHAPTER_DATE_FORMATTER.print(date)
  }

  fun chapterCoverUrl(chapterPagesUrl: HttpUrl = AppConstants.chapterPagesEndpoint): HttpUrl {
    return chapterPagesUrl.newBuilder()
      .addEncodedPathSegment(mangaChapterIpfsId.cid)
      // TODO(hardcoded): 3/29/2021: For now it's impossible to know page's image extension and
      //  there are no chapter covers.
      .addEncodedPathSegment("1.jpg")
      .build()
  }

  fun mangaChapterPageUrl(
    mangaPage: Int,
    pageExtension: String = AppConstants.preferredPageImageExtension
  ): MangaPageUrl {
    return MangaPageUrl("https://ipfs.io/ipfs/${mangaChapterIpfsId.cid}/${mangaPage}.$pageExtension".toHttpUrl())
  }

  companion object {
    private val MANGA_CHAPTER_DATE_FORMATTER = DateTimeFormat.fullDate()
  }

}

@JsonClass(generateAdapter = true)
data class MangaChapterRemote(
  @Json(name = "no") val no: Int,
  @Json(name = "cid") val cid: String?,
  @Json(name = "title") val title: String,
  @Json(name = "group") val group: String,
  @Json(name = "date") val date: String?,
  @Json(name = "pages") val pages: Int
)