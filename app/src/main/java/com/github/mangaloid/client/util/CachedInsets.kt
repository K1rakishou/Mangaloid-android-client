package com.github.mangaloid.client.util

import androidx.core.graphics.Insets


data class CachedInsets(
  var left: Int = 0,
  var right: Int = 0,
  var top: Int = 0,
  var bottom: Int = 0,
) {

  fun updateFromInsets(insets: Insets) {
    this.left = insets.left
    this.right = insets.right
    this.top = insets.top
    this.bottom = insets.bottom
  }

}