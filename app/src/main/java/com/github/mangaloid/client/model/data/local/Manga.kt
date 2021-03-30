package com.github.mangaloid.client.model.data.local

import com.github.mangaloid.client.di.DependenciesGraph
import okhttp3.HttpUrl


data class Manga(
  val mangaId: MangaId,
  val mangaIpfsId: MangaIpfsId,
  val title: String,
  val description: String,
  val chapters: List<MangaChapter>
) {

  fun coverUrl(coversEndpoint: HttpUrl = DependenciesGraph.coversEndpoint): HttpUrl {
    return coversEndpoint.newBuilder()
      .addEncodedPathSegment("${mangaId.id}.jpg")
      .build()
  }

}