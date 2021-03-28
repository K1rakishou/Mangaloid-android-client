package com.github.mangaloid.client.model

inline class MangaId(val id: Int) {
  companion object {

    fun fromRawValueOrNull(value: Int): MangaId? {
      if (value < 0) {
        return null
      }

      return MangaId(value)
    }

  }
}

inline class MangaIpfsId(val id: String)