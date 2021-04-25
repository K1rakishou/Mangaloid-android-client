package com.github.mangaloid.client.ui.widget.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mangaloid.client.core.data_structure.AsyncData
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.model.data.ExtensionId
import com.github.mangaloid.client.ui.widget.MangaloidImage
import dev.chrisbanes.accompanist.insets.navigationBarsPadding
import dev.chrisbanes.accompanist.insets.statusBarsPadding

@Composable
fun MangaloidDrawer() {
  val mangaloidDrawerViewModel = viewModel<MangaloidDrawerViewModel>()
  val mangaloidDrawerState by mangaloidDrawerViewModel.stateViewable.collectAsState()

  val selectedExtension = mangaloidDrawerState.currentExtension
  if (selectedExtension == null) {
    MangaloidDrawerLoadingContent()
    return
  }

  when (val extensionsAsync = mangaloidDrawerState.extensionsAsync) {
    is AsyncData.NotInitialized -> return
    is AsyncData.Loading -> {
      // TODO: 4/4/2021
      MangaloidDrawerLoadingContent()
      return
    }
    is AsyncData.Error -> {
      // TODO: 4/4/2021 shouldn't happen but at least add logging
      return
    }
    is AsyncData.Data -> {
      MangaloidDrawerContent(
        selectedExtensionId = selectedExtension.extensionId,
        extensions = extensionsAsync.data,
        onSelected = { extensionId -> mangaloidDrawerViewModel.selectExtension(extensionId = extensionId) }
      )
    }
  }
}

@Composable
private fun MangaloidDrawerLoadingContent() {
  Box(modifier = Modifier.fillMaxSize()) {
    CircularProgressIndicator(
      modifier = Modifier
        .align(Alignment.Center)
        .size(42.dp, 42.dp)
    )
  }
}

@Composable
private fun MangaloidDrawerContent(
  selectedExtensionId: ExtensionId,
  extensions: List<AbstractMangaExtension>,
  onSelected: (ExtensionId) -> Unit
) {
  Box(
    modifier = Modifier
      .background(MaterialTheme.colors.surface)
      .navigationBarsPadding(left = false, right = false)
      .statusBarsPadding()
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize()
    ) {
      items(count = extensions.size) { index ->
        val extension = extensions[index]

        MangaloidDrawerItem(
          extension = extension,
          isSelected = extension.extensionId == selectedExtensionId,
          onSelected = onSelected
        )
      }
    }
  }
}

@Composable
private fun MangaloidDrawerItem(
  extension: AbstractMangaExtension,
  isSelected: Boolean,
  onSelected: (ExtensionId) -> Unit
) {
  val background = if (isSelected) {
    MaterialTheme.colors.primary
  } else {
    MaterialTheme.colors.surface
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .background(background)
      .clickable { onSelected(extension.extensionId) }
      .padding(horizontal = 8.dp, vertical = 2.dp)
  ) {
    MangaloidImage(
      data = extension.icon,
      modifier = Modifier
        .width(42.dp)
        .height(42.dp)
        .clip(CircleShape)
        .align(Alignment.CenterVertically),
      contentScale = ContentScale.FillBounds,
    )

    Spacer(modifier = Modifier.width(8.dp))

    Text(
      text = extension.name,
      fontSize = 20.sp,
      color = Color.White,
      modifier = Modifier
        .weight(1f)
        .align(Alignment.CenterVertically)
    )
  }
}
