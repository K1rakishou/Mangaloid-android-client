package com.github.mangaloid.client.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.github.mangaloid.client.R

class AsyncDataView @JvmOverloads constructor(
  context: Context,
  attributeSet: AttributeSet? = null,
  defAttrStyle: Int = 0
) : FrameLayout(context, attributeSet, defAttrStyle) {
  private val loadingView = inflate(context, R.layout.async_data_loading_view, null)
  private val errorView = inflate(context, R.layout.async_data_error_view, null)

  private var errorText = errorView.findViewById<TextView>(R.id.error_text)
  private var retryButton = errorView.findViewById<TextView>(R.id.retry_button)

  fun onTap(func: (() -> Unit)?) {
    if (func == null) {
      this.setOnClickListener(null)
      return
    }

    this.setOnClickListener { func() }
  }

  fun onErrorButtonClicked(func: (() -> Unit)?) {
    if (func == null) {
      retryButton.setOnClickListener(null)
      return
    }

    retryButton.setOnClickListener {
      func()
    }
  }

  suspend fun changeState(newState: State) {
    removeAllViews()

    when (newState) {
      State.Uninitialized -> {
        // no-op
      }
      State.Loading -> {
        addView(loadingView)
      }
      is State.Error -> {
        addView(errorView)
        errorText.text = newState.throwable.toString()
      }
      is State.Success -> {
        inflate(context, newState.resultViewId, this)
      }
    }
  }

  sealed class State {
    object Uninitialized : State()
    object Loading: State()
    class Error(val throwable: Throwable): State()
    class Success(@LayoutRes val resultViewId: Int): State()
  }

}