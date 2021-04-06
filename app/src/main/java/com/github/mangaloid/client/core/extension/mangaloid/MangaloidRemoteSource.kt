package com.github.mangaloid.client.core.extension.mangaloid

import com.github.mangaloid.client.core.data_structure.ModularResult
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.*
import com.github.mangaloid.client.util.suspendConvertIntoJsonObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.joda.time.format.DateTimeFormat


class MangaloidRemoteSource(
  private val moshi: Moshi,
  private val okHttpClient: OkHttpClient
) {
  private val mangaRemoteListAdapter by lazy {
    return@lazy moshi.adapter<List<MangaloidMangaRemote>>(
      Types.newParameterizedType(MutableList::class.java, MangaloidMangaRemote::class.java)
    )
  }

  suspend fun loadManga(
    extensionId: ExtensionId,
    dbEndpoint: HttpUrl,
    chapterPagesUrl: HttpUrl,
    coversUrl: HttpUrl
  ): ModularResult<List<Manga>> {
    return ModularResult.Try {
      val request = Request.Builder()
        .get()
        .url(dbEndpoint)
        .build()

      val mangaList = okHttpClient.suspendConvertIntoJsonObject<List<MangaloidMangaRemote>>(
        request = request,
        moshi = moshi,
        adapterFunc = { mangaRemoteListAdapter }
      ).unwrap()

      return@Try mangaList.mapNotNull { mangaRemote ->
        val mangaId = MangaId.fromRawValueOrNull(mangaRemote.id)
          ?: return@mapNotNull null

        val chapters = mangaRemote.chapters.mapIndexedNotNull { index, mangaChapterRemote ->
          // TODO: 4/1/2021 some chapters have no cid
          val cid = mangaChapterRemote.cid
            ?: return@mapIndexedNotNull null

          // TODO: 4/1/2021 some chapters have no date
          val date = mangaChapterRemote.date ?: "01-01-2021"

          val prevChapterId = mangaRemote.chapters.getOrNull(index - 1)?.no?.let { no -> MangaChapterId(no) }
          val chapterId = MangaChapterId(mangaChapterRemote.no)
          val nextChapterId = mangaRemote.chapters.getOrNull(index + 1)?.no?.let { no -> MangaChapterId(no) }

          return@mapIndexedNotNull MangaChapter(
            extensionId = extensionId,
            ownerMangaId = mangaId,
            prevChapterId = prevChapterId,
            chapterId = chapterId,
            nextChapterId = nextChapterId,
            mangaChapterIpfsId =  MangaChapterIpfsId(cid),
            title = mangaChapterRemote.title,
            group = mangaChapterRemote.group,
            date = MANGA_CHAPTER_DATE_PARSER.parseDateTime(date),
            pages = mangaChapterRemote.pages,
            mangaChapterMeta = MangaChapterMeta(
              chapterId = chapterId,
              lastViewedPageIndex = null // TODO: 4/5/2021 load this from the DB
            ),
            chapterPagesUrl = chapterPagesUrl
          )
        }

        return@mapNotNull Manga(
          mangaId = mangaId,
          title = mangaRemote.title,
          chapters = chapters,
          coversUrl = coversUrl
        )
      }
    }
  }

  companion object {
    private val MANGA_CHAPTER_DATE_PARSER = DateTimeFormat.forPattern("MM-dd-yyyy")
  }

}