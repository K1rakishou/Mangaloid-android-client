package com.github.mangaloid.client.ui.widget.toolbar

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.mangaloid.client.util.AndroidUtils.toDp
import dev.chrisbanes.accompanist.insets.LocalWindowInsets
import dev.chrisbanes.accompanist.insets.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.github.mangaloid.client.R
import com.github.mangaloid.client.ui.theme.Typography

private val toolbarHeightDp = 48.dp

@Composable
fun MangaloidToolbar(
  navController: NavHostController,
  toolbarViewModel: MangaloidToolbarViewModel
) {
  val toolbarHeight = LocalWindowInsets.current.statusBars.top.toDp() + toolbarHeightDp
  val toolbarState by toolbarViewModel.stateViewable.collectAsState()

  Surface(modifier = Modifier
    .fillMaxWidth()
    .height(toolbarHeight)
    .background(MaterialTheme.colors.primary)
    .statusBarsPadding()
  ) {
    Crossfade(targetState = toolbarState.toolbarScreen) {
      MainToolbarContent(navController, toolbarState)
    }
  }
}

@Composable
private fun MainToolbarContent(
  navController: NavHostController,
  toolbarState: MangaloidToolbarViewModel.ToolbarState
) {
  Row(modifier = Modifier
    .fillMaxSize()
    .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    when (toolbarState.leftButton) {
      is MangaloidToolbarViewModel.ToolbarButton.BackArrow -> {
        ToolbarLeftButtonArrowBack(onClicked = { navController.popBackStack() })
      }
      null -> {
        // no-op
      }
    }
    
    Column(modifier = Modifier
      .fillMaxHeight()
      .wrapContentWidth()
    ) {
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
  }
}

@Composable
fun RowScope.ToolbarLeftButtonArrowBack(onClicked: () -> Unit) {
  Spacer(modifier = Modifier.width(8.dp))

  Image(
    painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
    contentDescription = "Back",
    modifier = Modifier
      .clickable { onClicked() }
      .fillMaxHeight()
      .width(32.dp)
      .align(Alignment.CenterVertically)
  )

  Spacer(modifier = Modifier.width(8.dp))
}
