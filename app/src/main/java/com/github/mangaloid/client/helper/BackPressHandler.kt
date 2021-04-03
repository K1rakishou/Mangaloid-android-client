package com.github.mangaloid.client.helper

import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.*

@Composable
fun BackPressHandler(onBackPressed: () -> Boolean) {
  val currentOnBackPressed by rememberUpdatedState(onBackPressed)

  val backCallback = remember {
    object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (currentOnBackPressed()) {
          return
        }

        isEnabled = false
        remove()
      }
    }
  }

  val backDispatcher = LocalBackPressedDispatcher.current

  DisposableEffect(backDispatcher) {
    backDispatcher.addCallback(backCallback)
    onDispose { backCallback.remove() }
  }
}

val LocalBackPressedDispatcher =
  staticCompositionLocalOf<OnBackPressedDispatcher> { error("No Back Dispatcher provided") }