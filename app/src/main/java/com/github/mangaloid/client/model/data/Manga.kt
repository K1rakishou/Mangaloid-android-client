package com.github.mangaloid.client.model.data

import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.util.mutableListWithCap
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class Manga(
  val extensionId: ExtensionId,
  val mangaId: MangaId,
  val type: MangaContentType,
  val titles: List<String>,
  val artists: List<String>,
  val authors: List<String>,
  val genres: List<String>,
  val countryOfOriginal: String,
  val publicationStatus: String,
  val malId: Int,
  val anilistId: Int,
  val mangaUpdatesId: Int,
  val coversUrl: HttpUrl
) {
  private val chapters = mutableListWithCap<MangaChapter>(16)
  private val lastChaptersUpdateTime = AtomicLong(0)

  val fullTitlesString by lazy { titles.joinToString() }
  val fullArtistString by lazy { artists.joinToString() }
  val fullAuthorsString by lazy { authors.joinToString() }
  val fullGenresString by lazy { genres.joinToString() }

  @Synchronized
  fun replaceChapters(newChapters: List<MangaChapter>) {
    if (newChapters.isEmpty()) {
      return
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
      ?.copy()
  }

  @Synchronized
  fun getChapterByIndex(index: Int): MangaChapter {
    return chapters.get(index).copy()
  }

  fun needChaptersUpdate(): Boolean {
    return (System.currentTimeMillis() - lastChaptersUpdateTime.get()) > ONE_HOUR
  }

  fun coverThumbnailUrl(): HttpUrl {
    return coversUrl.newBuilder()
      .addEncodedQueryParameter("id", mangaId.id.toString())
      .build()
  }

  fun toDebugString(): String {
    return buildString {
      append("mangaId: ")
      append(mangaId.id)

      titles.firstOrNull()?.let { title ->
        append(", title: ")
        append(title)
      }

      appendLine()

      if (chapters.isNotEmpty()) {
        chapters.forEachIndexed { index, chapter ->
          if (index == chapters.lastIndex) {
            appendLine("Chapter $index: $chapter")
          } else {
            append("Chapter $index: $chapter")
          }
        }
      } else {
        append("No chapters!")
      }
    }
  }

  companion object {
    private val ONE_HOUR = TimeUnit.HOURS.toMillis(1)
  }

}