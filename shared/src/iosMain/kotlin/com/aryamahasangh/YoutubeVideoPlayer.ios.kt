package com.aryamahasangh

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun YoutubeVideoPlayer(videoUrl: String) {
    UIKitView(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { 
            val configuration = WKWebViewConfiguration().apply {
                allowsInlineMediaPlayback = true
                allowsPictureInPictureMediaPlayback = true
            }
            // Create WKWebView with CGRectZero and let UIKit manage the frame
            WKWebView(CGRectZero.readValue(), configuration)
        },
        update = { webView ->
            // Add defensive check to prevent loading during invalid states
            // Ensure update happens on main thread
            val updateAction = {
                try {
                    if (webView.isLoading()) {
                        webView.stopLoading()
                    }
                    
                    val embedUrl = "https://www.youtube.com/embed/$videoUrl"
                    val url = NSURL.URLWithString(embedUrl)
                    if (url != null) {
                        val request = NSURLRequest.requestWithURL(url)
                        webView.loadRequest(request)
                    }
                } catch (e: Exception) {
                    println("Error loading YouTube video: ${e.message}")
                }
            }
            
            if (NSThread.isMainThread()) {
                updateAction()
            } else {
                dispatch_async(dispatch_get_main_queue()) {
                    updateAction()
                }
            }
        },
        onRelease = { webView ->
            // Properly clean up the WKWebView to prevent memory leaks
            // Ensure cleanup happens on main thread
            val cleanupAction = {
                try {
                    webView.stopLoading()
                    webView.configuration.userContentController.removeAllUserScripts()
                } catch (e: Exception) {
                    println("Error releasing YouTube player: ${e.message}")
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
