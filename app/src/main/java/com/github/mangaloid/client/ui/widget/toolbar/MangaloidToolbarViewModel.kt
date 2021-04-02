package com.github.mangaloid.client.ui.widget.toolbar

import androidx.compose.runtime.Immutable
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.model.data.Manga

class MangaloidToolbarViewModel : ViewModelWithState<MangaloidToolbarViewModel.ToolbarState>(ToolbarState.default()) {

  fun updateToolbar(updater: ToolbarState.() -> ToolbarState) {
    updateState { updater(stateViewable.value) }
  }

  @Immutable
  data class ToolbarState(
    val toolbarScreen: ToolbarScreen,
    val title: String?,
    val subtitle: String?,
    val leftButton: ToolbarButton?
  ) {

    fun onlyTitle(title: String): ToolbarState {
      return copy(
        title = title,
        subtitle = null,
        leftButton = null
      )
    }

    fun mainScreenToolbar(): ToolbarState {
      return copy(
        toolbarScreen = ToolbarScreen.ChaptersToolbar,
        title = "Mangaloid",
        subtitle = null,
        leftButton = null
      )
    }

    fun chaptersScreenToolbar(manga: Manga): ToolbarState {
      return copy(
        toolbarScreen = ToolbarScreen.ChaptersToolbar,
        title = manga.title,
        subtitle = "${manga.chapters.size} chapters",
        leftButton = ToolbarButton.BackArrow()
      )
    }

    companion object {
      fun default(): ToolbarState = ToolbarState(
        toolbarScreen = ToolbarScreen.MainToolbar,
        title = null,
        subtitle = null,
        leftButton = null
      )
    }

  }

  enum class ToolbarScreen {
    MainToolbar,
    ChaptersToolbar
  }

  sealed class ToolbarButton(val id: Int) {
    class BackArrow : ToolbarButton(TOOLBAR_BUTTON_BACK_ARROW)
  }

  companion object {
    const val TOOLBAR_BUTTON_BACK_ARROW = 0
  }
}