package com.aryamahasangh.features.activities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aryamahasangh.WebView
import com.aryamahasangh.utils.logger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerDialog(
  onDismiss: () -> Unit,
  onLocationPicked: (LatLng) -> Unit
) {
  val scope = rememberCoroutineScope()
  var location by remember { mutableStateOf<LatLng?>(null) }
  var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
  var locationFetchJob by remember { mutableStateOf<Job?>(null) }

  // Trigger location fetch only once when dialog appears
  LaunchedEffect(Unit) {
    locationFetchJob = scope.launch {
      location = getCurrentLocation()
    }
  }
  
  // Cleanup when dialog is disposed
  DisposableEffect(Unit) {
    onDispose {
      // Cancel any ongoing location fetch
      locationFetchJob?.cancel()
    }
  }
  
  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
      )
  ) {
    Surface(
      modifier =
        Modifier.fillMaxWidth(0.95f)
          .fillMaxHeight(0.9f),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp
    ) {
      Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
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
                selectedLocation?.let {
                  onLocationPicked(it)
                }
                onDismiss()
              }
            ) {
              Text("चुनें")
            }
          }
        )
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
          println("current location: $location")
          if (location != null) {
            val html = generate(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
            
            // Track if the WebView is active
            var isWebViewActive by remember { mutableStateOf(true) }
            
            // Cleanup WebView when it's no longer active
            DisposableEffect(Unit) {
              isWebViewActive = true
              onDispose {
                isWebViewActive = false
              }
            }
            
            WebView(
              url = html,
              onScriptResult = { result ->
                // Only process results if WebView is still active
                if (isWebViewActive) {
                  try {
                    val json = Json.parseToJsonElement(result).jsonObject
                    val lat = json["lat"]?.jsonPrimitive?.doubleOrNull
                    val lng = json["lng"]?.jsonPrimitive?.doubleOrNull
                    if (lat != null && lng != null) {
                      selectedLocation = LatLng(lat, lng)
                      logger.info { "selected location: ${selectedLocation!!.latitude}, ${selectedLocation!!.longitude} " }
                    }
                  } catch (e: Exception) {
                    logger.error { e.message ?: "Error parsing location" }
                    println("Error parsing location: ${e.message}")
                  }
                }
              }
            )
          } else {
            Text(
              text = "Loading map...",
              modifier = Modifier.align(Alignment.Center)
            )
          }
        }
      }
    }
  }
}

data class LatLng(val latitude: Double, val longitude: Double)

// FIXME use https://compass.jordond.dev/ to get the current location
suspend fun getCurrentLocation(): LatLng? {
  val client = HttpClient()
  val response = client.get("https://ipapi.co/latlong/")
  var s = ""
  if (response.status == HttpStatusCode.OK) {
    s = response.bodyAsText()
  }
  if(s.isNotEmpty()) {
    val location = s.split(",").map { it.toDouble() }
    return LatLng(location[0], location[1])
  }else{
    return LatLng(28.644800, 77.216721)
  }
}

