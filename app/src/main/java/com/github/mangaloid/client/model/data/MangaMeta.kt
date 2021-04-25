package com.github.mangaloid.client.model.data

class MangaMeta(
  var databaseId: Long?,
  val mangaDescriptor: MangaDescriptor,
  var bookmarked: Boolean,
  var lastViewedChapterId: MangaChapterId = MangaChapterId.defaultZeroChapter()
) {
  @Synchronized
  fun hasDatabaseId(): Boolean = databaseId != null && databaseId!! >= 0L

  fun isNotReading(): Boolean = lastViewedChapterId.isZeroChapter()

  fun deepCopy(
    databaseId: Long? = this.databaseId,
    bookmarked: Boolean = this.bookmarked,
    lastViewedChapterId: MangaChapterId = this.lastViewedChapterId
  ): MangaMeta {
    return MangaMeta(
      databaseId = databaseId,
      mangaDescriptor = mangaDescriptor,
      bookmarked = bookmarked,
      lastViewedChapterId = lastViewedChapterId
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MangaMeta

    if (databaseId != other.databaseId) return false
    if (mangaDescriptor != other.mangaDescriptor) return false
    if (bookmarked != other.bookmarked) return false
    if (lastViewedChapterId != other.lastViewedChapterId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = databaseId?.hashCode() ?: 0
    result = 31 * result + mangaDescriptor.hashCode()
    result = 31 * result + bookmarked.hashCode()
    result = 31 * result + lastViewedChapterId.hashCode()
    return result
  }

  override fun toString(): String {
    return "MangaMeta(databaseId=$databaseId, mangaDescriptor=$mangaDescriptor, " +
      "bookmarked=$bookmarked, lastViewedChapterId=$lastViewedChapterId)"
  }

  companion object {
    fun createNew(mangaDescriptor: MangaDescriptor): MangaMeta {
      return MangaMeta(
        databaseId = null,
        mangaDescriptor = mangaDescriptor,
        bookmarked = false,
        lastViewedChapterId = MangaChapterId.defaultZeroChapter()
      )
    }
  }

}