package com.github.mangaloid.client.model.data

data class MangaChapterMeta(
  var databaseId: Long?,
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val lastViewedPageIndex: LastViewedPageIndex
) {

  fun deepCopy(): MangaChapterMeta {
    return MangaChapterMeta(
      databaseId = databaseId,
      mangaChapterDescriptor = mangaChapterDescriptor,
      lastViewedPageIndex = lastViewedPageIndex.deepCopy()
    )
  }

}

data class LastViewedPageIndex(
  // Used to restore last opened manga page when opening a manga chapter that was already previously
  // opened at least once.
  var lastViewedPageIndex: Int,
  // Used to figure out whether the chapter is fully read or not
  var lastReadPageIndex: Int
) {

  fun deepCopy(): LastViewedPageIndex {
    return LastViewedPageIndex(
      lastViewedPageIndex = lastViewedPageIndex,
      lastReadPageIndex = lastReadPageIndex
    )
  }

  fun update(newLastViewedPageIndex: Int, newLastReadPageIndex: Int) {
    this.lastViewedPageIndex = newLastViewedPageIndex
    this.lastReadPageIndex = Math.max(this.lastReadPageIndex, newLastReadPageIndex)
  }

}