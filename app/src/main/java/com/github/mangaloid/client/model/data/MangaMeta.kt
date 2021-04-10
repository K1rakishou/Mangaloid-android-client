package com.github.mangaloid.client.model.data

data class MangaMeta(
  var databaseId: Long?,
  val mangaDescriptor: MangaDescriptor,
  var bookmarked: Boolean = false
) {
  @Synchronized
  fun hasDatabaseId(): Boolean = databaseId != null && databaseId!! >= 0L
}