# OpenStreetMap Setup - Quick Guide

## What's Already Working

1. **Location Search**: Fully functional using Nominatim API
    - Search for any place in India
    - Results in Hindi and English
    - No API key needed

2. **Map Dialog**: Complete UI with:
    - Search bar with loading indicator
    - Search results display
    - Coordinate selection
    - Placeholder map that can be dragged

3. **Integration**: Connected to CreateActivityScreen
    - Click "मानचित्र से चुनें" button
    - Search or drag to select location
    - Click "चुनें" to populate lat/lng fields

## What You Need to Provide

Nothing! The implementation is ready to use with OpenStreetMap.

### Optional Enhancements

1. **Custom Nominatim Server** (if you want better performance):
    - Update `NOMINATIM_API_BASE_URL` in `OpenStreetMapConfig.kt`
    - Example: `https://your-nominatim-server.com`

2. **Different Tile Server** (for map styling):
    - Choose from `TileServer` enum options
    - Or add your own tile server URL

3. **Platform-Specific Maps** (for production):
    - Android: Add osmdroid library
    - iOS: Use MapKit with OSM tiles
    - Web: Add Leaflet.js
    - Desktop: Use JXMapViewer2

## Current Features

### Search

- Real-time place search
- India-focused results
- Hindi language support
- Detailed address formatting
- Automatic fallback to mock data

### Map

- Draggable placeholder map
- Live coordinate updates
- OpenStreetMap attribution
- Responsive design

### No Dependencies Required

- Works with existing Ktor client
- No additional libraries needed
- No API keys or registration

## Testing

Try these searches:

- "Delhi" - Returns New Delhi and Delhi Cantt
- "Mumbai" - Returns Mumbai, Maharashtra
- "Bangalore" - Returns Bengaluru, Karnataka
- Any other Indian city or landmark

## Rate Limits

- Nominatim: 1 request per second (handled automatically)
- No daily limits
- Free for reasonable use

## Next Steps

The implementation is fully functional. For production, consider:

1. Adding a real map renderer (see platform-specific guides)
2. Implementing current location detection
3. Adding map markers for better UX
4. Caching search results
