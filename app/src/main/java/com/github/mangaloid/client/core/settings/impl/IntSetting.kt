package com.github.mangaloid.client.core.settings.impl

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlinx.coroutines.sync.withLock

class IntSetting(
  sharedPreferences: SharedPreferences,
  key: String,
  default: Int
) : Setting<Int>(sharedPreferences, key, default) {

  suspend fun incrementAndGet(): Int {
    return mutex.withLock {
      val newValue = get() + 1
      set(newValue)

      return@withLock newValue
    }
  }

  override suspend fun load(): Int? {
    return try {
      sharedPreferences.getInt(key, default)
    } catch (error: Throwable) {
      default
    }
  }

  @SuppressLint("ApplySharedPref")
  override suspend fun store(value: Int, sync: Boolean) {
    if (sync) {
      sharedPreferences.edit().putInt(key, value).commit()
    } else {
      sharedPreferences.edit().putInt(key, value).apply()
    }
  }
}