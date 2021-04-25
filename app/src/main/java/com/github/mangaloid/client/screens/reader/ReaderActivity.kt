package com.github.mangaloid.client.screens.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.MangaloidCoroutineScope
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.model.data.MangaChapterDescriptor
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

    val mangaChapterDescriptor = intent.getParcelableExtra<MangaChapterDescriptor>(MangaChapterDescriptor.KEY)
    if (mangaChapterDescriptor == null) {
      Logger.e(TAG, "onCreate() Bad ${MangaChapterDescriptor.KEY} parameter")
      finish()
      return
    }

    window.setupFullscreen()
    window.hideSystemUI()
    setContentView(R.layout.reader_activity_layout)

    val readerViewPager = findViewById<ReaderScreenPagerWithImages>(R.id.reader_view_pager)

    val readerScreenViewModel by viewModels<ReaderScreenViewModel>(
      factoryProducer = {
        viewModelProviderFactoryOf {
          ReaderScreenViewModel(initialMangaChapterDescriptor = mangaChapterDescriptor)
        }
      }
    )

    readerViewPager.init(this, readerScreenViewModel)

    coroutineScope.launch {
      readerScreenViewModel.stateViewable.collect { state ->
        when (val currentMangaChapterAsync = state.currentMangaChapterAsync) {
          is AsyncData.NotInitialized -> {
            // no-op
          }
          is AsyncData.Loading -> {
            readerViewPager.onMangaLoadProgress()
          }
          is AsyncData.Error -> {
            readerViewPager.onMangaLoadError(currentMangaChapterAsync.throwable) {
              readerScreenViewModel.reloadMangaChapter()
            }
          }
          is AsyncData.Data -> {
            readerViewPager.onMangaLoaded(currentMangaChapterAsync.data,)
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
      mangaChapterDescriptor: MangaChapterDescriptor
    ) {
      val intent = Intent(context, ReaderActivity::class.java)
      intent.putExtra(MangaChapterDescriptor.KEY, mangaChapterDescriptor)

      context.startActivity(intent)
    }
  }

}