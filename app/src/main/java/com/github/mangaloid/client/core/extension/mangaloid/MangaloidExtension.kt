package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.model.source.remote.MangaloidRemoteSource
import kotlinx.coroutines.sync.withLock

class MangaloidExtension(
  private val mangaloidRemoteSource: MangaloidRemoteSource = DependenciesGraph.mangaloidRemoteSource
) : AbstractMangaExtension() {

  override val mangaExtensionId: ExtensionId
    get() = ExtensionId.Mangaloid

  override suspend fun loadCatalogManga(): ModularResult<List<Manga>> {
    val mangaLoadedFromServerResult = mangaloidRemoteSource.loadManga()
    if (mangaLoadedFromServerResult is ModularResult.Error) {
      return ModularResult.error(mangaLoadedFromServerResult.error)
    }

    val mangaList = (mangaLoadedFromServerResult as ModularResult.Value).value
    if (mangaList.isEmpty()) {
      return ModularResult.value(emptyList())
    }

    mangaList.forEach { manga ->
      mutex.withLock {
        mangaCache.put(manga.mangaId, manga)
      }
    }

    return ModularResult.value(mangaList)
  }


  override suspend fun getMangaChapterByIdFromCache(
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ): MangaChapter? {
    return mutex.withLock {
      mangaCache[mangaId]
        ?.chapters
        ?.firstOrNull { mangaChapter -> mangaChapter.chapterId == mangaChapterId }
    }
  }

  override suspend fun getMangaByIdFromCache(mangaId: MangaId): Manga? {
    return mutex.withLock { mangaCache[mangaId] }
  }

}