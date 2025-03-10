package org.aryamahasangh

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
//    ComposeViewport(document.body!!) {
//        App()
//    }
  CanvasBasedWindow(canvasElementId = "ComposeTarget") {
    val quoteContainer = document.querySelector(".quote-container")
    quoteContainer?.remove()
    CompositionLocalProvider(LocalLayerContainer provides document.getElementById("components")!!) {
      App()
    }
  }
}