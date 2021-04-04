package com.github.mangaloid.client.util

import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.core.ModularResult.Companion.Try
import com.github.mangaloid.client.core.cache.CacheHandler
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun OkHttpClient.suspendCall(request: Request): Response {
  return withContext(Dispatchers.IO) { suspendCallInternal(request) }
}

suspend inline fun <reified T> OkHttpClient.suspendConvertIntoJsonObject(
  request: Request,
  moshi: Moshi,
  crossinline adapterFunc: () -> JsonAdapter<T> = { moshi.adapter<T>(T::class.java) }
): ModularResult<T> {
  return withContext(Dispatchers.IO) {
    Try {
      val response = suspendCall(request)

      if (!response.isSuccessful) {
        throw HttpError(response.code)
      }

      val body = response.body
        ?: throw IOException("Response has no body")

      val resultObject = body.source().use { bufferedSource ->
        adapterFunc().fromJson(bufferedSource)
      }

      if (resultObject == null) {
        throw IOException("Failed to convert json into object '${T::class.java.simpleName}'")
      }

      return@Try resultObject
    }
  }
}

suspend fun OkHttpClient.suspendStoreIntoCacheIfNotCachedYet(
  cacheHandler: CacheHandler,
  url: String,
  request: Request,
  cancellationCheckFunc: () -> Boolean = { false },
  progressFunc: (suspend (Float) -> Unit)? = null
): ModularResult<File> {
  return withContext(Dispatchers.IO) {
    return@withContext Try {
      val existingCacheFile = cacheHandler.getCacheFileOrNull(url)
      if (existingCacheFile != null
        && existingCacheFile.length() > 0
        && cacheHandler.isAlreadyDownloaded(existingCacheFile)) {
        return@Try existingCacheFile
      }

      val cacheFile = cacheHandler.getOrCreateCacheFile(url)
        ?: throw IOException("Failed to get or create cache file for url: '$url'")

      val response = suspendCallInternal(request)
      if (!response.isSuccessful) {
        throw HttpError(response.code)
      }

      val body = response.body
        ?: throw IOException("Response has no body")

      try {
        body.source().inputStream().use { inputStream ->
          cacheFile.outputStream().use { outputStream ->
            copyToProgressive(
              this,
              contentLength = body.contentLength(),
              inputStream = inputStream,
              outputStream = outputStream,
              cancellationCheckFunc = cancellationCheckFunc,
              progressFunc = progressFunc
            )
          }
        }
      } catch (error: CancellationException) {
        cacheHandler.deleteCacheFile(cacheFile)
        throw error
      }

      if (cacheFile.length() == 0L) {
        cacheHandler.deleteCacheFile(cacheFile)
        throw IOException("CacheFile size is 0 after downloading")
      }

      cacheHandler.markFileDownloaded(cacheFile)
      cacheHandler.fileWasAdded(cacheFile.length())

      return@Try cacheFile
    }
  }
}

private suspend fun copyToProgressive(
  scope: CoroutineScope,
  contentLength: Long,
  inputStream: InputStream,
  outputStream: OutputStream,
  bufferSize: Int = DEFAULT_BUFFER_SIZE,
  cancellationCheckFunc: () -> Boolean,
  progressFunc: (suspend (Float) -> Unit)? = null
): Long {
  val buffer = ByteArray(bufferSize)

  if (contentLength > 0 && progressFunc != null) {
    progressFunc.invoke(0f)
  }

  var bytesCopied: Long = 0
  var bytes = inputStream.read(buffer)

  while (bytes >= 0) {
    scope.ensureActive()

    if (cancellationCheckFunc()) {
      throw CancellationException()
    }

    if (contentLength > 0 && progressFunc != null) {
      progressFunc.invoke(bytesCopied.toFloat() / contentLength.toFloat())
    }

    outputStream.write(buffer, 0, bytes)
    bytesCopied += bytes
    bytes = inputStream.read(buffer)
  }

  if (contentLength > 0 && progressFunc != null) {
    progressFunc.invoke(1f)
  }

  return bytesCopied
}

private suspend fun OkHttpClient.suspendCallInternal(request: Request): Response {
  return suspendCancellableCoroutine { continuation ->
    val call = newCall(request)

    continuation.invokeOnCancellation {
      Try { call.cancel() }.ignore()
    }

    call.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        continuation.resumeWithException(e)
      }

      override fun onResponse(call: Call, response: Response) {
        continuation.resume(response)
      }
    })
  }
}

class HttpError(val status: Int) : Exception("Bad response status: $status")