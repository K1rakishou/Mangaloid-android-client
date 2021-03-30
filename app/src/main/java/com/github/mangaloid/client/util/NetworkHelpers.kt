package com.github.mangaloid.client.util

import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.core.ModularResult.Companion.Try
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun OkHttpClient.suspendCall(request: Request): Response {
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

suspend inline fun <reified T> OkHttpClient.suspendConvertIntoJsonObject(
  request: Request,
  moshi: Moshi,
  adapterFunc: () -> JsonAdapter<T> = { moshi.adapter<T>(T::class.java) }
): ModularResult<T> {
  return Try {
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

class HttpError(val status: Int) : Exception("Bad response status: $status")