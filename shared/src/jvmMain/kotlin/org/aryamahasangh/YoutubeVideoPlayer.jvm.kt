package org.aryamahasangh

import androidx.compose.runtime.Composable
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState

@Composable
actual fun YoutubeVideoPlayer(videoUrl: String) {
  val state = rememberWebViewState("https://www.youtube.com/embed/$videoUrl")
  WebView(state)
}