package com.github.mangaloid.client

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

object Graph {
  lateinit var okHttpClient: OkHttpClient

  val mangaRepository by lazy { MangaRepository() }

  fun init(context: Context) {
    okHttpClient = OkHttpClient.Builder()
      .cache(Cache(File(context.cacheDir, "okhttp_cache"), 32 * 1024 * 1024))
      .build()
  }

}