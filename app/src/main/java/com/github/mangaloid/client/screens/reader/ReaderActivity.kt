package com.github.mangaloid.client.screens.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.MangaloidCoroutineScope
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.util.FullScreenUtils.hideSystemUI
import com.github.mangaloid.client.util.FullScreenUtils.setupFullscreen
import com.github.mangaloid.client.util.FullScreenUtils.toggleSystemUI
import com.github.mangaloid.client.util.Logger
import com.github.mangaloid.client.util.viewModelProviderFactoryOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ReaderActivity : ComponentActivity(), ReaderScreenPagerWithImages.ReaderActivityCallbacks {
  private val coroutineScope = MangaloidCoroutineScope()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val extensionId = ExtensionId.fromRawValueOrNull(intent.getIntExtra(ExtensionId.EXTENSION_ID_KEY, -1))
    if (extensionId == null) {
      Logger.e(TAG, "onCreate() Bad ${ExtensionId.EXTENSION_ID_KEY} parameter")
      finish()
      return
    }

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
    window.hideSystemUI()
    setContentView(R.layout.reader_activity_layout)

    val readerViewPager = findViewById<ReaderScreenPagerWithImages>(R.id.reader_view_pager)
    readerViewPager.init(this)

    val readScreenViewModel by viewModels<ReaderScreenViewModel>(
      factoryProducer = {
        viewModelProviderFactoryOf {
          ReaderScreenViewModel(
            extensionId = extensionId,
            mangaId = mangaId,
            initialMangaChapterId = mangaChapterId
          )
        }
      }
    )

    coroutineScope.launch {
      readScreenViewModel.stateViewable.collect { state ->
        val currentMangaChapterResult = state.currentMangaChapterResult
          ?: return@collect

        when (currentMangaChapterResult) {
          is ModularResult.Error -> {
            Logger.e(TAG, "currentMangaChapterResult is error", currentMangaChapterResult.error)
            return@collect
          }
          is ModularResult.Value -> {
            val viewableMangaChapter = currentMangaChapterResult.value

            readerViewPager.onMangaLoaded(
              lastViewedPageIndex = viewableMangaChapter.mangaChapterMeta.lastViewedPageIndex,
              viewableMangaChapter = viewableMangaChapter,
              readerScreenViewModel = readScreenViewModel
            )
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    coroutineScope.cancelChildren()
  }

  override fun toggleFullScreenMode() {
    window.toggleSystemUI()
  }

  override fun closeReader() {
    finish()
  }

  companion object {
    private const val TAG = "ReaderActivity"

    fun launch(
      context: Context,
      extensionId: ExtensionId,
      mangaId: MangaId,
      mangaChapterId: MangaChapterId
    ) {
      val intent = Intent(context, ReaderActivity::class.java)
      intent.putExtra(ExtensionId.EXTENSION_ID_KEY, extensionId.rawId)
      intent.putExtra(MangaId.MANGA_ID_KEY, mangaId.id)
      intent.putExtra(MangaChapterId.MANGA_CHAPTER_ID_KEY, mangaChapterId.id)

      context.startActivity(intent)
    }
  }

}