package com.github.mangaloid.client.core.settings.enums

import com.github.mangaloid.client.core.settings.impl.EnumSetting

enum class SwipeDirection(val key: String) : EnumSetting.EnumItem {
  LeftToRight("left_to_right"),
  RightToLeft("right_to_left");

  override val itemKey: String
    get() = key

}