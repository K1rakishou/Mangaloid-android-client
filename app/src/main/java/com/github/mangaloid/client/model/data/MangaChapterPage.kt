package com.github.mangaloid.client.model.data

import okhttp3.HttpUrl

data class MangaChapterPage(
  val mangaChapterPageDescriptor: MangaChapterPageDescriptor,
  val pageFileName: String,
  val pageFileSize: Long,
  val url: HttpUrl,
  // non-zero based
  val pageCount: Int,
  // For next chapter preloading
  val nextChapterId: MangaChapterId?
) {
  val extensionId: ExtensionId
    get() = mangaChapterPageDescriptor.extensionId
  val mangaId: MangaId
    get() = mangaChapterPageDescriptor.mangaId
  val mangaChapterId: MangaChapterId
    get() = mangaChapterPageDescriptor.mangaChapterId

  // zero-based
  val currentPage: Int
    get() = mangaChapterPageDescriptor.mangaPageIndex

  fun sliceNextPages(count: Int): List<Int> {
    val start = currentPage + 1
    val end = (start + count).coerceAtMost(pageCount)

    return (start until end).map { pageIndex -> pageIndex }
  }

  fun debugMangaPageId(): String {
    return "E:${extensionId.id}-M:${mangaId.id}-C:${mangaChapterId.id}-(cp:${currentPage}/pc:${pageCount})-(${url}/${pageFileName})"
  }

}