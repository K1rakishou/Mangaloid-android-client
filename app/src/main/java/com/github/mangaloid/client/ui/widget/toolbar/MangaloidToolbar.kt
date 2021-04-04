package com.github.mangaloid.client.ui.widget.toolbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.mangaloid.client.helper.BackPressHandler
import com.github.mangaloid.client.ui.theme.Typography
import com.github.mangaloid.client.ui.widget.drawer.MangaloidDrawerViewModel
import com.github.mangaloid.client.util.AndroidUtils.toDp
import dev.chrisbanes.accompanist.insets.LocalWindowInsets
import dev.chrisbanes.accompanist.insets.navigationBarsPadding
import dev.chrisbanes.accompanist.insets.statusBarsPadding

private val toolbarHeightDp = 48.dp

@Composable
fun MangaloidToolbar(
  navController: NavHostController,
  toolbarViewModel: MangaloidToolbarViewModel,
  drawerViewModel: MangaloidDrawerViewModel
) {
  val toolbarHeight = LocalWindowInsets.current.statusBars.top.toDp() + toolbarHeightDp
  val toolbarState by toolbarViewModel.stateViewable.collectAsState()

  BackPressHandler {
    toolbarViewModel.popToolbarStateToRoot()
    return@BackPressHandler false
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(toolbarHeight)
      .background(color = MaterialTheme.colors.primary)
      .statusBarsPadding()
      .navigationBarsPadding(left = true, right = true, bottom = false)
  ) {
    ToolbarContent(
      toolbarViewModel = toolbarViewModel,
      toolbarState = toolbarState,
      onToolbarButtonClicked = { toolbarButtonId ->
        when (toolbarButtonId) {
          MangaloidToolbarViewModel.ToolbarButtonId.NoId -> {
            // no-op
          }
          MangaloidToolbarViewModel.ToolbarButtonId.BackArrow -> {
            navController.popBackStack()
          }
          MangaloidToolbarViewModel.ToolbarButtonId.MangaSearch -> {
            toolbarViewModel.pushToolbarState()
            toolbarViewModel.updateToolbarDoNotTouchStack {
              searchToolbar(MangaloidToolbarViewModel.SearchType.MangaSearch)
            }
          }
          MangaloidToolbarViewModel.ToolbarButtonId.MangaChapterSearch -> {
            toolbarViewModel.pushToolbarState()
            toolbarViewModel.updateToolbarDoNotTouchStack {
              searchToolbar(MangaloidToolbarViewModel.SearchType.MangaChapterSearch)
            }
          }
          MangaloidToolbarViewModel.ToolbarButtonId.CloseSearch -> {
            toolbarViewModel.popToolbarState()
          }
          MangaloidToolbarViewModel.ToolbarButtonId.ClearSearch -> {
            toolbarViewModel.updateToolbarDoNotTouchStack {
              copy(searchInfo = searchInfo?.copy(query = ""))
            }
          }
          MangaloidToolbarViewModel.ToolbarButtonId.DrawerMenu -> {
            drawerViewModel.openDrawer()
          }
        }
      }
    )
  }
}

@Composable
private fun ToolbarContent(
  toolbarViewModel: MangaloidToolbarViewModel,
  toolbarState: MangaloidToolbarViewModel.ToolbarState,
  onToolbarButtonClicked: (MangaloidToolbarViewModel.ToolbarButtonId) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxSize()
      .background(color = MaterialTheme.colors.primary)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    PositionToolbarButton(toolbarState.leftButton, onToolbarButtonClicked)

    when (toolbarState.toolbarType) {
      MangaloidToolbarViewModel.ToolbarType.MainToolbar,
      MangaloidToolbarViewModel.ToolbarType.ChaptersToolbar -> {
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .wrapContentWidth()
        ) {
          ToolbarSimpleTitleMiddlePart(toolbarState)
        }
      }
      MangaloidToolbarViewModel.ToolbarType.SearchToolbar -> {
        Row(
          modifier = Modifier
            .fillMaxHeight()
            .wrapContentWidth()
        ) {
          ToolbarSearchMiddlePart(toolbarViewModel, toolbarState)
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    toolbarState.rightButtons.forEach { toolbarButton ->
      PositionToolbarButton(toolbarButton = toolbarButton, onToolbarButtonClicked)
    }
  }
}

@Composable
fun RowScope.ToolbarSearchMiddlePart(
  toolbarViewModel: MangaloidToolbarViewModel,
  toolbarState: MangaloidToolbarViewModel.ToolbarState
) {
  var textState by remember { mutableStateOf(TextFieldValue(toolbarState.searchInfo?.query ?: "")) }
  val textStyle = remember { TextStyle(color = Color.White, fontSize = 18.sp) }
  val cursorBrush = remember { SolidColor(Color.White) }
  val focusRequester = FocusRequester()

  BasicTextField(
    modifier = Modifier
      .weight(1f)
      .align(Alignment.CenterVertically)
      .focusModifier()
      .focusRequester(focusRequester),
    singleLine = true,
    cursorBrush = cursorBrush,
    value = textState,
    textStyle = textStyle,
    onValueChange = { changedText ->
      if (toolbarViewModel.currentState().isSearch()) {
        toolbarViewModel.updateToolbarDoNotTouchStack {
          copy(searchInfo = searchInfo?.copy(query = changedText.text))
        }

        textState = changedText
      }
    }
  )

  DisposableEffect(Unit) {
    focusRequester.requestFocus()
    onDispose { }
  }
}

@Composable
fun ColumnScope.ToolbarSimpleTitleMiddlePart(toolbarState: MangaloidToolbarViewModel.ToolbarState) {
  toolbarState.title?.let { toolbarTitle ->
    val textSize = if (toolbarState.subtitle.isNullOrEmpty()) {
      26.sp
    } else {
      19.sp
    }

    Text(
      text = toolbarTitle,
      color = Color.White,
      fontSize = textSize,
      style = Typography.h3,
      modifier = Modifier
        .wrapContentWidth()
        .wrapContentHeight()
    )
  }

  toolbarState.subtitle?.let { toolbarSubtitle ->
    Text(
      text = toolbarSubtitle,
      color = Color.White,
      fontSize = 12.sp,
      style = Typography.subtitle2,
      modifier = Modifier
        .wrapContentWidth()
        .wrapContentHeight()
    )
  }
}

@Composable
fun RowScope.PositionToolbarButton(
  toolbarButton: MangaloidToolbarViewModel.ToolbarButton?,
  onToolbarButtonClicked: (MangaloidToolbarViewModel.ToolbarButtonId) -> Unit
) {
  if (toolbarButton == null) {
    return
  }

  return when (toolbarButton) {
    is MangaloidToolbarViewModel.ToolbarButton.BackArrow,
    is MangaloidToolbarViewModel.ToolbarButton.HamburgMenu,
    is MangaloidToolbarViewModel.ToolbarButton.MangaSearchButton,
    is MangaloidToolbarViewModel.ToolbarButton.MangaChapterSearchButton,
    is MangaloidToolbarViewModel.ToolbarButton.ClearSearchButton -> {
      Spacer(modifier = Modifier.width(4.dp))

      Image(
        painter = painterResource(id = toolbarButton.iconDrawableId),
        contentDescription = toolbarButton.contentDescription,
        modifier = Modifier
          .clickable { onToolbarButtonClicked(toolbarButton.toolbarButtonId) }
          .fillMaxHeight()
          .width(32.dp)
          .align(Alignment.CenterVertically)
      )

      Spacer(modifier = Modifier.width(4.dp))

      Unit
    }
  }
}
