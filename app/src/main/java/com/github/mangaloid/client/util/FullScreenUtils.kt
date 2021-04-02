package com.github.mangaloid.client.util

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager

object FullScreenUtils {

  fun Window.setupFullscreen() {
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT

    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  fun Window.hideSystemUI() {
    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
      or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_FULLSCREEN)
  }

  fun Window.showSystemUI() {
    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
  }

  fun Window.toggleSystemUI() {
    if (isSystemUIHidden()) {
      showSystemUI()
    } else {
      hideSystemUI()
    }
  }

  fun Window.isSystemUIHidden(): Boolean {
    return (decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_IMMERSIVE) != 0
  }

}