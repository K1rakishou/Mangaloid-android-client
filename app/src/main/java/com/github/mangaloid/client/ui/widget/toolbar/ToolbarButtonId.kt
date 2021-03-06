package com.github.mangaloid.client.ui.widget.toolbar

enum class ToolbarButtonId(val id: Int) {
  NoId(-1),
  BackArrow(0),
  MangaSearch(1),
  CloseSearch(2),
  ClearSearch(3),

  // Manga chapter screen
  MangaChapterSearch(900),
  MangaBookmark(901),
  MangaUnbookmark(902),

  DrawerMenu(1000)
}