package com.github.mangaloid.client.screens.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import com.github.mangaloid.client.ui.theme.MangaloidclientTheme


class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MangaloidclientTheme {
        Surface(color = MaterialTheme.colors.surface) {
          MainActivityRouter()
        }
      }
    }
  }
}