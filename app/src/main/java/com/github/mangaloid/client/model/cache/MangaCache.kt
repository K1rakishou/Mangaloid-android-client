package com.github.mangaloid.client.model.cache

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.model.data.MangaChapterPage
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.mutableListWithCap
import com.github.mangaloid.client.util.mutableMapWithCap
import com.github.mangaloid.client.util.putIfNotExists
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MangaCache {
  private val mutex = Mutex()

  @GuardedBy("mutex")
  private val mangaCache = mutableMapWithCap<ExtensionId, MutableMap<MangaId, Manga>>(16)
  @GuardedBy("mutex")
  private val mangaChapterCache = mutableMapWithCap<MangaDescriptor, MutableList<MangaChapter>>(128)
  @GuardedBy("mutex")
  private val mangaChapterPageCache = mutableMapWithCap<MangaChapterDescriptor, MutableList<MangaChapterPage>>(32)

  @GuardedBy("mutex")
  private val mangaMetaCache = mutableMapWithCap<MangaDescriptor, MangaMeta>(128)
  @GuardedBy("mutex")
  private val mangaChapterMetaCache = mutableMapWithCap<MangaChapterDescriptor, MangaChapterMeta>(128)

  private val mangaChapterMetaUpdates = mutableMapWithCap<MangaChapterDescriptor, MutableSharedFlow<MangaChapterMeta>>(16)

  suspend fun allMangaMetaPreloaded(mangaList: List<Manga>): Boolean {
    return mutex.withLock {
      return@withLock mangaList.all { manga -> mangaMetaCache.containsKey(manga.mangaDescriptor) }
    }
  }

  suspend fun containsMangaMeta(mangaDescriptor: MangaDescriptor): Boolean {
    return mutex.withLock { mangaMetaCache.containsKey(mangaDescriptor) }
  }

  suspend fun getMangaMeta(mangaDescriptor: MangaDescriptor): MangaMeta? {
    return mutex.withLock { mangaMetaCache.get(mangaDescriptor) }
  }

  suspend fun putMangaMeta(mangaMeta: MangaMeta) {
    mutex.withLock { mangaMetaCache[mangaMeta.mangaDescriptor] = mangaMeta }
  }

  suspend fun allMangaChapterMetaPreloaded(mangaChapterList: List<MangaChapter>): Boolean {
    return mutex.withLock {
      return@withLock mangaChapterList.all { manga -> mangaChapterMetaCache.containsKey(manga.mangaChapterDescriptor) }
    }
  }

  suspend fun containsMangaChapterMeta(mangaChapterDescriptor: MangaChapterDescriptor): Boolean {
    return mutex.withLock { mangaChapterMetaCache.containsKey(mangaChapterDescriptor) }
  }

  suspend fun getMangaChapterMeta(mangaChapterDescriptor: MangaChapterDescriptor): MangaChapterMeta? {
    return mutex.withLock { mangaChapterMetaCache.get(mangaChapterDescriptor) }
  }

  suspend fun putMangaChapterMeta(mangaChapterMeta: MangaChapterMeta) {
    mutex.withLock { mangaChapterMetaCache[mangaChapterMeta.mangaChapterDescriptor] = mangaChapterMeta }
    notifyListenersMangaChapterMetaUpdated(mangaChapterMeta)
  }

  suspend fun getMangaChapterMetaUpdatesFlow(mangaChapterDescriptor: MangaChapterDescriptor): Flow<MangaChapterMeta> {
    return mutex.withLock {
      val fromCache = mangaChapterMetaUpdates[mangaChapterDescriptor]
      if (fromCache != null) {
        return@withLock fromCache
      }

      val mangaChapterMetaUpdatesFlow = MutableSharedFlow<MangaChapterMeta>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
      )

      mangaChapterMetaUpdates[mangaChapterDescriptor] = mangaChapterMetaUpdatesFlow
      return@withLock mangaChapterMetaUpdatesFlow
    }
  }

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

  suspend fun replaceMangaChapters(extensionId: ExtensionId, mangaId: MangaId, mangaChapters: List<MangaChapter>) {
    if (mangaChapters.isEmpty()) {
      return
    }

    mutex.withLock {
      val manga = mangaCache[extensionId]
        ?.get(mangaId)
        ?: return@withLock

      val mangaChapterDescriptors = mangaChapters.map { mangaChapter -> mangaChapter.mangaChapterDescriptor }
      manga.replaceChapterDescriptors(mangaChapterDescriptors)

      mangaChapters.forEach { mangaChapter ->
        val mangaDescriptor = mangaChapter.mangaChapterDescriptor.mangaDescriptor

        mangaChapterCache.putIfNotExists(mangaDescriptor, mutableListWithCap(32))
        mangaChapterCache[mangaDescriptor]!!.add(mangaChapter)
      }
    }
  }

  suspend fun updateMangaChapter(mangaChapter: MangaChapter) {
    mutex.withLock {
      val mangaDescriptor = mangaChapter.mangaChapterDescriptor.mangaDescriptor

      mangaChapterCache.putIfNotExists(mangaDescriptor, mutableListWithCap(32))
      val chapters = mangaChapterCache[mangaDescriptor]!!

      val indexOfChapter = chapters
        .indexOfFirst { chapter -> chapter.chapterId == mangaChapter.chapterId }

      if (indexOfChapter >= 0) {
        chapters[indexOfChapter] = mangaChapter
      }
    }
  }

  suspend fun updateMangaChapterPages(mangaChapterPages: List<MangaChapterPage>) {
    mutex.withLock {
      mangaChapterPages.forEach { mangaChapterPage ->
        val mangaChapterDescriptor = mangaChapterPage.mangaChapterPageDescriptor.mangaChapterDescriptor

        mangaChapterPageCache.putIfNotExists(mangaChapterDescriptor, mutableListWithCap(mangaChapterPages.size))

        mangaChapterPageCache[mangaChapterDescriptor]!!.clear()
        mangaChapterPageCache[mangaChapterDescriptor]!!.addAll(mangaChapterPages)
      }
    }
  }

  suspend fun getManga(mangaDescriptor: MangaDescriptor): Manga? {
    return mutex.withLock { mangaCache.get(mangaDescriptor.extensionId)?.get(mangaDescriptor.mangaId) }
  }

  suspend fun getChapter(mangaChapterDescriptor: MangaChapterDescriptor): MangaChapter? {
    return mutex.withLock {
      return@withLock mangaChapterCache[mangaChapterDescriptor.mangaDescriptor]
        ?.firstOrNull { mangaChapter -> mangaChapter.mangaChapterDescriptor == mangaChapterDescriptor }
    }
  }

  suspend fun getMangaChapterPage(mangaChapterPageDescriptor: MangaChapterPageDescriptor): MangaChapterPage? {
    return mutex.withLock {
      val mangaPages = mangaChapterPageCache[mangaChapterPageDescriptor.mangaChapterDescriptor]
        ?: return@withLock null

      return@withLock mangaPages
        .firstOrNull { mangaChapterPage -> mangaChapterPage.mangaChapterPageDescriptor == mangaChapterPageDescriptor }
    }
  }

  suspend fun contains(extensionId: ExtensionId, mangaId: MangaId): Boolean {
    return mutex.withLock { mangaCache[extensionId]?.containsKey(mangaId) ?: false }
  }

  suspend fun iterateMangaChapterPages(
    mangaChapterDescriptor: MangaChapterDescriptor,
    iterator: (Int, MangaChapterPage) -> Unit
  ) {
    mutex.withLock {
      val mangaChapterPages = mangaChapterPageCache[mangaChapterDescriptor]
        ?: return@withLock

      (mangaChapterPages.lastIndex downTo 0).forEach { index ->
        val downloadableMangaPage = mangaChapterPages[index]
        iterator(index, downloadableMangaPage)
      }
    }
  }

  private suspend fun notifyListenersMangaChapterMetaUpdated(mangaChapterMeta: MangaChapterMeta) {
    mangaChapterMetaUpdates[mangaChapterMeta.mangaChapterDescriptor]?.emit(mangaChapterMeta)
  }

}