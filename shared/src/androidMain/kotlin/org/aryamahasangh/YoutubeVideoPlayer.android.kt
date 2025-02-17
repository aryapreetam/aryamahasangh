package org.aryamahasangh

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
actual fun YoutubeVideoPlayer(videoUrl: String) {
  AndroidView(
    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
    factory = {
      YouTubePlayerView(context = it).apply {
        addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
          override fun onReady(youTubePlayer: YouTubePlayer) {
            youTubePlayer.cueVideo(videoUrl, 0f)
            super.onReady(youTubePlayer)
          }
        })
      }
    }
  )

//  Surface(
//    modifier = Modifier
//      .fillMaxWidth()
//      .fillMaxHeight()
//      .onGloballyPositioned { layoutCoordinates ->
//        componentSize.value = layoutCoordinates.size // Update size with the current layout size
//      }
//  ) {
//    AndroidView(
//      modifier = Modifier
//        .width(with(density) { componentSize.value.width.toDp() })
//        .height(with(density) { componentSize.value.height.toDp() }),
//      factory = {
//        YouTubePlayerView(context = it).apply {
//          addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
//            override fun onReady(youTubePlayer: YouTubePlayer) {
//              youTubePlayer.cueVideo(videoUrl, 0f)
//              super.onReady(youTubePlayer)
//            }
//          })
//        }
//      }
//    )
//  }
}