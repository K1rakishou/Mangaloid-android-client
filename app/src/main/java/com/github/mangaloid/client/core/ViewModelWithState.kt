package com.github.mangaloid.client.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ViewModelWithState<State>(
  private val defaultState: State
) : ViewModel() {
  private val _stateActual = MutableStateFlow<State>(defaultState)

  val stateViewable: StateFlow<State>
    get() = _stateActual

  protected suspend fun updateState(stateUpdater: suspend State.() -> State) {
    _stateActual.value = stateUpdater(_stateActual.value)
  }

  fun currentState(): State = _stateActual.value

}