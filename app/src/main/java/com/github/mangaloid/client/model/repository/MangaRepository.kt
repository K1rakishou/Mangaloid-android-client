package com.github.mangaloid.client.model.repository

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.model.source.MangaRemoteSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MangaRepository(
  private val mangaRemoteSource: MangaRemoteSource
) : BaseRepository() {
  private val mutex = Mutex()

  @GuardedBy("mutex")
  private val mangaCache = mutableMapOf<MangaId, Manga>()

  suspend fun loadMangaFromServer(): ModularResult<List<Manga>> {
    return repoAsync {
      val mangaLoadedFromServerResult = mangaRemoteSource.loadManga()
      if (mangaLoadedFromServerResult is ModularResult.Error) {
        return@repoAsync ModularResult.error(mangaLoadedFromServerResult.error)
      }

      val mangaList = (mangaLoadedFromServerResult as ModularResult.Value).value
      if (mangaList.isEmpty()) {
        return@repoAsync ModularResult.value(emptyList())
      }

      mangaList.forEach { manga ->
        mutex.withLock {
          mangaCache.put(manga.mangaId, manga)
        }
      }

      return@repoAsync ModularResult.value(mangaList)
    }
  }

  suspend fun getMangaChapterByIdFromCache(mangaId: MangaId, mangaChapterId: MangaChapterId): MangaChapter? {
    return mutex.withLock {
      mangaCache[mangaId]
        ?.chapters
        ?.firstOrNull { mangaChapter -> mangaChapter.chapterId == mangaChapterId }
    }
  }

  suspend fun getMangaByIdFromCache(mangaId: MangaId): Manga? {
    return mutex.withLock { mangaCache[mangaId] }
  }

}