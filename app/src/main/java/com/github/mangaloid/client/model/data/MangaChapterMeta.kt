package com.github.mangaloid.client.model.data

data class MangaChapterMeta(
  val chapterId: MangaChapterId,
  // Manga page that was viewed the last. Starts with 0.
  var lastViewedPageIndex: Int = 0
)