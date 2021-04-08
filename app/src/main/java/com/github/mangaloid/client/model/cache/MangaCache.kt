package com.github.mangaloid.client.model.cache

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.model.data.*
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

  suspend fun replaceMangaChapters(mangaDescriptor: MangaDescriptor, mangaChapters: List<MangaChapter>) {
    replaceMangaChapters(mangaDescriptor.extensionId, mangaDescriptor.mangaId, mangaChapters)
  }

  suspend fun replaceMangaChapters(extensionId: ExtensionId, mangaId: MangaId, mangaChapters: List<MangaChapter>) {
    if (mangaChapters.isEmpty()) {
      return
    }

    mutex.withLock {
      mangaCache[extensionId]
        ?.get(mangaId)
        ?.replaceChapters(mangaChapters)
    }
  }

  suspend fun updateMangaChapter(extensionId: ExtensionId, mangaChapter: MangaChapter) {
    mutex.withLock {
      mangaCache[extensionId]?.get(mangaChapter.mangaId)?.updateMangaChapter(mangaChapter)
    }
  }

  suspend fun getManga(mangaDescriptor: MangaDescriptor): Manga? {
    return mutex.withLock { mangaCache.get(mangaDescriptor.extensionId)?.get(mangaDescriptor.mangaId) }
  }

  suspend fun getChapter(mangaChapterDescriptor: MangaChapterDescriptor): MangaChapter? {
    return mutex.withLock {
      return@withLock mangaCache.get(mangaChapterDescriptor.extensionId)
        ?.get(mangaChapterDescriptor.mangaId)
        ?.getChapterByChapterId(mangaChapterDescriptor.mangaChapterId)
    }
  }

  suspend fun contains(extensionId: ExtensionId, mangaId: MangaId): Boolean {
    return mutex.withLock { mangaCache[extensionId]?.containsKey(mangaId) ?: false }
  }

}