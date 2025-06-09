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
                  println(selectedLocation)
                }
              } catch (e: Exception) {
                println("Error parsing location: ${e.message}")
              }
            }
          )
        }
      }
    }
  }
}

data class LatLng(val latitude: Double, val longitude: Double)


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
    </style>
</head>
<body>
    <div id="map"></div>

    <!-- Leaflet JS -->
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <!-- Leaflet Geocoder JS -->
    <script src="https://unpkg.com/leaflet-control-geocoder@2.4.0/dist/Control.Geocoder.js"></script>

    <script>
        let map;
        let marker;
        let selectedLocation = null;
        
        function initMap() {
            map = L.map('map').setView([0, 0], 2);
            
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(map);

            // Add marker at map center
            marker = L.marker(map.getCenter(), { draggable: false }).addTo(map);

            // Add search control
            const searchControl = L.Control.geocoder({
                defaultMarkGeocode: false,
                position: 'topleft',
                placeholder: 'Search location...',
                geocoder: L.Control.Geocoder.nominatim({
                    geocodingQueryParams: {
                        countrycodes: 'in',
                        limit: 5,
                        format: 'json',
                        addressdetails: 1
                    }
                }),
                suggestMinLength: 3,
                suggestTimeout: 250,
                queryMinLength: 3
            }).addTo(map);

            // Handle search results
            searchControl.on('markgeocode', function(e) {
                const center = e.geocode.center;
                map.setView(center, 16);
                updateMarker(center);
            });

            // Update marker position when map is moved
            map.on('move', function() {
                updateMarker(map.getCenter());
            });

            // Log location when panning ends
            map.on('moveend', function() {
                console.log('Location after panning:', selectedLocation);
            });

            // Try to get user's location
            map.setView([$lat, $lng], 13);

            // Move geocoder control to center top
            const geocoderContainer = document.querySelector('.leaflet-control-geocoder');
            if (geocoderContainer) {
                map.getContainer().appendChild(geocoderContainer);
            }
        }

        function updateMarker(position) {
            marker.setLatLng(position);
            selectedLocation = {
                lat: position.lat,
                lng: position.lng
            };
            // Notify location change through alert
            alert(JSON.stringify(selectedLocation));
        }

        // Initialize map when page loads
        initMap();
    </script>
</body>
</html>
  """.trimIndent()
}


