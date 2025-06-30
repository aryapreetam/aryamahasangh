package com.aryamahasangh

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun YoutubeVideoPlayer(videoUrl: String) {
    UIKitView(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { 
            val configuration = WKWebViewConfiguration()
            // JavaScript is enabled by default in WKWebView
            WKWebView(CGRectMake(0.0, 0.0, 0.0, 0.0), configuration)
        },
        update = { webView ->
            val embedUrl = "https://www.youtube.com/embed/$videoUrl"
            val url = NSURL.URLWithString(embedUrl)
            if (url != null) {
                val request = NSURLRequest.requestWithURL(url)
                webView.loadRequest(request)
            }
        }
    )
}
