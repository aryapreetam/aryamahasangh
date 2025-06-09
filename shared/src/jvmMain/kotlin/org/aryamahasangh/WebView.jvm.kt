package org.aryamahasangh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
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
    var isJavaFXReady by remember { mutableStateOf(false) }
    val jfxPanel = remember { JFXPanel() }
    val webViewRef = remember { mutableStateOf<javafx.scene.web.WebView?>(null) }

    // Initialize JavaFX in a controlled manner
    LaunchedEffect(Unit) {
        if (!isJavaFXReady) {
            thread(start = true) {
                try {
                    // Initialize on EDT
                    SwingUtilities.invokeAndWait {
                        try {
                            JavaFXPlatform.runLater {
                                try {
                                    val webView = javafx.scene.web.WebView()
                                    webViewRef.value = webView
                                    if (onScriptResult != null) {
                                        webView.engine.setOnAlert { event ->
                                            onScriptResult(event.data)
                                        }
                                    }
                                    webView.engine.loadContent(url, "text/html")
                                    jfxPanel.scene = Scene(webView)
                                    isJavaFXReady = true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Update content when URL changes
    LaunchedEffect(url) {
        webViewRef.value?.let { webView ->
            JavaFXPlatform.runLater {
                try {
                    webView.engine.loadContent(url, "text/html")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwingPanel(
            factory = {
                JPanel(java.awt.BorderLayout()).apply {
                    add(jfxPanel, java.awt.BorderLayout.CENTER)
                }
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
