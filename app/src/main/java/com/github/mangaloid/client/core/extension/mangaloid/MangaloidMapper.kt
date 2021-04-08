package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaContentType
import com.github.mangaloid.client.model.data.MangaId
import okhttp3.HttpUrl

object MangaloidMapper {

  fun mangaRemoteToManga(
    extensionId: ExtensionId,
    mangaRemote: MangaloidMangaRemote,
    mangaChapters: List<MangaChapter>,
    coversUrl: HttpUrl
  ): Manga {
    val mangaId = checkNotNull(MangaId.fromRawValueOrNull(mangaRemote.id)) {
      "Failed to convert mangaRemote.id: ${mangaRemote.id}"
    }

    return Manga(
      extensionId = extensionId,
      mangaId = mangaId,
      mangaContentType = MangaContentType.fromRawValue(mangaRemote.type),
      titles = mangaRemote.titles,
      description = null,
      artists = mangaRemote.artists,
      authors = mangaRemote.authors,
      genres = mangaRemote.genres,
      countryOfOrigin = mangaRemote.countryOfOrigin,
      publicationStatus = mangaRemote.publicationStatus,
      malId = mangaRemote.malId,
      anilistId = mangaRemote.anilistId,
      mangaUpdatesId = mangaRemote.mangaUpdatesId,
      coversUrl = coversUrl
    ).also { manga ->
      if (mangaChapters.isNotEmpty()) {
        manga.replaceChapters(mangaChapters)
      }
    }
  }

}