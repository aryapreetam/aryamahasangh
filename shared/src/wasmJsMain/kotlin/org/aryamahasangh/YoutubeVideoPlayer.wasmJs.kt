package org.aryamahasangh

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.w3c.dom.HTMLIFrameElement

@Composable
actual fun YoutubeVideoPlayer(videoUrl: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    HtmlView(
      modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
      factory = {
        val iframe = createElement("iframe") as HTMLIFrameElement
        iframe.setAttribute("width","100%")
        iframe.setAttribute("height","100%")
        iframe.setAttribute("src","https://www.youtube.com/embed/${videoUrl}")
        iframe
      }
    )
  }
}