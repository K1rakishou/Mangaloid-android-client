package com.github.mangaloid.client.core.settings.impl

import android.content.SharedPreferences
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

abstract class Setting<T : Any?>(
  protected val sharedPreferences: SharedPreferences,
  protected val key: String,
  protected val default: T
) {
  private var actual: MutableStateFlow<T>? = null
  protected val mutex = Mutex()

  private suspend fun init() {
    withContext(dispatcher) {
      if (actual == null) {
        actual = MutableStateFlow(load() ?: default)
      }
    }
  }

  open suspend fun get(): T {
    init()
    return actual!!.value
  }

  open suspend fun listen(): StateFlow<T> {
    init()
    return actual!!.asStateFlow()
  }

  open suspend fun set(value: T, sync: Boolean = false) {
    init()

    val prevValue = actual!!.value
    actual!!.value = value

    if (prevValue != value) {
      if (sync) {
        withContext(dispatcher) { store(value, true) }
      } else {
        store(value, false)
      }
    }
  }

  abstract suspend fun load(): T?
  abstract suspend fun store(value: T, sync: Boolean)

  companion object {
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  }
}