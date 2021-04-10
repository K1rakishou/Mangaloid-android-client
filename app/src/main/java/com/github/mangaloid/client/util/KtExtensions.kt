package com.github.mangaloid.client.util

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.internal.and
import okio.*
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.InputStream

fun safeCapacity(initialCapacity: Int): Int {
  return if (initialCapacity < 16) {
    16
  } else {
    initialCapacity
  }
}

inline fun <T> mutableListWithCap(initialCapacity: Int): MutableList<T> {
  return ArrayList(safeCapacity(initialCapacity))
}

inline fun <T> mutableListWithCap(collection: Collection<*>): MutableList<T> {
  return ArrayList(safeCapacity(collection.size))
}

inline fun <K, V> mutableMapWithCap(initialCapacity: Int): MutableMap<K, V> {
  return HashMap(safeCapacity(initialCapacity))
}

inline fun <K, V> mutableMapWithCap(collection: Collection<*>): MutableMap<K, V> {
  return HashMap(safeCapacity(collection.size))
}

inline fun <K, V> linkedMapWithCap(initialCapacity: Int): LinkedHashMap<K, V> {
  return LinkedHashMap(safeCapacity(initialCapacity))
}

inline fun <K, V> linkedMapWithCap(collection: Collection<*>): LinkedHashMap<K, V> {
  return LinkedHashMap(safeCapacity(collection.size))
}

inline fun <T> mutableSetWithCap(initialCapacity: Int): HashSet<T> {
  return HashSet(safeCapacity(initialCapacity))
}

inline fun <T> mutableSetWithCap(collection: Collection<*>): HashSet<T> {
  return HashSet(safeCapacity(collection.size))
}

@SuppressLint("DefaultLocale")
fun Long.toReadableFileSize(): String {
  // Nice stack overflow copy-paste, but it's been updated to be more correct
  // https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
  val s = if (this < 0) {
    "-"
  } else {
    ""
  }

  var b = if (this == Long.MIN_VALUE) {
    Long.MAX_VALUE
  } else {
    Math.abs(this)
  }

  return when {
    b < 1000L -> "$this B"
    b < 999950L -> String.format("%s%.1f kB", s, b / 1e3)
    1000.let { b /= it; b } < 999950L -> String.format("%s%.1f MB", s, b / 1e3)
    1000.let { b /= it; b } < 999950L -> String.format("%s%.1f GB", s, b / 1e3)
    1000.let { b /= it; b } < 999950L -> String.format("%s%.1f TB", s, b / 1e3)
    1000.let { b /= it; b } < 999950L -> String.format("%s%.1f PB", s, b / 1e3)
    else -> String.format("%s%.1f EB", s, b / 1e6)
  }
}

fun String.extractFileNameExtension(): String? {
  val index = this.lastIndexOf('.')
  if (index == -1) {
    return null
  }

  return this.substring(index + 1)
}

fun String.removeExtensionFromFileName(): String {
  val index = this.lastIndexOf('.')
  if (index == -1) {
    return this
  }

  return this.substring(0, index)
}

fun Int.toByteArray(): ByteArray {
  return byteArrayOf(
    (this ushr 24).toByte(),
    (this ushr 16).toByte(),
    (this ushr 8).toByte(),
    this.toByte()
  )
}

fun Int.toCharArray(): CharArray {
  return charArrayOf(
    (this ushr 24).toChar(),
    (this ushr 16).toChar(),
    (this ushr 8).toChar(),
    this.toChar()
  )
}

fun ByteArray.toInt(): Int {
  return (this[0] and 0xFF) shl 24 or
    ((this[1] and 0xFF) shl 16) or
    ((this[2] and 0xFF) shl 8) or
    ((this[3] and 0xFF) shl 0)
}

fun CharArray.toInt(): Int {
  return (this[0].toByte() and 0xFF) shl 24 or
    ((this[1].toByte() and 0xFF) shl 16) or
    ((this[2].toByte() and 0xFF) shl 8) or
    ((this[3].toByte() and 0xFF) shl 0)
}

fun File.hash(): String? {
  if (!this.exists()) {
    return null
  }

  return this.inputStream().hash()
}

fun InputStream.hash(): String {
  return HashingSource.md5(this.source()).use { hashingSource ->
    return@use hashingSource.buffer().use { source ->
      source.readAll(blackholeSink())
      return@use hashingSource.hash.hex()
    }
  }
}

fun String.hash(): String {
  return this.encodeUtf8().md5().hex()
}

fun stringsHash(inputStrings: Collection<String>): String {
  return HashingSink.sha256(blackholeSink()).use { hashingSink ->
    hashingSink.buffer().outputStream().use { outputStream ->
      inputStrings.forEach { inputString -> inputString.encodeUtf8().write(outputStream) }
    }

    return@use hashingSink.hash.hex()
  }
}

fun ByteArray.sha256HexString(): String {
  return this.toByteString(0, this.size).sha256().hex()
}

suspend fun <T> Mutex.withLockNonCancellable(owner: Any? = null, action: suspend () -> T): T {
  return withContext(NonCancellable) { withLock(owner) { action.invoke() } }
}

fun Throwable.errorMessageOrClassName(): String {
  if (!message.isNullOrBlank()) {
    return message!!
  }

  return this::class.java.name
}

fun View.setVisibilityFast(newVisibility: Int) {
  if (visibility != newVisibility) {
    visibility = newVisibility
  }
}

fun View.updateMargins(
  left: Int? = null,
  right: Int? = null,
  start: Int? = null,
  end: Int? = null,
  top: Int? = null,
  bottom: Int? = null
) {
  val layoutParams = layoutParams as? ViewGroup.MarginLayoutParams
    ?: return

  val newLeft = left ?: layoutParams.leftMargin
  val newRight = right ?: layoutParams.rightMargin
  val newStart = start ?: layoutParams.marginStart
  val newEnd = end ?: layoutParams.marginEnd
  val newTop = top ?: layoutParams.topMargin
  val newBottom = bottom ?: layoutParams.bottomMargin

  layoutParams.setMargins(
    newLeft,
    newTop,
    newRight,
    newBottom
  )

  layoutParams.marginStart = newStart
  layoutParams.marginEnd = newEnd
}

fun <K, V> MutableMap<K, V>.putIfNotExists(key: K, value: V) {
  if (!containsKey(key)) {
    put(key, value)
  }
}

fun HttpUrl.Builder.addEncodedQueryParameterIfNotEmpty(encodedName: String, encodedValue: String): HttpUrl.Builder {
  if (encodedValue.isNotEmpty()) {
    return addEncodedQueryParameter(encodedName, encodedValue)
  }

  return this
}

suspend fun <State> MutableStateFlow<State>.updateState(stateUpdater: suspend State.() -> State) {
  this.value = stateUpdater(this.value)
}