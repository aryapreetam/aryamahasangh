package com.aryamahasangh.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.aryamahasangh.imgcompress.*
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.util.GlobalMessageDuration
import com.aryamahasangh.util.GlobalMessageManager
import com.aryamahasangh.utils.logger
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

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

    HorizontalDivider()

    // Example 6: Image Compressor
    ImageCompressorExample()
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

@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun ImageCompressorExample(modifier: Modifier = Modifier) {
  val scope = rememberCoroutineScope()
  val uriHandler = LocalUriHandler.current
  var picked: PlatformFile? by remember { mutableStateOf(null) }
  var mode by remember { mutableStateOf<CompressionConfig>(CompressionConfig.ByQuality(75f)) }
  var maxEdge by remember { mutableStateOf(2560) }
  var originalBytes by remember { mutableStateOf<ByteArray?>(null) }
  var originalMime by remember { mutableStateOf("image/jpeg") }
  var compressed by remember { mutableStateOf<CompressedImage?>(null) }
  var originalDims by remember { mutableStateOf<Pair<Int, Int>?>(null) }
  var compressedDims by remember { mutableStateOf<Pair<Int, Int>?>(null) }

  fun dataUrl(bytes: ByteArray, mime: String): String {
    val b64 = Base64.encode(bytes)
    return "data:$mime;base64,$b64"
  }

  val pickLauncher =
    rememberFilePickerLauncher(
      type = PickerType.Image,
      mode = PickerMode.Single,
      title = "चित्र चुनें"
    ) { files ->
      val file = when (files) {
        is PlatformFile -> files
        is List<*> -> files.filterIsInstance<PlatformFile>().firstOrNull()
        else -> null
      }
      picked = file
      if (file != null) {
        scope.launch {
          originalBytes = file.readBytes()
          compressed = null
          compressedDims = null
          // derive mime from extension
          val ext = file.name.substringAfterLast('.', "").lowercase()
          originalMime = when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "image/jpeg"
          }
          val size = originalBytes?.size ?: 0
          val kb = size.toDouble() / 1024.0
          val kbRounded = (kb * 10.0).roundToInt() / 10.0
          logger.info { "ImageCompressor: original size = ${'$'}size bytes (${kbRounded} KB)" }
        }
      }
    }

  Column(modifier = modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(onClick = { pickLauncher.launch() }) { Text("चित्र चुनें") }

      when (mode) {
        is CompressionConfig.ByQuality -> {
          val q = (mode as CompressionConfig.ByQuality).qualityPercent
          Text("गुणवत्ता: ${q.toInt()}%")
        }

        is CompressionConfig.ByTargetSize -> {
          val kb = (mode as CompressionConfig.ByTargetSize).targetSizeKb
          Text("लक्ष्य आकार: ${kb}KB")
        }
      }
    }

    Spacer(Modifier.height(12.dp))

    // Mode controls
    var tab by rememberSaveable { mutableStateOf(0) }
    TabRow(selectedTabIndex = tab) {
      Tab(selected = tab == 0, onClick = { tab = 0; mode = CompressionConfig.ByQuality(75f) }) { Text("गुणवत्ता") }
      Tab(
        selected = tab == 1,
        onClick = { tab = 1; mode = CompressionConfig.ByTargetSize(100) }) { Text("लक्ष्य आकार") }
    }

    if (tab == 0) {
      val q = (mode as CompressionConfig.ByQuality).qualityPercent
      Slider(
        value = q / 100f,
        onValueChange = { v -> mode = CompressionConfig.ByQuality((v * 100f).coerceIn(0f, 100f)) })
    } else {
      var kbText by rememberSaveable { mutableStateOf("100") }
      OutlinedTextField(
        value = kbText,
        onValueChange = {
          kbText = it.filter { ch -> ch.isDigit() }.ifEmpty { "1" }
          mode = CompressionConfig.ByTargetSize(kbText.toInt())
        },
        label = { Text("KB") })
    }

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
      value = maxEdge.toString(),
      onValueChange = { v -> maxEdge = v.filter { it.isDigit() }.ifEmpty { "2560" }.toInt() },
      label = { Text("अधिकतम लंबी धार (px)") }
    )

    Spacer(Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
      Button(enabled = originalBytes != null, onClick = {
        scope.launch {
          try {
            val bytes = originalBytes ?: return@launch
            val mime = originalMime
            val out = ImageCompressor.compress(
              input = ImageData(bytes, mime),
              config = mode,
              resize = ResizeOptions(maxLongEdgePx = maxEdge)
            )
            compressed = out
            val orig = bytes.size
            val comp = out.compressedSize
            val pct = if (orig > 0) ((1.0 - (comp.toDouble() / orig.toDouble())) * 100.0) else 0.0
            val compKb = comp.toDouble() / 1024.0
            val compKbRounded = (compKb * 10.0).roundToInt() / 10.0
            val pctRounded = (pct * 10.0).roundToInt() / 10.0
            logger.info { "ImageCompressor: compressed size = ${'$'}comp bytes (${compKbRounded} KB), reduction = ${pctRounded}%" }
            originalDims?.let { (ow, oh) ->
              compressedDims?.let { (cw, ch) ->
                logger.info { "ImageCompressor: dimensions original = ${'$'}ow x ${'$'}oh, compressed = ${'$'}cw x ${'$'}ch" }
              }
            }
            GlobalMessageManager.showSuccess("चित्र संपीड़न सफल", GlobalMessageDuration.SHORT)
          } catch (t: Throwable) {
            GlobalMessageManager.showError("संपीड़न विफल — पुनः प्रयास करें", GlobalMessageDuration.LONG)
          }
        }
      }) { Text("संपीड़ित करें") }
    }

    Spacer(Modifier.height(16.dp))

    // Previews (adaptive; no forced full width) + click to open in website via data: URL
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("मूल चित्र")
        val oBytes = originalBytes
        if (oBytes != null) {
          AsyncImage(
            model = oBytes,
            contentDescription = "original",
            modifier = Modifier.size(160.dp).clickable {
              val url = dataUrl(oBytes, originalMime)
              uriHandler.openUri(url)
            },
            onSuccess = { state: AsyncImagePainter.State.Success ->
              val w = state.result.image.width
              val h = state.result.image.height
              originalDims = Pair(w, h)
              logger.info { "ImageCompressor: original dims = $w x $h" }
            }
          )
        }
        Text("आकार: ${originalBytes?.size ?: 0} B")
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("संपीड़ित चित्र")
        val c = compressed
        if (c != null) {
          AsyncImage(
            model = c.bytes,
            contentDescription = "compressed",
            modifier = Modifier.size(160.dp).clickable {
              val url = dataUrl(c.bytes, "image/webp")
              uriHandler.openUri(url)
            },
            onSuccess = { state: AsyncImagePainter.State.Success ->
              val w = state.result.image.width
              val h = state.result.image.height
              compressedDims = Pair(w, h)
              logger.info { "ImageCompressor: compressed dims = $w x $h" }
            }
          )
        }
        Text("आकार: ${compressed?.compressedSize ?: 0} B")
        if (compressed != null && originalBytes != null && originalBytes!!.isNotEmpty()) {
          val pct = ((1.0 - (compressed!!.compressedSize.toDouble() / originalBytes!!.size.toDouble())) * 100).toInt()
          Text("कमी: ${pct}%")
        }
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
