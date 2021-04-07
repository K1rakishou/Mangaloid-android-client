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

  suspend fun loadLibrary(extensionId: ExtensionId): ModularResult<List<Manga>> {
    return repoAsync {
      ModularResult.Try { emptyList() }
    }
  }

  suspend fun searchForManga(
    extensionId: ExtensionId,
    title: String,
    author: String = "",
    artist: String = "",
    genres: List<String> = emptyList()
  ): ModularResult<List<Manga>> {
    return repoAsync {
      val foundManga = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .searchForManga(title, author, artist, genres)

      if (foundManga is ModularResult.Value) {
        mangaCache.putMany(extensionId, foundManga.value)
      }

      return@repoAsync foundManga
    }
  }

  suspend fun getMangaByMangaId(extensionId: ExtensionId, mangaId: MangaId): ModularResult<Manga?> {
    return repoAsync {
      val fromCache = mangaCache.getManga(extensionId, mangaId)
      if (fromCache != null) {
        return@repoAsync refreshMangaThumbnailsIfNeeded(extensionId, fromCache)
      }

      val mangaLoadedFromServerResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .getMangaByMangaId(mangaId)

      if (mangaLoadedFromServerResult is ModularResult.Error) {
        return@repoAsync ModularResult.error(mangaLoadedFromServerResult.error)
      }

      val manga = (mangaLoadedFromServerResult as ModularResult.Value).value
        ?: return@repoAsync ModularResult.value(null)

      mangaCache.put(extensionId, manga)

      return@repoAsync ModularResult.value(manga)
    }
  }

  suspend fun refreshMangaThumbnailsIfNeeded(extensionId: ExtensionId, manga: Manga): ModularResult<Manga?> {
    return repoAsync {
      if (!manga.needChaptersUpdate()) {
        return@repoAsync ModularResult.value(manga)
      }

      val chaptersResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .getMangaChaptersByMangaId(manga.mangaId)

      if (chaptersResult is ModularResult.Error) {
        if (manga.hasChapters()) {
          return@repoAsync ModularResult.value(manga)
        }

        // We have no chapters and we failed to get them from the server. We should notify the user.
        return@repoAsync ModularResult.error(chaptersResult.error)
      }

      val mangaChapters = (chaptersResult as ModularResult.Value).value
      manga.replaceChapters(mangaChapters)

      return@repoAsync ModularResult.value(manga)
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

  class MangaNotFound(
    extensionId: ExtensionId,
    mangaId: MangaId
  ) : Exception("Manga not found. (extensionId=${extensionId.rawId}, mangaId=${mangaId.id})")

  class MangaHasNoChapters(
    extensionId: ExtensionId,
    mangaId: MangaId
  ) : Exception("Manga has no chapters. (extensionId=${extensionId.rawId}, mangaId=${mangaId.id})")

  class MangaChapterNotFound(
    extensionId: ExtensionId,
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ) : Exception("Manga chapter not found. (extensionId=${extensionId.rawId}, mangaId=${mangaId.id}, mangaChapterId=${mangaChapterId.id})")

}