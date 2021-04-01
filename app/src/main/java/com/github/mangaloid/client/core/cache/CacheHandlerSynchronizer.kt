package com.github.mangaloid.client.core.cache

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.util.withLockNonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

/**
 * A synchronizer class for CacheHandler that allows synchronization on a value of the key (by it's value).
 * This is very useful for CacheHandler since we won't to lock disk access per file separately not
 * for the whole disk at a time. This should drastically improve CacheHandler's performance when
 * many different threads access different files. In the previous implementation we would lock
 * access to disk globally every time a thread is doing something with a file which could slow down
 * everything when there were a lot of disk access from multiple threads
 * (Album with 5 columns + prefetch + high-res thumbnails + huge cache size (1GB+).)
 * */
class CacheHandlerSynchronizer {
  @GuardedBy("this")
  private val synchronizerMap = HashMap<String, WeakReference<Mutex>>()

  private val globalMutex = Mutex()
  private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

  private fun getOrCreateMutex(key: String): Mutex {
    return synchronized(this) {
      var value = synchronizerMap[key]?.get()
      if (value == null) {
        value = Mutex()
        synchronizerMap[key] = WeakReference(value)
      }

      return@synchronized value
    }
  }

  suspend fun <T : Any?> withLocalLock(key: String, func: suspend () -> T): T {
    return withContext(dispatcher) {
      return@withContext getOrCreateMutex(key).withLockNonCancellable { func() }
    }
  }

  suspend fun <T : Any?> withGlobalLock(func: suspend () -> T): T {
    return withContext(dispatcher) {
      return@withContext globalMutex.withLockNonCancellable { func() }
    }
  }

}