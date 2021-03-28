package com.github.mangaloid.client.util

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager

object FullScreenUtils {

  fun Window.setupFullscreen() {
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  fun Window.setupStatusAndNavBarColors(lightStatusBar: Boolean, lightNavBar: Boolean) {
    var newSystemUiVisibility = decorView.systemUiVisibility

    if (AndroidUtils.isAndroidM()) {
      newSystemUiVisibility = when {
        lightStatusBar -> {
          newSystemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        else -> {
          newSystemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
      }
    }

    if (AndroidUtils.isAndroidO()) {
      newSystemUiVisibility = when {
        lightNavBar -> {
          newSystemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
        else -> {
          newSystemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
      }
    }

    decorView.systemUiVisibility = newSystemUiVisibility
  }

  fun Window.hideSystemUI(lightStatusBar: Boolean, lightNavBar: Boolean) {
    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
      or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_FULLSCREEN)

    setupStatusAndNavBarColors(lightStatusBar, lightNavBar)
  }

  fun Window.showSystemUI(lightStatusBar: Boolean, lightNavBar: Boolean) {
    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    setupStatusAndNavBarColors(lightStatusBar, lightNavBar)
  }

  fun Window.toggleSystemUI(lightStatusBar: Boolean, lightNavBar: Boolean) {
    if (isSystemUIHidden()) {
      showSystemUI(lightStatusBar, lightNavBar)
    } else {
      hideSystemUI(lightStatusBar, lightNavBar)
    }
  }

  fun Window.isSystemUIHidden(): Boolean {
    return (decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_IMMERSIVE) != 0
  }

}