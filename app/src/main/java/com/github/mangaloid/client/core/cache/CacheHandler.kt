package com.github.mangaloid.client.core.cache

import android.content.Context
import android.os.Environment
import androidx.annotation.GuardedBy
import com.github.mangaloid.client.core.AppConstants
import com.github.mangaloid.client.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.format.ISODateTimeFormat
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * CacheHandler has been re-worked a little bit because old implementation was relying on the
 * lastModified file flag which doesn't work on some Android versions/different phones. It was decided
 * to instead use a meta file for every cache file which will contain the following information:
 * 1. Time of creation of the cache file (in millis).
 * 2. A flag that indicates whether the download has been completed or not.
 *
 * We need creation time to not delete cache file for active downloads or for downloads that has
 * just been completed (otherwise the user may see a black screen instead of an image/webm). The
 * minimum cache file life time is 5 minutes. That means we won't delete any cache files (and their
 * meta files) for at least 5 minutes.
 *
 * CacheHandler now also caches file chunks that are used by [ConcurrentChunkedFileDownloader] as well
 * as all media files retrieved via [ImageLoaderV2]
 */
class CacheHandler(
  private val appContext: Context,
  private val appScope: CoroutineScope,
  private val cacheHandlerSynchronizer: CacheHandlerSynchronizer,
  private val verboseLogs: Boolean,
  private val cacheDirFile: File,
  private val chunksCacheDirFile: File
) {
  /**
   * An estimation of the current size of the directory. Used to check if trim must be run
   * because the folder exceeds the maximum size.
   */
  private val size = AtomicLong()
  private val lastTrimTime = AtomicLong(0)
  private val trimRunning = AtomicBoolean(false)
  private val recalculationRunning = AtomicBoolean(false)
  private val trimChunksRunning = AtomicBoolean(false)
  private val directoriesChecked = AtomicBoolean(false)

  // 128MB
  private val fileCacheDiskSizeBytes: Long = 128L * (1024L * 1024L)

  @GuardedBy("itself")
  private val filesOnDiskCache = mutableSetWithCap<String>(128)
  @GuardedBy("itself")
  private val fullyDownloadedFiles = mutableSetWithCap<String>(128)

  init {
    backgroundRecalculateSize()
    clearChunksCacheDir()
  }

  suspend fun getCacheFileOrNull(url: String): File? {
    createDirectories()
    val cacheFile = getCacheFileInternal(url)

    return cacheHandlerSynchronizer.withLocalLock(cacheFile.name) {
      try {
        if (!cacheFile.exists()) {
          return@withLocalLock null
        }

        if (!getCacheFileMetaInternal(url).exists()) {
          return@withLocalLock null
        }

        return@withLocalLock cacheFile
      } catch (error: IOException) {
        Logger.e(TAG, "Error while trying to get cache file", error)

        createDirectories(forced = true)
        deleteCacheFile(cacheFile.name)
        return@withLocalLock null
      }
    }
  }

  /**
   * Either returns already downloaded file or creates an empty new one on the disk (also creates
   * cache file meta with default parameters)
   * */
  suspend fun getOrCreateCacheFile(url: String): File? {
    createDirectories()
    val cacheFile = getCacheFileInternal(url)

    return cacheHandlerSynchronizer.withLocalLock(cacheFile.name) {
      try {
        if (!cacheFile.exists() && !cacheFile.createNewFile()) {
          throw IOException("Couldn't create cache file, path = ${cacheFile.absolutePath}")
        }

        val cacheFileMeta = getCacheFileMetaInternal(url)
        if (!cacheFileMeta.exists()) {
          if (!cacheFileMeta.createNewFile()) {
            throw IOException("Couldn't create cache file meta, path = ${cacheFileMeta.absolutePath}")
          }

          val result = updateCacheFileMeta(
            file = cacheFileMeta,
            overwrite = true,
            createdOn = System.currentTimeMillis(),
            fileDownloaded = false
          )

          if (!result) {
            throw IOException("Cache file meta update failed")
          }
        }

        val cacheFileName = cacheFile.name
        synchronized(filesOnDiskCache) { filesOnDiskCache.add(cacheFileName) }

        return@withLocalLock cacheFile
      } catch (error: IOException) {
        Logger.e(TAG, "Error while trying to get or create cache file", error)

        createDirectories(forced = true)
        deleteCacheFile(cacheFile.name)
        return@withLocalLock null
      }
    }
  }

  suspend fun getChunkCacheFileOrNull(chunkStart: Long, chunkEnd: Long, url: String): File? {
    val chunkCacheFile = getChunkCacheFileInternal(chunkStart, chunkEnd, url)

    return cacheHandlerSynchronizer.withLocalLock(chunkCacheFile.name) {
      if (chunkCacheFile.exists()) {
        return@withLocalLock chunkCacheFile
      }

      return@withLocalLock null
    }
  }

  suspend fun getOrCreateChunkCacheFile(chunkStart: Long, chunkEnd: Long, url: String): File? {
    val chunkCacheFile = getChunkCacheFileInternal(chunkStart, chunkEnd, url)

    return cacheHandlerSynchronizer.withLocalLock(chunkCacheFile.name) {
      if (chunkCacheFile.exists()) {
        if (!chunkCacheFile.delete()) {
          throw IOException("Couldn't delete old chunk cache file")
        }
      }

      if (!chunkCacheFile.createNewFile()) {
        throw IOException("Couldn't create new chunk cache file")
      }

      return@withLocalLock chunkCacheFile
    }
  }

  suspend fun cacheFileExists(fileUrl: String): Boolean {
    val fileName = formatCacheFileName(hashUrl(fileUrl))
    return synchronized(filesOnDiskCache) { filesOnDiskCache.contains(fileName) }
  }

  suspend fun deleteCacheFileByUrl(url: String): Boolean {
    return deleteCacheFile(hashUrl(url))
  }

  suspend fun isAlreadyDownloaded(fileUrl: String): Boolean {
    return isAlreadyDownloaded(getCacheFileInternal(fileUrl))
  }

  /**
   * Checks whether this file is already downloaded by reading it's meta info. If a file has no
   * meta info or it cannot be read - deletes the file so it can be re-downloaded again with all
   * necessary information
   *
   * [cacheFile] must be the cache file, not cache file meta!
   * */
  suspend fun isAlreadyDownloaded(cacheFile: File): Boolean {
    createDirectories()
    val cacheFileName = cacheFile.name

    return cacheHandlerSynchronizer.withLocalLock(cacheFileName) {
      try {
        val containsInCache = synchronized(fullyDownloadedFiles) { fullyDownloadedFiles.contains(cacheFileName) }
        if (containsInCache) {
          return@withLocalLock true
        }

        if (!cacheFile.exists()) {
          deleteCacheFile(cacheFile.name)
          return@withLocalLock false
        }

        if (!cacheFileName.endsWith(CACHE_EXTENSION)) {
          Logger.e(TAG, "Not a cache file! file = " + cacheFile.absolutePath)
          deleteCacheFile(cacheFile.name)
          return@withLocalLock false
        }

        val cacheFileMetaFile = getCacheFileMetaByCacheFile(cacheFile)
        if (cacheFileMetaFile == null) {
          Logger.e(TAG, "Couldn't get cache file meta by cache file, file = ${cacheFile.absolutePath}")
          deleteCacheFile(cacheFile.name)
          return@withLocalLock false
        }

        if (!cacheFileMetaFile.exists()) {
          Logger.e(TAG, "Cache file meta does not exist, cacheFileMetaFile = ${cacheFileMetaFile.absolutePath}")
          deleteCacheFile(cacheFile.name)
          return@withLocalLock false
        }

        if (cacheFileMetaFile.length() <= 0) {
          // File is empty
          deleteCacheFile(cacheFile.name)
          return@withLocalLock false
        }

        val cacheFileMeta = readCacheFileMeta(cacheFileMetaFile)
        if (cacheFileMeta == null) {
          deleteCacheFile(cacheFile.name)
          return@withLocalLock false
        }

        val isDownloaded = cacheFileMeta.isDownloaded
        if (isDownloaded) {
          synchronized(fullyDownloadedFiles) { fullyDownloadedFiles += cacheFileName }
        } else {
          synchronized(fullyDownloadedFiles) { fullyDownloadedFiles -= cacheFileName }
        }

        return@withLocalLock isDownloaded
      } catch (error: Throwable) {
        Logger.e(TAG, "Error while trying to check whether the file is already downloaded", error)
        deleteCacheFile(cacheFile.name)
        return@withLocalLock false
      }
    }
  }

  suspend fun markFileDownloaded(output: File): Boolean {
    return cacheHandlerSynchronizer.withLocalLock(output.name) {
      try {
        createDirectories()

        if (!output.exists()) {
          Logger.e(TAG, "File does not exist! file = ${output.absolutePath}")
          deleteCacheFile(output.name)
          return@withLocalLock false
        }

        val cacheFileMeta = getCacheFileMetaByCacheFile(output)
        if (cacheFileMeta == null) {
          deleteCacheFile(output.name)
          return@withLocalLock false
        }

        val updateResult = updateCacheFileMeta(
          file = cacheFileMeta,
          overwrite = false,
          createdOn = null,
          fileDownloaded = true
        )

        if (!updateResult) {
          deleteCacheFile(output.name)
        } else {
          val outputFileName = output.name
          synchronized(fullyDownloadedFiles) { fullyDownloadedFiles += outputFileName }
        }

        return@withLocalLock updateResult
      } catch (error: Throwable) {
        Logger.e(TAG, "Error while trying to mark file as downloaded", error)
        deleteCacheFile(output.name)
        return@withLocalLock false
      }
    }
  }

  fun getSize(): Long {
    return size.get()
  }

  /**
   * When a file is downloaded we add it's size to the total cache directory size variable and
   * check whether it exceeds the maximum cache size or not. If it does then the trim() operation
   * is executed in a background thread.
   * */
  suspend fun fileWasAdded(fileLen: Long) {
    val totalSize = size.addAndGet(fileLen.coerceAtLeast(0))
    val trimTime = lastTrimTime.get()
    val now = System.currentTimeMillis()

    val minTrimInterval = if (AppConstants.isDevBuild()) {
      0
    } else {
      // If the user scrolls through high-res images very fast we may end up in a situation
      // where the cache limit is hit but all the files in it were created earlier than
      // MIN_CACHE_FILE_LIFE_TIME ago. So in such case trim() will be called on EVERY
      // new opened image and since trim() is a pretty slow operation it may overload the
      // disk IO. So to avoid it we run trim() only once per MIN_TRIM_INTERVAL.
      MIN_TRIM_INTERVAL
    }

    if (AppConstants.isDevBuild()) {
      Logger.d(TAG, "fileWasAdded() fileLen=${fileLen.toReadableFileSize()}, totalSize=${totalSize.toReadableFileSize()}")
    }

    val canRunTrim = totalSize > fileCacheDiskSizeBytes
      && now - trimTime > minTrimInterval
      && trimRunning.compareAndSet(false, true)

    if (canRunTrim) {
      try {
        trim()
      } catch (e: Exception) {
        Logger.e(TAG, "trim() error", e)
      } finally {
        lastTrimTime.set(now)
        trimRunning.set(false)
      }
    }
  }

  /**
   * For now only used in developer settings. Clears the cache completely.
   * */
  suspend fun clearCache() {
    Logger.d(TAG, "Clearing cache")

    cacheHandlerSynchronizer.withGlobalLock {
      if (cacheDirFile.exists() && cacheDirFile.isDirectory) {
        for (file in cacheDirFile.listFiles() ?: emptyArray()) {
          if (!deleteCacheFile(file.name)) {
            Logger.d(TAG, "Could not delete cache file while clearing cache ${file.absolutePath}")
          }
        }
      }

      if (chunksCacheDirFile.exists() && chunksCacheDirFile.isDirectory) {
        for (file in chunksCacheDirFile.listFiles() ?: emptyArray()) {
          if (!file.delete()) {
            Logger.d(TAG, "Could not delete cache chunk file while clearing cache ${file.absolutePath}")
          }
        }
      }

      synchronized(filesOnDiskCache) { filesOnDiskCache.clear() }
      synchronized(fullyDownloadedFiles) { fullyDownloadedFiles.clear() }

      recalculateSize()
    }
  }

  /**
   * Deletes a cache file with it's meta. Also decreases the total cache size variable by the size
   * of the file.
   * */
  suspend fun deleteCacheFile(cacheFile: File): Boolean {
    return cacheHandlerSynchronizer.withLocalLock(cacheFile.name) {
      deleteCacheFile(cacheFile.name)
    }
  }

  private fun deleteCacheFile(fileName: String): Boolean {
    val originalFileName = fileName.removeExtensionFromFileName()
    if (originalFileName.isEmpty()) {
      Logger.e(TAG, "Couldn't parse original file name, fileName = $fileName")
      return false
    }

    val cacheFileName = formatCacheFileName(originalFileName)
    val cacheMetaFileName = formatCacheFileMetaName(originalFileName)

    val cacheFile = File(cacheDirFile, cacheFileName)
    val cacheMetaFile = File(cacheDirFile, cacheMetaFileName)
    val cacheFileSize = cacheFile.length()

    val deleteCacheFileResult = !cacheFile.exists() || cacheFile.delete()
    if (!deleteCacheFileResult) {
      Logger.e(TAG, "Failed to delete cache file, fileName = ${cacheFile.absolutePath}")
    }

    val deleteCacheFileMetaResult = !cacheMetaFile.exists() || cacheMetaFile.delete()
    if (!deleteCacheFileMetaResult) {
      Logger.e(TAG, "Failed to delete cache file meta = ${cacheMetaFile.absolutePath}")
    }

    synchronized(filesOnDiskCache) { filesOnDiskCache.remove(cacheFileName) }
    synchronized(fullyDownloadedFiles) { fullyDownloadedFiles.remove(cacheFileName) }

    if (deleteCacheFileResult && deleteCacheFileMetaResult) {
      val fileSize = if (cacheFileSize < 0) {
        0
      } else {
        cacheFileSize
      }

      if (fileSize > 0) {
        size.getAndAdd(-fileSize)
        if (size.get() < 0L) {
          size.set(0L)
        }

        if (verboseLogs) {
          Logger.d(
            TAG, "Deleted $cacheFileName and it's meta $cacheMetaFileName, " +
              "fileSize = ${fileSize.toReadableFileSize()}, " +
              "cache size = ${size.get().toReadableFileSize()}")
        }
      }

      return true
    }

    // Only one of the files could be deleted
    return false
  }

  private fun getCacheFileMetaByCacheFile(cacheFile: File): File? {
    val fileNameWithExtension = cacheFile.name
    if (!fileNameWithExtension.endsWith(CACHE_EXTENSION)) {
      Logger.e(TAG, "Bad file (not a cache file), file = ${cacheFile.absolutePath}")
      return null
    }

    val originalFileName = fileNameWithExtension.removeExtensionFromFileName()
    if (originalFileName.isEmpty()) {
      Logger.e(TAG, "Bad fileNameWithExtension, fileNameWithExtension = $fileNameWithExtension")
      return null
    }

    return File(cacheDirFile, formatCacheFileMetaName(originalFileName))
  }

  @Throws(IOException::class)
  private fun updateCacheFileMeta(
    file: File,
    overwrite: Boolean,
    createdOn: Long?,
    fileDownloaded: Boolean?
  ): Boolean {
    if (!file.exists()) {
      Logger.e(TAG, "Cache file meta does not exist!")
      return false
    }

    if (!file.name.endsWith(CACHE_META_EXTENSION)) {
      Logger.e(TAG, "Not a cache file meta! file = ${file.absolutePath}")
      return false
    }

    val prevCacheFileMeta = readCacheFileMeta(file).let { cacheFileMeta ->
      when {
        !overwrite && cacheFileMeta != null -> {
          require(!(createdOn == null && fileDownloaded == null)) {
            "Only one parameter may be null when updating!"
          }

          val updatedCreatedOn = createdOn ?: cacheFileMeta.createdOn
          val updatedFileDownloaded = fileDownloaded ?: cacheFileMeta.isDownloaded

          return@let CacheFileMeta(
            CURRENT_META_FILE_VERSION,
            updatedCreatedOn,
            updatedFileDownloaded
          )
        }
        else -> {
          if (createdOn == null || fileDownloaded == null) {
            throw IOException(
              "Both parameters must not be null when writing! " +
                "(Probably prevCacheFileMeta couldn't be read, check the logs)"
            )
          }

          return@let CacheFileMeta(CURRENT_META_FILE_VERSION, createdOn, fileDownloaded)
        }
      }
    }

    return file.outputStream().use { stream ->
      return@use PrintWriter(stream).use { pw ->
        val toWrite = String.format(
          Locale.ENGLISH,
          CACHE_FILE_META_CONTENT_FORMAT,
          CURRENT_META_FILE_VERSION,
          prevCacheFileMeta.createdOn,
          prevCacheFileMeta.isDownloaded
        )

        val lengthChars = toWrite.length.toCharArray()
        pw.write(lengthChars)
        pw.write(toWrite)
        pw.flush()

        return@use true
      }
    }
  }

  @Throws(IOException::class)
  private fun readCacheFileMeta(cacheFileMeta: File): CacheFileMeta? {
    if (!cacheFileMeta.exists()) {
      throw IOException("Cache file meta does not exist, path = ${cacheFileMeta.absolutePath}")
    }

    if (!cacheFileMeta.isFile()) {
      throw IOException("Input file is not a file!")
    }

    if (!cacheFileMeta.canRead()) {
      throw IOException("Couldn't read cache file meta")
    }

    if (cacheFileMeta.length() <= 0) {
      // This is a valid case
      return null
    }

    if (!cacheFileMeta.name.endsWith(CACHE_META_EXTENSION)) {
      throw IOException("Not a cache file meta! file = ${cacheFileMeta.absolutePath}")
    }

    return cacheFileMeta.reader().use { reader ->
      val lengthBuffer = CharArray(CACHE_FILE_META_HEADER_SIZE)

      var read = reader.read(lengthBuffer)
      if (read != CACHE_FILE_META_HEADER_SIZE) {
        throw IOException(
          "Couldn't read content size of cache file meta, read $read"
        )
      }

      val length = lengthBuffer.toInt()
      if (length < 0 || length > MAX_CACHE_META_SIZE) {
        throw IOException("Cache file meta is too big or negative (${length} bytes)." +
          " It was probably corrupted. Deleting it.")
      }

      val contentBuffer = CharArray(length)
      read = reader.read(contentBuffer)

      if (read != length) {
        throw IOException("Couldn't read content cache file meta, read = $read, expected = $length")
      }

      val content = String(contentBuffer)
      val split = content.split(",").toTypedArray()

      if (split.size != CacheFileMeta.PARTS_COUNT) {
        throw IOException("Couldn't split meta content ($content), split.size = ${split.size}")
      }

      val fileVersion = split[0].toInt()
      if (fileVersion != CURRENT_META_FILE_VERSION) {
        throw IOException("Bad file version: $fileVersion")
      }

      return@use CacheFileMeta(
        fileVersion,
        split[1].toLong(),
        split[2].toBoolean()
      )
    }
  }

  private fun getCacheFileInternal(url: String): File {
    createDirectories()

    val fileName = formatCacheFileName(hashUrl(url))
    return File(cacheDirFile, fileName)
  }

  private fun getChunkCacheFileInternal(
    chunkStart: Long,
    chunkEnd: Long,
    url: String
  ): File {
    createDirectories()

    val fileName = formatChunkCacheFileName(chunkStart, chunkEnd, hashUrl(url))
    return File(chunksCacheDirFile, fileName)
  }

  internal fun getCacheFileMetaInternal(url: String): File {
    createDirectories()

    // AbstractFile expects all file names to have extensions
    val fileName = formatCacheFileMetaName(hashUrl(url))
    return File(cacheDirFile, fileName)
  }

  internal fun hashUrl(url: String): String {
    return url.hash()
  }

  private fun formatChunkCacheFileName(
    chunkStart: Long,
    chunkEnd: Long,
    originalFileName: String
  ): String {
    return String.format(
      Locale.ENGLISH,
      CHUNK_CACHE_FILE_NAME_FORMAT,
      originalFileName,
      chunkStart,
      chunkEnd,
      // AbstractFile expects all file names to have extensions
      CHUNK_CACHE_EXTENSION
    )
  }

  private fun formatCacheFileName(originalFileName: String): String {
    return String.format(
      Locale.ENGLISH,
      CACHE_FILE_NAME_FORMAT,
      originalFileName,
      // AbstractFile expects all file names to have extensions
      CACHE_EXTENSION
    )
  }

  private fun formatCacheFileMetaName(originalFileName: String): String {
    return String.format(
      Locale.ENGLISH,
      CACHE_FILE_NAME_FORMAT,
      originalFileName,
      // AbstractFile expects all file names to have extensions
      CACHE_META_EXTENSION
    )
  }

  private fun createDirectories(forced: Boolean = false) {
    if (!forced && !directoriesChecked.compareAndSet(false, true)) {
      return
    }

    if (forced) {
      Logger.d(TAG, "createDirectories(forced)")
    }

    if (!cacheDirFile.exists() && !cacheDirFile.mkdirs()) {
      throw RuntimeException(
        "Unable to create file cache dir ${cacheDirFile.absolutePath}, " +
          "additional info = ${getAdditionalDebugInfo(cacheDirFile)}")
    }

    if (!chunksCacheDirFile.exists() && !chunksCacheDirFile.mkdirs()) {
      throw RuntimeException(
        "Unable to create file chunks cache dir ${chunksCacheDirFile.absolutePath}, " +
          "additional info = ${getAdditionalDebugInfo(chunksCacheDirFile)}")
    }
  }

  private fun getAdditionalDebugInfo(file: File): String {
    val state = Environment.getExternalStorageState(file)
    val externalCacheDir = appContext.externalCacheDir?.absolutePath ?: "<null>"
    val internalCacheDir = appContext.cacheDir ?: "<null>"
    val availableSpace = AndroidUtils.getAvailableSpaceInBytes(file)

    return "(exists = ${file.exists()}, " +
      "parent exists = ${file.parentFile?.exists()}, " +
      "canRead = ${file.canRead()}, " +
      "canWrite = ${file.canWrite()}, " +
      "isDirectory = ${file.isDirectory}, " +
      "state = ${state}, " +
      "availableSpace = ${availableSpace}, " +
      "externalCacheDir = ${externalCacheDir}, " +
      "internalCacheDir = ${internalCacheDir})"
  }

  private fun clearChunksCacheDir() {
    if (trimChunksRunning.compareAndSet(false, true)) {
      appScope.launch { clearChunksCacheDirInternal() }
    }
  }

  private suspend fun clearChunksCacheDirInternal() {
    if (trimChunksRunning.get()) {
      return
    }

    try {
      cacheHandlerSynchronizer.withGlobalLock {
        if (chunksCacheDirFile.exists()) {
          chunksCacheDirFile.listFiles()?.forEach { file -> file.delete() }
        }
      }
    } finally {
      trimChunksRunning.set(false)
    }
  }

  private fun backgroundRecalculateSize() {
    if (recalculationRunning.get()) {
      return
    }

    appScope.launch { recalculateSize() }
  }

  @OptIn(ExperimentalTime::class)
  private suspend fun recalculateSize() {
    var calculatedSize: Long = 0

    if (!recalculationRunning.compareAndSet(false, true)) {
      return
    }

    val time = measureTime {
      synchronized(filesOnDiskCache) { filesOnDiskCache.clear() }

      try {
        cacheHandlerSynchronizer.withGlobalLock {
          val files = cacheDirFile.listFiles() ?: emptyArray()
          for (file in files) {
            if (file.name.endsWith(CACHE_META_EXTENSION)) {
              continue
            }

            calculatedSize += file.length()
            val cacheFileName = file.name

            synchronized(filesOnDiskCache) { filesOnDiskCache.add(cacheFileName) }
          }
        }

        size.set(calculatedSize)
      } finally {
        recalculationRunning.set(false)
      }
    }

    Logger.d(
      TAG, "recalculateSize() took $time, " +
      "filesOnDiskCount=${filesOnDiskCache.size}, " +
      "fullyDownloadedFilesCount=${fullyDownloadedFiles.size}")
  }

  private suspend fun trim() {
    BackgroundUtils.ensureBackgroundThread()
    createDirectories()

    val directoryFiles = cacheHandlerSynchronizer.withGlobalLock {
      return@withGlobalLock cacheDirFile.listFiles() ?: emptyArray()
    }

    // Don't try to trim empty directories or just two files in it.
    // Two (not one) because for every cache file we now have a cache file meta with some
    // additional info.
    if (directoryFiles.size <= 2) {
      return
    }

    val start = System.currentTimeMillis()

    // LastModified doesn't work on some platforms/phones
    // (https://issuetracker.google.com/issues/36930892)
    // so we have to use a workaround. When creating a cache file for a download we also create a
    // meta file where we will put some info about this download: the main file creation time and
    // a flag that will tell us whether the download is complete or not. So now we need to parse
    // the creation time from the meta file to sort cache files in ascending order (from the
    // oldest cache file to the newest).

    var totalDeleted = 0L
    var filesDeleted = 0

    val sortedFiles = groupFilterAndSortFiles(directoryFiles)
    val now = System.currentTimeMillis()

    val currentCacheSizeToUse = if (size.get() > fileCacheDiskSizeBytes) {
      size.get()
    } else {
      fileCacheDiskSizeBytes
    }

    val sizeDiff = (size.get() - fileCacheDiskSizeBytes).coerceAtLeast(0)
    val calculatedSizeToFree = (currentCacheSizeToUse / (100f / 25f)).toLong()
    val sizeToFree = sizeDiff + calculatedSizeToFree

    Logger.d(TAG, "trim() started, " +
      "currentCacheSize=${size.get().toReadableFileSize()}, " +
      "fileCacheDiskSizeBytes=${fileCacheDiskSizeBytes.toReadableFileSize()}, " +
      "sizeToFree=${sizeToFree.toReadableFileSize()}")

    // We either delete all files we can in the cache directory or at most half of the cache
    for (cacheFile in sortedFiles) {
      val file = cacheFile.file
      val createdOn = cacheFile.createdOn

      val minCacheFileLifeTime = if (AppConstants.isDevBuild()) {
        0
      } else {
        // Do not delete fresh files because it may happen right at the time user switched
        // to it. Since the list is sorted there is no point to iterate it anymore since all
        // the following files will be "too young" to be deleted so we just break out of
        // the loop.
        MIN_CACHE_FILE_LIFE_TIME
      }

      if (now - createdOn < minCacheFileLifeTime) {
        break
      }

      if (totalDeleted >= sizeToFree) {
        break
      }

      val fileSize = file.length()

      if (deleteCacheFile(file.name)) {
        totalDeleted += fileSize
        ++filesDeleted
      }

      if (System.currentTimeMillis() - start > MAX_TRIM_TIME_MS) {
        Logger.d(TAG, "Exiting trim() early, the time bound exceeded")
        break
      }
    }

    val timeDiff = System.currentTimeMillis() - start
    recalculateSize()

    Logger.d(TAG, "trim() ended (took ${timeDiff} ms), filesDeleted=$filesDeleted, " +
      "total space freed=${totalDeleted.toReadableFileSize()}")
  }

  private suspend fun groupFilterAndSortFiles(directoryFiles: Array<File>): List<CacheFile> {
    BackgroundUtils.ensureBackgroundThread()

    val groupedCacheFiles = filterAndGroupCacheFilesWithMeta(directoryFiles)
    val cacheFiles = ArrayList<CacheFile>(groupedCacheFiles.size)

    for ((abstractFile, abstractFileMeta) in groupedCacheFiles) {
      val cacheFileMeta = try {
        readCacheFileMeta(abstractFileMeta)
      } catch (error: IOException) {
        null
      }

      if (cacheFileMeta == null) {
        Logger.e(TAG, "Couldn't read cache meta for file = ${abstractFile.absolutePath}")

        if (!deleteCacheFile(abstractFile.name)) {
          Logger.e(TAG, "Couldn't delete cache file with meta for file = ${abstractFile.absolutePath}")
        }
        continue
      }

      cacheFiles.add(CacheFile(abstractFile, cacheFileMeta))
    }

    // Sort in ascending order, the oldest files are in the beginning of the list
    Collections.sort(cacheFiles, CACHE_FILE_COMPARATOR)
    return cacheFiles
  }

  private suspend fun filterAndGroupCacheFilesWithMeta(
    directoryFiles: Array<File>
  ): List<GroupedCacheFile> {
    BackgroundUtils.ensureBackgroundThread()

    val grouped = directoryFiles
      .map { file -> Pair(file, file.name) }
      .filter { (_, fileName) ->
        // Either cache file or cache meta
        fileName.endsWith(CACHE_EXTENSION) || fileName.endsWith(CACHE_META_EXTENSION)
      }
      .groupBy { (_, fileName) -> fileName.removeExtensionFromFileName() }

    val groupedCacheFileList = mutableListWithCap<GroupedCacheFile>(grouped.size / 2)

    for ((fileName, groupOfFiles) in grouped) {
      // We have already filtered all non-cache related files so it's safe to delete them here.
      // We delete files without where either the cache file or cache file meta (or both) are
      // missing.
      if (groupOfFiles.isEmpty()) {
        deleteCacheFile(fileName)
        continue
      }

      // We also handle a hypothetical case where there are more than one cache file/meta with
      // the same name
      if (groupOfFiles.size != 2) {
        groupOfFiles.forEach { (file, _) -> deleteCacheFile(file.name) }
        continue
      }

      val (file1, fileName1) = groupOfFiles[0]
      val (file2, fileName2) = groupOfFiles[1]

      val cacheFile = when {
        fileName1.endsWith(CACHE_EXTENSION) -> file1
        fileName2.endsWith(CACHE_EXTENSION) -> file2
        else -> throw IllegalStateException(
          "Neither of grouped files is a cache file! " +
            "fileName1 = $fileName1, fileName2 = $fileName2"
        )
      }

      val cacheFileMeta = when {
        fileName1.endsWith(CACHE_META_EXTENSION) -> file1
        fileName2.endsWith(CACHE_META_EXTENSION) -> file2
        else -> throw IllegalStateException(
          "Neither of grouped files is a cache file meta! " +
            "fileName1 = $fileName1, fileName2 = $fileName2"
        )
      }

      groupedCacheFileList += GroupedCacheFile(cacheFile, cacheFileMeta)
    }

    return groupedCacheFileList
  }

  private data class GroupedCacheFile(
    val cacheFile: File,
    val cacheFileMeta: File
  )

  private class CacheFile(
    val file: File,
    private val cacheFileMeta: CacheFileMeta
  ) {

    val createdOn: Long
      get() = cacheFileMeta.createdOn

    override fun toString(): String {
      return "CacheFile{" +
        "file=${file.absolutePath}" +
        ", cacheFileMeta=${cacheFileMeta}" +
        "}"
    }

  }

  internal class CacheFileMeta(
    val version: Int = CURRENT_META_FILE_VERSION,
    val createdOn: Long,
    val isDownloaded: Boolean
  ) {

    override fun toString(): String {
      return "CacheFileMeta{" +
        "createdOn=${formatter.print(createdOn)}" +
        ", downloaded=$isDownloaded" +
        '}'
    }

    companion object {
      const val PARTS_COUNT = 3

      private val formatter = DateTimeFormatterBuilder()
        .append(ISODateTimeFormat.date())
        .appendLiteral(' ')
        .append(ISODateTimeFormat.hourMinuteSecond())
        .appendTimeZoneOffset(null, true, 2, 2)
        .toFormatter()
    }
  }

  companion object {
    private const val TAG = "CacheHandler"

    private const val CURRENT_META_FILE_VERSION = 1
    private const val CACHE_FILE_META_HEADER_SIZE = 4
    private const val MAX_TRIM_TIME_MS = 1500L

    // I don't think it will ever get this big but just in case don't forget to update it if it
    // ever gets
    private const val MAX_CACHE_META_SIZE = 1024L

    private const val CACHE_FILE_NAME_FORMAT = "%s.%s"
    private const val CHUNK_CACHE_FILE_NAME_FORMAT = "%s_%d_%d.%s"
    private const val CACHE_FILE_META_CONTENT_FORMAT = "%d,%d,%b"
    internal const val CACHE_EXTENSION = "cache"
    internal const val CACHE_META_EXTENSION = "cache_meta"
    internal const val CHUNK_CACHE_EXTENSION = "chunk"

    private val MIN_CACHE_FILE_LIFE_TIME = TimeUnit.MINUTES.toMillis(1)
    private val MIN_TRIM_INTERVAL = TimeUnit.SECONDS.toMillis(15)

    private val CACHE_FILE_COMPARATOR = Comparator<CacheFile> { cacheFile1, cacheFile2 ->
      cacheFile1.createdOn.compareTo(cacheFile2.createdOn)
    }
  }
}