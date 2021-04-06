package com.github.mangaloid.client.core


object AppConstants {
  const val preferredPageImageExtension = "jpg"
  const val fileCacheDir = "filecache"
  const val fileChunksCacheDir = "file_chunks_cache"

  fun isDevBuild(): Boolean {
    return true
  }
}