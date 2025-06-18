# ImagePickerComponent Documentation

A comprehensive and flexible image picker component for selecting single or multiple images/documents with support for
validation,
edit mode, and customizable configurations. This component intelligently handles both new image selection and existing
image management with three distinct picker types.

## Features

- **Three Picker Types**: IMAGE, IMAGE_AND_DOCUMENT, and PROFILE_PHOTO
- **Single/Multiple Selection**: Configure to allow single or multiple image selection
- **Custom Labels**: Fully customizable labels in Hindi/Devanagari
- **Format Support**: Specify supported image/document formats (jpg, png, pdf, etc.)
- **Edit Mode Support**: Works seamlessly with existing images for edit scenarios
- **Built-in Validation**: Comprehensive validation with Hindi error messages
- **Image Preview**: Visual preview grid with thumbnails and proper spacing
- **State Management**: Clean state management with separate tracking for new and deleted images
- **Mobile-Friendly**: Optimized for mobile devices with proper touch targets
- **Visual Indicators**: Shows modification status in edit mode
- **Smart Empty State**: Contextual empty state based on mandatory/optional configuration
- **No Duplicate Close Buttons**: Fixed issue with duplicate close buttons when using PhotoItem

## Picker Types

### 1. IMAGE

- Only image files (jpg, png, gif, etc.)
- Square thumbnails with grid layout
- Default formats: jpg, jpeg, png, gif, webp

### 2. IMAGE_AND_DOCUMENT

- Both images and documents (pdf, doc, etc.)
- Shows document icon for non-image files
- Default formats: jpg, jpeg, png, gif, webp, pdf, doc, docx

### 3. PROFILE_PHOTO

- Single image selection only
- Circular preview with overlay
- Larger preview size (150dp)
- Special UI for profile photos
- Default formats: jpg, jpeg, png

## Basic Usage

```kotlin
import org.aryamahasangh.components.*

@Composable
fun MyScreen() {
    var imageState by remember { mutableStateOf(ImagePickerState()) }
    
    ImagePickerComponent(
        state = imageState,
        onStateChange = { imageState = it }
    )
}
```

## Configuration Options

Control the behavior using `ImagePickerConfig`:

```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "चित्र जोड़ें",              // Custom label
        type = ImagePickerType.IMAGE,      // Picker type
        allowMultiple = true,              // Allow multiple selection (ignored for PROFILE_PHOTO)
        maxImages = 10,                    // Maximum number of images (1 for PROFILE_PHOTO)
        supportedFormats = listOf("jpg", "jpeg", "png", "gif", "webp"), // Optional custom formats
        isMandatory = false,               // Whether at least one image is required
        minImages = 1,                     // Minimum required images (if mandatory)
        showPreview = true,                // Show image preview grid
        previewSize = 100,                 // Preview thumbnail size in dp
        allowCamera = false                // Future camera support
    )
)
```

### Common Configurations

#### Profile Photo
```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "फ़ोटो अपलोड करें",
        type = ImagePickerType.PROFILE_PHOTO,
        isMandatory = true
    )
)
```

#### Multiple Images
```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "छायाचित्र",
        type = ImagePickerType.IMAGE,
        allowMultiple = true,
        maxImages = 5
    )
)
```

#### Documents with Images

```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "दस्तावेज़ संलग्न करें",
        type = ImagePickerType.IMAGE_AND_DOCUMENT,
        allowMultiple = true,
        maxImages = 5,
        isMandatory = true,
        minImages = 1
    )
)
```

## State Management

### ImagePickerState
```kotlin
data class ImagePickerState(
    val newImages: List<PlatformFile> = emptyList(),      // Newly selected images
    val existingImageUrls: List<String> = emptyList(),    // URLs of existing images
    val deletedImageUrls: Set<String> = emptySet()        // URLs marked for deletion
)
```

### Useful Properties and Functions
- `totalImages`: Total count of active images (new + existing - deleted)
- `hasImages`: Whether any images are selected
- `getActiveImageUrls()`: Get all existing URLs that aren't marked for deletion
- `hasChanges()`: Check if state has been modified (useful for edit mode)

## Edit Mode

Perfect for editing existing content with images:

```kotlin
// Initialize with existing images
var imageState by remember {
    mutableStateOf(
        ImagePickerState(
            existingImageUrls = listOf(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg"
            )
        )
    )
}

// The component automatically shows "संशोधित" indicator when changes are made
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "चित्र/पत्रिकाएं जोड़ें",
        type = ImagePickerType.IMAGE,
        allowMultiple = true,
        maxImages = 10
    )
)

// Check for changes before saving
if (imageState.hasChanges()) {
    Button(onClick = {
        // Handle save with upload/delete operations
        val finalUrls = handleImageUpload(imageState)
    }) {
        Text("सहेजें")
    }
}
```

## Validation

Use the built-in validation function:

```kotlin
val error = validateImagePickerState(imageState, config)
```

**Error messages in Hindi (context-aware based on type):**

- Profile Photo: "प्रोफ़ाइल फ़ोटो चुनना आवश्यक है"
- Images: "कम से कम एक चित्र चुनना आवश्यक है"
- Documents: "कम से कम एक फ़ाइल चुनना आवश्यक है"
- Multiple: "कम से कम X चित्र/फ़ाइलें चुनना आवश्यक है"
- Maximum: "अधिकतम X चित्र/फ़ाइलें ही चुनी जा सकती हैं"

## Integration with Forms

### Replace Existing Image Handling

If you're using the current image handling pattern (like in CreateActivityFormScreen.kt), you can replace it with this
component:

