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
import com.github.mangaloid.client.core.extension.ExtensionId
import com.github.mangaloid.client.model.data.MangaId
import com.github.mangaloid.client.screens.chapters.ChaptersScreen
import com.github.mangaloid.client.screens.reader.ReaderActivity
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbar
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel

@Composable
fun MainActivityRouter(extensionId: ExtensionId) {
  val toolbarViewModel: MangaloidToolbarViewModel = viewModel()
  val navController = rememberNavController()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { MangaloidToolbar(navController, toolbarViewModel) },
    content = { MainActivityRouterContent(extensionId, navController, toolbarViewModel) }
  )
}

@Composable
fun MainActivityRouterContent(
  extensionId: ExtensionId,
  navController: NavHostController,
  toolbarViewModel: MangaloidToolbarViewModel
) {
  NavHost(navController = navController, startDestination = "main") {
    composable(route = "main") {
      MainScreen(
        extensionId = extensionId,
        toolbarViewModel = toolbarViewModel,
        onMangaClicked = { clickedManga ->
          navController.navigate("chapters/${clickedManga.mangaId.id}")
        }
      )
    }

    composable(
      route = "chapters/{${MangaId.MANGA_ID_KEY}}",
      arguments = listOf(navArgument(MangaId.MANGA_ID_KEY) { type = NavType.IntType })
    ) { backstackEntry ->
      val mangaIdParam = MangaId.fromRawValueOrNull(backstackEntry.arguments?.getInt(MangaId.MANGA_ID_KEY))
      val extensionIdParam = ExtensionId.fromRawValueOrNull(backstackEntry.arguments?.getInt(ExtensionId.EXTENSION_ID_KEY))

      requireNotNull(mangaIdParam) { "MangaId must not be null" }
      requireNotNull(extensionIdParam) { "ExtensionId must not be null" }

      val context = LocalContext.current

      ChaptersScreen(
        extensionId = extensionIdParam,
        mangaId = mangaIdParam,
        toolbarViewModel = toolbarViewModel,
        onMangaChapterClicked = { clickedMangaId, clickedMangaChapterId ->
          ReaderActivity.launch(
            context = context,
            extensionId = extensionIdParam,
            mangaId = clickedMangaId,
            mangaChapterId = clickedMangaChapterId
          )
        }
      )
    }
  }
}