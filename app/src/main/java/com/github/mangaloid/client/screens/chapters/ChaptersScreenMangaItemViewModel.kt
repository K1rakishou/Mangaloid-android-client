package com.github.mangaloid.client.screens.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.cache.MangaCache
import com.github.mangaloid.client.model.data.MangaChapter
import com.github.mangaloid.client.model.data.MangaChapterDescriptor
import com.github.mangaloid.client.model.data.MangaChapterMeta
import com.github.mangaloid.client.util.updateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChaptersScreenMangaItemViewModel(
  private val mangaChapterDescriptor: MangaChapterDescriptor,
  private val mangaCache: MangaCache = DependenciesGraph.mangaCache
) : ViewModel() {
  private val _mangaItemMangaChapterState = MutableStateFlow(MangaItemMangaChapterState())
  private val _mangaItemMangaChapterMetaState = MutableStateFlow(MangaItemMangaChapterMetaState())

  val mangaItemMangaChapterState: StateFlow<MangaItemMangaChapterState>
    get() = _mangaItemMangaChapterState
  val mangaItemMangaChapterMetaState: StateFlow<MangaItemMangaChapterMetaState>
    get() = _mangaItemMangaChapterMetaState

  init {
    viewModelScope.launch {
      mangaCache.getMangaChapterMetaUpdatesFlow(mangaChapterDescriptor)
        .collect { mangaChapterMeta ->
          _mangaItemMangaChapterMetaState.updateState { copy(mangaChapterMeta = mangaChapterMeta.deepCopy()) }
        }
    }

    viewModelScope.launch {
      val mangaChapter = mangaCache.getChapter(mangaChapterDescriptor)
      _mangaItemMangaChapterState.updateState { copy(mangaChapter = mangaChapter?.deepCopy()) }

      val mangaChapterMeta = mangaCache.getMangaChapterMeta(mangaChapterDescriptor)
      _mangaItemMangaChapterMetaState.updateState { copy(mangaChapterMeta = mangaChapterMeta?.deepCopy()) }
    }
  }

  data class MangaItemMangaChapterState(
    val mangaChapter: MangaChapter? = null
  )

  data class MangaItemMangaChapterMetaState(
    val mangaChapterMeta: MangaChapterMeta? = null
  )
}