**Before:**
```kotlin
var attachedDocuments by remember { mutableStateOf(emptyList<PlatformFile>()) }
var existingMediaUrls by remember { mutableStateOf(emptyList<String>()) }
var deletedMediaUrls by remember { mutableStateOf(emptySet<String>()) }

// Complex UI code for showing images...
Text("संबधित चित्र एवं पत्रिकाएं:", style = MaterialTheme.typography.labelLarge)
// FlowRow with custom image display...
ButtonForFilePicker("चित्र/पत्रिकाएं जोड़ें") { ... }
```

**After:**
```kotlin
var imageState by remember { mutableStateOf(ImagePickerState()) }

ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "संबधित चित्र एवं पत्रिकाएं",
        type = ImagePickerType.IMAGE,
        allowMultiple = true,
        maxImages = 10,
        isMandatory = false
    )
)
```

### Complete Form Example
```kotlin
@Composable
fun CreateActivityForm() {
    var imageState by remember { mutableStateOf(ImagePickerState()) }
    var showErrors by remember { mutableStateOf(false) }
    
    val imageConfig = ImagePickerConfig(
        label = "संबधित चित्र एवं पत्रिकाएं",
        type = ImagePickerType.IMAGE,
        allowMultiple = true,
        maxImages = 10,
        isMandatory = false
    )
    
    val imageError = if (showErrors) {
        validateImagePickerState(imageState, imageConfig)
    } else null
    
    Column {
        // Other form fields...
        
        ImagePickerComponent(
            state = imageState,
            onStateChange = { imageState = it },
            config = imageConfig,
            error = imageError
        )
        
        Button(onClick = {
            showErrors = true
            if (imageError == null) {
                // Handle submission
                scope.launch {
                    val finalUrls = handleImageUpload(imageState)
                    // Submit form with finalUrls
                }
            }
        }) {
            Text("Submit")
        }
    }
}
```

## Image Upload Handling

```kotlin
suspend fun handleImageUpload(
    imageState: ImagePickerState
): List<String> {
    val finalUrls = mutableListOf<String>()
    
    // Keep existing images that weren't deleted
    finalUrls.addAll(imageState.getActiveImageUrls())
    
    // Delete removed images
    if (imageState.deletedImageUrls.isNotEmpty()) {
        val filesToDelete = imageState.deletedImageUrls.map { url ->
            url.substringAfterLast("/")
        }
        bucket.delete(filesToDelete)
    }
    
    // Upload new images
    imageState.newImages.forEach { file ->
        val uploadResponse = bucket.upload(
            path = "${Clock.System.now().epochSeconds}_${file.name}",
            data = file.readBytes()
        )
        finalUrls.add(bucket.publicUrl(uploadResponse.path))
    }
    
    return finalUrls
}
```

## Different Use Cases

### Student Profile Photo
```kotlin
config = ImagePickerConfig(
    label = "छात्र की फोटो",
    type = ImagePickerType.PROFILE_PHOTO,
    isMandatory = true
)
```

### Activity Images
```kotlin
config = ImagePickerConfig(
    label = "छायाचित्र",
    type = ImagePickerType.IMAGE,
    allowMultiple = true,
    maxImages = 20,
    isMandatory = false,
    previewSize = 150
)
```

### Documents with Images
```kotlin
config = ImagePickerConfig(
    label = "दस्तावेज़ संलग्न करें",
    type = ImagePickerType.IMAGE_AND_DOCUMENT,
    allowMultiple = true,
    maxImages = 5,
    isMandatory = true,
    minImages = 2
)
```

### Certificates/Media
```kotlin
config = ImagePickerConfig(
    label = "चित्र/पत्रिकाएं जोड़ें",
    type = ImagePickerType.IMAGE_AND_DOCUMENT,
    allowMultiple = true,
    maxImages = 8,
    isMandatory = false,
    previewSize = 100
)
```

## UI Features

1. **Type-Specific UI**: Different layouts for profile photos vs regular images/documents
2. **Smart Empty State**: Shows context-aware messages based on type and mandatory status
3. **Modification Indicator**: "संशोधित" badge appears when changes are made in edit mode
4. **Document Icons**: Shows file type icon for non-image documents
5. **No Duplicate Close Buttons**: Fixed issue with duplicate remove buttons
6. **Profile Photo Overlay**: Special overlay UI for profile photo selection
7. **Circular Preview**: Profile photos show in circular format
8. **Grid Layout**: Regular images/documents show in responsive grid
9. **Add More Button**: Appears in the grid when under the maximum limit
10. **Image Count**: Shows current/max count with proper terminology (चित्र/फ़ाइलें)
11. **Error Display**: Type-aware validation errors in Hindi

## Migration Benefits

- **90% Less Code**: Replace complex image handling with a single component
- **Consistent UX**: Same behavior across all forms in your app
- **Better Error Handling**: Built-in validation with proper Hindi messages
- **Edit Mode Ready**: Automatically handles existing images and modifications
- **Type Safety**: Strong typing with clear state management
- **Future Proof**: Ready for camera support and other enhancements
- **Type-Specific Behavior**: Different UI/UX for different use cases

## Notes

- The component no longer uses `PhotoItem` internally to avoid duplicate close buttons
- Supports both local file selection and existing image URLs
- Automatically filters files by supported formats based on type
- Prevents selection beyond the maximum limit
- Profile photo type automatically sets `allowMultiple = false` and `maxImages = 1`
- Works across all Compose Multiplatform targets (Android, iOS, Web, Desktop)
- Future versions will support camera capture functionality
- Optimized for accessibility with proper content descriptions
