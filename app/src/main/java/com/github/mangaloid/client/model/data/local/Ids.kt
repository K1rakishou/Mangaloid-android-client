package com.github.mangaloid.client.model.data.local

inline class MangaId(val id: Int) {

  companion object {
    const val MANGA_ID_KEY = "manga_id"

    fun fromRawValueOrNull(value: Int?): MangaId? {
      if (value == null || value < 0) {
        return null
      }

      return MangaId(value)
    }
  }
}

inline class MangaChapterId(val id: Int) {
  companion object {
    const val MANGA_CHAPTER_ID_KEY = "manga_chapter_id"

    fun fromRawValueOrNull(value: Int?): MangaChapterId? {
      if (value == null || value < 0) {
        return null
      }

      return MangaChapterId(value)
    }
  }
}

inline class MangaIpfsId(val id: String)