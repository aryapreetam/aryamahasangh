package org.aryamahasangh.features.activities

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
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.aryamahasangh.WebView
import org.aryamahasangh.utils.logger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerDialog(onDismiss: () -> Unit, onLocationPicked: (LatLng) -> Unit){
  val scope = rememberCoroutineScope()
  var location by remember { mutableStateOf<LatLng?>(null) }
  var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

  // Trigger location fetch only once when dialog appears
  LaunchedEffect(Unit) {
    scope.launch {
      location = getCurrentLocation()
    }
  }
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      dismissOnBackPress = true,
      dismissOnClickOutside = false,
      usePlatformDefaultWidth = false
    )
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth(0.95f)
        .fillMaxHeight(0.9f),
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp
    ) {
      Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()){
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
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()){
          println("current location: $location")
          if(location != null) {
            val html = generate(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
            WebView(
              url = html,
              onScriptResult = { result ->
                try {
                  val json = Json.parseToJsonElement(result).jsonObject
                  val lat = json["lat"]?.jsonPrimitive?.doubleOrNull
                  val lng = json["lng"]?.jsonPrimitive?.doubleOrNull
                  if (lat != null && lng != null) {
                    selectedLocation = LatLng(lat, lng)
                    logger.info {"selected location: ${selectedLocation!!.latitude}, ${selectedLocation!!.longitude} "}
                  }
                } catch (e: Exception) {
                  logger.error{ e.message ?: "Error parsing location" }
                  println("Error parsing location: ${e.message}")
                }
              }
            )
          }else{
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
  val response = client.get("http://ip-api.com/json/")
  var s = ""
  if(response.status == HttpStatusCode.OK) {
    s = response.bodyAsText()
  }
  println(s)
  val json = Json.parseToJsonElement(s).jsonObject

  return LatLng(
    json["lat"]?.jsonPrimitive?.doubleOrNull ?: return null,
    json["lon"]?.jsonPrimitive?.doubleOrNull ?: return null
  )
}

// FIXME migrate leaflet to 2.0 once stable
fun generate(lat: Double, lng: Double): String{
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
        .leaflet-control-geocoder {
            position: fixed !important;
            top: 10px !important;
            left: 60% !important;
            transform: translateX(-55%) !important;
            z-index: 1000;
            width: 80%;
            max-width: 800px;
        }
        .leaflet-control-geocoder-form input {
            width: 100%;
            padding: 12px 16px;
            font-size: 16px;
            border: 1px solid #ccc;
            border-radius: 4px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            background: white;
        }
        .leaflet-control-geocoder-alternatives {
            width: 100%;
            background: white;
            border-radius: 4px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.2);
            max-height: 300px;
            overflow-y: auto;
            margin-top: 4px;
        }
        .leaflet-control-geocoder-alternatives li {
            padding: 8px 12px;
            cursor: pointer;
            border-bottom: 1px solid #eee;
        }
        .leaflet-control-geocoder-alternatives li:hover {
            background: #f0f0f0;
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

    <!-- Leaflet JS -->
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <!-- Leaflet Geocoder JS -->
    <script src="https://unpkg.com/leaflet-control-geocoder@2.4.0/dist/Control.Geocoder.js"></script>

    <script>
        let map;
        let selectedLocation = null;
        let tileLayer;
        let platform = 'web'; // Default to web platform
        let searchControl;
        
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

            // Geocoder cache
            const geocoderCache = new Map();
            const CACHE_DURATION = 24 * 60 * 60 * 1000; // 24 hours

            // Custom geocoder with caching
            const customGeocoder = {
                geocode: async function(query, cb, context) {
                    const cacheKey = query.toLowerCase().trim();
                    const now = Date.now();
                    
                    // Check cache first
                    if (geocoderCache.has(cacheKey)) {
                        const cached = geocoderCache.get(cacheKey);
                        if (now - cached.timestamp < CACHE_DURATION) {
                            cb.call(context, cached.results);
                            return;
                        }
                        geocoderCache.delete(cacheKey);
                    }
                    
                    try {
                        const nominatim = new L.Control.Geocoder.Nominatim({
                            geocodingQueryParams: {
                                countrycodes: 'in',
                                limit: 5,
                                format: 'jsonv2',
                                addressdetails: 1,
                                bounded: 1
                            }
                        });
                        
                        nominatim.geocode(query, function(results) {
                            // Cache results
                            geocoderCache.set(cacheKey, {
                                results: results,
                                timestamp: now
                            });
                            
                            cb.call(context, results);
                        }, context);
                    } catch (e) {
                        console.error('Geocoding error:', e);
                        cb.call(context, []);
                    }
                }
            };

            // Add search control with optimized settings
            searchControl = L.Control.geocoder({
                defaultMarkGeocode: false,
                position: 'topleft',
                placeholder: 'Search location...',
                geocoder: customGeocoder,
                suggestMinLength: 3,
                suggestTimeout: 250,
                queryMinLength: 3
            }).addTo(map);

            // Handle search results
            searchControl.on('markgeocode', function(e) {
                console.log('Full search result:', JSON.stringify(e.geocode, null, 2));
                const center = e.geocode.center;
                
                // Check for bounding box in properties
                if (e.geocode.properties && e.geocode.properties.boundingbox) {
                    const bb = e.geocode.properties.boundingbox;
                    console.log('Bounding box found:', bb);
                    
                    // Create bounds from the boundingbox array [south, north, west, east]
                    const bounds = L.latLngBounds(
                        [parseFloat(bb[0]), parseFloat(bb[2])], // southwest
                        [parseFloat(bb[1]), parseFloat(bb[3])]  // northeast
                    );
                    
                    // Let Leaflet determine the appropriate zoom level
                    map.fitBounds(bounds, {
                        padding: [50, 50],
                        maxZoom: 17 // Just as a safety limit
                    });
                } else {
                    console.log('No bounding box available');
                    map.setView(center, 13);
                }
                updateLocation();
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

            // Move geocoder control to center top
            const geocoderContainer = document.querySelector('.leaflet-control-geocoder');
            if (geocoderContainer) {
                map.getContainer().appendChild(geocoderContainer);
            }
            
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


