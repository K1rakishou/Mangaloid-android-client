package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.AppConstants
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.HttpUrl


data class Manga(
  val mangaId: MangaId,
  val title: String,
  val chapters: List<MangaChapter>
) {

  fun coverUrl(coversEndpoint: HttpUrl = AppConstants.coversEndpoint): HttpUrl {
    return coversEndpoint.newBuilder()
      .addEncodedPathSegment("${mangaId.id}.jpg")
      .build()
  }

  fun toDebugString(): String {
    return buildString {
      append("mangaId: ")
      append(mangaId.id)
      append(", title: ")
      append(title)

      appendLine()

      if (chapters.isNotEmpty()) {
        chapters.forEachIndexed { index, chapter ->
          if (index == chapters.lastIndex) {
            appendLine("Chapter $index: $chapter")
          } else {
            append("Chapter $index: $chapter")
          }
        }
      } else {
        append("No chapters!")
      }
    }
  }

}

@JsonClass(generateAdapter = true)
data class MangaRemote(
  @Json(name = "id") val id: Int,
  @Json(name = "title") val title: String,
  @Json(name = "chapters") val chapters: List<MangaChapterRemote>,
)