package com.github.mangaloid.client.util

import android.os.Build

object AndroidUtils {

  fun isAndroid11(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
  }

  fun isAndroid10(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
  }

  fun isAndroidO(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
  }

  fun isAndroidL_MR1(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
  }

  fun isAndroidP(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
  }

  fun isAndroidM(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
  }

  fun isAndroidN(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
  }

  fun isAndroidNMR1(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
  }

}