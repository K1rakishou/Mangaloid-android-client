package com.github.mangaloid.client.helper

import android.view.GestureDetector
import android.view.MotionEvent

class SimpleTapListener(
  private val onTap: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

  override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
    onTap()
    return true
  }

}