package com.aryamahasangh

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() =
  application {
    // No longer need ConfigInitializer - secrets are loaded automatically via KMP-Secrets-Plugin

    Window(
      onCloseRequest = ::exitApplication,
      alwaysOnTop = true,
      state =
        rememberWindowState(
          width = 380.dp,
          height = 1200.dp,
          position = WindowPosition.Aligned(Alignment.TopEnd)
        ),
      title = "Arya Mahasangh"
    ) {
      App()
    }
  }
