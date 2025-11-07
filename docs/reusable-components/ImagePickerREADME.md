# ImagePickerComponent Documentation

A comprehensive and flexible image picker component for selecting single or multiple images/documents with support for camera capture, validation, edit mode, and customizable configurations. This component intelligently handles both new image selection and existing image management with three distinct picker types.

## Features

- **Camera Capture (Android/iOS)**: Take photos directly from camera with comprehensive permission handling
- **Gallery Selection**: Select existing images from device gallery
- **Platform-Aware UI**: Shows Camera/Gallery modal on mobile, direct gallery on Desktop/Web
- **Three Picker Types**: IMAGE, IMAGE_AND_DOCUMENT, and PROFILE_PHOTO
- **Single/Multiple Selection**: Configure to allow single or multiple image selection
- **Custom Labels**: Fully customizable labels in Hindi/Devanagari
- **Format Support**: Specify supported image/document formats (jpg, png, pdf, etc.)
- **Background Compression**: Automatic image compression with progress indicator
- **Edit Mode Support**: Works seamlessly with existing images for edit scenarios
- **Built-in Validation**: Comprehensive validation with Hindi error messages
- **Image Preview**: Visual preview grid with thumbnails and proper spacing
- **State Management**: Clean state management with separate tracking for new and deleted images
- **Permission Handling**: Complete Android/iOS camera permission flow with Settings dialog
- **Mobile-Friendly**: Optimized for mobile devices with proper touch targets
- **Visual Indicators**: Shows modification status in edit mode
- **Smart Empty State**: Contextual empty state based on mandatory/optional configuration

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
import com.aryamahasangh.components.*

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
        
        // Camera Control (NEW)
        allowCamera = true,                // Enable camera on mobile (default: true)
                                          // Set to false for gallery-only selection
        
        // Compression Options
        enableBackgroundCompression = true,   // Enable automatic compression
        compressionTargetKb = 100,           // Target size in KB
        showCompressionProgress = true       // Show compression progress
    )
)
```

### Camera Control

**`allowCamera` Parameter:**

- **`true` (Default):** On Android/iOS, shows camera/gallery selection modal. On Desktop/Web, direct gallery picker.
- **`false`:** Direct gallery picker only on ALL platforms (no camera option, even on mobile)

**Use Cases:**

```kotlin
// Default - Camera enabled on mobile
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it }
    // allowCamera defaults to true
)

// Gallery only - Disable camera even on mobile
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        allowCamera = false  // No camera option, gallery only
    )
)

// Document selection - Typically no camera needed
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        type = ImagePickerType.IMAGE_AND_DOCUMENT,
        allowCamera = false  // Documents selected from storage, not captured
    )
)
```

### Common Configurations

#### Profile Photo (with Camera)
```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "फ़ोटो अपलोड करें",
        type = ImagePickerType.PROFILE_PHOTO,
        isMandatory = true,
        allowCamera = true  // User can take selfie
    )
)
```

#### Profile Photo (Gallery Only)
```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "फ़ोटो अपलोड करें",
        type = ImagePickerType.PROFILE_PHOTO,
        isMandatory = true,
        allowCamera = false  // Only select from existing photos
    )
)
```

#### Multiple Images (with Camera)
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

---

## 📸 Camera Capture Feature

**Platform Support:** Android ✅ | iOS ✅ | Desktop ❌ | Web ❌

The ImagePickerComponent now includes built-in camera capture functionality on mobile platforms (Android and iOS). Users can choose to take a new photo with their camera or select an existing one from their gallery.

### How It Works

#### On Mobile (Android/iOS)

When user clicks the "Add Image" button:
1. **Modal Bottom Sheet appears** with two options:
   - **"कैमरा से लें"** (Take from Camera) - Opens camera to capture new photo
   - **"गैलरी से चुनें"** (Select from Gallery) - Opens gallery picker

2. **Camera Option:**
   - Requests camera permission if not granted
   - Opens native camera app
   - Captured photo is automatically compressed
   - Photo is added to the image state

3. **Gallery Option:**
   - Opens native gallery/file picker
   - User selects image(s) based on configuration
   - Selected images are automatically compressed
   - Images are added to the image state

#### On Desktop/Web

- **No modal shown** - directly opens file picker
- Camera option not available (camera not supported)
- Only gallery/file selection available

### Usage Example

```kotlin
@Composable
fun ProfilePhotoScreen() {
    var imageState by remember { mutableStateOf(ImagePickerState()) }
    
    ImagePickerComponent(
        state = imageState,
        onStateChange = { newState ->
            imageState = newState
            
            // Access captured/selected images
            if (newState.newImages.isNotEmpty()) {
                val latestImage = newState.newImages.last()
                
                // Check if it was captured from camera
                if (newState.isCapturedFile(latestImage)) {
                    println("Photo captured from camera!")
                } else {
                    println("Photo selected from gallery")
                }
                
                // Get compressed image data if available
                val compressedBytes = newState.getCompressedBytes(latestImage)
                if (compressedBytes != null) {
                    // Upload compressed image
                    uploadImage(compressedBytes)
                } else {
                    // Upload original
                    uploadImage(latestImage.readBytes())
                }
            }
        },
        config = ImagePickerConfig(
            type = ImagePickerType.PROFILE_PHOTO,
            enableBackgroundCompression = true,
            compressionTargetKb = 40 // Compress to ~40KB
        )
    )
}
```

### Accessing Image Data

#### Get the Latest Image

```kotlin
val latestImage: PlatformFile? = imageState.newImages.lastOrNull()
```

#### Read Image Bytes

```kotlin
// Method 1: Get compressed bytes (if compression is enabled)
val compressedBytes: ByteArray? = imageState.getCompressedBytes(latestImage)

