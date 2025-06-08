package org.aryamahasangh.features.activities

// OpenStreetMap and Nominatim API Configuration
object OpenStreetMapConfig {
  // Nominatim API endpoint (no API key required)
  const val NOMINATIM_API_BASE_URL = "https://nominatim.openstreetmap.org"

  // Default map settings
  const val DEFAULT_ZOOM = 15
  const val DEFAULT_LAT = 28.6139 // New Delhi
  const val DEFAULT_LNG = 77.2090

  // User agent for API requests (required by Nominatim)
  const val USER_AGENT = "AryaMahasangh/1.0"

  // Rate limiting - Nominatim requires max 1 request per second
  const val MIN_SEARCH_DELAY_MS = 1000L
}

// Tile server options for OpenStreetMap
enum class TileServer(val url: String, val attribution: String) {
  OSM_STANDARD(
    url = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
    attribution = "© OpenStreetMap contributors"
  ),
  OSM_DE(
    url = "https://tile.openstreetmap.de/{z}/{x}/{y}.png",
    attribution = "© OpenStreetMap contributors"
  ),
  CARTO_LIGHT(
    url = "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
    attribution = "© OpenStreetMap contributors, © CARTO"
  )
}
