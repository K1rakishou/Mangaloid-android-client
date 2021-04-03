package com.github.mangaloid.client.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainScreenSearch(searchQuery: String) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(
      text = "Manga search is not implemented yet (query='$searchQuery')",
      modifier = Modifier.align(Alignment.Center)
    )
  }
}