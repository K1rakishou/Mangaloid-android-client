package com.github.mangaloid.client

import android.app.Application
import com.github.mangaloid.client.di.Graph
import com.github.mangaloid.client.util.Logger

class Mangaloid : Application() {

  override fun onCreate() {
    super.onCreate()

    Thread.currentThread().setUncaughtExceptionHandler { t, e ->
      Logger.e("APP", "Unhandled exception in thread ${t.name}", e)
      System.exit(-1)
    }

    Graph.init(this)
  }
}