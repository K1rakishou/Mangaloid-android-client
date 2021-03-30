package com.github.mangaloid.client.model.data.local

import com.github.mangaloid.client.di.DependenciesGraph
import okhttp3.HttpUrl

data class MangaChapter(
  val chapterId: MangaChapterId,
  val mangaIpfsId: MangaIpfsId,
  val chapterTitle: String,
  val pages: Int
) {

  fun chapterCoverUrl(chapterPagesUrl: HttpUrl = DependenciesGraph.chapterPagesEndpoint): HttpUrl {
    return chapterPagesUrl.newBuilder()
      .addEncodedPathSegment(mangaIpfsId.id)
      // TODO(hardcoded): 3/29/2021: For now it's impossible to know page's image extension and
      //  there are no chapter covers.
      .addEncodedPathSegment("1.jpg")
      .build()
  }

}