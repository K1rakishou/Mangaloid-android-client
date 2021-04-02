package com.github.mangaloid.client.util

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.ViewGroup
import androidx.compose.ui.unit.Dp

import java.io.File

object AndroidUtils {
  lateinit var appContext: Context

  val mpMpLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT
  )

  val mpWpLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
  )

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

  fun getAvailableSpaceInBytes(file: File): Long {
    val stat = StatFs(file.path)
    return stat.availableBlocksLong * stat.blockSizeLong
  }

  fun Int.toDp(): Dp {
    return Dp(this.toFloat() / (appContext.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT.toFloat()))
  }

}