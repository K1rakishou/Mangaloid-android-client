package com.github.mangaloid.client.model.repository

import androidx.room.withTransaction
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.core.extension.MangaExtensionManager
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.database.MangaloidDatabase
import com.github.mangaloid.client.database.entity.MangaChapterMetaEntity
import com.github.mangaloid.client.database.entity.MangaMetaEntity
import com.github.mangaloid.client.database.mapper.MangaChapterMetaMapper
import com.github.mangaloid.client.model.cache.MangaCache
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.Logger

class MangaRepository(
  private val appSettings: AppSettings,
  private val mangaloidDatabase: MangaloidDatabase,
  private val mangaCache: MangaCache,
  private val mangaExtensionManager: MangaExtensionManager
) : BaseRepository() {

  suspend fun loadLibrary(extensionId: ExtensionId): ModularResult<List<Manga>> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "loadLibrary(extensionId=${extensionId.id})")
        val result = emptyList<Manga>()
        Logger.d(TAG, "loadLibrary(extensionId=${extensionId.id}) success")

        return@Try result
      }
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
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "searchForManga(extensionId=${extensionId.id}, title='${title}', author='${author}', " +
          "artist='${artist}', genres='${genres.joinToString()}')")

        val foundMangaResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
          .searchForManga(title, author, artist, genres)

        if (foundMangaResult is ModularResult.Error) {
          Logger.e(TAG, "searchForManga(extensionId=${extensionId.id}, title='${title}', author='${author}', " +
            "artist='${artist}', genres='${genres.joinToString()}') getMangaExtensionById() error", foundMangaResult.error)

          return@repoAsync ModularResult.error(foundMangaResult.error)
        }

        val foundManga = (foundMangaResult as ModularResult.Value).value
        mangaCache.putMany(extensionId, foundManga)
        preloadMangaMeta(extensionId, foundManga)

        Logger.d(TAG, "searchForManga(extensionId=${extensionId.id}, title='${title}', author='${author}', " +
          "artist='${artist}', genres='${genres.joinToString()}') success")

        return@Try foundManga
      }
    }
  }

  suspend fun getMangaChapter(mangaChapterDescriptor: MangaChapterDescriptor): ModularResult<MangaChapter> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "getMangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor)")

        createDefaultMangaMetaInTheDatabaseIfNeeded(mangaChapterDescriptor.mangaDescriptor)
          .peekError { error ->
            Logger.e(TAG, "getMangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor) " +
              "createDefaultMangaMetaInTheDatabaseIfNeeded() error", error)
          }
          .unwrap()

        var chapterFromCache = mangaCache.getChapter(mangaChapterDescriptor)
        if (chapterFromCache != null) {
          if (!chapterFromCache.needChapterPagesUpdate()) {
            Logger.d(TAG, "getMangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor) success " +
              "needChapterPagesUpdate == false")
            return@Try chapterFromCache
          }

          return@Try refreshMangaChapterPagesIfNeeded(mangaChapterDescriptor.extensionId, chapterFromCache)
            .peekError { error ->
              Logger.e(TAG, "getMangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor) " +
                "refreshMangaChapterPagesIfNeeded() error", error)
            }
            .unwrap()
        }

        getManga(mangaChapterDescriptor.mangaDescriptor)
          .peekError { error ->
            Logger.e(TAG, "getMangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor) " +
              "getManga() error", error)
          }
          .unwrap()

        chapterFromCache = mangaCache.getChapter(mangaChapterDescriptor)
        if (chapterFromCache == null) {
          Logger.d(TAG, "getMangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor) " +
            "getChapter() chapterFromCache == null")

          throw MangaChapterNotFound(mangaChapterDescriptor)
        }

        Logger.d(TAG, "getMangaChapter(mangaChapterDescriptor=$mangaChapterDescriptor) success")
        return@Try chapterFromCache
      }
    }
  }

  suspend fun getManga(mangaDescriptor: MangaDescriptor): ModularResult<Manga?> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor)")

        createDefaultMangaMetaInTheDatabaseIfNeeded(mangaDescriptor)
          .peekError { error ->
            Logger.e(TAG, "getManga(mangaDescriptor=$mangaDescriptor) " +
              "createDefaultMangaMetaInTheDatabaseIfNeeded() error", error)
          }
          .unwrap()

        val fromCache = mangaCache.getManga(mangaDescriptor)
        if (fromCache != null) {
          Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor) success")
          return@repoAsync refreshMangaChaptersIfNeeded(mangaDescriptor.extensionId, fromCache)
            .peekError { error ->
              Logger.e(TAG, "getManga(mangaDescriptor=$mangaDescriptor) refreshMangaChaptersIfNeeded() error", error)
            }
        }

        val mangaLoadedFromServerResult =
          mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(mangaDescriptor.extensionId)
            .getManga(mangaDescriptor.mangaId)

        if (mangaLoadedFromServerResult is ModularResult.Error) {
          Logger.e(TAG, "getManga(mangaDescriptor=$mangaDescriptor) getManga() error", mangaLoadedFromServerResult.error)
          return@repoAsync ModularResult.error(mangaLoadedFromServerResult.error)
        }

        val manga = (mangaLoadedFromServerResult as ModularResult.Value).value
        if (manga == null) {
          Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor) getManga() manga == null")
          return@repoAsync ModularResult.value(null)
        }

        mangaCache.put(mangaDescriptor.extensionId, manga)
        preloadMangaMeta(mangaDescriptor.extensionId, listOf(manga))

        Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor) success")

        return@repoAsync ModularResult.value(manga)
      }
    }
  }

  suspend fun refreshMangaChaptersIfNeeded(extensionId: ExtensionId, manga: Manga): ModularResult<Manga?> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        if (!manga.needChaptersUpdate()) {
          return@Try manga
        }

        Logger.d(TAG, "refreshMangaChaptersIfNeeded(extensionId=${extensionId.id}, mangaId=${manga.mangaId.id})")

        val chaptersResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
          .getMangaChapters(manga.mangaId)

        if (chaptersResult is ModularResult.Error) {
          Logger.e(TAG, "refreshMangaChaptersIfNeeded(extensionId=${extensionId.id}, mangaId=${manga.mangaId.id}) " +
            "getMangaExtensionById() error", chaptersResult.error)

          if (manga.hasChapters()) {
            return@Try manga
          }

          // We have no chapters and we failed to get them from the server. We should notify the user.
          throw chaptersResult.error
        }

        val mangaChapters = (chaptersResult as ModularResult.Value).value
        mangaCache.replaceMangaChapters(extensionId, manga.mangaId, mangaChapters)
        preloadMangaChapterMeta(manga.mangaId, mangaChapters)

        Logger.d(TAG, "refreshMangaChaptersIfNeeded(extensionId=${extensionId.id}, mangaId=${manga.mangaId.id}) success")
        return@Try manga
      }
    }
  }

  suspend fun refreshMangaChapterPagesIfNeeded(
    extensionId: ExtensionId,
    mangaChapter: MangaChapter
  ): ModularResult<MangaChapter> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        if (!mangaChapter.needChapterPagesUpdate()) {
          return@Try mangaChapter
        }

        Logger.d(TAG, "refreshMangaChapterPagesIfNeeded(extensionId=${extensionId.id}, " +
          "mangaChapterId=${mangaChapter.chapterId.id})")

        val mangaChapterPagesResult = mangaExtensionManager.getMangaExtensionById<AbstractMangaExtension>(extensionId)
          .getMangaChapterPages(mangaChapter)

        if (mangaChapterPagesResult is ModularResult.Error) {
          Logger.e(TAG, "refreshMangaChapterPagesIfNeeded(extensionId=${extensionId.id}, " +
            "mangaChapterId=${mangaChapter.chapterId.id}) getMangaExtensionById() error", mangaChapterPagesResult.error)
          throw mangaChapterPagesResult.error
        }

        val mangaChapterPages = (mangaChapterPagesResult as ModularResult.Value).value
        if (mangaChapterPages.isEmpty()) {
          Logger.d(TAG, "refreshMangaChapterPagesIfNeeded(extensionId=${extensionId.id}, " +
            "mangaChapterId=${mangaChapter.chapterId.id}) getMangaExtensionById() empty")
          return@Try mangaChapter
        }

        mangaChapter.replaceChapterPageDescriptors(mangaChapterPages.map { it.mangaChapterPageDescriptor })
        mangaCache.updateMangaChapter(mangaChapter)
        mangaCache.updateMangaChapterPages(mangaChapterPages)

        Logger.d(TAG, "refreshMangaChapterPagesIfNeeded(extensionId=${extensionId.id}, " +
          "mangaChapterId=${mangaChapter.chapterId.id}) success")

        return@Try mangaChapter
      }
    }
  }

  // TODO: 4/10/2021 once bookmark button is added
  suspend fun updateMangaMeta(mangaMeta: MangaMeta): ModularResult<Unit> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "updateMangaMeta(mangaMeta=${mangaMeta.mangaDescriptor})")

        val mangaMetaEntity = MangaMetaEntity(
          id = mangaMeta.databaseId ?: 0L,
          extensionId = mangaMeta.mangaDescriptor.extensionId.id,
          mangaId = mangaMeta.mangaDescriptor.mangaId.id,
          bookmarked = mangaMeta.bookmarked
        )

        val databaseId = mangaloidDatabase.mangaMetaDao().createNewOrUpdate(mangaMetaEntity)
        if (databaseId >= 0L) {
          mangaMeta.databaseId = databaseId
        }

        mangaCache.putMangaMeta(mangaMeta)
      }
    }
  }

  suspend fun updateMangaChapterMeta(mangaChapterMeta: MangaChapterMeta): ModularResult<Unit> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        val cachedMangaMeta = mangaCache.getMangaMeta(mangaChapterMeta.mangaChapterDescriptor.mangaDescriptor)
        val mangaMetaDatabaseId = cachedMangaMeta?.databaseId

        if (cachedMangaMeta == null || mangaMetaDatabaseId == null || mangaMetaDatabaseId < 0) {
          // TODO: 4/10/2021 get it from the DB?
          Logger.e(TAG, "updateMangaChapterMeta(mangaChapterDescriptor=${mangaChapterMeta.mangaChapterDescriptor}) " +
            "bad mangaMetaDatabaseId: ${mangaMetaDatabaseId}")
          return@Try
        }

        val cachedMangaChapterMeta = mangaCache.getMangaChapterMeta(mangaChapterMeta.mangaChapterDescriptor)
        val prevLastReadChapterPageIndex = cachedMangaChapterMeta?.lastViewedPageIndex?.lastReadPageIndex ?: 0

        Logger.d(TAG, "updateMangaChapterMeta(mangaChapterDescriptor=${mangaChapterMeta.mangaChapterDescriptor})")

        val mangaChapterMetaEntity = MangaChapterMetaEntity(
          id = mangaChapterMeta.databaseId ?: 0L,
          ownerMangaMetaId = mangaMetaDatabaseId,
          mangaChapterId = mangaChapterMeta.mangaChapterDescriptor.mangaChapterId.id,
          lastViewedChapterPageIndex = mangaChapterMeta.lastViewedPageIndex.lastViewedPageIndex,
          lastReadChapterPageIndex = Math.max(
            prevLastReadChapterPageIndex,
            mangaChapterMeta.lastViewedPageIndex.lastReadPageIndex
          )
        )

        val databaseId = mangaloidDatabase.mangaChapterMetaDao().createNewOrUpdate(mangaChapterMetaEntity)
        if (databaseId >= 0L) {
          mangaChapterMeta.databaseId = databaseId
        }

        mangaCache.putMangaChapterMeta(mangaChapterMeta)
      }
    }
  }

  private suspend fun createDefaultMangaMetaInTheDatabaseIfNeeded(mangaDescriptor: MangaDescriptor): ModularResult<Unit> {
    return ModularResult.Try {
      val hasDatabaseId = mangaCache.getMangaMeta(mangaDescriptor)?.hasDatabaseId()
        ?: false

      if (hasDatabaseId) {
        return@Try
      }

      mangaloidDatabase.withTransaction {
        val extensionId = mangaDescriptor.extensionId
        val mangaId = mangaDescriptor.mangaId

        Logger.d(TAG, "createDefaultMangaMetaInTheDatabaseIfNeeded()")

        val mangaMeta = mangaloidDatabase.mangaMetaDao().selectById(extensionId.id, mangaId.id)
          ?.let { mangaMetaEntity -> MangaMeta(mangaMetaEntity.id, mangaDescriptor, mangaMetaEntity.bookmarked) }

        if (mangaMeta != null) {
          mangaCache.putMangaMeta(mangaMeta)
          Logger.d(TAG, "createDefaultMangaMetaInTheDatabaseIfNeeded() already exists in the DB")

          return@withTransaction
        }

        val mangaMetaEntity = MangaMetaEntity(
          id = 0L,
          extensionId = extensionId.id,
          mangaId = mangaId.id,
          bookmarked = false
        )

        val databaseId = mangaloidDatabase.mangaMetaDao().createNew(mangaMetaEntity)
        if (databaseId < 0L) {
          // Already exists
          return@withTransaction
        }

        val newMangaMeta = MangaMeta(
          databaseId = databaseId,
          mangaDescriptor = mangaDescriptor,
          bookmarked = false
        )

        mangaCache.putMangaMeta(newMangaMeta)
        Logger.d(TAG, "createDefaultMangaMetaInTheDatabaseIfNeeded() success")
      }
    }
  }

  suspend fun preloadMangaMeta(
    extensionId: ExtensionId,
    mangaList: List<Manga>
  ) {
    if (mangaCache.allMangaMetaPreloaded(mangaList)) {
      return
    }

    val mangaIdList = mangaList.map { foundMangaRemote -> foundMangaRemote.mangaId.id }
    val mangaMetaEntityList = mangaloidDatabase.mangaMetaDao().selectByIdMany(extensionId.id, mangaIdList)

    mangaList.forEach { manga ->
      if (mangaCache.containsMangaMeta(manga.mangaDescriptor)) {
        return@forEach
      }

      var mangaMeta = mangaMetaEntityList
        .firstOrNull { mangaMetaEntity -> mangaMetaEntity.mangaId == manga.mangaId.id }
        ?.let { mangaMetaEntity -> MangaMeta(mangaMetaEntity.id, manga.mangaDescriptor, mangaMetaEntity.bookmarked) }

      if (mangaMeta == null) {
        mangaMeta = MangaMeta(
          databaseId = null,
          mangaDescriptor = manga.mangaDescriptor,
          bookmarked = false
        )
      }

      mangaCache.putMangaMeta(mangaMeta)
    }
  }

  suspend fun preloadMangaChapterMeta(
    mangaId: MangaId,
    mangaChapterList: List<MangaChapter>
  ) {
    if (mangaCache.allMangaChapterMetaPreloaded(mangaChapterList)) {
      return
    }

    val mangaChapterIds = mangaChapterList.map { mangaChapter -> mangaChapter.chapterId.id }
    val mangaChapterMetaEntityList = mangaloidDatabase.mangaChapterMetaDao().selectByIdMany(mangaId.id, mangaChapterIds)

    mangaChapterList.forEach { mangaChapter ->
      if (mangaCache.containsMangaChapterMeta(mangaChapter.mangaChapterDescriptor)) {
        return@forEach
      }

      var mangaChapterMeta = mangaChapterMetaEntityList
        .firstOrNull { mangaChapterMetaEntity -> mangaChapterMetaEntity.mangaChapterId == mangaChapter.chapterId.id }
        ?.let { mangaChapterMetaEntity ->
          return@let MangaChapterMetaMapper.fromMangaChapterMetaEntity(
            mangaChapter.mangaChapterDescriptor,
            mangaChapterMetaEntity
          )
        }

      if (mangaChapterMeta == null) {
        mangaChapterMeta = MangaChapterMeta(
          databaseId = null,
          mangaChapterDescriptor = mangaChapter.mangaChapterDescriptor,
          lastViewedPageIndex = LastViewedPageIndex(
            lastViewedPageIndex = 0,
            lastReadPageIndex = 0
          )
        )
      }

      mangaCache.putMangaChapterMeta(mangaChapterMeta)
    }
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