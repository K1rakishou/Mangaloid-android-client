package com.github.mangaloid.client.core

import com.github.mangaloid.client.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.ConcurrentHashMap

class LimitingConcurrentCoroutineExecutor(
  private val maxCoroutinesCount: Int,
  private val coroutineScope: CoroutineScope,
  private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {
  private val semaphore = Semaphore(permits = maxCoroutinesCount)

  private val activeJobsMap = ConcurrentHashMap<Any, Job>()

  init {
    require(maxCoroutinesCount > 0) { "Bad maxCoroutinesCount param" }
  }

  fun post(key: Any?, func: suspend () -> Unit) {
    val job = coroutineScope.launch(context = dispatcher) {
      semaphore.acquire()

      try {
        ensureActive()
        func()
      } catch (error: Throwable) {
        if (error is CancellationException) {
          throw error
        }

        Logger.e(TAG, "func() error", error)
      } finally {
        semaphore.release()
      }
    }

    if (key != null) {
      activeJobsMap[key] = job
    }
  }

  fun cancel(key: Any) {
    activeJobsMap.remove(key)?.cancel()
  }

  companion object {
    private const val TAG = "LimitingConcurrentCoroutineExecutor"
  }

}