package org.aryamahasangh.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile

/**
 * Type of picker - determines behavior and UI
 */
enum class ImagePickerType {
  IMAGE, // Only images (jpg, png, etc.)
  IMAGE_AND_DOCUMENT, // Images and documents (pdf, doc, etc.)
  PROFILE_PHOTO // Single profile photo with circular preview
}

/**
 * Data class representing image picker configuration
 */
data class ImagePickerConfig(
  val label: String = "चित्र जोड़ें",
  val type: ImagePickerType = ImagePickerType.IMAGE,
  val allowMultiple: Boolean = true,
  val maxImages: Int = 10,
  val supportedFormats: List<String>? = null, // null means use default for type
  val isMandatory: Boolean = false,
  val minImages: Int = 1,
  val showPreview: Boolean = true,
  val previewSize: Int = 100,
  val allowCamera: Boolean = false // Future camera support
) {
  // Computed properties based on type
  val effectiveAllowMultiple: Boolean
    get() = if (type == ImagePickerType.PROFILE_PHOTO) false else allowMultiple

  val effectiveMaxImages: Int
    get() = if (type == ImagePickerType.PROFILE_PHOTO) 1 else maxImages

  val effectiveSupportedFormats: List<String>
    get() =
      supportedFormats ?: when (type) {
        ImagePickerType.IMAGE -> listOf("jpg", "jpeg", "png", "gif", "webp")
        ImagePickerType.IMAGE_AND_DOCUMENT -> listOf("jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx")
        ImagePickerType.PROFILE_PHOTO -> listOf("jpg", "jpeg", "png")
      }
}

/**
 * Data class representing the state of selected images
 */
data class ImagePickerState(
  val newImages: List<PlatformFile> = emptyList(),
  val existingImageUrls: List<String> = emptyList(),
  val deletedImageUrls: Set<String> = emptySet()
) {
  val totalImages: Int
    get() = newImages.size + existingImageUrls.filter { it !in deletedImageUrls }.size

  val hasImages: Boolean
    get() = totalImages > 0
}

