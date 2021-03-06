package com.github.mangaloid.client.model.repository

import androidx.room.withTransaction
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.core.extension.MangaExtensionManager
import com.github.mangaloid.client.database.MangaloidDatabase
import com.github.mangaloid.client.database.entity.MangaChapterMetaEntity
import com.github.mangaloid.client.database.entity.MangaMetaEntity
import com.github.mangaloid.client.database.mapper.MangaChapterMetaMapper
import com.github.mangaloid.client.model.cache.MangaCache
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.errorMessageOrClassName
import kotlinx.coroutines.*

class MangaRepository(
  private val appScope: CoroutineScope,
  private val mangaloidDatabase: MangaloidDatabase,
  private val mangaCache: MangaCache,
  private val mangaExtensionManager: MangaExtensionManager
) : BaseRepository() {

  suspend fun loadLibrary(extensionId: ExtensionId): ModularResult<List<Manga>> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "loadLibrary(extensionId=${extensionId.id})")
        val bookmarkedMangaMetaList = mangaloidDatabase.mangaMetaDao().selectAllBookmarkedByExtensionId(extensionId.id)

        // TODO: 4/11/2021 switch to a batch api once it's implemented on the server
        val libraryMangaList = supervisorScope {
          return@supervisorScope bookmarkedMangaMetaList
            .chunked(4)
            .flatMap { chunk ->
              return@flatMap chunk.map { mangaMetaEntity ->
                return@map appScope.async(Dispatchers.Default) {
                  val mangaDescriptor = MangaDescriptor(
                    extensionId = extensionId,
                    mangaId = MangaId.fromRawValueOrNull(mangaMetaEntity.mangaId)!!
                  )

                  return@async getManga(mangaDescriptor)
                    .peekError { error -> Logger.e(TAG, "getManga($mangaDescriptor) error=${error.errorMessageOrClassName()}") }
                    .valueOrNull()
                }
              }.awaitAll()
                .filterNotNull()
            }
        }

        Logger.d(TAG, "loadLibrary(extensionId=${extensionId.id}) success, loaded ${libraryMangaList.size} manga")

        return@Try libraryMangaList
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

        val foundMangaResult = getMangaExtension(extensionId)
          .searchForManga(title, author, artist, genres)

        if (foundMangaResult is ModularResult.Error) {
          Logger.e(TAG, "searchForManga(extensionId=${extensionId.id}, title='${title}', author='${author}', " +
            "artist='${artist}', genres='${genres.joinToString()}') getMangaExtensionById() error", foundMangaResult.error)

          throw foundMangaResult.error
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
          Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor) success from cache")

          return@Try refreshMangaChaptersIfNeeded(mangaDescriptor.extensionId, fromCache)
            .peekError { error ->
              Logger.e(TAG, "getManga(mangaDescriptor=$mangaDescriptor) refreshMangaChaptersIfNeeded() error", error)
            }
            .unwrap()
        }

        val mangaLoadedFromServerResult = getMangaExtension(mangaDescriptor.extensionId)
          .getManga(mangaDescriptor.mangaId)

        if (mangaLoadedFromServerResult is ModularResult.Error) {
          Logger.e(TAG, "getManga(mangaDescriptor=$mangaDescriptor) getManga() error", mangaLoadedFromServerResult.error)
          throw mangaLoadedFromServerResult.error
        }

        val manga = (mangaLoadedFromServerResult as ModularResult.Value).value
        if (manga == null) {
          Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor) getManga() manga == null")
          return@Try null
        }

        mangaCache.put(mangaDescriptor.extensionId, manga)
        preloadMangaMeta(mangaDescriptor.extensionId, listOf(manga))
        Logger.d(TAG, "getManga(mangaDescriptor=$mangaDescriptor) success")

        return@Try manga
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

        val chaptersResult = getMangaExtension(extensionId)
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
        preloadMangaChapterMeta(manga.mangaDescriptor, mangaChapters)

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

        val mangaChapterPagesResult = getMangaExtension(extensionId)
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

  suspend fun updateMangaMeta(
    mangaDescriptor: MangaDescriptor,
    updater: suspend (MangaMeta) -> MangaMeta
  ): ModularResult<Unit> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "updateMangaMeta(mangaDescriptor=${mangaDescriptor})")
        val updatedMangaMeta = updater(mangaCache.getOrCreateMangaMeta(mangaDescriptor))

        val mangaMetaEntity = MangaMetaEntity(
          id = updatedMangaMeta.databaseId ?: 0L,
          extensionId = updatedMangaMeta.mangaDescriptor.extensionId.id,
          mangaId = updatedMangaMeta.mangaDescriptor.mangaId.id,
          bookmarked = updatedMangaMeta.bookmarked,
          lastViewedChapterId = updatedMangaMeta.lastViewedChapterId.id
        )

        val databaseId = mangaloidDatabase.mangaMetaDao().createNewOrUpdate(mangaMetaEntity)
        require(databaseId >= 0L) { "Bad databaseId: ${databaseId}" }
        updatedMangaMeta.databaseId = databaseId

        mangaCache.putMangaMeta(updatedMangaMeta)
      }
    }
  }

  suspend fun updateMangaChapterMeta(
    mangaChapterDescriptor: MangaChapterDescriptor,
    updater: suspend (MangaChapterMeta) -> MangaChapterMeta
  ): ModularResult<Unit> {
    return repoAsync {
      return@repoAsync ModularResult.Try {
        Logger.d(TAG, "updateMangaChapterMeta(mangaChapterDescriptor=${mangaChapterDescriptor})")
        val updatedMangaChapterMeta = updater(mangaCache.getOrCreateMangaChapterMeta(mangaChapterDescriptor))

        val mangaChapterMetaEntity = MangaChapterMetaEntity(
          id = updatedMangaChapterMeta.databaseId ?: 0L,
          ownerMangaMetaId = updatedMangaChapterMeta.mangaChapterDescriptor.mangaId.id,
          mangaChapterId = updatedMangaChapterMeta.mangaChapterDescriptor.mangaChapterId.id,
          lastViewedChapterPageIndex = updatedMangaChapterMeta.lastViewedPageIndex.lastViewedPageIndex,
          lastReadChapterPageIndex = Math.max(
            updatedMangaChapterMeta.lastViewedPageIndex.lastReadPageIndex,
            updatedMangaChapterMeta.lastViewedPageIndex.lastReadPageIndex
          )
        )

        val databaseId = mangaloidDatabase.mangaChapterMetaDao().createNewOrUpdate(mangaChapterMetaEntity)
        require(databaseId >= 0L) { "Bad databaseId: ${databaseId}" }
        updatedMangaChapterMeta.databaseId = databaseId

        mangaCache.putMangaChapterMeta(updatedMangaChapterMeta)
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
        ?.let { mangaMetaEntity ->
          return@let MangaMeta(
            databaseId = mangaMetaEntity.id,
            mangaDescriptor = manga.mangaDescriptor,
            bookmarked = mangaMetaEntity.bookmarked,
            lastViewedChapterId = MangaChapterId.fromRawValueOrNull(mangaMetaEntity.lastViewedChapterId)
          )
        }

      if (mangaMeta == null) {
        mangaMeta = MangaMeta.createNew(manga.mangaDescriptor)
      }

      Logger.d(TAG, "preloadMangaMeta mangaMeta=$mangaMeta")
      mangaCache.putMangaMeta(mangaMeta)
    }
  }

  suspend fun preloadMangaChapterMeta(
    mangaDescriptor: MangaDescriptor,
    mangaChapterList: List<MangaChapter>
  ) {
    if (mangaCache.allMangaChapterMetaPreloaded(mangaChapterList)) {
      return
    }

    val mangaMetaDatabaseId = mangaCache.getMangaMeta(mangaDescriptor)?.databaseId
    checkNotNull(mangaMetaDatabaseId) { "Whoops, forgot to preload it first!" }

    val mangaChapterIds = mangaChapterList.map { mangaChapter -> mangaChapter.chapterId.id }
    val mangaChapterMetaEntityList = mangaloidDatabase.mangaChapterMetaDao().selectByIdMany(
      ownerMangaMetaId = mangaMetaDatabaseId,
      mangaChapterIds = mangaChapterIds
    )

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
        mangaChapterMeta = MangaChapterMeta.createNew(mangaChapter.mangaChapterDescriptor)
      }

      mangaCache.putMangaChapterMeta(mangaChapterMeta)
    }
  }

  private suspend fun getMangaExtension(extensionId: ExtensionId): AbstractMangaExtension {
    return mangaExtensionManager.getMangaExtensionById(extensionId)
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
          ?.let { mangaMetaEntity ->
            return@let MangaMeta(
              databaseId = mangaMetaEntity.id,
              mangaDescriptor = mangaDescriptor,
              bookmarked = mangaMetaEntity.bookmarked,
              lastViewedChapterId = MangaChapterId.fromRawValueOrNull(mangaMetaEntity.lastViewedChapterId)
            )
          }

        if (mangaMeta != null) {
          mangaCache.putMangaMeta(mangaMeta)
          Logger.d(TAG, "createDefaultMangaMetaInTheDatabaseIfNeeded() already exists in the DB")

          return@withTransaction
        }

        val mangaMetaEntity = MangaMetaEntity(
          id = 0L,
          extensionId = extensionId.id,
          mangaId = mangaId.id,
          bookmarked = false,
          lastViewedChapterId = MangaChapterId.defaultZeroChapter().id
        )

        var databaseId = mangaloidDatabase.mangaMetaDao().createNew(mangaMetaEntity)
        if (databaseId < 0L) {
          val selectedId = mangaloidDatabase.mangaMetaDao().selectById(extensionId.id, mangaId.id)?.id
          if (selectedId == null || selectedId < 0L) {
            throw InconsistencyError("Failed to create new mangaMetaEntity ($mangaMetaEntity) and " +
              "failed to select it from the DB by ids (${extensionId.id}, ${mangaId.id})")
          }

          databaseId = selectedId
        }

        val newMangaMeta = MangaMeta(
          databaseId = databaseId,
          mangaDescriptor = mangaDescriptor,
          bookmarked = false,
          lastViewedChapterId = MangaChapterId.defaultZeroChapter()
        )

        mangaCache.putMangaMeta(newMangaMeta)
        Logger.d(TAG, "createDefaultMangaMetaInTheDatabaseIfNeeded() success")
      }
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

  class InconsistencyError(message: String) : Exception(message)

  companion object {
    private const val TAG = "MangaRepository"
  }

}