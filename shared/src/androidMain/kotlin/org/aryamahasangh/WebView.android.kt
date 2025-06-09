package org.aryamahasangh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState

@Composable
actual fun WebView(url: String, onScriptResult: ((String) -> Unit)?) {
  WebView(
    modifier = Modifier.fillMaxSize(),
    state = rememberWebViewState(url),
    onCreated = { it.settings.javaScriptEnabled = true }
  )
}
