package com.github.mangaloid.client.core.extension.mangaloid

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaloidMangaRemote(
  @Json(name = "id") val id: Int,
  @Json(name = "type") val type: String,
  @Json(name = "titles") val titles: List<String>,
  @Json(name = "artists") val artists: List<String>,
  @Json(name = "authors") val authors: List<String>,
  @Json(name = "genres") val genres: List<String>,
  @Json(name = "country_of_origin") val countryOfOrigin: String,
  @Json(name = "publication_status") val publicationStatus: String,
  @Json(name = "mal_id") val malId: Int,
  @Json(name = "anilist_id") val anilistId: Int,
  @Json(name = "mangaupdates_id") val mangaUpdatesId: Int
)

@JsonClass(generateAdapter = true)
data class MangaloidMangaChapterRemote(
  @Json(name = "id") val id: Int,
  @Json(name = "manga_id") val mangaId: Int,
  @Json(name = "chapter_no") val chapterNo: Int,
  @Json(name = "chapter_postfix") val chapterPostfix: String,
  @Json(name = "ordinal") val ordinal: Int?,
  @Json(name = "title") val title: String,
  @Json(name = "page_count") val pageCount: Int,
  @Json(name = "version") val version: Int,
  @Json(name = "language_id") val languageId: String,
  @Json(name = "group_id") val groupId: Int?,
//  @Json(name = "date_added") val dateAdded: Int, // TODO: 4/7/2021: uncomment me once it's fixed on the server side
  @Json(name = "ipfs_link") val ipfsLink: String,
)