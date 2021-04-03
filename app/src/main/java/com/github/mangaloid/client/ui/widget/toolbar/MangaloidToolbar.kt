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
import com.github.mangaloid.client.util.AndroidUtils.toDp
import dev.chrisbanes.accompanist.insets.LocalWindowInsets
import dev.chrisbanes.accompanist.insets.statusBarsPadding

private val toolbarHeightDp = 48.dp

@Composable
fun MangaloidToolbar(
  navController: NavHostController,
  toolbarViewModel: MangaloidToolbarViewModel
) {
  val toolbarHeight = LocalWindowInsets.current.statusBars.top.toDp() + toolbarHeightDp
  val toolbarState by toolbarViewModel.stateViewable.collectAsState()

  if (toolbarState.toolbarType == MangaloidToolbarViewModel.ToolbarType.Uninitialized) {
    return
  }

  BackPressHandler {
    toolbarViewModel.popToolbarStateToRoot()
    return@BackPressHandler false
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(toolbarHeight)
      .background(MaterialTheme.colors.primary)
      .statusBarsPadding()
  ) {
    ToolbarContent(
      toolbarViewModel = toolbarViewModel,
      toolbarState = toolbarState,
      onToolbarButtonClicked = { toolbarButtonId ->
        when (toolbarButtonId) {
          MangaloidToolbarViewModel.ToolbarButtonId.ToolbarButtonBackArrow -> {
            navController.popBackStack()
          }
          MangaloidToolbarViewModel.ToolbarButtonId.ToolbarButtonMangaSearch -> {
            toolbarViewModel.pushToolbarState()
            toolbarViewModel.updateToolbarDoNotTouchStack {
              searchToolbar(MangaloidToolbarViewModel.SearchType.MangaSearch)
            }
          }
          MangaloidToolbarViewModel.ToolbarButtonId.ToolbarButtonMangaChapterSearch -> {
            toolbarViewModel.pushToolbarState()
            toolbarViewModel.updateToolbarDoNotTouchStack {
              searchToolbar(MangaloidToolbarViewModel.SearchType.MangaChapterSearch)
            }
          }
          MangaloidToolbarViewModel.ToolbarButtonId.ToolbarButtonCloseSearch -> {
            toolbarViewModel.popToolbarState()
          }
          MangaloidToolbarViewModel.ToolbarButtonId.ToolbarButtonClearSearch -> {
            toolbarViewModel.updateToolbarDoNotTouchStack {
              copy(searchInfo = searchInfo?.copy(query = ""))
            }
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
  Row(modifier = Modifier
    .fillMaxSize()
    .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    PositionToolbarButton(toolbarState.leftButton, onToolbarButtonClicked)
    
    when (toolbarState.toolbarType) {
      MangaloidToolbarViewModel.ToolbarType.Uninitialized -> {
        // no-op
      }
      MangaloidToolbarViewModel.ToolbarType.MainToolbar,
      MangaloidToolbarViewModel.ToolbarType.ChaptersToolbar -> {
        Column(modifier = Modifier
          .fillMaxHeight()
          .wrapContentWidth()
        ) {
          ToolbarSimpleTitleMiddlePart(toolbarState)
        }
      }
      MangaloidToolbarViewModel.ToolbarType.SearchToolbar -> {
        Row(modifier = Modifier
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
      24.sp
    } else {
      19.sp
    }

    Text(text = toolbarTitle,
      color = Color.White,
      fontSize = textSize,
      style = Typography.h3,
      modifier = Modifier
        .wrapContentWidth()
        .wrapContentHeight()
    )
  }

  toolbarState.subtitle?.let { toolbarSubtitle ->
    Text(text = toolbarSubtitle,
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

  when (toolbarButton) {
    is MangaloidToolbarViewModel.ToolbarButton.BackArrow,
    is MangaloidToolbarViewModel.ToolbarButton.MangaSearchButton,
    is MangaloidToolbarViewModel.ToolbarButton.MangaChapterSearchButton,
    is MangaloidToolbarViewModel.ToolbarButton.ClearSearchButton -> {
      Spacer(modifier = Modifier.width(4.dp))

      Image(
        painter = painterResource(id = toolbarButton.iconDrawable),
        contentDescription = toolbarButton.contentDescription,
        modifier = Modifier
          .clickable { onToolbarButtonClicked(toolbarButton.toolbarButtonId) }
          .fillMaxHeight()
          .width(32.dp)
          .align(Alignment.CenterVertically)
      )

      Spacer(modifier = Modifier.width(4.dp))
    }
  }
}
