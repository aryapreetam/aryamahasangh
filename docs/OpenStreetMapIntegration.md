# OpenStreetMap Integration Guide

## Overview

The map location picker in CreateActivityScreen uses OpenStreetMap with Nominatim API for place search. This is a free,
open-source alternative to Google Maps that doesn't require API keys.

## Features

### Current Implementation

1. **Placeholder Map**: A draggable map simulation that allows testing the UI
2. **Nominatim Search**: Real place search using OpenStreetMap's Nominatim API
3. **India-focused**: Search results are restricted to India
4. **Hindi Support**: Search supports Hindi language results
5. **No API Key Required**: OpenStreetMap services are free to use

### How It Works

#### Search Functionality

- Uses Nominatim API at `https://nominatim.openstreetmap.org`
- Rate limited to 1 request per second (as required by Nominatim)
- Returns up to 10 results per search
- Includes detailed address information
- Falls back to mock data if API fails

#### Map Display

Currently shows a placeholder that:

- Displays a grid background
- Has a center location marker
- Updates coordinates when dragged
- Shows OpenStreetMap attribution

## Platform-Specific Implementation

To implement actual OpenStreetMap on each platform:

### Android

Use osmdroid library:

```kotlin
// In build.gradle.kts
androidMain.dependencies {
    implementation("org.osmdroid:osmdroid-android:6.1.17")
}

// MapView implementation
@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    onLocationChanged: (Double, Double) -> Unit,
    modifier: Modifier
) {
    AndroidView(
        factory = { context ->
            org.osmdroid.views.MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(latitude, longitude))
            }
        },
        modifier = modifier
    )
}
```

### iOS

Use MapKit with OpenStreetMap tiles or a library like MapboxMaps:

```swift
// Using MapKit with custom tile overlay
let tileOverlay = MKTileOverlay(urlTemplate: "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
mapView.addOverlay(tileOverlay)
```

### Web

Use Leaflet.js:

```kotlin
// In jsMain
@Composable
actual fun MapView(
    latitude: Double,
    longitude: Double,
    onLocationChanged: (Double, Double) -> Unit,
    modifier: Modifier
) {
    // Use Compose HTML to embed Leaflet map
    Div(attrs = {
        id("map")
        style {
            width(100.percent)
            height(100.percent)
        }
    })
    
    LaunchedEffect(latitude, longitude) {
        // Initialize Leaflet map
        js("""
            var map = L.map('map').setView([$latitude, $longitude], 15);
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(map);
        """)
    }
}
```

### Desktop

Use JXMapViewer2 or similar:

```kotlin
// In desktopMain
implementation("org.jxmapviewer:jxmapviewer2:2.5")
```

## API Usage Guidelines

### Nominatim API

1. **Rate Limiting**: Maximum 1 request per second
2. **User Agent**: Must include a valid User-Agent header
3. **Usage Policy**: See https://operations.osmfoundation.org/policies/nominatim/
4. **No API Key**: Free to use, but respect usage limits

### Tile Servers

Available tile servers in `TileServer` enum:

- **OSM Standard**: Default OpenStreetMap tiles
- **OSM DE**: German mirror (often faster)
- **CARTO Light**: Light-themed maps

## Testing

The current implementation allows full testing of:

- Location search functionality
- Coordinate selection
- UI/UX flow
- Error handling

## Security & Privacy

1. **No API Keys**: No need to manage or secure API keys
2. **Open Source**: All map data is open source
3. **Privacy**: No user tracking by default
4. **Self-Hosting Option**: Can host your own Nominatim instance for better privacy

## Customization

### Search Parameters

Modify in `searchLocation()`:

- `countrycodes`: Change from "in" to other country codes
- `accept-language`: Modify language preference
- `limit`: Change number of results

### Map Styling

For actual implementations:

- Use different tile servers for different styles
- Apply custom overlays
- Add markers and popups

## Troubleshooting

### Search Not Working

1. Check internet connection
2. Verify you're not exceeding rate limits
3. Check if Nominatim service is up
4. Falls back to mock data automatically

### Map Not Displaying

1. Ensure tile server URL is correct
2. Check CORS settings for web
3. Verify permissions on mobile platforms

## Advantages over Google Maps

1. **Free**: No API costs
2. **No API Key**: Simpler setup
3. **Open Source**: Community-driven data
4. **Privacy**: No tracking
5. **Offline Capable**: Can download tiles for offline use
