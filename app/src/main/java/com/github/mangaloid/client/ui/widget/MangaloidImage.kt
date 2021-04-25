package com.github.mangaloid.client.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.github.mangaloid.client.R
import com.github.mangaloid.client.ui.widget.manga.CircularProgressIndicatorWidget
import com.github.mangaloid.client.util.errorMessageOrClassName
import com.google.accompanist.coil.CoilImage
import com.google.accompanist.imageloading.ImageLoadState

@Composable
fun MangaloidImage(data: Any, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
  CoilImage(
    data = data,
    contentDescription = null,
    contentScale = contentScale,
    loading = { CircularProgressIndicatorWidget() },
    error = { error -> MangaloidImageError(error) },
    modifier = modifier
  )
}

@Composable
fun BoxScope.MangaloidImageError(error: ImageLoadState.Error) {
  Column(modifier = Modifier
    .wrapContentWidth()
    .wrapContentHeight()
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_baseline_warning_24),
      contentDescription = null,
      modifier = Modifier.align(Alignment.CenterHorizontally),
      tint = Color.Black
    )

    Text(
      text = error.throwable.errorMessageOrClassName(),
      modifier = Modifier.align(Alignment.CenterHorizontally)
    )
  }
}
