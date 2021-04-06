package com.github.mangaloid.client.core.settings.impl

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlinx.coroutines.sync.withLock

class BooleanSetting(
  sharedPreferences: SharedPreferences,
  key: String,
  default: Boolean
) : Setting<Boolean>(sharedPreferences, key, default) {

  suspend fun toggle(): Boolean {
    return mutex.withLock {
      val newValue = get().not()
      set(newValue)

      return@withLock newValue
    }
  }

  override suspend fun load(): Boolean? {
    return try {
      sharedPreferences.getBoolean(key, default)
    } catch (error: Throwable) {
      default
    }
  }

  @SuppressLint("ApplySharedPref")
  override suspend fun store(value: Boolean, sync: Boolean) {
    if (sync) {
      sharedPreferences.edit().putBoolean(key, value).commit()
    } else {
      sharedPreferences.edit().putBoolean(key, value).apply()
    }
  }

}