// Method 2: Read original bytes
val originalBytes: ByteArray = latestImage.readBytes()

// Recommended: Use compressed if available, fallback to original
val imageBytes = imageState.getCompressedBytes(latestImage) ?: latestImage.readBytes()
```

#### Check Image Source

```kotlin
if (imageState.isCapturedFile(latestImage)) {
    // Image was captured from camera
    // Might need different handling
} else {
    // Image was selected from gallery
}
```

#### Get Image Name and Extension

```kotlin
val fileName: String = latestImage.name
val extension: String = fileName.substringAfterLast('.', "").lowercase()
```

### Image Upload Example

```kotlin
@Composable
fun UploadImageScreen() {
    var imageState by remember { mutableStateOf(ImagePickerState()) }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadedUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Column {
        ImagePickerComponent(
            state = imageState,
            onStateChange = { imageState = it },
            config = ImagePickerConfig(
                type = ImagePickerType.PROFILE_PHOTO,
                enableBackgroundCompression = true,
                compressionTargetKb = 40,
                showCompressionProgress = true
            )
        )
        
        // Upload button appears when image is selected
        if (imageState.hasImages) {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val image = imageState.newImages.first()
                            
                            // Use compressed data if available
                            val imageBytes = imageState.getCompressedBytes(image) 
                                ?: image.readBytes()
                            
                            // Upload to Supabase Storage
                            val fileName = "profile_${System.currentTimeMillis()}.webp"
                            val uploadResponse = bucket.upload(
                                path = fileName,
                                data = imageBytes
                            )
                            
                            // Get public URL
                            uploadedUrl = bucket.publicUrl(uploadResponse.path)
                            
                            // Save URL to database
                            saveProfileImage(uploadedUrl!!)
                            
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                }
            ) {
                Text("अपलोड करें")
            }
        }
        
        // Show uploaded image URL
        uploadedUrl?.let { url ->
            Text("अपलोड सफल: $url")
        }
    }
}
```

### Permission Handling

#### Android

The component automatically handles camera permissions:

1. **First Request:** Permission dialog appears when user selects camera
2. **Grant:** Camera opens immediately (no extra click needed)
3. **Deny:** Error message shown, user can try again
4. **Multiple Denials:** Settings dialog appears with button to open app Settings
5. **Permanently Denied:** User is guided to enable permission in Settings

**Manifest Permission (Already Added):**
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

**No Code Needed:** Permission flow is completely handled by the component.

#### iOS

FileKit handles iOS camera permissions automatically:

- First request shows system permission dialog
- Uses `NSCameraUsageDescription` from Info.plist (already configured)
- Message shown: "कैमरा से फोटो लेने के लिए अनुमति आवश्यक है"

**No Code Needed:** Permission flow is handled by FileKit and iOS system.

### Image Compression

Camera-captured images are automatically compressed using the same logic as gallery images:

```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        // Enable background compression
        enableBackgroundCompression = true,
        
        // Target size in KB (default: 200KB)
        compressionTargetKb = 40, // For profile photos
        
        // Show compression progress to user
        showCompressionProgress = true
    )
)
```

**Compression Details:**
- Uses WebP format for better compression
- Maintains aspect ratio
- Max long edge: 2560px (configurable in ImageCompressor)
- Profile photos: ~40KB target
- Regular photos: ~200KB target
- Documents: No compression

### Complete Profile Photo Example

```kotlin
@Composable
fun AddMemberProfilePhoto(
    existingImageUrl: String? = null,
    onImageUploaded: (String) -> Unit
) {
    var imageState by remember {
        mutableStateOf(
            ImagePickerState(
                existingImageUrls = existingImageUrl?.let { listOf(it) } ?: emptyList()
            )
        )
    }
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image Picker with Camera Support
        ImagePickerComponent(
            state = imageState,
            onStateChange = { imageState = it },
            config = ImagePickerConfig(
                label = "प्रोफ़ाइल फ़ोटो",
                type = ImagePickerType.PROFILE_PHOTO,
                isMandatory = true,
                enableBackgroundCompression = true,
                compressionTargetKb = 40,
                showCompressionProgress = true
            )
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Upload Button (only shown when image is selected)
        if (imageState.hasImages && imageState.hasChanges()) {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            // Get the new image
                            val newImage = imageState.newImages.firstOrNull()
                            
                            if (newImage != null) {
                                // Use compressed bytes
                                val imageBytes = imageState.getCompressedBytes(newImage)
                                    ?: newImage.readBytes()
                                
                                // Upload to Supabase
                                val fileName = "profile_${Clock.System.now().epochSeconds}.webp"
                                val uploadResponse = bucket.upload(
                                    path = fileName,
                                    data = imageBytes
                                )
                                
                                val publicUrl = bucket.publicUrl(uploadResponse.path)
                                
                                // Notify parent
                                onImageUploaded(publicUrl)
                                
                                // Update state to show uploaded image
                                imageState = ImagePickerState(
                                    existingImageUrls = listOf(publicUrl)
                                )
                            }
                        } catch (e: Exception) {
                            // Show error to user
                            println("Upload failed: ${e.message}")
                        }
                    }
                }
            ) {
                Text("अपलोड करें")
            }
        }
    }
}
```

### Error Handling

The component shows appropriate error messages in Hindi:

| Scenario | Error Message |
|----------|---------------|
| Permission Denied (Can Retry) | "कैमरा अनुमति अस्वीकृत। पुनः प्रयास करें।" |
| Permission Permanently Denied | Dialog: "कैमरा अनुमति आवश्यक" with Settings button |
| Compression Error | "संपीड़न में त्रुटि: [error details]" |
| Format Not Supported | File is filtered out silently |

### Platform Detection

To check if camera is supported at runtime:

```kotlin
import com.aryamahasangh.components.isCameraSupported

