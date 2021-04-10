package com.github.mangaloid.client.core.settings

import com.github.mangaloid.client.core.settings.impl.IntSetting

interface IAppSettings {
  val pagesToPreloadCount: IntSetting
}