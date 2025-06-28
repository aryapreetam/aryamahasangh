# UI Components Gallery

A showcase application for all reusable UI components in the AryaMahasangh project.

## Features

- **Interactive Demos**: See components in action with different states
- **Hot Reload**: Changes are reflected immediately during development
- **Multiplatform**: Supports Desktop and WASM targets
- **Material 3 Design**: Clean, modern interface

## Components Showcased

### Buttons

- **StatefulSubmitButton**: Button with loading, success, and error states
- **SimpleSubmitButton**: Simplified version with automatic error handling

## Development

### Running the Gallery

```bash
# Desktop (with hot reload)
./gradlew :ui-components-gallery:desktopRun

# WASM (browser)
./gradlew :ui-components-gallery:wasmJsBrowserDevelopmentRun
```

### Hot Reload

The gallery includes Compose Hot Reload for rapid development:

- Make changes to any UI component or gallery code
- Changes are reflected immediately without restarting the application
- Supports all common UI changes (layouts, colors, text, etc.)

### Adding New Components

1. Add your component to the appropriate category screen (e.g., `ButtonsGalleryScreen.kt`)
2. Create a demo function showcasing different states/properties
3. Add the component category to `GalleryHomeScreen.kt` if needed

## Language Guidelines

- **App UI**: English (navigation, descriptions, titles)
- **Component Content**: Pure Sanskrit/Hindi with Devanagari script
- No Urdu/Persian loanwords in Hindi text

## Architecture

```
ui-components-gallery/
├── src/
│   ├── commonMain/kotlin/org/aryamahasangh/gallery/
│   │   ├── Gallery.kt                    # Main navigation
│   │   └── screens/
│   │       ├── GalleryHomeScreen.kt      # Component categories
│   │       └── ButtonsGalleryScreen.kt   # Button demos
│   └── desktopMain/kotlin/
│       └── main.kt                       # Desktop entry point
└── build.gradle.kts                      # Multiplatform configuration
```

## Technologies

- **Kotlin Multiplatform**: Cross-platform code sharing
- **Compose Multiplatform**: UI framework
- **Navigation Compose**: Screen navigation
- **Material 3**: Design system
- **Hot Reload**: Development acceleration
