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
  @Json(name = "cid") val cid: String?, // TODO: 4/5/2021 remove nullability once it's fixed on the server side
  @Json(name = "title") val title: String,
  @Json(name = "group") val group: String,
  @Json(name = "date") val date: String?, // TODO: 4/5/2021 remove nullability once it's fixed on the server side
  @Json(name = "pages") val pages: Int
)