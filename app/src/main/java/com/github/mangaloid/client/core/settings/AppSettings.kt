package com.github.mangaloid.client.core.settings

import android.content.Context
import com.github.mangaloid.client.core.settings.impl.IntSetting


class AppSettings(
  private val appContext: Context,
) : IAppSettings {
  private val sharedPreferences by lazy { appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

  override val pagesToPreloadCount: IntSetting by lazy {
    IntSetting(
      sharedPreferences = sharedPreferences,
      key = "pages_to_preload_count",
      default = 5
    )
  }

}