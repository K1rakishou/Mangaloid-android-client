package com.github.mangaloid.client.core.settings.impl

import android.annotation.SuppressLint
import android.content.SharedPreferences

class EnumSetting<T>(
  sharedPreferences: SharedPreferences,
  key: String,
  default: T,
  enumClass: Class<T>,
  private val keyExtractor: (T) -> String = { value -> value.itemKey }
) : Setting<T>(sharedPreferences, key, default) where T : Enum<T>, T : EnumSetting.EnumItem {
  private val items by lazy { enumClass.enumConstants }

  init {
    require(enumClass.isEnum) { "${enumClass.javaClass.simpleName} must be a enum!" }
  }

  override suspend fun load(): T? {
    return try {
      val enumItemKey = sharedPreferences.getString(key, keyExtractor(default))
      return items.firstOrNull { enumItem -> keyExtractor(enumItem) == enumItemKey } ?: default
    } catch (error: Throwable) {
      default
    }
  }

  @SuppressLint("ApplySharedPref")
  override suspend fun store(value: T, sync: Boolean) {
    if (sync) {
      sharedPreferences.edit().putString(key, keyExtractor(value)).commit()
    } else {
      sharedPreferences.edit().putString(key, keyExtractor(value)).apply()
    }
  }

  interface EnumItem {
    val itemKey: String
  }
}