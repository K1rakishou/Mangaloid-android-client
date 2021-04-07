package com.github.mangaloid.client.core.extension

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.page_loader.DownloadableMangaPage
import com.github.mangaloid.client.model.data.*
import okhttp3.HttpUrl

abstract class AbstractMangaExtension {
  abstract val extensionId: ExtensionId
  abstract val name: String
  abstract val icon: HttpUrl

  abstract suspend fun searchForManga(
    title: String,
    author: String,
    artist: String,
    genres: List<String>
  ): ModularResult<List<Manga>>

  abstract suspend fun getManga(mangaId: MangaId): ModularResult<Manga?>

  abstract suspend fun getMangaChapters(mangaId: MangaId): ModularResult<List<MangaChapter>>

  abstract suspend fun getMangaChapterPages(mangaChapter: MangaChapter): ModularResult<List<DownloadableMangaPage>>
}