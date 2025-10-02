package com.aryamahasangh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

//@Composable
//actual fun WebView(url: String) {
//  //window.location.href = "https://www.openstreetmap.org/#map=11/16.6405/74.4557"
//  //embedIframe("https://www.openstreetmap.org/#map=11/16.6405/74.4557")
//  HtmlView(
//    modifier = Modifier.fillMaxSize(),
//    factory = {
//      val iframe = createElement("iframe") as HTMLIFrameElement
//      iframe.id = "iframe-overlay"
//      iframe.src = url
////      iframe.style.padding = "16px"
////      iframe.style.border = "16px"
////      iframe.style.position = "absolute"
//      iframe.style.width = "100vw"
//      iframe.style.height = "100vh"
//      iframe.style.zIndex = "9999"
//      iframe.style.background = "transparent"
//
////      iframe.setAttribute("width", "100%")
////      iframe.setAttribute("height", "100%")
////      iframe.setAttribute("src", url)
//      iframe
//    }
//  )
//
//  // showIframeOverlayOpenStreetMap(28.613, 77.224)
//
////  val iframe = document.createElement("iframe") as HTMLIFrameElement
////  iframe.id = "iframe-overlay"
////  iframe.src = url
////  iframe.style.padding = "16px"
////  iframe.style.border = "16px"
////  iframe.style.position = "absolute"
////  iframe.style.width = "100vw"
////  iframe.style.height = "100vh"
////  iframe.style.zIndex = "9999"
////  iframe.style.background = "transparent"
////
////  document.body?.appendChild(iframe)
//}

//fun showIframeOverlayOpenStreetMap(lat: Double, lon: Double, zoom: Int = 18) {
//  val delta = 0.001  // controls zoom area
//  val lon1 = lon - delta
//  val lat1 = lat - delta
//  val lon2 = lon + delta
//  val lat2 = lat + delta
//
//  val embedUrl = "https://www.openstreetmap.org/export/embed.html?" +
//    "bbox=$lon1,$lat1,$lon2,$lat2" +
//    "&layer=mapnik&marker=$lat,$lon"
//
//  // Remove existing iframe if it exists
////  val existingIframe = document.getElementById("iframe-overlay")
////  existingIframe?.let {
////    document.body?.removeChild(it)
////  }
//
//  // Create new iframe
//  val iframe = document.createElement("iframe") as HTMLIFrameElement
//  iframe.id = "iframe-overlay"
//  iframe.src = embedUrl
//  iframe.style.padding = "16px"
//  iframe.style.border = "16px"
//  iframe.style.position = "absolute"
//  iframe.style.width = "100vw"
//  iframe.style.height = "100vh"
//  iframe.style.zIndex = "9999"
//  iframe.style.background = "transparent"
//
//  document.body?.appendChild(iframe)
//}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun WebView(url: String, onScriptResult: ((String) -> Unit)?) {

  DisposableEffect(Unit) {
    // Setup message listener for iframe communication
    val messageHandler: (Event) -> Unit = { event ->
      if (event is MessageEvent) {
        val iframe = document.getElementById("map-iframe") as? HTMLIFrameElement
        if (event.source == iframe?.contentWindow) {
          onScriptResult?.invoke(event.data.toString())
        }
      }
    }

    window.addEventListener("message", messageHandler)

    onDispose {
      window.removeEventListener("message", messageHandler)
    }
  }

//  HtmlView(
//    modifier = Modifier.fillMaxSize(),
//    factory = {
//      val iframe = createElement("iframe") as HTMLIFrameElement
//      iframe.id = "map-iframe"

      // Modify the HTML content to include postMessage communication
      // no longer necessary since we are handling that in html string
      val modifiedHtml = url.replace(
        "console.log(selectedLocation);",
        "window.parent.postMessage(JSON.stringify(selectedLocation), '*');"
      )

//      iframe.srcdoc = url
//      iframe.style.width = "100vw"
//      iframe.style.height = "100vh"
//      iframe.style.zIndex = "9999"
//      iframe.style.background = "transparent"
//      iframe
//    }
//  )
  WebElementView(
    factory = {
      (document.createElement("iframe")
        as HTMLIFrameElement)
        .apply {
          id = "map-iframe"
          srcdoc = modifiedHtml
          style.width = "100vw"
          style.height = "100vh"
          style.zIndex = "9999"
          style.background = "transparent"
        }
    },
    modifier = Modifier.fillMaxSize(),
    update = { iframe -> iframe.srcdoc = iframe.srcdoc }
  )
}
