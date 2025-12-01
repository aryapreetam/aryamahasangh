package com.aryamahasangh

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSString
import platform.Foundation.NSThread
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.WebKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.concurrent.Volatile

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebView(url: String, onScriptResult: ((String) -> Unit)?) {
  UIKitView(
    factory = {
      val container = UIView()
      
      // Create WKWebView configuration
      val config = WKWebViewConfiguration().apply {
        allowsInlineMediaPlayback = true
        allowsAirPlayForMediaPlayback = true
        allowsPictureInPictureMediaPlayback = true
      }
      
      // Create message handler inside factory to ensure it's properly tied to this WebView instance
      // This prevents reuse issues during orientation changes and ensures proper cleanup
      val messageHandler = LocationMessageHandler(onScriptResult)
      
      // Create WKWebView inside the factory to ensure proper lifecycle management
      val webView = WKWebView(CGRectZero.readValue(), config).apply {
        translatesAutoresizingMaskIntoConstraints = false
        
        // Enable java script content
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        
        // Wrap message handler addition in try-catch to prevent C++ exceptions
        try {
          // Only add message handler if it hasn't been added before
          if (!messageHandler.isRegistered) {
            configuration.userContentController.addScriptMessageHandler(
              messageHandler,
              "iosLocationHandler"
            )
            messageHandler.markAsRegistered()
            
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
            configuration.userContentController.addUserScript(script)
          }
        } catch (e: Exception) {
          println("Error setting up WebView message handler: ${e.message}")
        }
      }
      
      container.addSubview(webView)

      NSLayoutConstraint.activateConstraints(
        listOf(
          webView.topAnchor.constraintEqualToAnchor(container.topAnchor),
          webView.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor),
          webView.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor),
          webView.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor)
        )
      )

      // Load HTML on main thread to ensure thread safety
      if (NSThread.isMainThread()) {
        try {
          webView.loadHTMLString(url, baseURL = null)
        } catch (e: Exception) {
          println("Error loading HTML in WebView: ${e.message}")
        }
      } else {
        dispatch_async(dispatch_get_main_queue()) {
          try {
            webView.loadHTMLString(url, baseURL = null)
          } catch (e: Exception) {
            println("Error loading HTML in WebView: ${e.message}")
          }
        }
      }

      container
    },
    modifier = Modifier.fillMaxSize(),
    properties = UIKitInteropProperties(
      isInteractive = true,
      isNativeAccessibilityEnabled = true
    ),
    onRelease = { container ->
      // Safely clean up the webview when the UIKitView is released
      // Ensure cleanup happens on main thread
      val cleanupAction = {
        container.subviews.forEach { subview ->
          if (subview is WKWebView) {
            try {
              subview.stopLoading()
              
              // Only remove if the message handler was registered
              if (messageHandler.isRegistered) {
                subview.configuration.userContentController.removeScriptMessageHandlerForName(
                  "iosLocationHandler"
                )
                messageHandler.markAsUnregistered()
              }
              
              subview.removeFromSuperview()
            } catch (e: Exception) {
              println("Error cleaning up WebView: ${e.message}")
            }
          }
        }
      }
      
      if (NSThread.isMainThread()) {
        cleanupAction()
      } else {
        dispatch_async(dispatch_get_main_queue()) {
          cleanupAction()
        }
      }
    }
  )
}

private class LocationMessageHandler(
  private val onScriptResult: ((String) -> Unit)?
) : NSObject(), WKScriptMessageHandlerProtocol {
  
  @Volatile
  private var isValid: Boolean = true
  
  @Volatile
  private var registered: Boolean = false
  
  val isRegistered: Boolean
    get() = registered
  
  fun markAsRegistered() {
    registered = true
  }
  
  fun markAsUnregistered() {
    registered = false
  }
  
  fun invalidate() {
    isValid = false
  }
  
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    // Check if the handler is still valid before processing
    if (!isValid) {
      println("LocationMessageHandler: Ignoring message, handler is invalidated")
      return
    }
    
    val message = didReceiveScriptMessage.body as? NSString
    if (message != null) {
      println("Received location update in iOS bridge: $message")
      try {
        onScriptResult?.invoke(message.toString())
      } catch (e: Exception) {
        println("Error invoking onScriptResult callback: ${e.message}")
      }
    }
  }
}
