package com.github.mangaloid.client.screens.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.mangaloid.client.model.MangaId
import com.github.mangaloid.client.ui.theme.MangaloidclientTheme


class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MangaloidclientTheme {
        MainScreen(MangaId(0))
      }
    }
  }
}