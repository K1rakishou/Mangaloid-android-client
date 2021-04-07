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
import com.github.mangaloid.client.util.Logger

class MangaRepository(
  private val mangaCache: MangaCache,
  private val mangaExtensionManager: MangaExtensionManager
) : BaseRepository() {

  suspend fun loadLibrary(extensionId: ExtensionId): ModularResult<List<Manga>> {
    return repoAsync {
      Logger.d(TAG, "loadLibrary(extensionId=${extensionId.id})")
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
      Logger.d(TAG, "searchForManga(extensionId=${extensionId.id}, title='${title}', author='${author}', " +
        "artist='${artist}', genres='${genres.joinToString()}')")

      val foundManga = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .searchForManga(title, author, artist, genres)

      if (foundManga is ModularResult.Value) {
        mangaCache.putMany(extensionId, foundManga.value)
      }

      return@repoAsync foundManga
    }
  }

  suspend fun getMangaChapterById(
    extensionId: ExtensionId,
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ): ModularResult<MangaChapter> {
    return repoAsync {
      Logger.d(TAG, "getMangaChapterById(extensionId=${extensionId.id}, mangaId=${mangaId.id}, mangaChapterId=${mangaChapterId.id})")

      return@repoAsync ModularResult.Try {
        var chapterFromCache = mangaCache.getChapter(extensionId, mangaId, mangaChapterId)
        if (chapterFromCache != null) {
          if (!chapterFromCache.needChapterPagesUpdate()) {
            return@Try chapterFromCache
          }

          return@Try refreshMangaChapterPagesIfNeeded(extensionId, chapterFromCache)
            .unwrap()
        }

        getMangaByMangaId(extensionId, mangaId)
          .unwrap()

        chapterFromCache = mangaCache.getChapter(extensionId, mangaId, mangaChapterId)
        if (chapterFromCache == null) {
          throw MangaChapterNotFound(extensionId, mangaId, mangaChapterId)
        }

        return@Try chapterFromCache
      }
    }
  }

  suspend fun getMangaByMangaId(extensionId: ExtensionId, mangaId: MangaId): ModularResult<Manga?> {
    return repoAsync {
      Logger.d(TAG, "getMangaByMangaId(extensionId=${extensionId.id}, mangaId=${mangaId.id})")

      val fromCache = mangaCache.getManga(extensionId, mangaId)
      if (fromCache != null) {
        return@repoAsync refreshMangaThumbnailsIfNeeded(extensionId, fromCache)
      }

      val mangaLoadedFromServerResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .getManga(mangaId)

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

      Logger.d(TAG, "refreshMangaThumbnailsIfNeeded(extensionId=${extensionId.id}, mangaId=${manga.mangaId.id})")

      val chaptersResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .getMangaChapters(manga.mangaId)

      if (chaptersResult is ModularResult.Error) {
        if (manga.hasChapters()) {
          return@repoAsync ModularResult.value(manga)
        }

        // We have no chapters and we failed to get them from the server. We should notify the user.
        return@repoAsync ModularResult.error(chaptersResult.error)
      }

      val mangaChapters = (chaptersResult as ModularResult.Value).value
      manga.replaceChapters(mangaChapters)
      mangaCache.replaceMangaChapters(extensionId, manga.mangaId, mangaChapters)

      return@repoAsync ModularResult.value(manga)
    }
  }

  suspend fun refreshMangaChapterPagesIfNeeded(
    extensionId: ExtensionId,
    mangaChapter: MangaChapter
  ): ModularResult<MangaChapter> {
    return repoAsync {
      if (!mangaChapter.needChapterPagesUpdate()) {
        return@repoAsync ModularResult.value(mangaChapter)
      }

      Logger.d(TAG, "refreshMangaChapterPagesIfNeeded(extensionId=${extensionId.id}, mangaChapterId=${mangaChapter.chapterId.id})")

      val mangaChapterPagesResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
        .getMangaChapterPages(mangaChapter)

      if (mangaChapterPagesResult is ModularResult.Error) {
        return@repoAsync ModularResult.error(mangaChapterPagesResult.error)
      }

      val mangaChapterPages = (mangaChapterPagesResult as ModularResult.Value).value
      if (mangaChapterPages.isEmpty()) {
        return@repoAsync ModularResult.value(mangaChapter)
      }

      mangaChapter.replaceChapterPages(mangaChapterPages)
      mangaCache.updateMangaChapter(extensionId, mangaChapter)

      return@repoAsync ModularResult.value(mangaChapter)
    }
  }

  class MangaNotFound(
    extensionId: ExtensionId,
    mangaId: MangaId
  ) : Exception("Manga not found. (extensionId=${extensionId.id}, mangaId=${mangaId.id})")

  class MangaHasNoChapters(
    extensionId: ExtensionId,
    mangaId: MangaId
  ) : Exception("Manga has no chapters. (extensionId=${extensionId.id}, mangaId=${mangaId.id})")

  class MangaChapterNotFound(
    extensionId: ExtensionId,
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ) : Exception("Manga chapter not found. (extensionId=${extensionId.id}, mangaId=${mangaId.id}, mangaChapterId=${mangaChapterId.id})")

  companion object {
    private const val TAG = "MangaRepository"
  }

}