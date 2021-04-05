package com.github.mangaloid.client.model.data

import okhttp3.HttpUrl


data class Manga(
  val mangaId: MangaId,
  val title: String,
  val chapters: List<MangaChapter>,
  val coversUrl: HttpUrl
) {

  fun coverUrl(): HttpUrl {
    return coversUrl.newBuilder()
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