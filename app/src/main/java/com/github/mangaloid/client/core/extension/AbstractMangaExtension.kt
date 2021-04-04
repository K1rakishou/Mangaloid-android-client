package com.github.mangaloid.client.core.extension

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import kotlinx.coroutines.sync.Mutex

abstract class AbstractMangaExtension {
  protected val mutex = Mutex()
  @GuardedBy("mutex")
  protected val mangaCache = mutableMapOf<MangaId, Manga>()

  abstract val mangaExtensionId: ExtensionId

  abstract suspend fun loadCatalogManga(): ModularResult<List<Manga>>
  abstract suspend fun getMangaChapterByIdFromCache(mangaId: MangaId, mangaChapterId: MangaChapterId): MangaChapter?
  abstract suspend fun getMangaByIdFromCache(mangaId: MangaId): Manga?
}