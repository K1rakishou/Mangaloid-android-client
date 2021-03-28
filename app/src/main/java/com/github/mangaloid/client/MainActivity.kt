package com.github.mangaloid.client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.mangaloid.client.model.MangaId
import com.github.mangaloid.client.screens.ReaderActivity
import com.github.mangaloid.client.ui.theme.MangaloidclientTheme


class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MangaloidclientTheme {
        MainActivityContent(MangaId(0))
      }
    }
  }
}

@Composable
private fun MainActivityContent(mangaId: MangaId) {
  val background = remember { Color.Black }

  Surface(color = background) {
    Box(modifier = Modifier.fillMaxSize()) {
      val context = LocalContext.current

      Button(
        modifier = Modifier.size(192.dp, 48.dp).align(Alignment.Center),
        onClick = { ReaderActivity.launch(context, mangaId) }
      ) {
        Text(text = "Start reader")
      }
    }
  }
}