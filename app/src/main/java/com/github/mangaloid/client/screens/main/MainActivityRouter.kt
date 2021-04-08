package com.github.mangaloid.client.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import com.github.mangaloid.client.model.data.MangaDescriptor
import com.github.mangaloid.client.screens.chapters.ChaptersScreen
import com.github.mangaloid.client.screens.reader.ReaderActivity
import com.github.mangaloid.client.ui.widget.drawer.MangaloidDrawer
import com.github.mangaloid.client.ui.widget.drawer.MangaloidDrawerViewModel
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbar
import com.github.mangaloid.client.ui.widget.toolbar.MangaloidToolbarViewModel
import dev.chrisbanes.accompanist.insets.navigationBarsPadding

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
      Box(modifier = Modifier.navigationBarsPadding()) {
        MainScreen(
          toolbarViewModel = toolbarViewModel,
          drawerViewModel = drawerViewModel,
          onMangaClicked = { clickedManga ->
            navController.currentBackStackEntry
              ?.arguments
              ?.putParcelable(MangaDescriptor.KEY, clickedManga.mangaDescriptor)

            navController.navigate("chapters")
          }
        )
      }
    }

    composable(route = "chapters") {
      val mangaDescriptor = navController.previousBackStackEntry
        ?.arguments
        ?.getParcelable<MangaDescriptor>(MangaDescriptor.KEY)
      requireNotNull(mangaDescriptor) { "MangaDescriptor must not be null" }

      val context = LocalContext.current

      Box(modifier = Modifier.navigationBarsPadding()) {
        ChaptersScreen(
          mangaDescriptor = mangaDescriptor,
          toolbarViewModel = toolbarViewModel,
          onMangaChapterClicked = { mangaChapterDescriptor ->
            ReaderActivity.launch(
              context = context,
              mangaChapterDescriptor = mangaChapterDescriptor
            )
          }
        )
      }
    }
  }
}