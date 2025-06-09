package org.aryamahasangh

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewStateWithHTMLData

@SuppressLint("JavascriptInterface")
@Composable
actual fun WebView(url: String, onScriptResult: ((String) -> Unit)?) {
  val webViewState = rememberWebViewStateWithHTMLData(data = url, encoding = "UTF-8", mimeType = "text/html")
  val locationBridge = remember { LocationBridge(onScriptResult) }

  WebView(
    modifier = Modifier.fillMaxSize(),
    state = webViewState,
    onCreated = { webView ->
      webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
      }
      
      // Add JavaScript interface
      webView.addJavascriptInterface(locationBridge, "AndroidLocationBridge")

      // No need to override window.postMessage since we're using direct bridge calls
    }
  )
}

private class LocationBridge(private val onScriptResult: ((String) -> Unit)?) {
  @JavascriptInterface
  fun onLocationUpdate(data: String) {
    try {
      onScriptResult?.invoke(data)
    } catch (e: Exception) {
      println( "Error in location bridge: ${e.message}" )
    }
  }
}
