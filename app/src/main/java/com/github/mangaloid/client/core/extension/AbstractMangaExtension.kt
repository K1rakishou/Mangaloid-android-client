package com.github.mangaloid.client.core.extension

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaId
import okhttp3.HttpUrl

abstract class AbstractMangaExtension {
  abstract val extensionId: ExtensionId
  abstract val name: String
  abstract val icon: HttpUrl

  abstract suspend fun searchForManga(title: String, author: String, artist: String, genres: List<String>): ModularResult<List<Manga>>
  abstract suspend fun getMangaByMangaId(mangaId: MangaId): ModularResult<Manga?>
  abstract suspend fun getMangaChaptersByMangaId(mangaId: MangaId): ModularResult<List<MangaChapter>>
}