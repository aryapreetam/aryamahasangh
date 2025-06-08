package org.aryamahasangh.features.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NominatimResult(
  val lat: String,
  val lon: String,
  val display_name: String,
  val place_id: Long? = null,
  val osm_type: String? = null,
  val osm_id: Long? = null,
  val boundingbox: List<String>? = null,
  val address: Map<String, String>? = null
)

data class LatLng(
  val latitude: Double,
  val longitude: Double
)

data class SearchResult(
  val name: String,
  val address: String,
  val latLng: LatLng
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerDialog(
  initialLatitude: Double? = null,
  initialLongitude: Double? = null,
  onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
  onDismiss: () -> Unit
) {
  // Force complete recreation by using a unique key
  val dialogKey = remember { Clock.System.now().toEpochMilliseconds() }

  var searchQuery by remember { mutableStateOf("") }
  var selectedLocation by remember {
    mutableStateOf(
      if (initialLatitude != null && initialLongitude != null) {
        LatLng(initialLatitude, initialLongitude)
      } else {
        // Default to New Delhi coordinates
        LatLng(28.6139, 77.2090)
      }
    )
  }
  var searchResults by remember { mutableStateOf(emptyList<SearchResult>()) }
  var isSearching by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  key(dialogKey) {
    Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
      )
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(0.95f)
          .fillMaxHeight(0.9f)
          .wideScreenOptional(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        Column(
          modifier = Modifier.fillMaxSize()
        ) {
          // Header
          TopAppBar(
            title = { Text("स्थान चुनें") },
            navigationIcon = {
              IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "बंद करें")
              }
            },
            actions = {
              TextButton(
                onClick = {
                  onLocationSelected(selectedLocation.latitude, selectedLocation.longitude)
                  onDismiss()
                }
              ) {
                Text("चुनें")
              }
            }
          )

          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("स्थान खोजें...") },
            leadingIcon = {
              Icon(Icons.Default.Search, contentDescription = "खोजें")
            },
            trailingIcon = {
              if (isSearching) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  strokeWidth = 2.dp
                )
              } else if (searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = {
                    scope.launch {
                      isSearching = true
                      // Simulate search - replace with actual search implementation
                      searchLocation(searchQuery) { results ->
                        searchResults = results
                        isSearching = false
                      }
                    }
                  }
                ) {
                  Icon(Icons.Default.Search, contentDescription = "खोजें")
                }
              }
            },
            singleLine = true
          )

          // Search Results (if any)
          if (searchResults.isNotEmpty()) {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(0.3f)
                .padding(horizontal = 16.dp)
            ) {
              items(searchResults.size) { index ->
                val result = searchResults[index]
                Card(
                  onClick = {
                    selectedLocation = result.latLng
                    searchResults = emptyList()
                    searchQuery = ""
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp)
                  ) {
                    Text(
                      text = result.name,
                      style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                      text = result.address,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          // Map Container
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            // Force WebViewMap recreation with unique key
            key("${dialogKey}_map_${selectedLocation.latitude}_${selectedLocation.longitude}") {
              WebViewMap(
                latitude = selectedLocation.latitude,
                longitude = selectedLocation.longitude,
                onLocationChanged = { lat, lng ->
                  selectedLocation = LatLng(lat, lng)
                },
              )
            }

            // Current location button
            FloatingActionButton(
              onClick = {
                // Get current location - platform specific implementation
                getCurrentLocation { lat, lng ->
                  selectedLocation = LatLng(lat, lng)
                }
              },
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp),
              containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
              Icon(
                Icons.Default.MyLocation,
                contentDescription = "वर्तमान स्थान",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }
        }
      }
    }
  }
}

// Extension function for wide screen adaptation
@Composable
private fun Modifier.wideScreenOptional(): Modifier {
  return this.then(
    Modifier.widthIn(max = 800.dp)
  )
}

