package com.github.mangaloid.client.util

import android.os.Looper

object BackgroundUtils {

  fun isMainThread(): Boolean {
    return Thread.currentThread() === Looper.getMainLooper().thread
  }

  fun ensureMainThread() {
    if (isMainThread()) {
      return
    }

    throw IllegalStateException("Can only be executed on the main thread!")

  }

  fun ensureBackgroundThread() {
    if (!isMainThread()) {
      return
    }

    throw IllegalStateException("Cannot be executed on the main thread!")
  }

}