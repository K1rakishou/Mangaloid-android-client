package com.github.mangaloid.client.model.data


enum class ExtensionId(val id: Long) {
  Mangaloid(0);

  companion object {
    fun fromRawValueOrNull(rawId: Long?): ExtensionId? {
      if (rawId == null || rawId < 0) {
        return null
      }

      return values().firstOrNull { extensionId -> extensionId.id == rawId }
    }
  }
}

inline class MangaId(val id: Long) {

  companion object {
    fun fromRawValueOrNull(value: Long?): MangaId? {
      if (value == null || value < 0) {
        return null
      }

      return MangaId(value)
    }
  }
}

inline class MangaChapterId(val id: Long) {
  companion object {
    fun fromRawValueOrNull(value: Long?): MangaChapterId? {
      if (value == null || value < 0) {
        return null
      }

      return MangaChapterId(value)
    }
  }
}

inline class MangaChapterIpfsId(val ipfsId: String)