package com.github.mangaloid.client.di

import android.content.Context
import com.github.mangaloid.client.model.repository.MangaRepository
import com.github.mangaloid.client.model.source.MangaRemoteSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

object DependenciesGraph {
  lateinit var okHttpClient: OkHttpClient
  lateinit var moshi: Moshi
  
  private val baseBackendUrl = "https://amangathing.ddns.net".toHttpUrl()
  private val baseIpfsUrl = "https://ipfs.io/ipfs".toHttpUrl()

  val appCoroutineScope = CoroutineScope(Dispatchers.Main)

  val dbEndpoint = baseBackendUrl.newBuilder()
    .addEncodedPathSegment("db.json")
    .build()
  val coversEndpoint = baseBackendUrl.newBuilder()
    .addEncodedPathSegment("img")
    .addEncodedPathSegment("covers")
    .build()
  val chapterPagesEndpoint = baseIpfsUrl.newBuilder()
    .build()

  private val mangaRemoteSource by lazy { MangaRemoteSource(moshi, okHttpClient, dbEndpoint) }
  val mangaRepository by lazy { MangaRepository(mangaRemoteSource) }

  fun init(context: Context) {
    okHttpClient = OkHttpClient.Builder()
      .build()

    moshi = Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

}