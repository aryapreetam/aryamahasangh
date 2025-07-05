package com.aryamahasangh

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
  // No longer need ConfigInitializer - secrets are loaded automatically via KMP-Secrets-Plugin
  App()
}