/**
 * Comprehensive image picker component for selecting single or multiple images
 *
 * Features:
 * - Single/Multiple image selection
 * - Custom labels in Hindi/Devanagari
 * - Support for existing images (edit mode)
 * - Image preview with thumbnails
 * - Validation with Hindi error messages
 * - Format filtering
 * - Maximum image limits
 * - Profile photo mode with circular preview
 * - Document support
 *
 * @param state Current state of selected images
 * @param onStateChange Callback when image state changes
 * @param config Configuration for the image picker
 * @param error Error message to display
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImagePickerComponent(
  state: ImagePickerState,
  onStateChange: (ImagePickerState) -> Unit,
  config: ImagePickerConfig = ImagePickerConfig(),
  error: String? = null,
  modifier: Modifier = Modifier
) {
  val pickerMode =
    if (config.effectiveAllowMultiple) {
      PickerMode.Multiple()
    } else {
      PickerMode.Single
    }

  val pickerType =
    when (config.type) {
      ImagePickerType.IMAGE, ImagePickerType.PROFILE_PHOTO -> PickerType.Image
      ImagePickerType.IMAGE_AND_DOCUMENT -> PickerType.ImageAndVideo // Using this as a proxy for all files
    }

  val imagePickerLauncher =
    rememberFilePickerLauncher(
      type = pickerType,
      mode = pickerMode,
      title = config.label
    ) { files ->
      if (files != null) {
        val filesToAdd =
          when (files) {
            is List<*> -> files.filterIsInstance<PlatformFile>()
            is PlatformFile -> listOf(files)
            else -> emptyList()
          }

        // Filter by supported formats
        val validFiles =
          filesToAdd.filter { file ->
            val extension = file.name.substringAfterLast('.', "").lowercase()
            extension in config.effectiveSupportedFormats
          }

        // Ensure we don't exceed max images
        val currentTotal = state.totalImages
        val availableSlots = config.effectiveMaxImages - currentTotal
        val filesToKeep = validFiles.take(availableSlots)

        onStateChange(
          state.copy(
            newImages = (state.newImages + filesToKeep).distinct()
          )
        )
      }
    }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Profile photo mode has different layout
    if (config.type == ImagePickerType.PROFILE_PHOTO) {
      ProfilePhotoPickerContent(
        state = state,
        onStateChange = onStateChange,
        config = config,
        error = error,
        onPickerLaunch = { imagePickerLauncher.launch() }
      )
    } else {
      // Regular image/document picker layout
      RegularPickerContent(
        state = state,
        onStateChange = onStateChange,
        config = config,
        error = error,
        onPickerLaunch = { imagePickerLauncher.launch() }
      )
    }
  }
}

@Composable
private fun ProfilePhotoPickerContent(
  state: ImagePickerState,
  onStateChange: (ImagePickerState) -> Unit,
  config: ImagePickerConfig,
  error: String?,
  onPickerLaunch: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Profile photo with overlay
    Box(
      modifier =
        Modifier
          .size(150.dp)
          .clickable {
            if (state.totalImages == 0) {
              onPickerLaunch()
            }
          },
      contentAlignment = Alignment.Center
    ) {
      when {
        // Show existing image
        state.existingImageUrls.isNotEmpty() && state.existingImageUrls.first() !in state.deletedImageUrls -> {
          AsyncImage(
            model = state.existingImageUrls.first(),
            contentDescription = "Profile Photo",
            modifier =
              Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
          )
        }
        // Show new selected image
        state.newImages.isNotEmpty() -> {
          // For profile photo, we don't use PhotoItem to avoid duplicate close button
          // Instead, we'll show the image directly with our own close button
          var bytes by remember(state.newImages.first()) { mutableStateOf<ByteArray?>(null) }

          LaunchedEffect(state.newImages.first()) {
            bytes = state.newImages.first().readBytes()
          }

          bytes?.let {
            AsyncImage(
              model = it,
              contentDescription = "Profile Photo",
              modifier =
                Modifier
                  .size(150.dp)
                  .clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )
          }
        }
        // Show placeholder
        else -> {
          AsyncImage(
            model = null,
            contentDescription = "Profile Photo Placeholder",
            modifier =
              Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
            placeholder =
              BrushPainter(
                Brush.linearGradient(
                  listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFDDDDDD)
                  )
                )
              )
          )

          // Overlay for adding photo
          Box(
            modifier =
              Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                modifier = Modifier.size(40.dp),
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = "Upload Photo",
                tint = Color.White
              )
              Text(
                config.label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
              )
            }
          }
        }
      }

      // Remove button for profile photo
      if (state.totalImages > 0) {
        Surface(
          modifier =
            Modifier
              .align(Alignment.TopEnd)
              .padding(4.dp)
              .size(32.dp)
              .clickable {
                if (state.newImages.isNotEmpty()) {
                  onStateChange(state.copy(newImages = emptyList()))
                } else if (state.existingImageUrls.isNotEmpty()) {
                  onStateChange(
                    state.copy(
                      deletedImageUrls = state.deletedImageUrls + state.existingImageUrls.first()
                    )
                  )
                }
              },
          shape = CircleShape,
          color = MaterialTheme.colorScheme.error
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "हटाएं",
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onError
            )
          }
        }
      }
    }

    // Change photo button
    if (state.totalImages > 0) {
      OutlinedButton(
        onClick = onPickerLaunch,
        modifier = Modifier.padding(top = 8.dp)
      ) {
        Text("चित्र बदलें")
      }
    }

    // Error message
    error?.let {
      Text(
        text = it,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegularPickerContent(
  state: ImagePickerState,
  onStateChange: (ImagePickerState) -> Unit,
  config: ImagePickerConfig,
  error: String?,
  onPickerLaunch: () -> Unit
) {
  // Header with label and add button
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = config.label,
      style = MaterialTheme.typography.titleMedium
    )

    if (state.totalImages < config.effectiveMaxImages) {
      OutlinedButton(
        onClick = onPickerLaunch,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Icon(
          if (config.allowCamera) Icons.Default.PhotoCamera else Icons.Default.Add,
          contentDescription = "जोड़ें",
          modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("जोड़ें")
      }
    }
  }

  // Show selected images count
  if (state.totalImages > 0) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "${state.totalImages}/${config.effectiveMaxImages} ${
          if (config.type == ImagePickerType.IMAGE_AND_DOCUMENT) "संचिका" else "चित्र"
        } चुने गए",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (state.hasChanges()) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.padding(horizontal = 4.dp)
        ) {
          Text(
            text = "संशोधित",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }
    }
  }

  // Image preview grid
  if (config.showPreview) {
    if (state.totalImages == 0) {
      // Empty state
      Card(
        modifier =
          Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onPickerLaunch() },
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
        border =
          BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          )
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = config.label,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              if (config.isMandatory) {
                "${if (config.type == ImagePickerType.IMAGE_AND_DOCUMENT) "प्रलेख" else "चित्र"} चुनना अपेक्षित है"
              } else {
                "${if (config.type == ImagePickerType.IMAGE_AND_DOCUMENT) "प्रलेख" else "चित्र"} चुनने के लिए यहाँ क्लिक करें"
              },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      // Image grid
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Show existing images (from URLs)
        state.existingImageUrls.filterNot { it in state.deletedImageUrls }.forEach { url ->
          ImageThumbnail(
            imageUrl = url,
            size = config.previewSize,
            onRemove = {
              onStateChange(
                state.copy(
                  deletedImageUrls = state.deletedImageUrls + url
                )
              )
            },
            showDeletedOverlay = false,
            isDocument =
              config.type == ImagePickerType.IMAGE_AND_DOCUMENT &&
                url.substringAfterLast('.').lowercase() in listOf("pdf", "doc", "docx")
          )
        }

        // Show newly selected images
        state.newImages.forEach { file ->
          ImageThumbnail(
            file = file,
            size = config.previewSize,
            onRemove = {
              onStateChange(
                state.copy(
                  newImages = state.newImages.filter { it != file }
                )
              )
            },
            showDeletedOverlay = false,
            isDocument =
              config.type == ImagePickerType.IMAGE_AND_DOCUMENT &&
                file.name.substringAfterLast('.').lowercase() in listOf("pdf", "doc", "docx")
          )
        }

        // Add more button if not at max
        if (state.totalImages < config.effectiveMaxImages) {
          AddImageButton(
            size = config.previewSize,
            onClick = onPickerLaunch
          )
        }
      }
    }
  }

  // Error message
  error?.let {
    Text(
      text = it,
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.padding(start = 16.dp)
    )
  }
}

/**
 * Image thumbnail with remove button
 * Fixed to avoid duplicate close buttons when using PhotoItem
 */
