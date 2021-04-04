package com.github.mangaloid.client.model.repository

import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.core.extension.MangaExtensionManager
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId

class MangaRepository(
  private val mangaExtensionManager: MangaExtensionManager
) : BaseRepository() {

  suspend fun loadMangaFromServer(extensionId: ExtensionId): ModularResult<List<Manga>> {
    return repoAsync {
      return@repoAsync mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .loadCatalogManga()
    }
  }

  suspend fun getMangaChapterByIdFromCache(
    extensionId: ExtensionId,
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ): MangaChapter? {
    return repoAsync {
      return@repoAsync mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .getMangaChapterByIdFromCache(mangaId, mangaChapterId)
    }
  }

  suspend fun getMangaByIdFromCache(extensionId: ExtensionId, mangaId: MangaId): Manga? {
    return repoAsync {
      return@repoAsync mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .getMangaByIdFromCache(mangaId)
    }
  }

}