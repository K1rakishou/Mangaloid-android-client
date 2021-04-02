package com.github.mangaloid.client.screens.main

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import com.github.mangaloid.client.ui.theme.MangaloidclientTheme
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets


class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.setupEdgeToEdge()

    setContent {
      ProvideWindowInsets {
        MangaloidclientTheme {
          Surface(color = MaterialTheme.colors.surface) {
            MainActivityRouter()
          }
        }
      }
    }
  }

  fun Window.setupEdgeToEdge() {
    statusBarColor = Color.TRANSPARENT
    navigationBarColor = Color.TRANSPARENT

    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
  }

}