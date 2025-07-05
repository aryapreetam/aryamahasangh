package com.aryamahasangh

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import aryamahasangh.composeapp.generated.resources.noto_sans_devanagari
import kotlinx.browser.document
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.preloadFont

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class, InternalComposeUiApi::class)
fun main() {
  // No longer need ConfigInitializer - secrets are loaded automatically via KMP-Secrets-Plugin

  configureWebResources {
    // Overrides the resource location
    resourcePathMapping { path -> "./$path" }
  }
  CanvasBasedWindow(canvasElementId = "ComposeTarget") {
    val quoteContainer = document.querySelector(".quote-container")
    quoteContainer?.remove()

    CompositionLocalProvider(LocalLayerContainer provides document.getElementById("components")!!) {
      val fontDevanagari by preloadFont(Res.font.noto_sans_devanagari)
      if (fontDevanagari != null) {
        println("font devanagari is ready")
        App()
      } else {
        // Displays the progress indicator to address a FOUT or the app being temporarily non-functional during loading
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Image(
            modifier = Modifier.width(250.dp),
            painter = painterResource(Res.drawable.mahasangh_logo_without_background),
            contentDescription = "Arya Mahasangh logo"
          )
        }
        println("Fonts are not ready yet")
      }
    }
  }
}
