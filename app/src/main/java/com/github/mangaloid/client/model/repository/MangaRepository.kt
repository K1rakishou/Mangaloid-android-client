package com.github.mangaloid.client.model.repository

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.core.extension.MangaExtensionManager
import com.github.mangaloid.client.model.cache.MangaCache
import com.github.mangaloid.client.model.data.*
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

  suspend fun getMangaChapter(mangaChapterDescriptor: MangaChapterDescriptor): ModularResult<MangaChapter> {
    return repoAsync {
      Logger.d(TAG, "getMangaChapterById(mangaChapterDescriptor=$mangaChapterDescriptor)")

      return@repoAsync ModularResult.Try {
        var chapterFromCache = mangaCache.getChapter(mangaChapterDescriptor)
        if (chapterFromCache != null) {
          if (!chapterFromCache.needChapterPagesUpdate()) {
            return@Try chapterFromCache
          }

          return@Try refreshMangaChapterPagesIfNeeded(mangaChapterDescriptor.extensionId, chapterFromCache)
            .unwrap()
        }

        getManga(mangaChapterDescriptor.mangaDescriptor)
          .unwrap()

        chapterFromCache = mangaCache.getChapter(mangaChapterDescriptor)
        if (chapterFromCache == null) {
          throw MangaChapterNotFound(mangaChapterDescriptor)
        }

        return@Try chapterFromCache
      }
    }
  }

  suspend fun getManga(mangaDescriptor: MangaDescriptor): ModularResult<Manga?> {
    return repoAsync {
      Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor)")

      val fromCache = mangaCache.getManga(mangaDescriptor)
      if (fromCache != null) {
        return@repoAsync refreshMangaChaptersIfNeeded(mangaDescriptor.extensionId, fromCache)
      }

      val mangaLoadedFromServerResult =
        mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(mangaDescriptor.extensionId)
          .getManga(mangaDescriptor.mangaId)

      if (mangaLoadedFromServerResult is ModularResult.Error) {
        return@repoAsync ModularResult.error(mangaLoadedFromServerResult.error)
      }

      val manga = (mangaLoadedFromServerResult as ModularResult.Value).value
        ?: return@repoAsync ModularResult.value(null)

      mangaCache.put(mangaDescriptor.extensionId, manga)

      return@repoAsync ModularResult.value(manga)
    }
  }

  suspend fun refreshMangaChaptersIfNeeded(extensionId: ExtensionId, manga: Manga): ModularResult<Manga?> {
    return repoAsync {
      if (!manga.needChaptersUpdate()) {
        return@repoAsync ModularResult.value(manga)
      }

      Logger.d(TAG, "refreshMangaChapters(extensionId=${extensionId.id}, mangaId=${manga.mangaId.id})")

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

  suspend fun updateMangaChapterMeta(mangaChapterMeta: MangaChapterMeta) {
    // TODO: 4/8/2021
  }

  class MangaNotFound(
    mangaDescriptor: MangaDescriptor
  ) : Exception("Manga not found. (mangaDescriptor=$mangaDescriptor)")

  class MangaHasNoChapters(
    mangaDescriptor: MangaDescriptor
  ) : Exception("Manga has no chapters. (mangaDescriptor=$mangaDescriptor)")

  class MangaChapterNotFound(
    mangaChapterDescriptor: MangaChapterDescriptor
  ) : Exception("Manga chapter not found. (mangaChapterDescriptor=$mangaChapterDescriptor)")

  companion object {
    private const val TAG = "MangaRepository"
  }

}