suspend fun searchLocation(query: String, onResults: (List<SearchResult>) -> Unit) {
  // Rate limiting - Nominatim requires max 1 request per second
  delay(OpenStreetMapConfig.MIN_SEARCH_DELAY_MS)

  try {
    val client = HttpClient {
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
          isLenient = true
        })
      }
    }

    val response: HttpResponse = client.get("${OpenStreetMapConfig.NOMINATIM_API_BASE_URL}/search") {
      parameter("q", query)
      parameter("format", "json")
      parameter("countrycodes", "in") // Restrict to India
      parameter("limit", 10)
      parameter("addressdetails", 1)
      parameter("extratags", 1)
      parameter("accept-language", "hi,en") // Hindi first, then English
      headers {
        append("User-Agent", OpenStreetMapConfig.USER_AGENT)
      }
    }

    val nominatimResults = response.body<List<NominatimResult>>()

    val results = nominatimResults.map { result ->
      SearchResult(
        name = extractPlaceName(result),
        address = formatAddress(result),
        latLng = LatLng(result.lat.toDouble(), result.lon.toDouble())
      )
    }

    onResults(results)
    client.close()
  } catch (e: Exception) {
    println("Error searching location: ${e.message}")
    // Fallback to mock results if API fails
    onResults(getMockResults(query))
  }
}

// Helper function to extract a user-friendly place name
private fun extractPlaceName(result: NominatimResult): String {
  val address = result.address
  return when {
    address?.get("road") != null -> address["road"]!!
    address?.get("neighbourhood") != null -> address["neighbourhood"]!!
    address?.get("suburb") != null -> address["suburb"]!!
    address?.get("city") != null -> address["city"]!!
    address?.get("state_district") != null -> address["state_district"]!!
    else -> result.display_name.split(",").firstOrNull() ?: "Unknown Place"
  }
}

// Helper function to format address
private fun formatAddress(result: NominatimResult): String {
  val parts = mutableListOf<String>()
  val address = result.address

  address?.let {
    it["road"]?.let { road -> parts.add(road) }
    it["neighbourhood"]?.let { neighbourhood -> if (neighbourhood != it["road"]) parts.add(neighbourhood) }
    it["suburb"]?.let { suburb -> if (suburb != it["neighbourhood"]) parts.add(suburb) }
    it["city"]?.let { city -> parts.add(city) }
    it["state_district"]?.let { district -> parts.add(district) }
    it["state"]?.let { state -> parts.add(state) }
  }

  return if (parts.isNotEmpty()) {
    parts.joinToString(", ")
  } else {
    result.display_name
  }
}

// Mock results for fallback
private fun getMockResults(query: String): List<SearchResult> {
  return when {
    query.contains("delhi", ignoreCase = true) -> listOf(
      SearchResult(
        name = "नई दिल्ली",
        address = "राष्ट्रीय राजधानी क्षेत्र, भारत",
        latLng = LatLng(28.6139, 77.2090)
      ),
      SearchResult(
        name = "दिल्ली कैंट",
        address = "दिल्ली कैंटोनमेंट, नई दिल्ली",
        latLng = LatLng(28.5969, 77.1585)
      )
    )

    query.contains("mumbai", ignoreCase = true) -> listOf(
      SearchResult(
        name = "मुंबई",
        address = "महाराष्ट्र, भारत",
        latLng = LatLng(19.0760, 72.8777)
      )
    )

    query.contains("bangalore", ignoreCase = true) || query.contains("bengaluru", ignoreCase = true) -> listOf(
      SearchResult(
        name = "बेंगलुरु",
        address = "कर्नाटक, भारत",
        latLng = LatLng(12.9716, 77.5946)
      )
    )

    else -> emptyList()
  }
}

// Placeholder for getting current location
fun getCurrentLocation(onLocation: (Double, Double) -> Unit) {
  // Mock current location - replace with actual location service
  // For now, return a default location (New Delhi)
  onLocation(28.6139, 77.2090)

  /*
  // Example for platform-specific implementation:
  // Android: Use FusedLocationProviderClient
  // iOS: Use CLLocationManager
  // Web: Use navigator.geolocation
  // Desktop: May need to use IP-based geolocation or ask user to enter manually
  */
}