if (isCameraSupported()) {
    // Camera is available (Android/iOS)
    // Modal will show Camera/Gallery options
} else {
    // Camera not supported (Desktop/Web)
    // Only gallery picker will be shown
}
```

---

## Configuration Options (Updated)

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
        
        // NEW: Compression Options
        enableBackgroundCompression = false,  // Enable automatic compression
        compressionTargetKb = 200,           // Target size in KB (default: 200)
        showCompressionProgress = false      // Show compression progress indicator
    )
)
```

### Common Configurations

#### Profile Photo with Camera
```kotlin
ImagePickerComponent(
    state = imageState,
    onStateChange = { imageState = it },
    config = ImagePickerConfig(
        label = "फ़ोटो अपलोड करें",
        type = ImagePickerType.PROFILE_PHOTO,
        isMandatory = true,
        enableBackgroundCompression = true,
        compressionTargetKb = 40 // Small size for profile photos
    )
)
```

#### Multiple Images with Camera
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
- **Camera capture fully supported** on Android and iOS with comprehensive permission handling
- Optimized for accessibility with proper content descriptions

---

## 🚀 Future Enhancements (TODO)

### 1. Camera Aspect Ratio Configuration

**Feature:** Configure camera viewport aspect ratio for different use cases

**Why Needed:**
- Profile photos need 1:1 (square) aspect ratio
- ID/Document capture works better with 4:3
- Landscape photos might use 16:9
- Current implementation uses platform default (usually 4:3 on Android, 16:9 on iOS)

**Proposed API:**

