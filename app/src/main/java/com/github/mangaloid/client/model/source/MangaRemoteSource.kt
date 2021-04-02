package com.github.mangaloid.client.model.source

import com.github.mangaloid.client.core.ModularResult
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.suspendConvertIntoJsonObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.joda.time.format.DateTimeFormat


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

        val chapters = mangaRemote.chapters.mapNotNull { mangaChapterRemote ->
          // TODO: 4/1/2021 some chapters have no cid
          val cid = mangaChapterRemote.cid
            ?: return@mapNotNull null

          // TODO: 4/1/2021 some chapters have no date
          val date = mangaChapterRemote.date
            ?: return@mapNotNull null

          val chapterId = MangaChapterId(mangaChapterRemote.no)

          return@mapNotNull MangaChapter(
            chapterId = chapterId,
            mangaChapterIpfsId =  MangaChapterIpfsId(cid),
            title = mangaChapterRemote.title,
            group = mangaChapterRemote.group,
            date = MANGA_CHAPTER_DATE_PARSER.parseDateTime(date),
            pages = mangaChapterRemote.pages,
            mangaChapterMeta = MangaChapterMeta(
              chapterId = chapterId,
              lastViewedPageIndex = 0
            )
          )
        }

        return@mapNotNull Manga(
          mangaId = mangaId,
          title = mangaRemote.title,
          chapters = chapters
        )
      }
    }
  }

  companion object {
    private val MANGA_CHAPTER_DATE_PARSER = DateTimeFormat.forPattern("MM-dd-yyyy")
  }

}