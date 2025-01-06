package org.aryamahasangh

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        alwaysOnTop = true,
        state = rememberWindowState(
            width = 400.dp,
            height = 800.dp,
            position = WindowPosition.Aligned(Alignment.TopEnd),
        ),
        title = "Arya Mahasangh",
    ) {
        App()
    }
}