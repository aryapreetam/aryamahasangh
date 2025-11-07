package com.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Integration example showing how to replace the current image handling
 * in CreateActivityFormScreen.kt (lines 1515-1620) with ImagePickerComponent
 */
@Composable
fun ActivityFormImagePickerIntegration() {
  var activityName by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }

  // Replace these existing states from CreateActivityFormScreen:
  // var attachedDocuments by remember { mutableStateOf(emptyList<PlatformFile>()) }
  // var existingMediaUrls by remember { mutableStateOf(emptyList<String>()) }
  // var deletedMediaUrls by remember { mutableStateOf(emptySet<String>()) }

  // With this single state:
  var imageState by remember { mutableStateOf(ImagePickerState()) }

  var showErrors by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  // Mock snackbar for demo
  val snackbarHostState = remember { SnackbarHostState() }

  // Configuration for activity images
  val imageConfig =
    ImagePickerConfig(
      label = "संबधित चित्र एवं पत्रिकाएं", // Matches the existing label
      type = ImagePickerType.IMAGE,
      allowMultiple = true,
      maxImages = 10,
      isMandatory = false // Images are optional for activities
    )

  // Validation
  val imageError =
    if (showErrors) {
      validateImagePickerState(imageState, imageConfig)
    } else {
      null
    }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        "गतिविधि बनाएं",
        style = MaterialTheme.typography.headlineMedium
      )

      // Other form fields...
      OutlinedTextField(
        value = activityName,
        onValueChange = { activityName = it },
        label = { Text("गतिविधि का नाम") },
        modifier = Modifier.fillMaxWidth()
      )

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("विवरण") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
      )

      // Replace the entire image handling section (lines 1515-1620) with this:
      ImagePickerComponent(
        state = imageState,
        onStateChange = { imageState = it },
        config = imageConfig,
        error = imageError,
        modifier = Modifier.fillMaxWidth()
      )

      // Submit button
      Button(
        onClick = {
          showErrors = true

          if (activityName.isNotBlank() && description.isNotBlank() && imageError == null) {
            scope.launch {
              try {
                // Handle image upload
                val finalImageUrls = handleImageUploadDemo(imageState)

                // Create activity with final image URLs
                println("Activity created with images: $finalImageUrls")
                snackbarHostState.showSnackbar("गतिविधि सफलतापूर्वक बनाई गई")
              } catch (e: Exception) {
                snackbarHostState.showSnackbar("त्रुटि: ${e.message}")
              }
            }
          }
        },
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("गतिविधि बनाएं")
      }
    }
  }
}

/**
 * Demo helper function to simulate image upload - replaces the complex upload logic
 * from CreateActivityFormScreen.kt submitForm() function
 */
suspend fun handleImageUploadDemo(imageState: ImagePickerState): List<String> {
  val finalImageUrls = mutableListOf<String>()

  // Keep existing files that weren't deleted
  finalImageUrls.addAll(imageState.getActiveImageUrls())

  // Simulate deleting files
  if (imageState.deletedImageUrls.isNotEmpty()) {
    println("Simulating deletion of: ${imageState.deletedImageUrls}")
  }

  // Simulate uploading new files
  imageState.newImages.forEach { file ->
    val simulatedUrl = "https://example.com/uploads/${Clock.System.now().epochSeconds}_${file.name}"
    finalImageUrls.add(simulatedUrl)
    println("Simulated upload: ${file.name} -> $simulatedUrl")
  }

  return finalImageUrls
}

/**
 * For edit mode, initialize ImagePickerState with existing images
 */
fun initializeImageStateForEdit(existingImageUrls: List<String>): ImagePickerState {
  return ImagePickerState(
    existingImageUrls = existingImageUrls
  )
}

/**
 * Example of different image picker configurations for various use cases
 */
