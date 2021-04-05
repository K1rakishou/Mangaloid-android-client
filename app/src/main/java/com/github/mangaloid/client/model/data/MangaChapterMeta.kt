package com.github.mangaloid.client.model.data

data class MangaChapterMeta(
  val chapterId: MangaChapterId,
  var lastViewedPageIndex: Int? = null
)