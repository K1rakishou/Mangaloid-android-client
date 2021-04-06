package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.settings.enums.SwipeDirection

data class MangaChapterMeta(
  val chapterId: MangaChapterId,
  var lastViewedPageIndex: LastViewedPageIndex? = null
)

data class LastViewedPageIndex(
  // Swipe direction of when this setting was remembered
  val swipeDirection: SwipeDirection,
  val pageIndex: Int
)