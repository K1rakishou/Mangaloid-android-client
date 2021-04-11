package com.github.mangaloid.client.screens.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.cache.MangaCache
import com.github.mangaloid.client.model.cache.MangaUpdates
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaDescriptor
import com.github.mangaloid.client.model.data.MangaMeta
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.util.updateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChaptersScreenViewModel(
  private val mangaDescriptor: MangaDescriptor,
  private val mangaUpdates: MangaUpdates = DependenciesGraph.mangaUpdates,
  private val mangaCache: MangaCache = DependenciesGraph.mangaCache,
  private val mangaRepository: MangaRepository = DependenciesGraph.mangaRepository
) : ViewModel() {
  private val _chaptersScreenViewModelState = MutableStateFlow(ChaptersScreenState())

  val chaptersScreenViewModelState: StateFlow<ChaptersScreenState>
    get() = _chaptersScreenViewModelState

  init {
    viewModelScope.launch {
      mangaUpdates.getMangaMetaUpdatesFlow(mangaDescriptor)
        .collect { mangaMeta ->
          val currentFullMangaInfoAsync = getCurrentFullMangaInfoOrNull()
            ?: return@collect

          val fullMangaInfo = FullMangaInfo(
            manga = currentFullMangaInfoAsync.manga,
            mangaMeta = mangaMeta.copy()
          )

          _chaptersScreenViewModelState.updateState {
            copy(currentFullMangaInfoAsync = AsyncData.Data(fullMangaInfo))
          }
        }
    }

    viewModelScope.launch {
      _chaptersScreenViewModelState.updateState {
        copy(currentFullMangaInfoAsync = AsyncData.Loading())
      }

      val mangaResult = mangaRepository.getManga(mangaDescriptor)
      if (mangaResult is ModularResult.Error) {
        _chaptersScreenViewModelState.updateState {
          copy(currentFullMangaInfoAsync = AsyncData.Error(mangaResult.error))
        }
        return@launch
      }

      val manga = (mangaResult as ModularResult.Value).value
      if (manga == null) {
        _chaptersScreenViewModelState.updateState {
          val error = MangaRepository.MangaNotFound(mangaDescriptor)
          copy(currentFullMangaInfoAsync = AsyncData.Error(error))
        }

        return@launch
      }

      if (!manga.hasChapters()) {
        _chaptersScreenViewModelState.updateState {
          val error = MangaRepository.MangaHasNoChapters(mangaDescriptor)
          copy(currentFullMangaInfoAsync = AsyncData.Error(error))
        }

        return@launch
      }

      val mangaMeta = mangaCache.getMangaMeta(mangaDescriptor)
      checkNotNull(mangaMeta) { "Failed to get manga meta for $mangaDescriptor" }

      _chaptersScreenViewModelState.updateState {
        val fullMangaInfo = FullMangaInfo(
          manga = manga.copy(),
          mangaMeta = mangaMeta.copy()
        )

        copy(currentFullMangaInfoAsync = AsyncData.Data(fullMangaInfo))
      }
    }
  }

  fun bookmarkUnbookmarkManga() {
    viewModelScope.launch {
      val currentFullMangaInfoAsync = getCurrentFullMangaInfoOrNull()
        ?: return@launch

      val currentMangaMeta = currentFullMangaInfoAsync.mangaMeta
      val updatedMangaMeta = currentMangaMeta.copy(bookmarked = currentMangaMeta.bookmarked.not())

      mangaRepository.updateMangaMeta(updatedMangaMeta)
    }
  }

  private fun getCurrentFullMangaInfoOrNull(): FullMangaInfo? {
    val currentMangaAsync = _chaptersScreenViewModelState.value.currentFullMangaInfoAsync
    if (currentMangaAsync !is AsyncData.Data) {
      return null
    }

    return currentMangaAsync.data
  }

  data class ChaptersScreenState(
    val currentFullMangaInfoAsync: AsyncData<FullMangaInfo> = AsyncData.NotInitialized()
  )

  data class FullMangaInfo(val manga: Manga, val mangaMeta: MangaMeta)
}