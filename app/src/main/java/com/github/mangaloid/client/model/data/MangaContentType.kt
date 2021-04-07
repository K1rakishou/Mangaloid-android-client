package com.github.mangaloid.client.model.data

enum class MangaContentType(val type: String) {
  Manga("Manga"),
  Webtoon("Webtoon");

  companion object {
    fun fromRawValue(typeRaw: String): MangaContentType {
      return values()
        .firstOrNull { mangaContentType -> mangaContentType.type.equals(typeRaw, ignoreCase = true) }
        ?: Manga
    }
  }
}