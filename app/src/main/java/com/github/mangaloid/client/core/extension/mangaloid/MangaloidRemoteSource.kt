package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.core.extension.data.IpfsDirectoryObjectLinkSortable
import com.github.mangaloid.client.core.extension.data.MangaChapterIpfsDirectory
import com.github.mangaloid.client.core.page_loader.DownloadableMangaPage
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.HttpError
import com.github.mangaloid.client.util.addEncodedQueryParameterIfNotEmpty
import com.github.mangaloid.client.util.suspendConvertIntoJsonObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.joda.time.DateTime


class MangaloidRemoteSource(
  private val moshi: Moshi,
  private val okHttpClient: OkHttpClient
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

      val foundMangaRemote = okHttpClient.suspendConvertIntoJsonObject<List<MangaloidMangaRemote>>(
        request = searchMangaRequest,
        moshi = moshi,
        adapterFunc = { mangaRemoteListAdapter }
      ).unwrap()

      if (foundMangaRemote == null) {
        return@Try emptyList()
      }

      return@Try foundMangaRemote.map { mangaloidMangaRemote ->
        return@map MangaloidMapper.mangaRemoteToManga(
          extensionId = extensionId,
          mangaRemote = mangaloidMangaRemote,
          mangaChapters = emptyList(),
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

      val mangaRemote = try {
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

      if (mangaRemote == null) {
        return@Try null
      }

      return@Try MangaloidMapper.mangaRemoteToManga(
        extensionId = extensionId,
        mangaRemote = mangaRemote,
        mangaChapters = emptyList(),
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

      val mangaChaptersRemote = try {
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

      if (mangaChaptersRemote == null) {
        return@Try emptyList()
      }

      return@Try mangaChaptersRemote.mapIndexedNotNull { index, mangaloidMangaChapterRemote ->
        val prevChapterId = mangaChaptersRemote.getOrNull(index - 1)?.chapterNo?.let { chapterNo -> MangaChapterId(chapterNo) }
        val chapterId = MangaChapterId(mangaloidMangaChapterRemote.chapterNo)
        val nextChapterId = mangaChaptersRemote.getOrNull(index + 1)?.chapterNo?.let { chapterNo -> MangaChapterId(chapterNo) }

        return@mapIndexedNotNull MangaChapter(
          extensionId = extensionId,
          mangaId = mangaId,
          prevChapterId = prevChapterId,
          chapterId = chapterId,
          nextChapterId = nextChapterId,
          mangaChapterIpfsId = MangaChapterIpfsId(mangaloidMangaChapterRemote.ipfsLink),
          title = mangaloidMangaChapterRemote.title,
          groupId = mangaloidMangaChapterRemote.groupId,
          chapterPostfix = mangaloidMangaChapterRemote.chapterPostfix,
          ordinal = mangaloidMangaChapterRemote.ordinal,
          version = mangaloidMangaChapterRemote.version,
          languageId = mangaloidMangaChapterRemote.languageId,
//          dateAdded = DateTime(mangaloidMangaChapterRemote.dateAdded), // TODO: 4/7/2021: uncomment me once it's fixed on the server side
          dateAdded = DateTime(),
          pageCount = mangaloidMangaChapterRemote.pageCount,
          mangaChapterMeta = MangaChapterMeta(
            chapterId = chapterId,
            lastViewedPageIndex = null // TODO: 4/5/2021 load this from the DB
          )
        )
      }
    }
  }

  suspend fun getMangaChapterPages(
    mangaChapter: MangaChapter,
    getChapterPagesByChapterIpfsIdEndpointUrl: HttpUrl,
    chapterPagesUrl: HttpUrl
  ): ModularResult<List<DownloadableMangaPage>> {
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

      return@Try ipfsDirectoryObjectLinksSorted.mapIndexed { index, ipfsDirectoryObjectLink ->
        val pageFullUrl = chapterPagesUrl.newBuilder()
          .addEncodedPathSegment(ipfsDirectoryObjectLink.fileIpfsHash)
          .build()

        return@mapIndexed DownloadableMangaPage(
          extensionId = mangaChapter.extensionId,
          mangaId = mangaChapter.mangaId,
          chapterId = mangaChapter.chapterId,
          pageFileName = ipfsDirectoryObjectLink.fileName,
          pageFileSize = ipfsDirectoryObjectLink.fileSize,
          url = pageFullUrl,
          currentPage = index,
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