```kotlin
enum class CameraAspectRatio {
  Default,      // Platform default (4:3 or 16:9)
  Square,       // 1:1 - Perfect for profile photos
  FourThree,    // 4:3 - Standard for documents
  SixteenNine   // 16:9 - Widescreen/landscape
}

data class ImagePickerConfig(
  // ...existing fields...
  val cameraAspectRatio: CameraAspectRatio = CameraAspectRatio.Default
)

// Usage
ImagePickerComponent(
  config = ImagePickerConfig(
    type = ImagePickerType.PROFILE_PHOTO,
    cameraAspectRatio = CameraAspectRatio.Square // Square camera viewport
  )
)
```

**Implementation Considerations:**

1. **FileKit Limitations:**
   - Check if FileKit 0.12.0 exposes aspect ratio control
   - If not, may need custom camera implementation

2. **Platform-Specific Approaches:**
   - **Android:** Use CameraX with `setAspectRatio()` or Camera2 API
   - **iOS:** Use AVFoundation with aspect ratio presets
   - **Fallback:** If not supported, show overlay guide (e.g., circle for profile)

3. **UI Enhancements:**
   - Show visual guide overlay (circle for profile, rectangle for document)
   - Real-time preview with aspect ratio applied
   - Crop markers or guides to help user frame the shot

4. **Research & Libraries:**
   - Evaluate CameraX Compose (Android)
   - Check Accompanist Camera utilities
   - Consider third-party multiplatform camera libraries
   - Fallback to cropping if camera control unavailable

**Estimated Effort:** 3-5 days (2-3 days if custom camera implementation needed)

**Priority:** Medium (Nice to have, but cropping can be a workaround)

---

### 2. Image Cropping for Gallery Selection

**Feature:** Allow users to crop images after selection from gallery

**Why Needed:**
- User selects landscape photo for profile but needs square (1:1)
- Need to remove unwanted parts of image
- Better control over final image appearance
- Especially important if camera aspect ratio control is not available

**Proposed API:**

```kotlin
enum class CropAspectRatio {
  Free,         // User can crop to any aspect ratio
  Square,       // Fixed 1:1 (for profile photos)
  FourThree,    // Fixed 4:3
  SixteenNine,  // Fixed 16:9
  Original      // Maintain original aspect ratio
}

data class ImagePickerConfig(
  // ...existing fields...
  val enableCropping: Boolean = false,
  val cropAspectRatio: CropAspectRatio = CropAspectRatio.Free,
  val cropAfterCapture: Boolean = false  // Also crop camera-captured images
)

// Usage
ImagePickerComponent(
  config = ImagePickerConfig(
    type = ImagePickerType.PROFILE_PHOTO,
    enableCropping = true,
    cropAspectRatio = CropAspectRatio.Square, // Force square crop
    cropAfterCapture = true  // Also crop camera photos
  )
)
```

**User Flow:**

```
User selects image from gallery
    ↓
Image Cropping Screen appears
    ├─ Show selected image with zoom/pan
    ├─ Overlay crop rectangle/circle
    ├─ Pinch to zoom
    ├─ Drag to reposition
    ├─ Optional rotate button
    └─ Action buttons
       ├─→ "काटें" (Crop) → Apply crop → Compress → Add
       └─→ "रद्द करें" (Cancel) → Back to picker
```

**Recommended Library:**

**Compose Image Cropper** (Recommended)
- Repository: https://github.com/SmartToolFactory/Compose-Image-Cropper
- Pure Compose Multiplatform
- Works on all platforms (Android, iOS, Web, Desktop)
- Customizable aspect ratios
- Gesture support (pinch, zoom, rotate)

**Implementation Approach:**

```kotlin
// 1. Add dependency
// gradle/libs.versions.toml
compose-image-cropper = "2.0.0"

// 2. Internal navigation state
sealed class ImagePickerScreen {
  object Selection : ImagePickerScreen()
  data class Crop(val file: PlatformFile) : ImagePickerScreen()
}

// 3. Integrate crop screen in ImagePickerComponent
val imagePickerLauncher = rememberFilePickerLauncher(...) { files ->
  if (config.enableCropping && files != null) {
    // Navigate to crop screen
    currentScreen = ImagePickerScreen.Crop(files.first())
  } else {
    // Process directly
    processFiles(files)
  }
}

// 4. Crop screen implementation
@Composable
private fun ImageCropScreen(
  file: PlatformFile,
  aspectRatio: CropAspectRatio,
  onCropComplete: (ByteArray) -> Unit,
  onCancel: () -> Unit
) {
  val imageBytes = remember(file) { file.readBytes() }
  
  ImageCropper(
    imageBitmap = imageBytes.toImageBitmap(),
    cropProperties = CropProperties(
      aspectRatio = when(aspectRatio) {
        CropAspectRatio.Square -> AspectRatio(1f)
        CropAspectRatio.FourThree -> AspectRatio(4f/3f)
        CropAspectRatio.SixteenNine -> AspectRatio(16f/9f)
        CropAspectRatio.Free -> AspectRatio.Unspecified
        CropAspectRatio.Original -> AspectRatio.Original
      },
      overlayType = if (aspectRatio == CropAspectRatio.Square) 
        OverlayType.Circle else OverlayType.Rectangle
    )
  )
  
  // UI with Crop/Cancel buttons
}
```

