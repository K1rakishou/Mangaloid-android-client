package com.github.mangaloid.client.core.extension

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.core.extension.mangaloid.MangaloidExtension
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MangaExtensionManager {
  private val mutex = Mutex()
  @GuardedBy("mutex")
  private val cachedMangaExtensions = mutableMapOf<ExtensionId, AbstractMangaExtension>()

  @Suppress("UNCHECKED_CAST")
  suspend fun <T> getMangaExtensionById(extensionId: ExtensionId): T {
    return mutex.withLock {
      if (cachedMangaExtensions.containsKey(extensionId)) {
        return@withLock cachedMangaExtensions[extensionId]!! as T
      }

      val extension = instantiateMangaExtension(extensionId)
      cachedMangaExtensions[extensionId] = extension

      return@withLock extension as T
    }
  }

  private fun instantiateMangaExtension(extensionId: ExtensionId): AbstractMangaExtension {
    return when (extensionId) {
      ExtensionId.Mangaloid -> MangaloidExtension()
    }
  }

}