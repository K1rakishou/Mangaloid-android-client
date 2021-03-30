package com.github.mangaloid.client

import android.app.Application
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.util.Logger

class MangaloidApp : Application() {

  override fun onCreate() {
    super.onCreate()

    Thread.currentThread().setUncaughtExceptionHandler { t, e ->
      Logger.e(Logger.APP_TAG, "Unhandled exception in thread ${t.name}", e)
      System.exit(-1)
    }

    DependenciesGraph.init(this)
  }
}