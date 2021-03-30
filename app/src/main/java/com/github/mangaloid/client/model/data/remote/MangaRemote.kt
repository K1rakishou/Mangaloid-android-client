package com.github.mangaloid.client.model.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaRemote(
  @Json(name = "id") val id: Int,
  @Json(name = "title") val title: String,
  @Json(name = "cid") val cid: String,
  @Json(name = "chapters") val chapters: List<MangaChapterRemote>,
)

@JsonClass(generateAdapter = true)
data class MangaChapterRemote(
  @Json(name = "pages") val pages: Int
)