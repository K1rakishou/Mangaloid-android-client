package com.github.mangaloid.client.util

import android.util.Log

object Logger {
  const val APP_TAG = "Mangaloid"

  fun d(tag: String, message: String) {
    Log.d("${APP_TAG} | $tag", message)
  }

  fun e(tag: String, message: String, throwable: Throwable? = null) {
    if (throwable == null) {
      Log.e("${APP_TAG} | $tag", message)
    } else {
      if (throwable.isExceptionImportant()) {
        Log.e("${APP_TAG} | $tag", message, throwable)
      } else {
        Log.e("${APP_TAG} | $tag", message + ", error=${throwable.errorMessageOrClassName()}")
      }
    }
  }

}