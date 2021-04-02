package com.github.mangaloid.client.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.screens.chapters.ChaptersScreen
import com.github.mangaloid.client.screens.reader.ReaderActivity
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbar
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel

@Composable
fun MainActivityRouter() {
  val toolbarViewModel: MangaloidToolbarViewModel = viewModel()
  val navController = rememberNavController()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { MangaloidToolbar(navController, toolbarViewModel) },
    content = { MainActivityRouterContent(navController, toolbarViewModel) }
  )
}

@Composable
fun MainActivityRouterContent(
  navController: NavHostController,
  toolbarViewModel: MangaloidToolbarViewModel
) {
  NavHost(navController = navController, startDestination = "main") {
    composable(route = "main") {
      MainScreen(
        toolbarViewModel = toolbarViewModel,
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
        toolbarViewModel = toolbarViewModel,
        onMangaChapterClicked = { clickedMangaId, clickedMangaChapterId ->
          ReaderActivity.launch(context, clickedMangaId, clickedMangaChapterId)
        }
      )
    }
  }
}