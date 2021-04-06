package com.github.mangaloid.client.core.settings.impl

import android.annotation.SuppressLint
import android.content.SharedPreferences

class StringSetting(
  sharedPreferences: SharedPreferences,
  key: String,
  default: String
) : Setting<String>(sharedPreferences, key, default) {

  override suspend fun load(): String? {
    return try {
      sharedPreferences.getString(key, default)
    } catch (error: Throwable) {
      default
    }
  }

  @SuppressLint("ApplySharedPref")
  override suspend fun store(value: String, sync: Boolean) {
    if (sync) {
      sharedPreferences.edit().putString(key, value).commit()
    } else {
      sharedPreferences.edit().putString(key, value).apply()
    }
  }
}