// FIXME migrate leaflet to 2.0 once stable
fun generate(
  lat: Double,
  lng: Double
): String {
  return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Location Picker</title>
        
        <!-- Leaflet CSS -->
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        
        <!-- Leaflet Geocoder CSS -->
        <link rel="stylesheet" href="https://unpkg.com/leaflet-control-geocoder@2.4.0/dist/Control.Geocoder.css" />
        
        <style>
            body {
                margin: 0;
                padding: 0;
            }
            #map {
                height: 100vh;
                width: 100%;
                background: #f0f0f0;
            }
            #search-container {
                position: fixed !important;
                top: 10px !important;
                left: 50px !important;  /* Position to the right of zoom controls */
                width: 300px !important; /* Fixed width */
                max-width: min(300px, calc(100% - 70px)) !important; /* Responsive but capped */
                transform: none !important;
                z-index: 1001; /* Increase z-index to ensure it's above other elements */
            }
            #search-input {
                width: 100%;
                padding: 8px 12px;
                font-size: 14px;
                border: 1px solid #ccc;
                border-radius: 4px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                background: white;
                box-sizing: border-box;
                font-family: system-ui, -apple-system, "Segoe UI", "Noto Sans", sans-serif;
            }
            #search-results {
                width: 100%;
                background: white;
                border-radius: 4px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                max-height: 300px;
                overflow-y: auto;
                margin-top: 4px;
                display: none;
                z-index: 1002;
                font-family: system-ui, -apple-system, "Segoe UI", "Noto Sans", sans-serif;
            }
            .search-result {
                padding: 8px 12px;
                cursor: pointer;
                border-bottom: 1px solid #eee;
            }
            .search-result:hover {
                background: #f0f0f0;
            }
            .search-result .main-text {
                font-size: 14px;
                margin-bottom: 4px;
            }
            .search-result .sub-text {
                font-size: 12px;
                color: #666;
            }
            #loading {
                position: fixed;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                z-index: 1000;
                background: rgba(255, 255, 255, 0.8);
                padding: 20px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            .spinner {
                width: 40px;
                height: 40px;
                border: 4px solid #f3f3f3;
                border-top: 4px solid #3498db;
                border-radius: 50%;
                animation: spin 1s linear infinite;
            }
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
            
            /* Center marker styles */
            .center-marker {
                position: fixed;
                top: 50%;
                left: 50%;
                width: 25px;
                height: 41px;
                margin-left: -12px;
                margin-top: -41px;
                z-index: 1000;
                pointer-events: none;
                background-image: url('https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png');
                background-size: contain;
                background-repeat: no-repeat;
            }
            .center-marker::after {
                content: '';
                position: absolute;
                bottom: 0;
                left: 50%;
                transform: translateX(-50%);
                width: 25px;
                height: 10px;
                background-image: url('https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png');
                background-size: contain;
                background-repeat: no-repeat;
            }
        </style>
        <script>
            // Set platform based on environment
            if (navigator.userAgent.includes('JavaFX')) {
                window.isDesktop = true;
            }
        </script>
    </head>
    <body>
        <div id="loading"><div class="spinner"></div></div>
        <div id="map"></div>
        <div class="center-marker"></div>
        <div id="search-container">
            <input type="text" id="search-input" placeholder="स्थान खोजें..." />
            <div id="search-results"></div>
        </div>
        <!-- Leaflet JS -->
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        
        <script>
            let map;
            let selectedLocation = null;
            let tileLayer;
            let platform = 'web'; // Default to web platform
            let searchControl;
            let searchTimeout;
            
            // India bounds
            const INDIA_BOUNDS = [
                [6.2325274, 68.1766451], // Southwest
                [35.6745457, 97.395561]  // Northeast
            ];
            
            function showLoading() {
                document.getElementById('loading').style.display = 'flex';
            }
            
            function hideLoading() {
                document.getElementById('loading').style.display = 'none';
            }

            // Function to set platform
            function setPlatform(platformName) {
                platform = platformName;
            }

            function notifyLocationUpdate(location) {
                const locationJson = JSON.stringify(location);
                
                switch(platform) {
                    case 'android':
                        if (window.AndroidLocationBridge) {
                            window.AndroidLocationBridge.onLocationUpdate(locationJson);
                        }
                        break;
                        
                    case 'ios':
                        if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosLocationHandler) {
                            window.webkit.messageHandlers.iosLocationHandler.postMessage(locationJson);
                        }
                        break;
                        
                    case 'desktop':
                        if (typeof alert !== 'undefined') {
                            alert(locationJson);
                        }
                        break;
                        
                    case 'web':
                    default:
                        window.parent.postMessage(locationJson, '*');
                        break;
                }
            }

            function updateLocation() {
                const center = map.getCenter();
                selectedLocation = {
                    lat: center.lat,
                    lng: center.lng
                };
                
                // Notify all platforms about location update
                notifyLocationUpdate(selectedLocation);
            }
            
            async function searchLocation(query) {
                try {
                    const searchResults = document.getElementById('search-results');
                    const searchInput = document.getElementById('search-input');
                    
                    // Show loading state
                    searchResults.innerHTML = '<div class="search-result"><div class="main-text">...</div></div>';
                    searchResults.style.display = 'block';
                    
                    const url = 'https://nominatim.openstreetmap.org/search?format=json&q=' + 
                               encodeURIComponent(query) + 
                               '&countrycodes=in&limit=5&addressdetails=1&accept-language=hi';
                               
                    const response = await fetch(url);
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    
                    const results = await response.json();
                    searchResults.innerHTML = '';
                    
                    if (results.length > 0) {
                        results.forEach(result => {
                            const div = document.createElement('div');
                            div.className = 'search-result';
                            
                            // Format the address components
                            const address = result.address || {};
                            const components = [];
                            
                            // Build address components
                            if (address.road) components.push(address.road);
                            if (address.suburb) components.push(address.suburb);
                            if (address.city || address.town || address.village) 
                                components.push(address.city || address.town || address.village);
                            if (address.state) components.push(address.state);
                            
                            // Get the main text (most specific name first)
                            const mainText = address.amenity || 
                                           address.building || 
                                           address.shop || 
                                           address.leisure || 
                                           address.tourism || 
                                           address.historic || 
                                           result.display_name.split(',')[0];
                            
                            // Get the sub text (either from components or fallback to display_name)
                            const subText = components.length > 0 ? 
                                          components.join(', ') : 
                                          result.display_name.split(',').slice(1).join(',').trim();
                            
                            // Create the result HTML
                            div.innerHTML = '<div class="main-text">' + mainText + '</div>' +
                                '<div class="sub-text">' + subText + '</div>';
                            
                            div.addEventListener('click', () => {
                                const bounds = [
                                    [result.boundingbox[0], result.boundingbox[2]], // southwest
                                    [result.boundingbox[1], result.boundingbox[3]]  // northeast
                                ];
                                
                                // If bounds are too small, just center on the point
                                const latDiff = Math.abs(bounds[0][0] - bounds[1][0]);
                                const lngDiff = Math.abs(bounds[0][1] - bounds[1][1]);
                                
                                if (latDiff < 0.001 || lngDiff < 0.001) {
                                    map.setView([result.lat, result.lon], 17);
                                } else {
                                    map.fitBounds(bounds, { 
                                        padding: [50, 50],
                                        maxZoom: 17
                                    });
                                }
                                
                                searchResults.style.display = 'none';
                                searchInput.value = ''; // Clear search input
                                updateLocation();
                            });
                            
                            searchResults.appendChild(div);
                        });
                        searchResults.style.display = 'block';
                    } else {
                        searchResults.innerHTML = '<div class="search-result"><div class="main-text">कोई परिणाम नहीं मिला</div></div>';
                        searchResults.style.display = 'block';
                    }
                } catch (error) {
                    console.error('Search error:', error);
                    const searchResults = document.getElementById('search-results');
                    searchResults.innerHTML = '<div class="search-result"><div class="main-text">खोज में त्रुटि</div></div>';
                    searchResults.style.display = 'block';
                }
            }
            
            async function initMap() {
                // Auto-detect platform if not set
                if (window.AndroidLocationBridge) {
                    setPlatform('android');
                } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.iosLocationHandler) {
                    setPlatform('ios');
                } else if (window.isDesktop) {
                    setPlatform('desktop');
                }
                
                // Initialize map with specific options for better performance
                map = L.map('map', {
                    zoomControl: true,
                    maxBounds: INDIA_BOUNDS,
                    maxBoundsViscosity: 1.0,
                    minZoom: 4,
                    maxZoom: 18,
                    zoomSnap: 0.5,
                    zoomDelta: 0.5,
                    wheelDebounceTime: 150
                });
                
                // Add tile layer with optimizations
                tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    attribution: '© OpenStreetMap contributors',
                    maxZoom: 18,
                    minZoom: 4,
                    bounds: INDIA_BOUNDS,
                    noWrap: true,
                    tileSize: 256,
                    updateWhenIdle: false,
                    updateWhenZooming: true,
                    keepBuffer: 2
                }).addTo(map);

                // Set up search input
                const searchInput = document.getElementById('search-input');
                searchInput.addEventListener('input', (e) => {
                    if (searchTimeout) {
                        clearTimeout(searchTimeout);
                    }
                    if (e.target.value.length >= 3) {
                        searchTimeout = setTimeout(() => {
                            searchLocation(e.target.value);
                        }, 300);
                    } else {
                        document.getElementById('search-results').style.display = 'none';
                    }
                });
                
                // Hide search results when clicking outside
                document.addEventListener('click', (e) => {
                    if (!e.target.closest('#search-container')) {
                        document.getElementById('search-results').style.display = 'none';
                    }
                });


                // Throttle location updates to reduce notifications
                let updateTimeout;
                function throttledLocationUpdate() {
                    if (updateTimeout) {
                        clearTimeout(updateTimeout);
                    }
                    updateTimeout = setTimeout(updateLocation, 100); // Wait for 100ms after last event
                }
                
                // Update location after movement ends
                map.on('moveend', throttledLocationUpdate);
                map.on('zoomend', throttledLocationUpdate);

                // Set initial view with smooth animation
                map.setView([$lat, $lng], 13, {
                    animate: true,
                    duration: 1
                });
                
                // Hide loading when map and tiles are ready
                map.whenReady(() => {
                    hideLoading();
                    // Initial location update
                    updateLocation();
                });

                // Handle tile loading errors
                tileLayer.on('tileerror', function(event) {
                    console.error('Tile loading error:', event);
                    // Retry loading the tile
                    event.tile.src = event.tile.src;
                });
            }

            // Initialize map when page loads
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', initMap);
            } else {
                initMap();
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}
