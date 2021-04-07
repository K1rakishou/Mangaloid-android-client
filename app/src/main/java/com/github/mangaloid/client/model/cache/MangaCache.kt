package com.github.mangaloid.client.model.cache

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.util.mutableMapWithCap
import com.github.mangaloid.client.util.putIfNotExists
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MangaCache {
  private val mutex = Mutex()

  @GuardedBy("mutex")
  private val mangaCache = mutableMapWithCap<ExtensionId, MutableMap<MangaId, Manga>>(16)

  suspend fun put(extensionId: ExtensionId, manga: Manga) {
    mutex.withLock {
      mangaCache.putIfNotExists(extensionId, mutableMapWithCap(64))
      mangaCache[extensionId]!!.put(manga.mangaId, manga)
    }
  }

  suspend fun putMany(extensionId: ExtensionId, mangaList: List<Manga>) {
    mutex.withLock {
      mangaList.forEach { manga ->
        mangaCache.putIfNotExists(extensionId, mutableMapWithCap(64))
        mangaCache[extensionId]!!.put(manga.mangaId, manga)
      }
    }
  }

  suspend fun getManga(extensionId: ExtensionId, mangaId: MangaId): Manga? {
    return mutex.withLock { mangaCache.get(extensionId)?.get(mangaId) }
  }

  suspend fun getChapter(
    extensionId: ExtensionId,
    mangaId: MangaId,
    mangaChapterId: MangaChapterId
  ): MangaChapter? {
    return mutex.withLock {
      return@withLock mangaCache.get(extensionId)
        ?.get(mangaId)
        ?.getChapterByChapterId(mangaChapterId)
    }
  }

  suspend fun contains(extensionId: ExtensionId, mangaId: MangaId): Boolean {
    return mutex.withLock { mangaCache[extensionId]?.containsKey(mangaId) ?: false }
  }
}