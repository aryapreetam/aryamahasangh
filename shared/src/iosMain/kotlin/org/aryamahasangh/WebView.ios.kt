package org.aryamahasangh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebView(url: String, onScriptResult: ((String) -> Unit)?) {
  val config = WKWebViewConfiguration().apply {
    allowsInlineMediaPlayback = true
    allowsAirPlayForMediaPlayback = true
    allowsPictureInPictureMediaPlayback = true
  }

  val webView = remember { WKWebView(CGRectZero.readValue(), config) }

  // Enable java script content
  webView.configuration.defaultWebpagePreferences.allowsContentJavaScript = true

  UIKitView(
    factory = {
      val container = UIView()

      webView.translatesAutoresizingMaskIntoConstraints = false
      container.addSubview(webView)

      NSLayoutConstraint.activateConstraints(
        listOf(
          webView.topAnchor.constraintEqualToAnchor(container.topAnchor),
          webView.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor),
          webView.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor),
          webView.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor)
        )
      )

      webView.loadHTMLString(url, baseURL = null)

      container
    },
    modifier = Modifier.fillMaxSize(),
    properties = UIKitInteropProperties(
      isInteractive = true,
      isNativeAccessibilityEnabled = true
    )
  )
}
