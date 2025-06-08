package org.aryamahasangh.features.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.aryamahasangh.WebView
import kotlin.math.roundToInt

@Composable
actual fun MapView(
  latitude: Double,
  longitude: Double,
  onLocationChanged: (Double, Double) -> Unit,
  modifier: Modifier
) {
}

@Composable
actual fun WebViewMap(
  latitude: Double,
  longitude: Double,
  onLocationChanged: (Double, Double) -> Unit,
  modifier: Modifier
) {
  // Use a more stable approach
  StableMapView(latitude, longitude, onLocationChanged, modifier)
}

@Composable
private fun StableMapView(
  latitude: Double,
  longitude: Double,
  onLocationChanged: (Double, Double) -> Unit,
  modifier: Modifier
) {
  var currentLatitude by remember(latitude) { mutableStateOf(latitude) }
  var currentLongitude by remember(longitude) { mutableStateOf(longitude) }
  var zoomLevel by remember { mutableStateOf(17) }
  val uriHandler = LocalUriHandler.current

  // Update current coordinates when input changes
  LaunchedEffect(latitude, longitude) {
    currentLatitude = latitude
    currentLongitude = longitude
  }

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    // Map background with fallback
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFE0E0E0))
    ) {
      // Try to load the map
      var mapLoadError by remember { mutableStateOf(false) }

      if (!mapLoadError) {
        OpenStreetMapWebView(
          currentLatitude,
          currentLongitude,
          zoomLevel,
          onError = { mapLoadError = true }
        )
      }

      // Fallback UI if map fails to load
      if (mapLoadError) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            "मानचित्र लोड नहीं हो सका",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "अक्षांश: ${(currentLatitude * 1000000).roundToInt() / 1000000.0}\n" +
              "देशांतर: ${(currentLongitude * 1000000).roundToInt() / 1000000.0}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedButton(
            onClick = {
              val url =
                "https://www.openstreetmap.org/?mlat=$currentLatitude&mlon=$currentLongitude#map=$zoomLevel/$currentLatitude/$currentLongitude"
              uriHandler.openUri(url)
            }
          ) {
            Text("ब्राउज़र में खोलें")
          }
        }
      }
    }

    // Gesture detection layer
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(currentLatitude, currentLongitude, zoomLevel) {
          detectTransformGestures { _, pan, zoom, _ ->
            // Handle zoom
            if (zoom != 1f) {
              val newZoom = (zoomLevel * zoom).toInt().coerceIn(10, 20)
              if (newZoom != zoomLevel) {
                zoomLevel = newZoom
              }
            }

            // Convert pan to coordinate changes based on zoom level
            val scaleFactor = 0.00001 * (20 - zoomLevel)
            val latChange = -pan.y * scaleFactor
            val lngChange = pan.x * scaleFactor

            // Update coordinates
            currentLatitude = (currentLatitude + latChange).coerceIn(-90.0, 90.0)
            currentLongitude = (currentLongitude + lngChange).let { lng ->
              when {
                lng > 180.0 -> lng - 360.0
                lng < -180.0 -> lng + 360.0
                else -> lng
              }
            }

            // Notify listener
            onLocationChanged(currentLatitude, currentLongitude)
          }
        }
    )

    // UI overlay container - ensure it's on top
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Top section
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.Center
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 4.dp
        ) {
          Text(
            "स्थान बदलने के लिए मानचित्र को खींचें",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall
          )
        }
      }

      // Spacer to push bottom content down
      Spacer(modifier = Modifier.weight(1f))

      // Center marker
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.LocationOn,
          contentDescription = "स्थान",
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.error
        )
      }

      // Another spacer
      Spacer(modifier = Modifier.weight(1f))

      // Bottom section
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        // Coordinates display
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 4.dp
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              "अक्षांश: ${(currentLatitude * 1000000).roundToInt() / 1000000.0}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              "देशांतर: ${(currentLongitude * 1000000).roundToInt() / 1000000.0}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        // Zoom controls
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FloatingActionButton(
            onClick = {
            zoomLevel = (zoomLevel + 1).coerceAtMost(20)
            },
            modifier = Modifier.size(40.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
          ) {
            Icon(
              Icons.Default.Add,
              contentDescription = "ज़ूम इन",
              modifier = Modifier.size(20.dp)
            )
          }

          FloatingActionButton(
            onClick = {
            zoomLevel = (zoomLevel - 1).coerceAtLeast(10)
            },
            modifier = Modifier.size(40.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
          ) {
            Icon(
              Icons.Default.Remove,
              contentDescription = "ज़ूम आउट",
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OpenStreetMapWebView(
  latitude: Double,
  longitude: Double,
  zoom: Int,
  onError: () -> Unit
) {
  val delta = 0.01 / zoom
  val lon1 = longitude - delta
  val lat1 = latitude - delta
  val lon2 = longitude + delta
  val lat2 = latitude + delta

  val embedUrl = remember(latitude, longitude, zoom) {
    "https://www.openstreetmap.org/export/embed.html?bbox=$lon1,$lat1,$lon2,$lat2"
  }

  // Use WebView without try-catch
  WebView(embedUrl)
}
