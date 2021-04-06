package com.github.mangaloid.client.ui.widget.toolbar

import androidx.annotation.DrawableRes
import com.github.mangaloid.client.R

sealed class ToolbarButton(
  val toolbarButtonId: ToolbarButtonId,
  val contentDescription: String,
  @DrawableRes val iconDrawableId: Int
) {
  class BackArrow(id: ToolbarButtonId) : ToolbarButton(
    toolbarButtonId = id,
    contentDescription = "Back",
    iconDrawableId = R.drawable.ic_baseline_arrow_back_24
  )

  class HamburgMenu : ToolbarButton(
    toolbarButtonId = ToolbarButtonId.DrawerMenu,
    contentDescription = "Drawer menu",
    iconDrawableId = R.drawable.ic_baseline_menu_24
  )

  class MangaSearchButton : ToolbarButton(
    toolbarButtonId = ToolbarButtonId.MangaSearch,
    contentDescription = "Manga search",
    iconDrawableId = R.drawable.ic_baseline_search_24
  )

  class MangaChapterSearchButton : ToolbarButton(
    toolbarButtonId = ToolbarButtonId.MangaChapterSearch,
    contentDescription = "Manga chapter search",
    iconDrawableId = R.drawable.ic_baseline_search_24
  )

  class ClearSearchButton : ToolbarButton(
    toolbarButtonId = ToolbarButtonId.ClearSearch,
    contentDescription = "Clear search",
    iconDrawableId = R.drawable.ic_baseline_close_24
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ToolbarButton

    if (toolbarButtonId != other.toolbarButtonId) return false
    if (contentDescription != other.contentDescription) return false
    if (iconDrawableId != other.iconDrawableId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = toolbarButtonId.id.hashCode()
    result = 31 * result + contentDescription.hashCode()
    result = 31 * result + iconDrawableId
    return result
  }
}