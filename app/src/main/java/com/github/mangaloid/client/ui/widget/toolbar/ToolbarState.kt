package com.github.mangaloid.client.ui.widget.toolbar

import androidx.compose.runtime.Immutable
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.model.data.MangaMeta

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

  fun titleWithBackButton(backButtonId: ToolbarButtonId, title: String): ToolbarState {
    return copy(
      title = title,
      subtitle = null,
      searchInfo = null,
      leftButton = ToolbarButton.BackArrow(backButtonId),
      rightButtons = listOf()
    )
  }

  fun mainScreenToolbar(): ToolbarState {
    return copy(
      toolbarType = ToolbarType.MainToolbar,
      title = DEFAULT_TOOLBAR_TITLE,
      subtitle = null,
      searchInfo = null,
      leftButton = ToolbarButton.HamburgMenu(),
      rightButtons = listOf(ToolbarButton.MangaSearchButton())
    )
  }

  fun searchToolbar(toolbarSearchType: ToolbarSearchType): ToolbarState {
    return copy(
      toolbarType = ToolbarType.SearchToolbar,
      title = null,
      subtitle = null,
      searchInfo = SearchInfo(query = "", toolbarSearchType = toolbarSearchType),
      leftButton = ToolbarButton.BackArrow(ToolbarButtonId.CloseSearch),
      rightButtons = listOf(ToolbarButton.ClearSearchButton())
    )
  }

  fun chaptersScreenToolbar(manga: Manga, mangaMeta: MangaMeta): ToolbarState {
    val rightButtons = mutableListOf<ToolbarButton>()

    rightButtons += ToolbarButton.MangaChapterSearchButton()

    rightButtons += if (mangaMeta.bookmarked) {
      ToolbarButton.MangaChapterUnbookmarkButton()
    } else {
      ToolbarButton.MangaChapterBookmarkButton()
    }

    return copy(
      toolbarType = ToolbarType.ChaptersToolbar,
      title = manga.titles.first(),
      subtitle = "${manga.chaptersCount()} chapters",
      searchInfo = null,
      leftButton = ToolbarButton.BackArrow(ToolbarButtonId.BackArrow),
      rightButtons = rightButtons
    )
  }

  data class SearchInfo(val query: String, val toolbarSearchType: ToolbarSearchType)

  companion object {
    fun default(): ToolbarState = ToolbarState(
      toolbarType = ToolbarType.MainToolbar,
      title = DEFAULT_TOOLBAR_TITLE,
      subtitle = null,
      searchInfo = null,
      leftButton = ToolbarButton.HamburgMenu(),
      rightButtons = listOf()
    )

    private const val DEFAULT_TOOLBAR_TITLE = "Mangaloid"
  }

}