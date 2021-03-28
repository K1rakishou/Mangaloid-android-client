package com.github.mangaloid.client

import android.app.Application

class Mangaloid : Application() {

  override fun onCreate() {
    super.onCreate()

    Graph.init(this)
  }
}