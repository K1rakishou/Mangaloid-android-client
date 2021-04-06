package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.Manga
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.di.DependenciesGraph
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class MangaloidExtension(
  private val mangaloidRemoteSource: MangaloidRemoteSource = DependenciesGraph.mangaloidRemoteSource
) : AbstractMangaExtension() {

  override val mangaExtensionId: ExtensionId
    get() = ExtensionId.Mangaloid

  override val name: String
    get() = "Mangaloid"

  override val icon: HttpUrl
    get() = "https://avatars.githubusercontent.com/u/81382042?s=200&v=4".toHttpUrl()

  val baseBackendUrl = "https://amangathing.ddns.net".toHttpUrl()
  val baseIpfsUrl = "https://ipfs.io/ipfs".toHttpUrl()

  val dbEndpoint = baseBackendUrl.newBuilder()
    .addEncodedPathSegment("db.json")
    .build()

  val coversUrl = baseBackendUrl.newBuilder()
    .addEncodedPathSegment("img")
    .addEncodedPathSegment("covers")
    .build()

  val chapterPagesUrl = baseIpfsUrl.newBuilder()
    .build()

  override suspend fun loadCatalogManga(): ModularResult<List<Manga>> {
    return mangaloidRemoteSource.loadManga(
      extensionId = mangaExtensionId,
      dbEndpoint = dbEndpoint,
      chapterPagesUrl = chapterPagesUrl,
      coversUrl = coversUrl
    )
  }

}