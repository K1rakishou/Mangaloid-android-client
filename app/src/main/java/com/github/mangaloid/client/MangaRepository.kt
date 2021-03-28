package com.github.mangaloid.client

import androidx.annotation.GuardedBy
import com.github.mangaloid.client.model.Manga
import com.github.mangaloid.client.model.MangaChapter
import com.github.mangaloid.client.model.MangaId
import com.github.mangaloid.client.model.MangaIpfsId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MangaRepository {
  private val mutex = Mutex()

  @GuardedBy("mutex")
  private val mangas = mapOf<MangaId, Manga>(
    MangaId(0) to Manga(
      mangaId = MangaId(0),
      mangaIpfsId = MangaIpfsId("bafybeihnoou2av5w2bzmwkl6hi25scyzz6sjwdfqp4cwq2ikf6dfmev3ta"),
      titles = listOf("Boku no Kokoro no Yabai Yatsu"),
      description = "Boku no Kokoro no Yabai Yatsu",
      chapters = listOf(
        MangaChapter(
          mangaIpfsId = MangaIpfsId("bafybeihnoou2av5w2bzmwkl6hi25scyzz6sjwdfqp4cwq2ikf6dfmev3ta"),
          chapterTitle = "Chapter 1",
          pages = 18,
        )
      )
    ),
    MangaId(1) to Manga(
      mangaId = MangaId(1),
      mangaIpfsId = MangaIpfsId("bafybeigfivshobq4h5x5qwmttgqimaufmcjl6hpjcrsedj7wxxduphp7g4"),
      titles = listOf("Otoyomegatari"),
      description = "Otoyomegatari",
      chapters = listOf(
        MangaChapter(
          mangaIpfsId = MangaIpfsId("bafybeigfivshobq4h5x5qwmttgqimaufmcjl6hpjcrsedj7wxxduphp7g4"),
          chapterTitle = "Chapter 1",
          pages = 39,
        )
      )
    ),
    MangaId(2) to Manga(
      mangaId = MangaId(2),
      mangaIpfsId = MangaIpfsId("bafybeibgnpbredeofwp364qomqpth55a6ui3oiy2ucm35fo3eimquoeob4"),
      titles = listOf("Spy X Family"),
      description = "Spy X Family",
      chapters = listOf(
        MangaChapter(
          mangaIpfsId = MangaIpfsId("bafybeibgnpbredeofwp364qomqpth55a6ui3oiy2ucm35fo3eimquoeob4"),
          chapterTitle = "Chapter 1",
          pages = 71,
        )
      )
    )
  )

  suspend fun getMangaById(mangaId: MangaId): Manga? {
    return mutex.withLock { mangas[mangaId] }
  }

}