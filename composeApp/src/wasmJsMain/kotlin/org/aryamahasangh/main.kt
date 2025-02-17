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
        CompositionLocalProvider(LocalLayerContainer provides document.getElementById("components")!!) {
            App()
        }
    }
}