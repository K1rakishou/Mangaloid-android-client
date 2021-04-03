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
    val searchQuery: String?,
    val leftButton: ToolbarButton?,
    val rightButtons: List<ToolbarButton>
  ) {

    fun onlyTitle(title: String): ToolbarState {
      return copy(
        title = title,
        subtitle = null,
        searchQuery = null,
        leftButton = null,
        rightButtons = listOf()
      )
    }

    fun mainScreenToolbar(): ToolbarState {
      return copy(
        toolbarType = ToolbarType.MainToolbar,
        title = "Mangaloid",
        subtitle = null,
        searchQuery = null,
        leftButton = null,
        rightButtons = listOf(ToolbarButton.SearchButton())
      )
    }

    fun searchToolbar(): ToolbarState {
      return copy(
        toolbarType = ToolbarType.SearchToolbar,
        title = null,
        subtitle = null,
        searchQuery = "",
        leftButton = ToolbarButton.BackArrow(TOOLBAR_BUTTON_CLOSE_SEARCH),
        rightButtons = listOf(ToolbarButton.ClearSearchButton())
      )
    }

    fun chaptersScreenToolbar(manga: Manga): ToolbarState {
      return copy(
        toolbarType = ToolbarType.ChaptersToolbar,
        title = manga.title,
        subtitle = "${manga.chapters.size} chapters",
        searchQuery = null,
        leftButton = ToolbarButton.BackArrow(TOOLBAR_BUTTON_BACK_ARROW)
      )
    }

    companion object {
      fun default(): ToolbarState = ToolbarState(
        toolbarType = ToolbarType.Uninitialized,
        title = null,
        subtitle = null,
        searchQuery = null,
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

  sealed class ToolbarButton(
    val id: Int,
    val contentDescription: String,
    @DrawableRes val iconDrawable: Int
  ) {
    class BackArrow(id: Int) : ToolbarButton(id, "Back", R.drawable.ic_baseline_arrow_back_24)
    class SearchButton : ToolbarButton(TOOLBAR_BUTTON_SEARCH, "Search", R.drawable.ic_baseline_search_24)
    class ClearSearchButton : ToolbarButton(TOOLBAR_BUTTON_CLEAR_SEARCH, "Clear search", R.drawable.ic_baseline_close_24)
  }

  companion object {
    const val TOOLBAR_BUTTON_BACK_ARROW = 0
    const val TOOLBAR_BUTTON_SEARCH = 1
    const val TOOLBAR_BUTTON_CLOSE_SEARCH = 2
    const val TOOLBAR_BUTTON_CLEAR_SEARCH = 3
  }
}