package org.aryamahasangh

import AppTheme
import androidx.compose.runtime.*
import org.aryamahasangh.auth.SessionManager
import org.aryamahasangh.di.KoinInitializer
import org.aryamahasangh.navigation.AppDrawer

// CompositionLocal for Authentication State
val LocalIsAuthenticated = compositionLocalOf { false }

// Initialize Koin for dependency injection
private val initKoin by lazy {
  KoinInitializer.init()
  true // Return a value to satisfy the lazy property
}


@Composable
fun App() {
  val demo = false
  if (!demo) {

    // Ensure Koin is initialized
    initKoin

    // Initialize session management when app starts
    LaunchedEffect(Unit) {
      SessionManager.initialize()
    }

    // Observe authentication state
    val isAuthenticated by SessionManager.isAuthenticated.collectAsState(initial = false)

    AppTheme {
      // Provide authentication state to the entire app
      CompositionLocalProvider(LocalIsAuthenticated provides isAuthenticated) {
        // Always show AppDrawer - login is optional
        AppDrawer()
      }
    }
  } else {
    //ImagePickerExample()
    //ActivityFormImagePickerIntegration()
//    DragAndDropSample()
    //DNDWithCursor()
  }
}



