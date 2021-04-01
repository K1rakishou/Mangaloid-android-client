package com.github.mangaloid.client.core

import okhttp3.HttpUrl.Companion.toHttpUrl

object AppConstants {
  const val preloadImagesCount = 3
  const val preferredPageImageExtension = "jpg"
  const val fileCacheDir = "filecache"
  const val fileChunksCacheDir = "file_chunks_cache"

  val baseBackendUrl = "https://amangathing.ddns.net".toHttpUrl()
  val baseIpfsUrl = "https://ipfs.io/ipfs".toHttpUrl()

  val dbEndpoint = AppConstants.baseBackendUrl.newBuilder()
    .addEncodedPathSegment("db.json")
    .build()

  val coversEndpoint = AppConstants.baseBackendUrl.newBuilder()
    .addEncodedPathSegment("img")
    .addEncodedPathSegment("covers")
    .build()

  val chapterPagesEndpoint = AppConstants.baseIpfsUrl.newBuilder()
    .build()

  fun isDevBuild(): Boolean {
    return true
  }
}