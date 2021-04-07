package com.github.mangaloid.client.core.page_loader

import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import okhttp3.HttpUrl

data class DownloadableMangaPage(
  val extensionId: ExtensionId,
  val mangaId: MangaId,
  val chapterId: MangaChapterId,
  val pageFileName: String,
  val pageFileSize: Long,
  val url: HttpUrl,
  // zero-based
  val currentPage: Int,
  // non-zero based
  val pageCount: Int,
  // For next chapter preloading
  val nextChapterId: MangaChapterId?
) {

  fun sliceNextPages(count: Int): List<Int> {
    val start = currentPage + 1
    val end = (start + count).coerceAtMost(pageCount)

    return (start until end).map { pageIndex -> pageIndex }
  }

  fun debugDownloadableMangaPageId(): String {
    return "E:${extensionId.id}-M:${mangaId.id}-C:${chapterId.id}-(cp:${currentPage}/pc:${pageCount})-(${url}/${pageFileName})"
  }

}