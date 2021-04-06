package com.github.mangaloid.client.di

import android.content.Context
import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.core.cache.CacheHandler
import com.github.mangaloid.client.core.cache.CacheHandlerSynchronizer
import com.github.mangaloid.client.core.extension.MangaExtensionManager
import com.github.mangaloid.client.core.page_loader.MangaPageLoader
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.core.extension.mangaloid.MangaloidRemoteSource
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.model.cache.MangaCache
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object DependenciesGraph {
  lateinit var appContext: Context
  lateinit var okHttpClient: OkHttpClient
  lateinit var moshi: Moshi

  private val verboseLogs = true
  private val cacheDir by lazy { appContext.getCacheDir() }
  val appCoroutineScope = CoroutineScope(Dispatchers.Main)

  val mangaloidRemoteSource by lazy {
    MangaloidRemoteSource(
      moshi = moshi,
      okHttpClient = okHttpClient
    )
  }

  val mangaExtensionManager by lazy { MangaExtensionManager() }

  private val cacheHandlerSynchronizer by lazy { CacheHandlerSynchronizer() }
  private val mangaCache by lazy { MangaCache() }

  val appSettings by lazy { AppSettings(appContext) }

  val mangaRepository by lazy {
    MangaRepository(
      mangaCache = mangaCache,
      mangaExtensionManager = mangaExtensionManager
    )
  }

  val mangaPageLoader by lazy {
    MangaPageLoader(
      appScope = appCoroutineScope,
      appSettings = appSettings,
      cacheHandler = cacheHandler,
      okHttpClient = okHttpClient
    )
  }

  val cacheHandler by lazy {
    CacheHandler(
      appContext = appContext,
      appScope = appCoroutineScope,
      cacheHandlerSynchronizer = cacheHandlerSynchronizer,
      verboseLogs = verboseLogs,
      cacheDirFile = File(cacheDir, AppConstants.fileCacheDir),
      chunksCacheDirFile = File(cacheDir, AppConstants.fileChunksCacheDir),
    )
  }

  fun init(context: Context) {
    appContext = context

    okHttpClient = OkHttpClient.Builder()
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .connectTimeout(20, TimeUnit.SECONDS)
      .build()

    moshi = Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

}