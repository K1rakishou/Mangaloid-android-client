package com.github.mangaloid.client.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
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
import com.github.mangaloid.client.ui.widget.drawer.MangaloidDrawer
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbar
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel
import com.github.mangaloid.client.ui.widget.drawer.MangaloidDrawerViewModel

@Composable
fun MainActivityRouter() {
  val toolbarViewModel: MangaloidToolbarViewModel = viewModel()
  val drawerViewModel: MangaloidDrawerViewModel = viewModel()

  val navController = rememberNavController()
  val scaffoldState = rememberScaffoldState()

  HandleDrawerOpenCloseState(
    drawerViewModel = drawerViewModel,
    scaffoldState = scaffoldState
  )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    scaffoldState = scaffoldState,
    topBar = {
      MangaloidToolbar(
        navController = navController,
        toolbarViewModel = toolbarViewModel,
        drawerViewModel = drawerViewModel
      )
    },
    drawerContent = { MangaloidDrawer() },
    content = {
      MainActivityRouterContent(
        navController = navController,
        toolbarViewModel = toolbarViewModel,
        drawerViewModel = drawerViewModel
      )
    }
  )
}

@Composable
private fun HandleDrawerOpenCloseState(
  drawerViewModel: MangaloidDrawerViewModel,
  scaffoldState: ScaffoldState
) {
  val openDrawer by drawerViewModel.listenForDrawerOpenCloseState().collectAsState()
  if (!openDrawer) {
    return
  }

  LaunchedEffect(Unit) {
    scaffoldState.drawerState.open()
    drawerViewModel.resetDrawerOpenCloseState()
  }
}

@Composable
fun MainActivityRouterContent(
  navController: NavHostController,
  toolbarViewModel: MangaloidToolbarViewModel,
  drawerViewModel: MangaloidDrawerViewModel
) {
  NavHost(navController = navController, startDestination = "main") {
    composable(route = "main") {
      MainScreen(
        toolbarViewModel = toolbarViewModel,
        drawerViewModel = drawerViewModel,
        onMangaClicked = { clickedManga -> navController.navigate("chapters/${clickedManga.mangaId.id}") }
      )
    }

    composable(
      route = "chapters/{${MangaId.MANGA_ID_KEY}}",
      arguments = listOf(navArgument(MangaId.MANGA_ID_KEY) { type = NavType.IntType })
    ) { backstackEntry ->
      val mangaIdParam =
        MangaId.fromRawValueOrNull(backstackEntry.arguments?.getInt(MangaId.MANGA_ID_KEY))
      val extensionIdParam =
        ExtensionId.fromRawValueOrNull(backstackEntry.arguments?.getInt(ExtensionId.EXTENSION_ID_KEY))

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