package com.github.mangaloid.client.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlin.coroutines.CoroutineContext

class MangaloidCoroutineScope: CoroutineScope {
  private val job = SupervisorJob()

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main

  fun cancelChildren() {
    job.cancelChildren()
  }

  fun cancel() {
    job.cancel()
  }

}