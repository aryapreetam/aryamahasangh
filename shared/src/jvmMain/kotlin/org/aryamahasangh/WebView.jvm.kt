package org.aryamahasangh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import javax.swing.JPanel
import javafx.application.Platform as JavaFXPlatform

private fun initJavaFX() {
  System.setProperty("prism.order", "sw")
  System.setProperty("prism.verbose", "true")
}

@Composable
actual fun WebView() {
  val url = "https://www.youtube.com/embed/cy6K36_OIUM"

  val jPanel: JPanel = remember { JPanel() }
  val jfxPanel = JFXPanel()

  Box(modifier = Modifier.fillMaxSize()) {
    SwingPanel(
      factory = {
        jfxPanel.apply { buildWebView(url, ) }
        jPanel.add(jfxPanel)
      },
      modifier = Modifier.fillMaxSize()
    )
  }
  DisposableEffect(url) { onDispose { jPanel.remove(jfxPanel) } }
}

private fun JFXPanel.buildWebView(url: String){
  initJavaFX()
  JavaFXPlatform.runLater {
    val webView = WebView()
    val webEngine = webView.engine

    webEngine.userAgent =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"

    webEngine.isJavaScriptEnabled = true
    webEngine.load(url)

    val scene = Scene(webView)
    setScene(scene)

    webEngine.loadWorker.stateProperty().addListener { _, _, newState ->
      when (newState) {
        Worker.State.SUCCEEDED -> {
          //isLoading.value = false
          println("Page loaded successfully")
        }
        Worker.State.RUNNING -> {
          //isLoading.value = true
          println("Page loaded running")
        }
        Worker.State.FAILED -> {
          //isLoading.value = false
          println("Page loaded failed")
        }
        Worker.State.READY -> println("Page loaded ready")
        Worker.State.SCHEDULED -> println("Page loaded scheduled")
        Worker.State.CANCELLED -> println("Page loaded cancelled")
      }
    }
  }
}
