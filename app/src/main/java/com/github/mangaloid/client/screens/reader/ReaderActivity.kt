package com.github.mangaloid.client.screens.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.mangaloid.client.model.data.local.MangaChapterId
import com.github.mangaloid.client.model.data.local.MangaId
import com.github.mangaloid.client.ui.theme.MangaloidclientTheme
import com.github.mangaloid.client.util.FullScreenUtils.hideSystemUI
import com.github.mangaloid.client.util.FullScreenUtils.setupFullscreen
import com.github.mangaloid.client.util.FullScreenUtils.toggleSystemUI
import com.github.mangaloid.client.util.Logger

class ReaderActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val mangaId = MangaId.fromRawValueOrNull(intent.getIntExtra(MangaId.MANGA_ID_KEY, -1))
    if (mangaId == null) {
      Logger.e(TAG, "onCreate() Bad ${MangaId.MANGA_ID_KEY} parameter")
      finish()
      return
    }

    val mangaChapterId = MangaChapterId.fromRawValueOrNull(intent.getIntExtra(MangaChapterId.MANGA_CHAPTER_ID_KEY, -1))
    if (mangaChapterId == null) {
      Logger.e(TAG, "onCreate() Bad ${MangaChapterId.MANGA_CHAPTER_ID_KEY} parameter")
      finish()
      return
    }

    window.setupFullscreen()
    window.hideSystemUI(lightStatusBar = true, lightNavBar = true)

    setContent {
      MangaloidclientTheme {
        ReaderScreen(
          mangaId = mangaId,
          mangaChapterId = mangaChapterId,
          toggleFullScreenModeFunc = { window.toggleSystemUI(lightStatusBar = true, lightNavBar = true) }
        )
      }
    }
  }

  companion object {
    private const val TAG = "ReaderActivity"

    fun launch(context: Context, mangaId: MangaId, mangaChapterId: MangaChapterId) {
      val intent = Intent(context, ReaderActivity::class.java)
      intent.putExtra(MangaId.MANGA_ID_KEY, mangaId.id)
      intent.putExtra(MangaChapterId.MANGA_CHAPTER_ID_KEY, mangaChapterId.id)

      context.startActivity(intent)
    }
  }

}