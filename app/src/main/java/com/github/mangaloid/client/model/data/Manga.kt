package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.util.mutableListWithCap
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class Manga(
  val mangaDescriptor: MangaDescriptor,
  val mangaContentType: MangaContentType,
  val titles: List<String>,
  val description: String?,
  val artists: List<String>,
  val authors: List<String>,
  val genres: List<String>,
  val countryOfOrigin: String,
  val publicationStatus: String,
  val malId: Int,
  val anilistId: Int,
  val mangaUpdatesId: Int,
  val coversUrl: HttpUrl
) {
  private val chapters = mutableListWithCap<MangaChapter>(16)
  private val lastChaptersUpdateTime = AtomicLong(0)

  val extensionId: ExtensionId
    get() = mangaDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaDescriptor.mangaId

  val fullTitlesString by lazy { titles.joinToString() }
  val fullArtistString by lazy { artists.joinToString() }
  val fullAuthorsString by lazy { authors.joinToString() }
  val fullGenresString by lazy { genres.joinToString() }

  @Synchronized
  fun replaceChapters(newChapters: List<MangaChapter>) {
    if (newChapters.isEmpty()) {
      return
    }

    newChapters.forEach { newChapter ->
      check(newChapter.extensionId == extensionId) { "ExtensionIds differ!" }
      check(newChapter.mangaId == mangaId) { "MangaIds differ!" }
    }

    this.chapters.clear()
    this.chapters.addAll(newChapters)

    lastChaptersUpdateTime.set(System.currentTimeMillis())
  }

  @Synchronized
  fun chaptersCount(): Int = chapters.size

  @Synchronized
  fun hasChapters(): Boolean {
    return chapters.isNotEmpty()
  }

  @Synchronized
  fun getChapterByChapterId(mangaChapterId: MangaChapterId): MangaChapter? {
    return chapters
      .firstOrNull { mangaChapter -> mangaChapter.chapterId == mangaChapterId }
  }

  @Synchronized
  fun getChapterByIndexReversed(index: Int): MangaChapter? {
    return chapters.getOrNull(chapters.lastIndex - index)
  }

  @Synchronized
  fun updateMangaChapter(mangaChapter: MangaChapter) {
    check(extensionId == mangaChapter.extensionId) { "ExtensionIds differ!" }
    check(mangaId == mangaChapter.mangaId) { "MangaIds differ!" }

    val indexOfChapter = chapters
      .indexOfFirst { chapter -> chapter.chapterId == mangaChapter.chapterId }

    if (indexOfChapter < 0) {
      return
    }

    chapters[indexOfChapter] = mangaChapter
  }

  fun needChaptersUpdate(): Boolean {
    return (System.currentTimeMillis() - lastChaptersUpdateTime.get()) > ONE_HOUR
  }

  fun coverThumbnailUrl(): HttpUrl {
    return coversUrl.newBuilder()
      .addEncodedQueryParameter("id", mangaId.id.toString())
      .build()
  }

  companion object {
    private val ONE_HOUR = TimeUnit.HOURS.toMillis(1)
  }

}