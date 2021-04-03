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
          MangaloidToolbarViewModel.TOOLBAR_BUTTON_BACK_ARROW -> {
            navController.popBackStack()
          }
          MangaloidToolbarViewModel.TOOLBAR_BUTTON_SEARCH -> {
            toolbarViewModel.pushToolbarState()
            toolbarViewModel.updateToolbarDoNotTouchStack { searchToolbar() }
          }
          MangaloidToolbarViewModel.TOOLBAR_BUTTON_CLOSE_SEARCH -> {
            toolbarViewModel.popToolbarState()
          }
          MangaloidToolbarViewModel.TOOLBAR_BUTTON_CLEAR_SEARCH -> {
            toolbarViewModel.popToolbarState()
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
  onToolbarButtonClicked: (Int) -> Unit
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
  var textState by remember { mutableStateOf(TextFieldValue(toolbarState.searchQuery ?: "")) }
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
      toolbarViewModel.updateToolbarDoNotTouchStack { copy(searchQuery = changedText.text) }
      textState = changedText
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
  onToolbarButtonClicked: (Int) -> Unit
) {
  when (toolbarButton) {
    is MangaloidToolbarViewModel.ToolbarButton.BackArrow,
    is MangaloidToolbarViewModel.ToolbarButton.SearchButton,
    is MangaloidToolbarViewModel.ToolbarButton.ClearSearchButton -> {
      Spacer(modifier = Modifier.width(4.dp))

      Image(
        painter = painterResource(id = toolbarButton.iconDrawable),
        contentDescription = toolbarButton.contentDescription,
        modifier = Modifier
          .clickable { onToolbarButtonClicked(toolbarButton.id) }
          .fillMaxHeight()
          .width(32.dp)
          .align(Alignment.CenterVertically)
      )

      Spacer(modifier = Modifier.width(4.dp))
    }
    null -> {
      // no-op
    }
  }
}
