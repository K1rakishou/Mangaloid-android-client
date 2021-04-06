package com.github.mangaloid.client.core.settings

import com.github.mangaloid.client.core.settings.enums.SwipeDirection
import com.github.mangaloid.client.core.settings.impl.EnumSetting
import com.github.mangaloid.client.core.settings.impl.IntSetting

interface IAppSettings {
  val readerSwipeDirection: EnumSetting<SwipeDirection>
  val pagesToPreloadCount: IntSetting
}