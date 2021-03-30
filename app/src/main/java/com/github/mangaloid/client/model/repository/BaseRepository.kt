package com.github.mangaloid.client.model.repository

import com.github.mangaloid.client.di.DependenciesGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

abstract class BaseRepository(
  private val appScope: CoroutineScope = DependenciesGraph.appCoroutineScope
) {

  @Suppress("RedundantAsync")
  suspend fun <T : Any?> repoAsync(func: suspend () -> T): T {
    return appScope.async(Dispatchers.IO) { func() }.await()
  }

}