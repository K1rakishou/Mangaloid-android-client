package com.github.mangaloid.client.model.cache

import com.github.mangaloid.client.model.data.MangaChapterDescriptor
import com.github.mangaloid.client.model.data.MangaChapterMeta
import com.github.mangaloid.client.model.data.MangaDescriptor
import com.github.mangaloid.client.model.data.MangaMeta
import com.github.mangaloid.client.util.mutableMapWithCap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MangaUpdates {
  private val mangaMetaUpdates = mutableMapWithCap<MangaDescriptor, MutableSharedFlow<MangaMeta>>(32)
  private val mangaChapterMetaUpdates = mutableMapWithCap<MangaChapterDescriptor, MutableSharedFlow<MangaChapterMeta>>(32)

  val mangaBookmarkUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  fun getMangaMetaUpdatesFlow(mangaDescriptor: MangaDescriptor): Flow<MangaMeta> {
    return getOrCreateSharedFlow(mangaDescriptor, mangaMetaUpdates)
  }

  fun getMangaChapterMetaUpdatesFlow(mangaChapterDescriptor: MangaChapterDescriptor): Flow<MangaChapterMeta> {
    return getOrCreateSharedFlow(mangaChapterDescriptor, mangaChapterMetaUpdates)
  }

  suspend fun notifyListenersMangaMetaUpdated(prevMangaMeta: MangaMeta?, newMangaMeta: MangaMeta) {
    val mangaDescriptor = newMangaMeta.mangaDescriptor

    if (prevMangaMeta != newMangaMeta) {
      mangaMetaUpdates[mangaDescriptor]?.emit(newMangaMeta)
    }

    if (prevMangaMeta?.bookmarked != newMangaMeta.bookmarked) {
      mangaBookmarkUpdates.emit(Unit)
    }
  }

  suspend fun notifyListenersMangaChapterMetaUpdated(
    prevMangaChapterMeta: MangaChapterMeta?,
    newMangaChapterMeta: MangaChapterMeta
  ) {
    val mangaChapterDescriptor = newMangaChapterMeta.mangaChapterDescriptor

    if (prevMangaChapterMeta != newMangaChapterMeta) {
      mangaChapterMetaUpdates[mangaChapterDescriptor]?.emit(newMangaChapterMeta)
    }
  }

  @Synchronized
  private fun <Key, FlowValue> getOrCreateSharedFlow(
    key: Key,
    updatesMap: MutableMap<Key, MutableSharedFlow<FlowValue>>
  ): SharedFlow<FlowValue> {
    val fromCache = updatesMap[key]
    if (fromCache != null) {
      return fromCache
    }

    val flow = MutableSharedFlow<FlowValue>(
      replay = 0,
      extraBufferCapacity = 16,
      onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    updatesMap[key] = flow
    return flow
  }

}