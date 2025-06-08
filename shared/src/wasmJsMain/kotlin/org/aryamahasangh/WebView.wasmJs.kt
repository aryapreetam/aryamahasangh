package org.aryamahasangh

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

@Composable
actual fun WebView() {
  //window.location.href = "https://www.openstreetmap.org/#map=11/16.6405/74.4557"
  //embedIframe("https://www.openstreetmap.org/#map=11/16.6405/74.4557")
//  HtmlView(
//    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
//    factory = {
//      val iframe = createElement("iframe") as HTMLIFrameElement
//      iframe.setAttribute("width", "100%")
//      iframe.setAttribute("height", "100%")
//      iframe.setAttribute("src", "https://www.openstreetmap.org/export/embed.html?bbox=77.223%2C28.612%2C77.225%2C28.614&layer=mapnik&marker=28.613%2C77.224")
//      iframe
//    }
//  )

  showIframeOverlayOpenStreetMap(28.613, 77.224)
}

fun showIframeOverlayOpenStreetMap(lat: Double, lon: Double, zoom: Int = 18) {
  val delta = 0.001  // controls zoom area
  val lon1 = lon - delta
  val lat1 = lat - delta
  val lon2 = lon + delta
  val lat2 = lat + delta

  val embedUrl = "https://www.openstreetmap.org/export/embed.html?" +
    "bbox=$lon1,$lat1,$lon2,$lat2" +
    "&layer=mapnik&marker=$lat,$lon"

  val iframe = document.createElement("iframe") as HTMLIFrameElement
  iframe.id = "iframe-overlay"
  iframe.src = embedUrl
  iframe.style.padding = "16px"
  iframe.style.border = "16px"
  iframe.style.position = "absolute"
  iframe.style.width = "100vw"
  iframe.style.height = "100vh"
  iframe.style.zIndex = "9999"
  iframe.style.background = "transparent"

  document.body?.appendChild(iframe)

  //return iframe
}
