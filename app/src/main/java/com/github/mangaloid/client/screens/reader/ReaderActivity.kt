package com.github.mangaloid.client.screens.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import com.github.mangaloid.client.model.MangaId
import com.github.mangaloid.client.ui.theme.MangaloidclientTheme
import com.github.mangaloid.client.util.FullScreenUtils.hideSystemUI
import com.github.mangaloid.client.util.FullScreenUtils.setupFullscreen
import com.github.mangaloid.client.util.FullScreenUtils.toggleSystemUI
import com.github.mangaloid.client.util.Logger

class ReaderActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.setupFullscreen()
    window.hideSystemUI(lightStatusBar = true, lightNavBar = true)

    val mangaId = MangaId.fromRawValueOrNull(intent.getIntExtra(MANGA_ID_KEY, -1))
    if (mangaId == null) {
      Logger.e(TAG, "onCreate() Bad $MANGA_ID_KEY parameter")
      finish()
      return
    }

    setContent {
      MangaloidclientTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
          ReaderScreen(
            mangaId = mangaId,
            toggleFullScreenModeFunc = { window.toggleSystemUI(lightStatusBar = true, lightNavBar = true) }
          )
        }
      }
    }
  }

  companion object {
    private const val TAG = "ReaderActivity"

    const val MANGA_ID_KEY = "manga_id"

    fun launch(context: Context, mangaId: MangaId) {
      val intent = Intent(context, ReaderActivity::class.java)
      intent.putExtra(MANGA_ID_KEY, mangaId.id)

      context.startActivity(intent)
    }
  }

}