@Composable
fun DifferentImagePickerConfigurations() {
  Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // 1. Student Photos (Single image, mandatory, profile photo type)
    var studentPhotoState by remember { mutableStateOf(ImagePickerState()) }

    ImagePickerComponent(
      state = studentPhotoState,
      onStateChange = { studentPhotoState = it },
      config =
        ImagePickerConfig(
          label = "छात्र की फोटो",
          type = ImagePickerType.PROFILE_PHOTO,
          isMandatory = true
        )
    )

    HorizontalDivider()

    // 2. Documents (Multiple images and documents, mandatory)
    var documentsState by remember { mutableStateOf(ImagePickerState()) }

    ImagePickerComponent(
      state = documentsState,
      onStateChange = { documentsState = it },
      config =
        ImagePickerConfig(
          label = "दस्तावेज़ संलग्न करें",
          type = ImagePickerType.IMAGE_AND_DOCUMENT,
          allowMultiple = true,
          maxImages = 5,
          isMandatory = true,
          minImages = 2,
          supportedFormats = listOf("jpg", "jpeg", "png", "pdf")
        )
    )

    HorizontalDivider()

    // 3. Gallery Images (Multiple images only, optional, large preview)
    var galleryState by remember { mutableStateOf(ImagePickerState()) }

    ImagePickerComponent(
      state = galleryState,
      onStateChange = { galleryState = it },
      config =
        ImagePickerConfig(
          label = "छायाचित्र",
          type = ImagePickerType.IMAGE,
          allowMultiple = true,
          maxImages = 20,
          isMandatory = false,
          previewSize = 150
        )
    )

    HorizontalDivider()

    // 4. Certificates (Multiple documents, optional, medium size)
    var certificatesState by remember { mutableStateOf(ImagePickerState()) }

    ImagePickerComponent(
      state = certificatesState,
      onStateChange = { certificatesState = it },
      config =
        ImagePickerConfig(
          label = "चित्र/पत्रिकाएं जोड़ें",
          type = ImagePickerType.IMAGE_AND_DOCUMENT,
          allowMultiple = true,
          maxImages = 8,
          isMandatory = false,
          previewSize = 100
        )
    )
  }
}

/**
 * Step-by-step migration guide comments for updating CreateActivityFormScreen.kt:
 *
 * 1. Replace these state variables:
 *    OLD:
 *    var attachedDocuments by remember { mutableStateOf(emptyList<PlatformFile>()) }
 *    var existingMediaUrls by remember { mutableStateOf(emptyList<String>()) }
 *    var deletedMediaUrls by remember { mutableStateOf(emptySet<String>()) }
 *
 *    NEW:
 *    var imageState by remember { mutableStateOf(ImagePickerState()) }
 *
 * 2. Replace the image section (lines 1515-1620):
 *    OLD:
 *    Text("संबधित चित्र एवं पत्रिकाएं:", style = MaterialTheme.typography.labelLarge)
 *    // Complex FlowRow with existing and new images...
 *    ButtonForFilePicker("चित्र/पत्रिकाएं जोड़ें", onFilesSelected = { ... })
 *
 *    NEW:
 *    ImagePickerComponent(
 *      state = imageState,
 *      onStateChange = { imageState = it },
 *      config = ImagePickerConfig(
 *        label = "संबधित चित्र एवं पत्रिकाएं",
 *        type = ImagePickerType.IMAGE,
 *        allowMultiple = true,
 *        maxImages = 10,
 *        isMandatory = false
 *      )
 *    )
 *
 * 3. For edit mode initialization:
 *    OLD:
 *    existingMediaUrls = activity.mediaFiles
 *    attachedDocuments = emptyList()
 *
 *    NEW:
 *    imageState = ImagePickerState(existingImageUrls = activity.mediaFiles)
 *
 * 4. In submitForm(), replace complex upload logic:
 *    OLD:
 *    val attachedImages = mutableListOf<String>()
 *    attachedImages.addAll(existingMediaUrls.filterNot { it in deletedMediaUrls })
 *    // Delete logic...
 *    // Upload logic...
 *
 *    NEW:
 *    val attachedImages = handleImageUpload(imageState)
 *
 * 5. Benefits:
 *    - Single component handles all image operations
 *    - Consistent UI across all forms
 *    - Built-in validation with Hindi messages
 *    - Cleaner state management
 *    - Easy to configure for different use cases
 *    - Better error handling
 *    - Visual indicators for edit mode changes
 *    - Support for profile photos, images, and documents
 */
