package com.github.mangaloid.client.model.source

import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.model.data.local.*
import com.github.mangaloid.client.model.data.remote.MangaRemote
import com.github.mangaloid.client.util.suspendConvertIntoJsonObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request


class MangaRemoteSource(
  private val moshi: Moshi,
  private val okHttpClient: OkHttpClient,
  private val dbEndpoint: HttpUrl
) {
  private val mangaRemoteListAdapter by lazy {
    return@lazy moshi.adapter<List<MangaRemote>>(
      Types.newParameterizedType(MutableList::class.java, MangaRemote::class.java)
    )
  }

  suspend fun loadManga(): ModularResult<List<Manga>> {
    return ModularResult.Try {
      val request = Request.Builder()
        .get()
        .url(dbEndpoint)
        .build()

      val mangaList = okHttpClient.suspendConvertIntoJsonObject<List<MangaRemote>>(
        request = request,
        moshi = moshi,
        adapterFunc = { mangaRemoteListAdapter }
      ).unwrap()

      return@Try mangaList.mapNotNull { mangaRemote ->
        val mangaId = MangaId.fromRawValueOrNull(mangaRemote.id)
          ?: return@mapNotNull null

        val mangaIpfsId = MangaIpfsId(mangaRemote.cid)

        val chapters = mangaRemote.chapters.mapIndexed { index, mangaChapterRemote ->
          return@mapIndexed MangaChapter(
            chapterId = MangaChapterId(index),
            mangaIpfsId = mangaIpfsId,
            // TODO(hardcoded): 3/29/2021: there is no chapters in the API for now
            chapterTitle = "Chapter ${index + 1}",
            pages = mangaChapterRemote.pages
          )
        }

        return@mapNotNull Manga(
          mangaId = mangaId,
          mangaIpfsId = mangaIpfsId,
          title = mangaRemote.title,
          // TODO(hardcoded): 3/29/2021 there is no manga description in the API for now
          description = "Default description for manga ${mangaId.id}",
          chapters = chapters
        )
      }
    }
  }

}