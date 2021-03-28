package com.github.mangaloid.client.model

data class MangaChapter(
  val mangaIpfsId: MangaIpfsId,
  val chapterTitle: String,
  val pages: Int
)