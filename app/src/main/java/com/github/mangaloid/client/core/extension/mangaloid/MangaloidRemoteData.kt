package com.github.mangaloid.client.core.extension.mangaloid

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaloidMangaRemote(
  @Json(name = "id") val id: Int,
  @Json(name = "title") val title: String,
  @Json(name = "chapters") val chapters: List<MangaloidMangaChapterRemote>,
)

@JsonClass(generateAdapter = true)
data class MangaloidMangaChapterRemote(
  @Json(name = "no") val no: Int,
  @Json(name = "cid") val cid: String?,
  @Json(name = "title") val title: String,
  @Json(name = "group") val group: String,
  @Json(name = "date") val date: String?,
  @Json(name = "pages") val pages: Int
)