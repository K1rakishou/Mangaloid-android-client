package com.github.mangaloid.client.core.extension

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.model.data.Manga
import okhttp3.HttpUrl

abstract class AbstractMangaExtension {
  abstract val mangaExtensionId: ExtensionId
  abstract val name: String
  abstract val icon: HttpUrl

  abstract suspend fun loadCatalogManga(): ModularResult<List<Manga>>
}