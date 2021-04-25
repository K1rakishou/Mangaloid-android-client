package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.mutableListWithCap
import okhttp3.HttpUrl

object MangaloidMapper {

  fun mangaRemoteToManga(
    mangaId: MangaId,
    extensionId: ExtensionId,
    mangaRemote: MangaloidMangaRemote,
    coversUrl: HttpUrl
  ): Manga {
    return Manga(
      mangaDescriptor = MangaDescriptor(extensionId, mangaId),
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
      coversUrl = coversUrl,
      chaptersCount = 0
    )
  }

}