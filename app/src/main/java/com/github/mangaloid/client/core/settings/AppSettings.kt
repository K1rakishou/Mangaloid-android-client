package com.github.mangaloid.client.core.settings

import android.content.Context
import com.github.mangaloid.client.core.settings.enums.SwipeDirection
import com.github.mangaloid.client.core.settings.impl.EnumSetting
import com.github.mangaloid.client.core.settings.impl.IntSetting


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

  override val pagesToPreloadCount: IntSetting by lazy {
    IntSetting(
      sharedPreferences = sharedPreferences,
      key = "pages_to_preload_count",
      default = 5
    )
  }

}