**Implementation Considerations:**

1. **Performance:**
   - Scale down large images for smooth cropping
   - Load full resolution only when saving final crop
   - Use Coil/Skia for efficient bitmap operations

2. **Memory Management:**
   - Release original bitmap after crop
   - Optimize for mobile devices with limited memory

3. **UI/UX:**
   - Show grid overlay (rule of thirds)
   - Smooth animations for crop handles
   - Clear visual feedback during cropping
   - "काटें" and "रद्द करें" buttons in Hindi

4. **Integration Points:**
   - After gallery selection → Show crop screen
   - After camera capture (optional, controlled by `cropAfterCapture`)
   - In edit mode → Allow re-cropping existing images

5. **Multi-Platform Testing:**
   - Test thoroughly on iOS (Skia rendering differences)
   - Ensure gesture handling works on touch and desktop
   - Verify accessibility on all platforms

6. **Compression:**
   - Apply cropping **before** compression for better quality
   - Maintain aspect ratio during compression
   - Update compression logic to handle cropped images

**Alternative Approach (If Library Not Available):**

Custom Canvas-based cropper:
```kotlin
@Composable
fun CustomImageCropper(
  bitmap: ImageBitmap,
  aspectRatio: Float,
  onCrop: (ByteArray) -> Unit
) {
  // Canvas + Box + gesture modifiers
  // Pinch to zoom, drag to pan
  // Draw crop overlay
  // Calculate crop region and apply
}
```

**Estimated Effort:** 4-6 days
- 1 day: Library evaluation and integration
- 2-3 days: UI implementation and gesture handling
- 1-2 days: Multi-platform testing and refinement

**Priority:** High (More important than camera aspect ratio, provides immediate value)

**Dependencies:**
- Compose Image Cropper library (or custom implementation)
- Coil3 for image loading/manipulation
- Kotlinx-coroutines for async crop operations

---

### 3. Other Future Enhancements

**3.1 Multiple Camera Capture Session**
- Capture multiple photos in one session without reopening camera
- Show thumbnail gallery as photos are taken
- "Done" button to finish and return all photos

**3.2 Camera Controls**
- Flash toggle (on/off/auto)
- Front/back camera switch
- Grid overlay option
- Timer for selfies

**3.3 Image Filters**
- Apply Instagram-style filters before upload
- Black & White, Sepia, Vintage, etc.
- Real-time preview

**3.4 Batch Operations**
- Apply same crop to multiple images
- Bulk compress with progress indicator
- Batch rotation/flip

**3.5 Advanced Permissions UX**
- Educational screen before requesting permission
- "Why we need camera" explanation in Hindi
- Better rationale dialog with examples

---

## 📚 Additional Resources

### Related Documentation
- **Planning Document:** `docs/planning/camera-capture-feature-implementation.md`
- **Camera Implementation:** `composeApp/src/.../components/CameraLauncher.*.kt`
- **Examples:** `composeApp/src/.../components/ImagePickerExample.kt`

### External Libraries
- **FileKit:** https://github.com/vinceglb/FileKit
- **Accompanist Permissions:** https://google.github.io/accompanist/permissions/
- **Compose Image Cropper:** https://github.com/SmartToolFactory/Compose-Image-Cropper

### Platform Documentation
- **Android Camera:** https://developer.android.com/training/camera2
- **iOS AVFoundation:** https://developer.apple.com/documentation/avfoundation
- **Compose Multiplatform:** https://www.jetbrains.com/lp/compose-multiplatform/

---

**Last Updated:** November 8, 2025  
**Current Version:** 2.0 (With Camera Capture)  
**Next Major Version:** 3.0 (With Cropping Support)

