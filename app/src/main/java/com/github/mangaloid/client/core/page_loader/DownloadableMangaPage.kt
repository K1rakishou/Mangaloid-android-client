package com.github.mangaloid.client.core.page_loader

import com.github.mangaloid.client.model.data.ExtensionId
import com.github.mangaloid.client.model.data.MangaChapterDescriptor
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import okhttp3.HttpUrl

data class DownloadableMangaPage(
  val mangaChapterDescriptor: MangaChapterDescriptor,
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
  val extensionId: ExtensionId
    get() = mangaChapterDescriptor.mangaDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaChapterDescriptor.mangaDescriptor.mangaId
  val mangaChapterId: MangaChapterId
    get() = mangaChapterDescriptor.mangaChapterId

  fun sliceNextPages(count: Int): List<Int> {
    val start = currentPage + 1
    val end = (start + count).coerceAtMost(pageCount)

    return (start until end).map { pageIndex -> pageIndex }
  }

  fun debugDownloadableMangaPageId(): String {
    return "E:${extensionId.id}-M:${mangaId.id}-C:${mangaChapterId.id}-(cp:${currentPage}/pc:${pageCount})-(${url}/${pageFileName})"
  }

}