@Composable
private fun ImageThumbnail(
  imageUrl: String? = null,
  file: PlatformFile? = null,
  size: Int,
  onRemove: () -> Unit,
  showDeletedOverlay: Boolean = false,
  isDocument: Boolean = false
) {
  Box(
    modifier = Modifier.size(size.dp)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      when {
        isDocument -> {
          // Show document icon for non-image files
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Default.PhotoLibrary, // You can use a document icon here
                contentDescription = "Document",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text =
                  file?.name?.substringAfterLast('.')?.uppercase()
                    ?: imageUrl?.substringAfterLast('.')?.uppercase()
                    ?: "DOC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        imageUrl != null -> {
          AsyncImage(
            model = imageUrl,
            contentDescription = "चित्र",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
        file != null -> {
          // For platform files, we show the image directly without using PhotoItem
          // to avoid duplicate close buttons
          var bytes by remember(file) { mutableStateOf<ByteArray?>(null) }

          LaunchedEffect(file) {
            bytes = file.readBytes()
          }

          bytes?.let {
            AsyncImage(
              model = it,
              contentDescription = file.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }

    // Show deleted overlay if needed
    if (showDeletedOverlay) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            "मिटाया गया",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onError
          )
        }
      }
    }

    // Single remove button - no duplicate from PhotoItem
    Surface(
      modifier =
        Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
          .size(24.dp)
          .clickable { onRemove() },
      shape = CircleShape,
      color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "हटाएं",
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onError
        )
      }
    }
  }
}

/**
 * Add image button
 */
@Composable
private fun AddImageButton(
  size: Int,
  onClick: () -> Unit
) {
  Surface(
    modifier =
      Modifier
        .size(size.dp)
        .clickable { onClick() },
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border =
      BorderStroke(
        width = 2.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      )
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = "और जोड़ें",
        modifier = Modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "जोड़ें",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Utility function to validate image picker state
 */
fun validateImagePickerState(
  state: ImagePickerState,
  config: ImagePickerConfig
): String? {
  return when {
    config.isMandatory && state.totalImages < config.minImages -> {
      if (config.minImages == 1) {
        when (config.type) {
          ImagePickerType.PROFILE_PHOTO -> "व्यक्तिगत चित्र चुनना आवश्यक है"
          ImagePickerType.IMAGE_AND_DOCUMENT -> "न्यूनतम एक प्रलेख चुनना आवश्यक है"
          else -> "न्यूनतम एक चित्र चुनना आवश्यक है"
        }
      } else {
        when (config.type) {
          ImagePickerType.IMAGE_AND_DOCUMENT -> "न्यूनतम ${config.minImages} प्रलेख चुनना आवश्यक है"
          else -> "न्यूनतम ${config.minImages} चित्र चुनना आवश्यक है"
        }
      }
    }

    state.totalImages > config.effectiveMaxImages -> {
      when (config.type) {
        ImagePickerType.IMAGE_AND_DOCUMENT -> "अधिकतम ${config.effectiveMaxImages} प्रलेख ही चुने जा सकते हैं"
        else -> "अधिकतम ${config.effectiveMaxImages} चित्र ही चुने जा सकते हैं"
      }
    }

    else -> null
  }
}

/**
 * Extension function to get all active image URLs (existing minus deleted)
 */
fun ImagePickerState.getActiveImageUrls(): List<String> {
  return existingImageUrls.filterNot { it in deletedImageUrls }
}

/**
 * Extension function to check if state has been modified
 */
fun ImagePickerState.hasChanges(): Boolean {
  return newImages.isNotEmpty() || deletedImageUrls.isNotEmpty()
}
