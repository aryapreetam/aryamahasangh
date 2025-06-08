package org.aryamahasangh.features.activities

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.aryamahasangh.WebView

@Composable
actual fun MapView(
  latitude: Double,
  longitude: Double,
  onLocationChanged: (Double, Double) -> Unit,
  modifier: Modifier
) {
  WebViewMap(latitude, longitude, onLocationChanged, modifier)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebViewMap(
  latitude: Double,
  longitude: Double,
  onLocationChanged: (Double, Double) -> Unit,
  modifier: Modifier
) {
  val delta = 0.001  // controls zoom area
  val lon1 = longitude - delta
  val lat1 = latitude - delta
  val lon2 = longitude + delta
  val lat2 = latitude + delta

  val embedUrl = "https://www.openstreetmap.org/export/embed.html?" +
    "bbox=$lon1,$lat1,$lon2,$lat2" +
    "&layer=mapnik&marker=$latitude,$longitude"
  WebView(embedUrl)
}
