package com.aryamahasangh

import androidx.compose.ui.window.ComposeUIViewController
import com.aryamahasangh.config.ConfigInitializer

fun MainViewController() =
  ComposeUIViewController {
    // Initialize cross-platform configuration
    ConfigInitializer.initializeBlocking()

    App()
  }
