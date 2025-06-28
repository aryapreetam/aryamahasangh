package org.aryamahasangh.gallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.aryamahasangh.gallery.screens.ButtonsGalleryScreen
import org.aryamahasangh.gallery.screens.GalleryHomeScreen

/**
 * Main Gallery composable that provides navigation between different component showcases
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gallery() {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = "home"
  ) {
    composable("home") {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
          TopAppBar(
            title = {
              Text(
                text = "UI Components Gallery",
                style = MaterialTheme.typography.titleLarge
              )
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      ) { paddingValues ->
        GalleryHomeScreen(
          onNavigateToButtons = { navController.navigate("buttons") },
          modifier = Modifier.padding(paddingValues)
        )
      }
    }

    composable("buttons") {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
          TopAppBar(
            title = {
              Text(
                text = "Buttons",
                style = MaterialTheme.typography.titleLarge
              )
            },
            navigationIcon = {
              IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Go back"
                )
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }
      ) { paddingValues ->
        ButtonsGalleryScreen(modifier = Modifier.padding(paddingValues))
      }
    }
  }
}
