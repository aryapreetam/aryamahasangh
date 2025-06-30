package com.aryamahasangh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSString
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.WebKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebView(url: String, onScriptResult: ((String) -> Unit)?) {
  val config = WKWebViewConfiguration().apply {
    allowsInlineMediaPlayback = true
    allowsAirPlayForMediaPlayback = true
    allowsPictureInPictureMediaPlayback = true
  }

  val messageHandler = remember { LocationMessageHandler(onScriptResult) }
  
  val webView = remember { 
    WKWebView(CGRectZero.readValue(), config).apply {
      // Add script message handler
      configuration.userContentController.addScriptMessageHandler(
        messageHandler,
        "iosLocationHandler"
      )
    }
  }

  // Enable java script content
  webView.configuration.defaultWebpagePreferences.allowsContentJavaScript = true

  // Inject JavaScript to intercept location updates
  val script = WKUserScript(
    source = """
      window.postMessage = function(data) {
        window.webkit.messageHandlers.iosLocationHandler.postMessage(data);
      };
    """.trimIndent(),
    injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
    forMainFrameOnly = true
  )
  webView.configuration.userContentController.addUserScript(script)

  DisposableEffect(Unit) {
    onDispose {
      // Remove the message handler when the view is disposed
      webView.configuration.userContentController.removeScriptMessageHandlerForName(
        "iosLocationHandler"
      )
    }
  }

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

private class LocationMessageHandler(
  private val onScriptResult: ((String) -> Unit)?
) : NSObject(), WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    val message = didReceiveScriptMessage.body as? NSString
    if (message != null) {
      println("Received location update in iOS bridge: $message")
      onScriptResult?.invoke(message.toString())
    }
  }
}
