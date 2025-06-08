package org.aryamahasangh

import AppTheme
import androidx.compose.runtime.Composable
import org.aryamahasangh.di.KoinInitializer
import org.aryamahasangh.navigation.AppDrawer
import org.jetbrains.compose.ui.tooling.preview.Preview

// Initialize Koin for dependency injection
private val initKoin by lazy {
  KoinInitializer.init()
  true // Return a value to satisfy the lazy property
}

@Composable
@Preview
fun App() {
  // Ensure Koin is initialized
  initKoin

  AppTheme {
    AppDrawer()
  }
}

