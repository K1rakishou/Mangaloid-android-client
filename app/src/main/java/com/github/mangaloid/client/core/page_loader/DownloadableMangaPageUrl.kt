package com.github.mangaloid.client.core.page_loader

import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.MangaChapterId
import com.github.mangaloid.client.model.data.MangaId
import okhttp3.HttpUrl

data class DownloadableMangaPageUrl(
  val extensionId: ExtensionId,
  val mangaId: MangaId,
  val chapterId: MangaChapterId,
  val url: HttpUrl,
  val currentPage: Int,
  val totalPages: Int,
  // For next chapter preloading
  val nextChapterId: MangaChapterId?
) {

  fun sliceNextPages(count: Int): List<Int> {
    val start = currentPage + 1
    val end = (start + count).coerceAtMost(totalPages)

    return (start until (end + 1)).map { pageIndex -> pageIndex }
  }

  fun debugDownloadableMangaPageId(): String {
    return "${extensionId.rawId}-${mangaId.id}-${chapterId.id}-${currentPage}/${totalPages}"
  }

}