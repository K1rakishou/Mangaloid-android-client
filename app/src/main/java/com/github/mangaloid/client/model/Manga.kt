package com.github.mangaloid.client.model


data class Manga(
  val mangaId: MangaId,
  val mangaIpfsId: MangaIpfsId,
  val titles: List<String>,
  val description: String,
  val chapters: List<MangaChapter>
)