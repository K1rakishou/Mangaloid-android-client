package com.github.mangaloid.client.model.data

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class MangaChapter(
  val mangaChapterDescriptor: MangaChapterDescriptor,
  val prevChapterId: MangaChapterId?,
  val nextChapterId: MangaChapterId?,
  val mangaChapterIpfsId: MangaChapterIpfsId,
  val groupId: Int?,
  val title: String,
  val chapterPostfix: String,
  val ordinal: Int?,
  val version: Int,
  val languageId: String,
  val dateAdded: DateTime,
  private val chapterPagesCount: Int,
  private val chapterPageDescriptors: MutableList<MangaChapterPageDescriptor>,
  private val lastChapterPagesUpdate: AtomicLong = AtomicLong(0)
) {
  val extensionId: ExtensionId
    get() = mangaChapterDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaChapterDescriptor.mangaId
  val chapterId: MangaChapterId
    get() = mangaChapterDescriptor.mangaChapterId
  val pageCount: Int
    get() {
      if (chapterPageDescriptors.isEmpty()) {
        return chapterPagesCount
      }

      return chapterPageDescriptors.size
    }

  @Synchronized
  fun replaceChapterPageDescriptors(newChapterPageDescriptors: List<MangaChapterPageDescriptor>) {
    this.chapterPageDescriptors.clear()
    this.chapterPageDescriptors.addAll(newChapterPageDescriptors)

    lastChapterPagesUpdate.set(System.currentTimeMillis())
  }

  @Synchronized
  fun needChapterPagesUpdate(): Boolean {
    if (chapterPageDescriptors.isEmpty()) {
      return true
    }

    return (System.currentTimeMillis() - lastChapterPagesUpdate.get()) > ONE_HOUR
  }

  @Synchronized
  fun getMangaChapterPageDescriptor(pageIndex: Int): MangaChapterPageDescriptor? {
    return chapterPageDescriptors.getOrNull(pageIndex)
  }

  fun formatDate(): String {
    return MANGA_CHAPTER_DATE_FORMATTER.print(dateAdded)
  }

  fun deepCopy(): MangaChapter {
    return MangaChapter(
      mangaChapterDescriptor = mangaChapterDescriptor,
      prevChapterId = prevChapterId,
      nextChapterId = nextChapterId,
      mangaChapterIpfsId = mangaChapterIpfsId,
      groupId = groupId,
      title = title,
      chapterPostfix = chapterPostfix,
      ordinal = ordinal,
      version = version,
      languageId = languageId,
      dateAdded = dateAdded,
      chapterPagesCount = chapterPagesCount,
      chapterPageDescriptors = chapterPageDescriptors,
      lastChapterPagesUpdate = lastChapterPagesUpdate,
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MangaChapter

    if (mangaChapterDescriptor != other.mangaChapterDescriptor) return false
    if (prevChapterId != other.prevChapterId) return false
    if (nextChapterId != other.nextChapterId) return false
    if (mangaChapterIpfsId != other.mangaChapterIpfsId) return false
    if (groupId != other.groupId) return false
    if (title != other.title) return false
    if (chapterPostfix != other.chapterPostfix) return false
    if (ordinal != other.ordinal) return false
    if (version != other.version) return false
    if (languageId != other.languageId) return false
    if (dateAdded != other.dateAdded) return false
    if (chapterPagesCount != other.chapterPagesCount) return false
    if (chapterPageDescriptors != other.chapterPageDescriptors) return false

    return true
  }

  override fun hashCode(): Int {
    var result = mangaChapterDescriptor.hashCode()
    result = 31 * result + (prevChapterId?.hashCode() ?: 0)
    result = 31 * result + (nextChapterId?.hashCode() ?: 0)
    result = 31 * result + mangaChapterIpfsId.hashCode()
    result = 31 * result + (groupId ?: 0)
    result = 31 * result + title.hashCode()
    result = 31 * result + chapterPostfix.hashCode()
    result = 31 * result + (ordinal ?: 0)
    result = 31 * result + version
    result = 31 * result + languageId.hashCode()
    result = 31 * result + dateAdded.hashCode()
    result = 31 * result + chapterPagesCount
    result = 31 * result + chapterPageDescriptors.hashCode()
    return result
  }

  override fun toString(): String {
    return "MangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor, prevChapterId=$prevChapterId, " +
      "nextChapterId=$nextChapterId, mangaChapterIpfsId=$mangaChapterIpfsId, groupId=$groupId, " +
      "title='$title', chapterPostfix='$chapterPostfix', ordinal=$ordinal, version=$version, " +
      "languageId='$languageId', dateAdded=$dateAdded, chapterPagesCount=$chapterPagesCount, " +
      "chapterPageDescriptorsCount=${chapterPageDescriptors.size}, lastChapterPagesUpdate=${lastChapterPagesUpdate.get()})"
  }

  companion object {
    private val MANGA_CHAPTER_DATE_FORMATTER = DateTimeFormat.fullDate()
    private val ONE_HOUR = TimeUnit.HOURS.toMillis(1)
  }

}