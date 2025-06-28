package org.aryamahasangh.gallery

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.aryamahasangh.gallery.Gallery

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "UI Components Gallery",
    state = rememberWindowState(width = 1200.dp, height = 800.dp)
  ) {
    Gallery()
  }
}
