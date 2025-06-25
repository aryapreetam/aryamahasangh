package org.aryamahasangh.utils

import androidx.compose.ui.platform.UriHandler
import org.aryamahasangh.isAndroid
import org.aryamahasangh.isIos

/**
 * Opens navigation/directions in maps application
 * @param uriHandler The UriHandler to open URIs
 * @param latitude Destination latitude
 * @param longitude Destination longitude
 * @param placeName Optional place name for better UX
 */
fun openDirections(
  uriHandler: UriHandler,
  latitude: Double,
  longitude: Double,
  placeName: String = ""
) {
  // Create a universal maps URL that works across platforms
  // This format is supported by Google Maps on all platforms
  val mapsUrl =
    buildString {
      append("https://www.google.com/maps/dir/?api=1")
      append("&destination=$latitude,$longitude")
      if (placeName.isNotEmpty()) {
        append("&destination_place_id=$placeName")
      }
      append("&travelmode=driving") // Can be: driving, walking, transit, bicycling
    }

  // Platform-specific handling
  when {
    isAndroid() -> {
      // For Android, try to use geo: URI first for native app support
      val geoUri = "geo:0,0?q=$latitude,$longitude($placeName)"
      try {
        uriHandler.openUri(geoUri)
      } catch (e: Exception) {
        // Fallback to web URL if geo: URI fails
        uriHandler.openUri(mapsUrl)
      }
    }

    isIos() -> {
      // For iOS, use Apple Maps URL scheme
      val appleMapsUrl = "maps://?daddr=$latitude,$longitude&dirflg=d"
      try {
        uriHandler.openUri(appleMapsUrl)
      } catch (e: Exception) {
        // Fallback to Google Maps web URL
        uriHandler.openUri(mapsUrl)
      }
    }

    else -> {
      // For web and desktop, use Google Maps web URL
      uriHandler.openUri(mapsUrl)
    }
  }
}
