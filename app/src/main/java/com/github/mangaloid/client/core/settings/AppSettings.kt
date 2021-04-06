package com.github.mangaloid.client.core.settings

import android.content.Context
import com.github.mangaloid.client.core.settings.enums.SwipeDirection
import com.github.mangaloid.client.core.settings.impl.EnumSetting


class AppSettings(
  private val appContext: Context,
) : IAppSettings {
  private val sharedPreferences by lazy { appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

  // TODO: 4/6/2021 can't be changed
  override val readerSwipeDirection by lazy {
    EnumSetting(
      sharedPreferences = sharedPreferences,
      key = "reader_swipe_direction",
      default = SwipeDirection.RightToLeft,
      enumClass = SwipeDirection::class.java
    )
  }

}