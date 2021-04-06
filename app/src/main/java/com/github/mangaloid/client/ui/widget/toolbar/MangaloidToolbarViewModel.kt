package com.github.mangaloid.client.ui.widget.toolbar

import androidx.lifecycle.viewModelScope
import com.github.mangaloid.client.core.ViewModelWithState
import com.github.mangaloid.client.core.coroutine_executor.SerializedCoroutineExecutor
import kotlinx.coroutines.flow.*
import java.util.*

class MangaloidToolbarViewModel : ViewModelWithState<ToolbarState>(ToolbarState.default()) {
  private val toolbarStateStack = Stack<ToolbarState>()
  private val toolbarCoroutineExecutor = SerializedCoroutineExecutor(viewModelScope)

  fun updateToolbar(updater: ToolbarState.() -> ToolbarState) {
    toolbarCoroutineExecutor.post {
      popToolbarStateToRoot()
      updateState { updater(stateViewable.value) }
    }
  }

  fun updateToolbarDoNotTouchStack(updater: ToolbarState.() -> ToolbarState) {
    toolbarCoroutineExecutor.post { updateState { updater(stateViewable.value) } }
  }

  fun pushToolbarState() {
    toolbarCoroutineExecutor.post { toolbarStateStack.push(stateViewable.value.copy()) }
  }

  fun popToolbarState() {
    toolbarCoroutineExecutor.post { updateState { toolbarStateStack.pop() } }
  }

  fun popToolbarStateToRoot(): Boolean {
    if (toolbarStateStack.isEmpty()) {
      return false
    }

    toolbarCoroutineExecutor.post {
      updateState {
        var toolbarState = toolbarStateStack.pop()

        while (!toolbarStateStack.isEmpty()) {
          toolbarState = toolbarStateStack.pop()
        }

        return@updateState toolbarState
      }
    }

    return true
  }
}