package org.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.aryamahasangh.navigation.LocalSnackbarHostState
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Example usage of ImagePickerComponent showing different configurations
 */
@Composable
fun ImagePickerExample() {
  val scrollState = rememberScrollState()

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Example 1: Profile Photo Picker
    ProfilePhotoPickerExample()

    HorizontalDivider()

    // Example 2: Multiple Image Picker
    MultipleImagePickerExample()

    HorizontalDivider()

    // Example 3: Image and Document Picker
    ImageAndDocumentPickerExample()

    HorizontalDivider()

    // Example 4: Image picker with validation
    ImagePickerWithValidationExample()

    HorizontalDivider()

    // Example 5: Edit mode with existing images
    EditModeImagePickerExample()
  }
}

@Composable
private fun ProfilePhotoPickerExample() {
  var imageState by remember { mutableStateOf(ImagePickerState()) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "प्रोफ़ाइल फ़ोटो चयनकर्ता (Profile Photo Picker)",
      style = MaterialTheme.typography.headlineSmall
    )

    ImagePickerComponent(
      state = imageState,
      onStateChange = { imageState = it },
      config =
        ImagePickerConfig(
          label = "फ़ोटो अपलोड करें",
          type = ImagePickerType.PROFILE_PHOTO,
          isMandatory = true
        ),
      modifier = Modifier.fillMaxWidth()
    )

    // Display selected image info
    if (imageState.hasImages) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text("चुना गया फ़ोटो:", style = MaterialTheme.typography.titleMedium)
          if (imageState.newImages.isNotEmpty()) {
            Text("नया फ़ोटो: ${imageState.newImages.first().name}")
          } else if (imageState.getActiveImageUrls().isNotEmpty()) {
            Text("मौजूदा फ़ोटो: ${imageState.getActiveImageUrls().first()}")
          }
        }
      }
    }
  }
}

@Composable
private fun MultipleImagePickerExample() {
  var imageState by remember { mutableStateOf(ImagePickerState()) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "बहु चित्र चयनकर्ता (Multiple Image Picker)",
      style = MaterialTheme.typography.headlineSmall
    )

    ImagePickerComponent(
      state = imageState,
      onStateChange = { imageState = it },
      config =
        ImagePickerConfig(
          label = "छायाचित्र",
          type = ImagePickerType.IMAGE,
          allowMultiple = true,
          maxImages = 5
        ),
      modifier = Modifier.fillMaxWidth()
    )

    // Display selected images info
    if (imageState.hasImages) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text("चुने गए चित्र:", style = MaterialTheme.typography.titleMedium)
          Text("नए चित्र: ${imageState.newImages.size}")
          Text("मौजूदा चित्र: ${imageState.getActiveImageUrls().size}")
          Text("कुल चित्र: ${imageState.totalImages}")
        }
      }
    }
  }
}

@Composable
private fun ImageAndDocumentPickerExample() {
  var imageState by remember { mutableStateOf(ImagePickerState()) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "चित्र और दस्तावेज़ चयनकर्ता (Image & Document Picker)",
      style = MaterialTheme.typography.headlineSmall
    )

    ImagePickerComponent(
      state = imageState,
      onStateChange = { imageState = it },
      config =
        ImagePickerConfig(
          label = "फ़ाइलें संलग्न करें",
          type = ImagePickerType.IMAGE_AND_DOCUMENT,
          allowMultiple = true,
          maxImages = 8
        ),
      modifier = Modifier.fillMaxWidth()
    )

    // Display selected files info
    if (imageState.hasImages) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text("चुनी गई फ़ाइलें:", style = MaterialTheme.typography.titleMedium)
          imageState.newImages.forEach { file ->
            Text("• ${file.name} (${file.name.substringAfterLast('.').uppercase()})")
          }
        }
      }
    }
  }
}

@Composable
private fun ImagePickerWithValidationExample() {
  var imageState by remember { mutableStateOf(ImagePickerState()) }
  var showErrors by remember { mutableStateOf(false) }

  val config =
    ImagePickerConfig(
      label = "दस्तावेज़ संलग्न करें",
      type = ImagePickerType.IMAGE_AND_DOCUMENT,
      allowMultiple = true,
      maxImages = 3,
      isMandatory = true,
      minImages = 1
    )

  val error =
    if (showErrors) {
      validateImagePickerState(imageState, config)
    } else {
      null
    }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "सत्यापन के साथ चित्र चयनकर्ता",
      style = MaterialTheme.typography.headlineSmall
    )

    ImagePickerComponent(
      state = imageState,
      onStateChange = {
        imageState = it
        // Clear errors as user selects images
        if (showErrors && it.totalImages >= config.minImages) {
          showErrors = false
        }
      },
      config = config,
      error = error,
      modifier = Modifier.fillMaxWidth()
    )

    Button(
      onClick = {
        showErrors = true
        val validationError = validateImagePickerState(imageState, config)
        if (validationError == null) {
          // Form is valid, proceed with submission
          println(
            "Files ready for upload: ${imageState.newImages.size} new, ${imageState.getActiveImageUrls().size} existing"
          )
        }
      },
      modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
      Text("जमा करें")
    }
  }
}

