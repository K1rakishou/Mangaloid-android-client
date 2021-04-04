package com.github.mangaloid.client.core.extension

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.core.extension.mangaloid.MangaloidExtension
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

class MangaExtensionManager {
  private val preloaded = AtomicBoolean(false)

  private val mutex = Mutex()
  @GuardedBy("mutex")
  private val cachedMangaExtensions = mutableMapOf<ExtensionId, AbstractMangaExtension>()

  suspend fun preloadAllExtensions(): List<AbstractMangaExtension> {
    return mutex.withLock {
      if (preloaded.compareAndSet(false, true)) {
        preloadAllExtensionsInternal()
      }

      return@withLock cachedMangaExtensions.values.toList()
    }
  }

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

  private fun preloadAllExtensionsInternal() {
    ExtensionId.values().forEach { extensionId ->
      if (cachedMangaExtensions.containsKey(extensionId)) {
        return@forEach
      }

      val extension = instantiateMangaExtension(extensionId)
      cachedMangaExtensions[extensionId] = extension
    }
  }

}