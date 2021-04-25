package com.github.mangaloid.client.model.data

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
  val coversUrl: HttpUrl,
  private var chaptersCount: Int,
) {
  private val lastChaptersUpdateTime = AtomicLong(0)

  val extensionId: ExtensionId
    get() = mangaDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaDescriptor.mangaId

  val fullTitlesString by lazy { titles.joinToString() }
  val fullArtistString by lazy { artists.joinToString() }
  val fullAuthorsString by lazy { authors.joinToString() }
  val fullGenresString by lazy { genres.joinToString() }

  fun onChaptersUpdated(newChaptersCount: Int) {
    this.chaptersCount = newChaptersCount
    lastChaptersUpdateTime.set(System.currentTimeMillis())
  }

  fun chaptersCount(): Int = chaptersCount

  fun hasChapters(): Boolean {
    return chaptersCount > 0
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