@Composable
private fun EditModeImagePickerExample() {
  // Simulate existing images from server
  val existingImageUrls =
    remember {
      listOf(
        "https://example.com/image1.jpg",
        "https://example.com/image2.jpg",
        "https://example.com/image3.jpg"
      )
    }

  var imageState by remember {
    mutableStateOf(
      ImagePickerState(
        existingImageUrls = existingImageUrls
      )
    )
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "संपादन मोड (Edit Mode)",
      style = MaterialTheme.typography.headlineSmall
    )

    ImagePickerComponent(
      state = imageState,
      onStateChange = { imageState = it },
      config =
        ImagePickerConfig(
          label = "चित्र/पत्रिकाएं जोड़ें",
          type = ImagePickerType.IMAGE,
          allowMultiple = true,
          maxImages = 10
        ),
      modifier = Modifier.fillMaxWidth()
    )

    if (imageState.hasChanges()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text("परिवर्तन:", style = MaterialTheme.typography.titleMedium)
          if (imageState.newImages.isNotEmpty()) {
            Text("✓ ${imageState.newImages.size} नए चित्र जोड़े गए")
          }
          if (imageState.deletedImageUrls.isNotEmpty()) {
            Text("✓ ${imageState.deletedImageUrls.size} चित्र हटाए गए")
          }
        }
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
      OutlinedButton(
        onClick = {
          // Reset to original state
          imageState = ImagePickerState(existingImageUrls = existingImageUrls)
        },
        enabled = imageState.hasChanges()
      ) {
        Text("रद्द करें")
      }

      Button(
        onClick = {
          // Save changes
          println("Images to upload: ${imageState.newImages}")
          println("Images to delete: ${imageState.deletedImageUrls}")
          println("Images to keep: ${imageState.getActiveImageUrls()}")
        },
        enabled = imageState.hasChanges()
      ) {
        Text("सहेजें")
      }
    }
  }
}

/**
 * Example of using ImagePickerComponent in a real form
 */
@Composable
fun ActivityFormWithImagePickerExample() {
  var activityName by remember { mutableStateOf("") }
  var imageState by remember { mutableStateOf(ImagePickerState()) }
  var showErrors by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current

  val imageConfig =
    ImagePickerConfig(
      label = "संबधित चित्र एवं पत्रिकाएं",
      type = ImagePickerType.IMAGE,
      allowMultiple = true,
      maxImages = 10,
      isMandatory = false
    )

  val imageError =
    if (showErrors) {
      validateImagePickerState(imageState, imageConfig)
    } else {
      null
    }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      "गतिविधि बनाएं",
      style = MaterialTheme.typography.headlineMedium
    )

    OutlinedTextField(
      value = activityName,
      onValueChange = { activityName = it },
      label = { Text("गतिविधि का नाम") },
      modifier = Modifier.fillMaxWidth(),
      isError = showErrors && activityName.isBlank(),
      supportingText = {
        if (showErrors && activityName.isBlank()) {
          Text("नाम आवश्यक है")
        }
      }
    )

    // Image picker integrated into the form
    ImagePickerComponent(
      state = imageState,
      onStateChange = { imageState = it },
      config = imageConfig,
      error = imageError,
      modifier = Modifier.fillMaxWidth()
    )

    Button(
      onClick = {
        showErrors = true

        val isValid = activityName.isNotBlank() && imageError == null

        if (isValid) {
          scope.launch {
            // Upload new images
            val uploadedUrls = mutableListOf<String>()

            try {
              // Simulate uploading images
              imageState.newImages.forEach { file ->
                println("Uploading: ${file.name}")
                // val url = uploadToServer(file)
                // uploadedUrls.add(url)
              }

              // Delete removed images
              imageState.deletedImageUrls.forEach { url ->
                println("Deleting: $url")
                // deleteFromServer(url)
              }

              // Final list of images = existing active + newly uploaded
              val finalImageUrls = imageState.getActiveImageUrls() + uploadedUrls

              println("Activity created with images: $finalImageUrls")

              snackbarHostState.showSnackbar("गतिविधि सफलतापूर्वक बनाई गई")
            } catch (e: Exception) {
              snackbarHostState.showSnackbar("चित्र अपलोड करने में त्रुटि: ${e.message}")
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

@Preview
@Composable
fun ImagePickerExamplePreview() {
  MaterialTheme {
    Surface {
      ImagePickerExample()
    }
  }
}
