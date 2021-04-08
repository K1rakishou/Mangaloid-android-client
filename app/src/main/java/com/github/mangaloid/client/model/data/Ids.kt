package com.github.mangaloid.client.model.data


enum class ExtensionId(val id: Int) {
  Mangaloid(0);

  companion object {
    fun fromRawValueOrNull(rawId: Int?): ExtensionId? {
      if (rawId == null || rawId < 0) {
        return null
      }

      return values().firstOrNull { extensionId -> extensionId.id == rawId }
    }
  }
}

inline class MangaId(val id: Int) {

  companion object {
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
    fun fromRawValueOrNull(value: Int?): MangaChapterId? {
      if (value == null || value < 0) {
        return null
      }

      return MangaChapterId(value)
    }
  }
}

inline class MangaChapterIpfsId(val ipfsId: String)