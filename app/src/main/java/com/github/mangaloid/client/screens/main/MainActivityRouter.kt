package com.github.mangaloid.client.screens.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.github.mangaloid.client.model.data.local.MangaId
import com.github.mangaloid.client.screens.chapters.ChaptersScreen
import com.github.mangaloid.client.screens.reader.ReaderActivity

@Composable
fun MainActivityRouter() {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = "main") {
    composable(route = "main") {
      MainScreen(
        onMangaClicked = { clickedManga ->
          navController.navigate("chapters/${clickedManga.mangaId.id}")
        })
    }

    composable(
      route = "chapters/{${MangaId.MANGA_ID_KEY}}",
      arguments = listOf(navArgument(MangaId.MANGA_ID_KEY) { type = NavType.IntType })
    ) { backstackEntry ->
      val mangaId = MangaId.fromRawValueOrNull(backstackEntry.arguments?.getInt(MangaId.MANGA_ID_KEY))
      requireNotNull(mangaId) { "MangaId must not be null" }
      val context = LocalContext.current

      ChaptersScreen(
        mangaId = mangaId,
        onMangaChapterClicked = { clickedMangaId, clickedMangaChapterId ->
          ReaderActivity.launch(context, clickedMangaId, clickedMangaChapterId)
        }
      )
    }
  }

}