package com.aryamahasangh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javafx.application.Platform as JavaFXPlatform

private var javaFXInitialized = false

private fun initJavaFX() {
  if (!javaFXInitialized) {
    javaFXInitialized = true
    // Initialize JavaFX toolkit
    SwingUtilities.invokeLater {
      JFXPanel() // This initializes JavaFX
    }
  }
}

@Composable
actual fun WebView(
  url: String,
  onScriptResult: ((String) -> Unit)?
) {
  initJavaFX()

  val jfxPanel = remember { JFXPanel() }

  Box(modifier = Modifier.fillMaxSize()) {
    SwingPanel(
      factory = {
        val panel = JPanel(java.awt.BorderLayout()).apply {
          add(jfxPanel, java.awt.BorderLayout.CENTER)
        }

        JavaFXPlatform.runLater {
          val webView = WebView()
          if (onScriptResult != null) {
            webView.engine.setOnAlert { event ->
              onScriptResult(event.data)
            }
          }
          webView.engine.loadContent(url, "text/html")
          jfxPanel.scene = Scene(webView)
        }

        panel
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}

// Function to get selected location script
fun getSelectedLocationScript(defaultLat: Double, defaultLng: Double): String = """
    const location = selectedLocation || { lat: $defaultLat, lng: $defaultLng };
    alert(JSON.stringify(location));
""".trimIndent()
