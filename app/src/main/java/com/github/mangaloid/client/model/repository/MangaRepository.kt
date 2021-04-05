package com.github.mangaloid.client.model.repository

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.core.extension.MangaExtensionManager
import com.github.mangaloid.client.model.cache.MangaCache
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId

class MangaRepository(
  private val mangaCache: MangaCache,
  private val mangaExtensionManager: MangaExtensionManager
) : BaseRepository() {

  suspend fun loadMangaFromServer(extensionId: ExtensionId): ModularResult<List<Manga>> {
    return repoAsync {
      val mangaLoadedFromServerResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .loadCatalogManga()

      if (mangaLoadedFromServerResult is ModularResult.Error) {
        return@repoAsync ModularResult.error(mangaLoadedFromServerResult.error)
      }

      val mangaList = (mangaLoadedFromServerResult as ModularResult.Value).value
      if (mangaList.isEmpty()) {
        return@repoAsync ModularResult.value(emptyList())
      }

      mangaCache.putMany(extensionId, mangaList)

      return@repoAsync ModularResult.value(mangaList)
    }
  }

  suspend fun getMangaByIdFromCache(extensionId: ExtensionId, mangaId: MangaId): Manga? {
    return repoAsync {
      return@repoAsync mangaCache.getManga(extensionId, mangaId)
    }
  }

  suspend fun getMangaChapterByIdFromCache(
    extensionId: ExtensionId,
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ): MangaChapter? {
    return repoAsync {
      return@repoAsync mangaCache.getChapter(extensionId, mangaId, mangaChapterId)
    }
  }

  class MangaChapterNotFound(
    extensionId: ExtensionId,
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ) : Exception("Manga chapter not found. (extensionId=${extensionId.rawId}, mangaId=${mangaId.id}, mangaChapterId=${mangaChapterId.id})")

}