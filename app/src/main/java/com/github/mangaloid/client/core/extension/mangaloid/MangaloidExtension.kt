package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.AbstractMangaExtension
import com.github.mangaloid.client.model.data.MangaChapterPage
import com.github.mangaloid.client.di.DependenciesGraph
import com.github.mangaloid.client.model.data.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class MangaloidExtension(
  private val mangaloidSource: MangaloidSource = DependenciesGraph.mangaloidRemoteSource
) : AbstractMangaExtension() {

  override val extensionId: ExtensionId
    get() = ExtensionId.Mangaloid

  override val name: String
    get() = "Mangaloid"

  override val icon: HttpUrl
    get() = "https://avatars.githubusercontent.com/u/81382042?s=200&v=4".toHttpUrl()

  val baseBackendUrl = "https://testing.mangaloid.moe/".toHttpUrl()

  val baseIpfsUrl = "https://ipfs.io/ipfs".toHttpUrl()

  val getChapterPagesByChapterIpfsIdEndpointUrl = "https://ipfs.cynic.moe/api/v0/ls".toHttpUrl()

  val getMangaByIdEndpointUrl = baseBackendUrl.newBuilder()
    .addEncodedPathSegment("manga")
    .addEncodedPathSegment("from_id")
    .build()
  val getMangaChaptersByMangaIdEndpointUrl = baseBackendUrl.newBuilder()
    .addEncodedPathSegment("manga")
    .addEncodedPathSegment("get_chapters")
    .build()
  val getMangaCoverThumbnailEndpointUrl = baseBackendUrl.newBuilder()
    .addEncodedPathSegment("manga")
    .addEncodedPathSegment("thumbnail")
    .build()
  val searchMangaEndpointUrl = baseBackendUrl.newBuilder()
   .addEncodedPathSegment("manga")
   .addEncodedPathSegment("search")
   .build()

  val chapterPagesUrl = baseIpfsUrl.newBuilder()
    .build()

  override suspend fun searchForManga(
    title: String,
    author: String,
    artist: String,
    genres: List<String>
  ): ModularResult<List<Manga>> {
    return mangaloidSource.searchForManga(
      extensionId = extensionId,
      title = title,
      author = author,
      artist = artist,
      genres = genres,
      searchMangaEndpointUrl = searchMangaEndpointUrl,
      getMangaCoverThumbnailEndpointUrl = getMangaCoverThumbnailEndpointUrl
    )
  }

  override suspend fun getManga(mangaId: MangaId): ModularResult<Manga?> {
    return mangaloidSource.getMangaByMangaId(
      extensionId = extensionId,
      mangaId = mangaId,
      getMangaByIdEndpointUrl = getMangaByIdEndpointUrl,
      getMangaCoverThumbnailEndpointUrl = getMangaCoverThumbnailEndpointUrl
    )
  }

  override suspend fun getMangaChapters(mangaId: MangaId): ModularResult<List<MangaChapter>> {
    return mangaloidSource.getMangaChaptersByMangaId(
      extensionId = extensionId,
      mangaId = mangaId,
      getMangaChaptersByMangaIdEndpointUrl = getMangaChaptersByMangaIdEndpointUrl,
    )
  }

  override suspend fun getMangaChapterPages(mangaChapter: MangaChapter): ModularResult<List<MangaChapterPage>> {
    return mangaloidSource.getMangaChapterPages(
      mangaChapter = mangaChapter,
      getChapterPagesByChapterIpfsIdEndpointUrl = getChapterPagesByChapterIpfsIdEndpointUrl,
      chapterPagesUrl = chapterPagesUrl
    )
  }
}