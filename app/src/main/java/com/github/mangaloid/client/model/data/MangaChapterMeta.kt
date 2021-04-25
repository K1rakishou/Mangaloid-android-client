package com.github.mangaloid.client.model.data

class MangaChapterMeta(
  var databaseId: Long?,
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val lastViewedPageIndex: LastViewedPageIndex
) {

  fun deepCopy(
    databaseId: Long? = this.databaseId,
    lastViewedPageIndex: LastViewedPageIndex = this.lastViewedPageIndex
  ): MangaChapterMeta {
    return MangaChapterMeta(
      databaseId = databaseId,
      mangaChapterDescriptor = mangaChapterDescriptor,
      lastViewedPageIndex = lastViewedPageIndex.deepCopy()
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MangaChapterMeta

    if (databaseId != other.databaseId) return false
    if (mangaChapterDescriptor != other.mangaChapterDescriptor) return false
    if (lastViewedPageIndex != other.lastViewedPageIndex) return false

    return true
  }

  override fun hashCode(): Int {
    var result = databaseId?.hashCode() ?: 0
    result = 31 * result + mangaChapterDescriptor.hashCode()
    result = 31 * result + lastViewedPageIndex.hashCode()
    return result
  }

  override fun toString(): String {
    return "MangaChapterMeta(databaseId=$databaseId, mangaChapterDescriptor=$mangaChapterDescriptor," +
      " lastViewedPageIndex=$lastViewedPageIndex)"
  }

  companion object {
    fun createNew(mangaChapterDescriptor: MangaChapterDescriptor): MangaChapterMeta {
      return MangaChapterMeta(
        databaseId = null,
        mangaChapterDescriptor = mangaChapterDescriptor,
        lastViewedPageIndex = LastViewedPageIndex(
          lastViewedPageIndex = 0,
          lastReadPageIndex = 0
        )
      )
    }
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