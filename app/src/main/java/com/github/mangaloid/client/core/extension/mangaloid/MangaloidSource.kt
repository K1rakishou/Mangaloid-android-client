package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.data.IpfsDirectoryObjectLinkSortable
import com.github.mangaloid.client.core.extension.data.MangaChapterIpfsDirectory
import com.github.mangaloid.client.model.data.MangaChapterPage
import com.github.mangaloid.client.core.settings.AppSettings
import com.github.mangaloid.client.database.MangaloidDatabase
import com.github.mangaloid.client.database.mapper.MangaChapterMetaMapper
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.HttpError
import com.github.mangaloid.client.util.addEncodedQueryParameterIfNotEmpty
import com.github.mangaloid.client.util.mutableListWithCap
import com.github.mangaloid.client.util.suspendConvertIntoJsonObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.joda.time.DateTime


class MangaloidSource(
  private val appSettings: AppSettings,
  private val moshi: Moshi,
  private val okHttpClient: OkHttpClient,
  private val mangaloidDatabase: MangaloidDatabase
) {
  private val mangaRemoteListAdapter by lazy {
    return@lazy moshi.adapter<List<MangaloidMangaRemote>>(
      Types.newParameterizedType(MutableList::class.java, MangaloidMangaRemote::class.java)
    )
  }
  private val mangaChaptersRemoteListAdapter by lazy {
    return@lazy moshi.adapter<List<MangaloidMangaChapterRemote>>(
      Types.newParameterizedType(MutableList::class.java, MangaloidMangaChapterRemote::class.java)
    )
  }

  suspend fun searchForManga(
    extensionId: ExtensionId,
    title: String,
    author: String,
    artist: String,
    genres: List<String>,
    searchMangaEndpointUrl: HttpUrl,
    getMangaCoverThumbnailEndpointUrl: HttpUrl
  ): ModularResult<List<Manga>> {
    return ModularResult.Try {
      val fullSearchMangaUrl = searchMangaEndpointUrl.newBuilder()
        .addEncodedQueryParameterIfNotEmpty("title", title)
        .addEncodedQueryParameterIfNotEmpty("author", author)
        .addEncodedQueryParameterIfNotEmpty("artist", artist)
        .addEncodedQueryParameterIfNotEmpty("genres", genres.joinToString())
        .build()

      val searchMangaRequest = Request.Builder()
        .get()
        .url(fullSearchMangaUrl)
        .build()

      val foundMangaRemoteList = okHttpClient.suspendConvertIntoJsonObject<List<MangaloidMangaRemote>>(
        request = searchMangaRequest,
        moshi = moshi,
        adapterFunc = { mangaRemoteListAdapter }
      ).unwrap()

      if (foundMangaRemoteList == null) {
        return@Try emptyList()
      }

      return@Try foundMangaRemoteList.map { mangaloidMangaRemote ->
        val mangaId = checkNotNull(MangaId.fromRawValueOrNull(mangaloidMangaRemote.id.toLong())) {
          "Failed to convert mangaloidMangaRemote.id: ${mangaloidMangaRemote.id}"
        }

        return@map MangaloidMapper.mangaRemoteToManga(
          mangaId = mangaId,
          extensionId = extensionId,
          mangaRemote = mangaloidMangaRemote,
          coversUrl = getMangaCoverThumbnailEndpointUrl
        )
      }
    }
  }

  suspend fun getMangaByMangaId(
    extensionId: ExtensionId,
    mangaId: MangaId,
    getMangaByIdEndpointUrl: HttpUrl,
    getMangaCoverThumbnailEndpointUrl: HttpUrl
  ): ModularResult<Manga?> {
    return ModularResult.Try {
      val fullGetMangaByIdUrl = getMangaByIdEndpointUrl.newBuilder()
        .addEncodedQueryParameter("id", mangaId.id.toString())
        .build()

      val getMangaByIdRequest = Request.Builder()
        .get()
        .url(fullGetMangaByIdUrl)
        .build()

      val mangaloidMangaRemote = try {
        okHttpClient.suspendConvertIntoJsonObject<MangaloidMangaRemote>(
          request = getMangaByIdRequest,
          moshi = moshi
        ).unwrap()
      } catch (httpError: HttpError) {
        if (httpError.isNotFound) {
          return@Try null
        }

        throw httpError
      }

      if (mangaloidMangaRemote == null) {
        return@Try null
      }

      val remoteMangaId = MangaId.fromRawValueOrNull(mangaloidMangaRemote.id.toLong())
      checkNotNull(remoteMangaId) {
        "Failed to convert mangaloidMangaRemote.id: ${mangaloidMangaRemote.id}"
      }
      check(remoteMangaId.id == mangaId.id) {
        "Manga ids differ! remoteMangaId.id=${remoteMangaId.id}, mangaId.id=${mangaId.id}"
      }

      return@Try MangaloidMapper.mangaRemoteToManga(
        mangaId = mangaId,
        extensionId = extensionId,
        mangaRemote = mangaloidMangaRemote,
        coversUrl = getMangaCoverThumbnailEndpointUrl
      )
    }
  }

  suspend fun getMangaChaptersByMangaId(
    extensionId: ExtensionId,
    mangaId: MangaId,
    getMangaChaptersByMangaIdEndpointUrl: HttpUrl,
  ): ModularResult<List<MangaChapter>> {
    return ModularResult.Try {
      val fullGetMangaChaptersByMangaIdEndpointUrl = getMangaChaptersByMangaIdEndpointUrl.newBuilder()
        .addEncodedQueryParameter("id", mangaId.id.toString())
        .build()

      val getMangaChaptersByMangaIdRequest = Request.Builder()
        .get()
        .url(fullGetMangaChaptersByMangaIdEndpointUrl)
        .build()

      val mangaChapterRemoteList = try {
        okHttpClient.suspendConvertIntoJsonObject<List<MangaloidMangaChapterRemote>>(
          request = getMangaChaptersByMangaIdRequest,
          moshi = moshi,
          adapterFunc = { mangaChaptersRemoteListAdapter }
        ).unwrap()
      } catch (httpError: HttpError) {
        if (httpError.isNotFound) {
          return@Try emptyList()
        }

        throw httpError
      }

      if (mangaChapterRemoteList == null) {
        return@Try emptyList()
      }

      return@Try mangaChapterRemoteList.mapIndexed { index, mangaloidMangaChapterRemote ->
        val chapterId = MangaChapterId(mangaloidMangaChapterRemote.chapterId.toLong())
        val prevChapterId = mangaChapterRemoteList.getOrNull(index - 1)
          ?.chapterId
          ?.let { chapterNo -> MangaChapterId(chapterNo.toLong()) }
        val nextChapterId = mangaChapterRemoteList.getOrNull(index + 1)
          ?.chapterId
          ?.let { chapterNo -> MangaChapterId(chapterNo.toLong()) }

        val mangaChapterDescriptor = MangaChapterDescriptor(
          mangaDescriptor = MangaDescriptor(
            extensionId = extensionId,
            mangaId = mangaId
          ),
          mangaChapterId = chapterId
        )


        return@mapIndexed MangaChapter(
          mangaChapterDescriptor = mangaChapterDescriptor,
          prevChapterId = prevChapterId,
          nextChapterId = nextChapterId,
          mangaChapterIpfsId = MangaChapterIpfsId(mangaloidMangaChapterRemote.ipfsLink),
          title = mangaloidMangaChapterRemote.title,
          groupId = mangaloidMangaChapterRemote.groupId,
          chapterPostfix = mangaloidMangaChapterRemote.chapterPostfix,
          ordinal = mangaloidMangaChapterRemote.ordinal,
          version = mangaloidMangaChapterRemote.version,
          languageId = mangaloidMangaChapterRemote.languageId,
          dateAdded = DateTime(mangaloidMangaChapterRemote.dateAdded),
          chapterPagesCount = mangaloidMangaChapterRemote.pageCount,
          chapterPageDescriptors = mutableListWithCap(16)
        )
      }
    }
  }

  suspend fun getMangaChapterPages(
    mangaChapter: MangaChapter,
    getChapterPagesByChapterIpfsIdEndpointUrl: HttpUrl,
    chapterPagesUrl: HttpUrl
  ): ModularResult<List<MangaChapterPage>> {
    return ModularResult.Try {
      val fullGetChapterPagesByChapterIpfsIdEndpointUrl = getChapterPagesByChapterIpfsIdEndpointUrl.newBuilder()
        .addEncodedPathSegment(mangaChapter.mangaChapterIpfsId.ipfsId)
        .build()

      val getChapterPagesByChapterIpfsIdRequest = Request.Builder()
        .get()
        .url(fullGetChapterPagesByChapterIpfsIdEndpointUrl)
        .build()

      val mangaChapterIpfsDirectory = try {
        okHttpClient.suspendConvertIntoJsonObject<MangaChapterIpfsDirectory>(
          request = getChapterPagesByChapterIpfsIdRequest,
          moshi = moshi
        ).unwrap()
      } catch (httpError: HttpError) {
        if (httpError.isNotFound) {
          return@Try emptyList()
        }

        throw httpError
      }

      if (mangaChapterIpfsDirectory == null) {
        return@Try emptyList()
      }

      val ipfsDirectoryObject = mangaChapterIpfsDirectory.ipfsDirectoryObjects
        // In case if the endpoint returns multiple objects, we only need one
        .firstOrNull { ipfsDirectoryObject -> ipfsDirectoryObject.objectHash == mangaChapter.mangaChapterIpfsId.ipfsId }
        ?: return@Try emptyList()

      val ipfsDirectoryObjectLinksSorted = ipfsDirectoryObject.links
        .mapNotNull { ipfsDirectoryObjectLink -> IpfsDirectoryObjectLinkSortable.fromIpfsDirectoryObjectLink(ipfsDirectoryObjectLink) }
        .sortedBy { ipfsDirectoryObjectLinkSortable -> ipfsDirectoryObjectLinkSortable.mangaPageIndex }

      return@Try ipfsDirectoryObjectLinksSorted.mapIndexed { pageIndex, ipfsDirectoryObjectLink ->
        val pageFullUrl = chapterPagesUrl.newBuilder()
          .addEncodedPathSegment(ipfsDirectoryObjectLink.fileIpfsHash)
          .build()

        return@mapIndexed MangaChapterPage(
          mangaChapterPageDescriptor = MangaChapterPageDescriptor(mangaChapter.mangaChapterDescriptor, pageIndex),
          pageFileName = ipfsDirectoryObjectLink.fileName,
          pageFileSize = ipfsDirectoryObjectLink.fileSize,
          url = pageFullUrl,
          pageCount = ipfsDirectoryObject.links.size,
          nextChapterId = mangaChapter.nextChapterId
        )
      }
    }
  }

  companion object {
    private const val TAG = "MangaloidRemoteSource"
  }

}