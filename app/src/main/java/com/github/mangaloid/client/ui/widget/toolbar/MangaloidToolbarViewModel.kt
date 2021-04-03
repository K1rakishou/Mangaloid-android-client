package com.github.mangaloid.client.ui.widget.toolbar

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.github.mangaloid.client.R
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.model.data.Manga
import java.util.*

class MangaloidToolbarViewModel : ViewModelWithState<MangaloidToolbarViewModel.ToolbarState>(ToolbarState.default()) {
  private val toolbarStateStack = Stack<ToolbarState>()

  fun updateToolbar(updater: ToolbarState.() -> ToolbarState) {
    popToolbarStateToRoot()
    updateState { updater(stateViewable.value) }
  }

  fun updateToolbarDoNotTouchStack(updater: ToolbarState.() -> ToolbarState) {
    updateState { updater(stateViewable.value) }
  }

  fun pushToolbarState() {
    toolbarStateStack.push(stateViewable.value.copy())
  }

  fun popToolbarState() {
    updateToolbarDoNotTouchStack { toolbarStateStack.pop() }
  }

  fun popToolbarStateToRoot(): Boolean {
    if (toolbarStateStack.isEmpty()) {
      return false
    }

    updateToolbarDoNotTouchStack {
      var toolbarState = toolbarStateStack.pop()

      while (!toolbarStateStack.isEmpty()) {
        toolbarState = toolbarStateStack.pop()
      }

      return@updateToolbarDoNotTouchStack toolbarState
    }

    return true
  }

  @Immutable
  data class ToolbarState(
    val toolbarType: ToolbarType,
    val title: String?,
    val subtitle: String?,
    val searchInfo: SearchInfo?,
    val leftButton: ToolbarButton?,
    val rightButtons: List<ToolbarButton>
  ) {

    fun isSearch(): Boolean = toolbarType == ToolbarType.SearchToolbar

    fun onlyTitle(title: String): ToolbarState {
      return copy(
        title = title,
        subtitle = null,
        searchInfo = null,
        leftButton = null,
        rightButtons = listOf()
      )
    }

    fun mainScreenToolbar(): ToolbarState {
      return copy(
        toolbarType = ToolbarType.MainToolbar,
        title = "Mangaloid",
        subtitle = null,
        searchInfo = null,
        leftButton = null,
        rightButtons = listOf(ToolbarButton.MangaSearchButton())
      )
    }

    fun searchToolbar(searchType: SearchType): ToolbarState {
      return copy(
        toolbarType = ToolbarType.SearchToolbar,
        title = null,
        subtitle = null,
        searchInfo = SearchInfo(query = "", searchType = searchType),
        leftButton = ToolbarButton.BackArrow(ToolbarButtonId.ToolbarButtonCloseSearch),
        rightButtons = listOf(ToolbarButton.ClearSearchButton())
      )
    }

    fun chaptersScreenToolbar(manga: Manga): ToolbarState {
      return copy(
        toolbarType = ToolbarType.ChaptersToolbar,
        title = manga.title,
        subtitle = "${manga.chapters.size} chapters",
        searchInfo = null,
        leftButton = ToolbarButton.BackArrow(ToolbarButtonId.ToolbarButtonBackArrow),
        rightButtons = listOf(ToolbarButton.MangaChapterSearchButton())
      )
    }

    companion object {
      fun default(): ToolbarState = ToolbarState(
        toolbarType = ToolbarType.Uninitialized,
        title = null,
        subtitle = null,
        searchInfo = null,
        leftButton = null,
        rightButtons = listOf()
      )
    }

  }

  enum class ToolbarType {
    Uninitialized,
    MainToolbar,
    ChaptersToolbar,
    SearchToolbar
  }

  enum class SearchType {
    MangaSearch,
    MangaChapterSearch
  }

  enum class ToolbarButtonId(val id: Int) {
    ToolbarButtonBackArrow(0),
    ToolbarButtonMangaSearch(1),
    ToolbarButtonCloseSearch(2),
    ToolbarButtonClearSearch(3),
    ToolbarButtonMangaChapterSearch(4)
  }

  data class SearchInfo(val query: String, val searchType: SearchType)

  sealed class ToolbarButton(
    val toolbarButtonId: ToolbarButtonId,
    val contentDescription: String,
    @DrawableRes val iconDrawable: Int
  ) {
    class BackArrow(id: ToolbarButtonId) : ToolbarButton(
      toolbarButtonId = id,
      contentDescription = "Back",
      iconDrawable = R.drawable.ic_baseline_arrow_back_24
    )

    class MangaSearchButton : ToolbarButton(
      ToolbarButtonId.ToolbarButtonMangaSearch,
      "Manga search",
      R.drawable.ic_baseline_search_24
    )

    class MangaChapterSearchButton : ToolbarButton(
      ToolbarButtonId.ToolbarButtonMangaChapterSearch,
      "Manga chapter search",
      R.drawable.ic_baseline_search_24
    )

    class ClearSearchButton : ToolbarButton(
      ToolbarButtonId.ToolbarButtonClearSearch,
      "Clear search",
      R.drawable.ic_baseline_close_24
    )

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as ToolbarButton

      if (toolbarButtonId != other.toolbarButtonId) return false
      if (contentDescription != other.contentDescription) return false
      if (iconDrawable != other.iconDrawable) return false

      return true
    }

    override fun hashCode(): Int {
      var result = toolbarButtonId.id.hashCode()
      result = 31 * result + contentDescription.hashCode()
      result = 31 * result + iconDrawable
      return result
    }
  }
}