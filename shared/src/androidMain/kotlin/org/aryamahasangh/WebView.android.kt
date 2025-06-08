package org.aryamahasangh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState

@Composable
actual fun WebView() {
  WebView(
    modifier = Modifier.fillMaxSize(),
    state = rememberWebViewState("https://www.youtube.com/embed/cy6K36_OIUM"),
    onCreated = { it.settings.javaScriptEnabled = true }
  )
}
