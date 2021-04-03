package com.github.mangaloid.client.screens.reader

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.mangaloid.client.R

@SuppressLint("ViewConstructor")
class PageLoadingErrorView(
  context: Context,
  private val error: Throwable,
  private val onRetryClicked: () -> Unit
) : ConstraintLayout(context) {
  init {
    inflate(context, R.layout.reader_screen_page_loading_error_view, this)

    val errorTextView = findViewById<TextView>(R.id.error_text)
    errorTextView.setText(error.toString())

    val retryButton = findViewById<AppCompatButton>(R.id.retry_button)
    retryButton.setOnClickListener {
      onRetryClicked()
      retryButton.isEnabled = false
    }
  }

}