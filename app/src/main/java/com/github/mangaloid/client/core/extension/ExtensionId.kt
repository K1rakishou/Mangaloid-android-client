package com.github.mangaloid.client.core.extension

enum class ExtensionId(val id: Int) {
  Mangaloid(0);

  companion object {
    const val EXTENSION_ID_KEY = "extension_id"

    fun fromRawValueOrNull(rawId: Int?): ExtensionId? {
      if (rawId == null || rawId < 0) {
        return null
      }

      return values().firstOrNull { extensionId -> extensionId.id == rawId }
    }
  }
}