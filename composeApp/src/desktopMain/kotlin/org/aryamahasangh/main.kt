package org.aryamahasangh

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.aryamahasangh.config.AppConfig
import java.io.File
import java.util.*

fun loadSecretsFromFile(): Map<String, String> {
    val file = File("secrets.properties")
    if (!file.exists()) {
        println("⚠️ secrets.properties not found.")
        return emptyMap()
    }

    val props = Properties().apply {
        file.inputStream().use { load(it) }
    }

    return props.entries.associate { it.key.toString() to it.value.toString() }
}

fun main() = application {
    AppConfig.init(loadSecretsFromFile())
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