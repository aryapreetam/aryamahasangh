package org.aryamahasangh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.util.*
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
actual fun WebView(url: String) {
  initJavaFX()

  // Force new WebView instance every time by using a fresh key
  val uniqueKey = remember(url, System.currentTimeMillis()) { UUID.randomUUID().toString() }

  Box(modifier = Modifier.fillMaxSize()) {
    key(uniqueKey) {
      SwingPanel(
        factory = {
          val jfxPanel = JFXPanel()
          val panel = JPanel(java.awt.BorderLayout()).apply {
            add(jfxPanel, java.awt.BorderLayout.CENTER)
          }

          JavaFXPlatform.runLater {
            val webView = WebView()
            webView.engine.load(url)
            jfxPanel.scene = Scene(webView)
          }

          panel
        },
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}
