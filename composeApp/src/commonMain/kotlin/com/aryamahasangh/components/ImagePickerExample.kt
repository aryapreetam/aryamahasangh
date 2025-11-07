package com.aryamahasangh.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aryamahasangh.imgcompress.*
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.util.GlobalMessageDuration
import com.aryamahasangh.util.GlobalMessageManager
import com.aryamahasangh.util.Result
import com.aryamahasangh.utils.FileUploadUtils
import com.aryamahasangh.utils.logger
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

// Platform-specific expect/actual pattern for getting image dimensions
expect fun getImageDimensions(bytes: ByteArray): Pair<Int, Int>?

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

    HorizontalDivider()

    // Example 7: Blur Effect Test
    BlurEffectTestSection()
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
private fun ImageCompressorExample(
  modifier: Modifier = Modifier
) {
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
  var isUploading by remember { mutableStateOf(false) }
  var uploadedUrl by remember { mutableStateOf<String?>(null) }

  fun dataUrl(bytes: ByteArray, mime: String): String {
    val b64 = Base64.encode(bytes)
    return "data:$mime;base64,$b64"
  }
  
  fun formatDimensions(dims: Pair<Int, Int>?): String {
    return dims?.let { "${it.first} × ${it.second}" } ?: "अज्ञात"
  }
  
  fun formatFileSize(bytes: Int): String {
    return when {
      bytes >= 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} MB"
      bytes >= 1024 -> "${(bytes / 1024.0 * 10).roundToInt() / 10.0} KB"
      else -> "$bytes B"
    }
  }

  val pickLauncher =
    rememberFilePickerLauncher(
      type = FileKitType.Image,
      mode = FileKitMode.Single,
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
          val bytes = file.readBytes()
          originalBytes = bytes
          compressed = null
          compressedDims = null
          uploadedUrl = null
          
          // Get actual image dimensions from bytes
          originalDims = getImageDimensions(bytes)
          
          // derive mime from extension
          val ext = file.name.substringAfterLast('.', "").lowercase()
          originalMime = when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "image/jpeg"
          }
          val size = bytes.size
          logger.info { "ImageCompressor: original size = $size bytes (${formatFileSize(size)})" }
          originalDims?.let { (w, h) ->
            logger.info { "ImageCompressor: original dimensions = $w x $h" }
          }
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

      // Sync kbText with current mode when tab changes
      LaunchedEffect(tab) {
        if (tab == 1 && mode is CompressionConfig.ByTargetSize) {
          kbText = (mode as CompressionConfig.ByTargetSize).targetSizeKb.toString()
        }
      }

      OutlinedTextField(
        value = kbText,
        onValueChange = { newValue ->
          val filteredValue = newValue.filter { ch -> ch.isDigit() }.ifEmpty { "1" }
          kbText = filteredValue
          val targetKb = filteredValue.toIntOrNull()?.coerceIn(1, 10000) ?: 1
          mode = CompressionConfig.ByTargetSize(targetKb)
        },
        label = { Text("KB") },
        supportingText = { Text("1-10000 KB की रेंज में") }
      )
    }

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
      value = maxEdge.toString(),
      onValueChange = { v -> maxEdge = v.filter { it.isDigit() }.ifEmpty { "2560" }.toInt() },
      label = { Text("अधिकतम लंबी धार (px)") }
    )

    Spacer(Modifier.height(16.dp))

    // Action buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(enabled = originalBytes != null, onClick = {
        scope.launch {
          try {
            val bytes = originalBytes ?: return@launch
            val mime = originalMime

            // Debug: Log the actual compression configuration being used
            when (mode) {
              is CompressionConfig.ByQuality -> {
                val quality = (mode as CompressionConfig.ByQuality).qualityPercent
                logger.info { "ImageCompressor: Using Quality mode with ${quality}% quality" }
              }

              is CompressionConfig.ByTargetSize -> {
                val targetKb = (mode as CompressionConfig.ByTargetSize).targetSizeKb
                val targetBytes = targetKb * 1024
                val minBytes = targetBytes
                val maxBytes = targetBytes + (targetBytes * 0.30).toInt()
                logger.info { "ImageCompressor: Using Target Size mode with ${targetKb}KB target (${targetBytes} bytes)" }
                logger.info { "ImageCompressor: Acceptable range: ${minBytes} bytes (${targetKb}KB) to ${maxBytes} bytes (${(maxBytes / 1024)}KB)" }
                logger.info { "ImageCompressor: Quality-preserving tolerance: NEVER below ${targetKb}KB, up to +30% acceptable" }
              }
            }

            val startTime = Clock.System.now()
            val out = ImageCompressor.compress(
              input = ImageData(bytes, mime),
              config = mode,
              resize = ResizeOptions(maxLongEdgePx = maxEdge)
            )
            val totalTime = Clock.System.now() - startTime
            compressed = out
            uploadedUrl = null // Reset upload state
            
            // Get compressed image dimensions
            compressedDims = getImageDimensions(out.bytes)
            
            val orig = bytes.size
            val comp = out.compressedSize
            val pct = if (orig > 0) ((1.0 - (comp.toDouble() / orig.toDouble())) * 100.0) else 0.0
            val pctRounded = (pct * 10.0).roundToInt() / 10.0

            // Enhanced logging with performance benchmarks
            logger.info { "ImageCompressor: compressed size = $comp bytes (${formatFileSize(comp)}), reduction = ${pctRounded}%" }
            logger.info { "ImageCompressor: total time = ${totalTime.inWholeMilliseconds}ms" }

            // VALIDATION: Check if we met target size requirements
            if (mode is CompressionConfig.ByTargetSize) {
              val targetKb = (mode as CompressionConfig.ByTargetSize).targetSizeKb
              val targetBytes = targetKb * 1024
              val actualKb = comp / 1024
              val maxAllowedBytes = targetBytes + (targetBytes * 0.30).toInt()
              val maxAllowedKb = maxAllowedBytes / 1024

              val status = when {
                comp < targetBytes -> {
                  logger.error { "🚨 CRITICAL: Result is ${targetBytes - comp} bytes (${targetKb - actualKb}KB) BELOW target!" }
                  logger.error { "🚨 This should NEVER happen with ultra-conservative quality prediction!" }
                  "❌ BELOW TARGET (should never happen!)"
                }

                comp > maxAllowedBytes -> {
                  logger.warn { "⚠️ Result is ${comp - maxAllowedBytes} bytes (${actualKb - maxAllowedKb}KB) above +30% tolerance" }
                  "⚠️ ABOVE +30% TOLERANCE"
                }

                else -> {
                  logger.info { "✅ Perfect! Result is ${comp - targetBytes} bytes (${actualKb - targetKb}KB) above target within tolerance" }
                  "✅ WITHIN ACCEPTABLE RANGE"
                }
              }

              logger.info { "ImageCompressor: Target Size Validation: $status" }
              logger.info { "ImageCompressor: Target: ${targetKb}KB (${targetBytes} bytes)" }
              logger.info { "ImageCompressor: Actual: ${actualKb}KB (${comp} bytes)" }
              logger.info { "ImageCompressor: Range: ${targetKb}KB-${maxAllowedKb}KB (${targetBytes}-${maxAllowedBytes} bytes)" }
              logger.info { "ImageCompressor: Margin: ${if (comp >= targetBytes) "+" else ""}${comp - targetBytes} bytes (${if (actualKb >= targetKb) "+" else ""}${actualKb - targetKb}KB)" }
            }

            compressedDims?.let { (w, h) ->
              logger.info { "ImageCompressor: compressed dimensions = $w x $h" }
            }
            out.metadata?.let { metadata ->
              logger.info { "ImageCompressor: internal processing time = ${metadata.elapsedMillis}ms" }
              logger.info { "ImageCompressor: iterations = ${metadata.iterations}" }
              metadata.estimatedQuality?.let { estimate ->
                logger.info { "ImageCompressor: estimated quality = $estimate" }
              }
              metadata.searchRange?.let { range ->
                logger.info { "ImageCompressor: search range = ${range.start}-${range.endInclusive}" }
              }
            }
            GlobalMessageManager.showSuccess("चित्र संपीड़न सफल (${totalTime.inWholeMilliseconds}ms)", GlobalMessageDuration.SHORT)
          } catch (t: Throwable) {
            GlobalMessageManager.showError("संपीड़न विफल — पुनः प्रयास करें", GlobalMessageDuration.LONG)
          }
        }
      }) { Text("संपीड़ित करें") }

      // Upload button
      Button(
        enabled = compressed != null && !isUploading,
        onClick = {
          scope.launch {
            isUploading = true
            try {
              val compressedImage = compressed ?: return@launch
              val result = FileUploadUtils.uploadCompressedImage(
                imageBytes = compressedImage.bytes,
                folder = "test"
              )

              when (result) {
                is Result.Success -> {
                  uploadedUrl = result.data
                  GlobalMessageManager.showSuccess(
                    "छवि अपलोड सफल - Supabase 'test' में संग्रहीत",
                    GlobalMessageDuration.LONG
                  )
                  logger.info { "ImageCompressor: uploaded to Supabase: ${result.data}" }
                }

                is Result.Error -> {
                  GlobalMessageManager.showError(
                    "अपलोड विफल: ${result.message}",
                    GlobalMessageDuration.LONG
                  )
                  logger.error { "ImageCompressor: upload failed: ${result.message}" }
                }

                is Result.Loading -> {
                  // This shouldn't happen with FileUploadUtils.uploadCompressedImage
                  // as it returns Success or Error directly, but handle it for completeness
                  logger.info { "ImageCompressor: unexpected loading state during upload" }
                }
              }
            } catch (e: Exception) {
              GlobalMessageManager.showError(
                "अपलोड त्रुटि: ${e.message}",
                GlobalMessageDuration.LONG
              )
              logger.error(e) { "ImageCompressor: upload exception" }
            } finally {
              isUploading = false
            }
          }
        }
      ) {
        if (isUploading) {
          Text("अपलोड हो रहा है...")
        } else {
          Text("Supabase में अपलोड करें")
        }
      }
    }

    Spacer(Modifier.height(16.dp))

    // Previews (adaptive; no forced full width) + click to open in website via data: URL
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("मूल चित्र", style = MaterialTheme.typography.titleMedium)
        val oBytes = originalBytes
        if (oBytes != null) {
          AsyncImage(
            model = oBytes,
            contentDescription = "original",
            modifier = Modifier.size(160.dp).clickable {
              val url = dataUrl(oBytes, originalMime)
              uriHandler.openUri(url)
            }
          )
        }
        
        // Enhanced info display
        Card(
          modifier = Modifier.width(160.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("आकार: ${formatFileSize(originalBytes?.size ?: 0)}", style = MaterialTheme.typography.bodySmall)
            Text("रिज़ॉल्यूशन: ${formatDimensions(originalDims)}", style = MaterialTheme.typography.bodySmall)
            originalDims?.let { (w, h) ->
              val mp = (w * h) / 1_000_000.0
              Text("मेगापिक्सेल: ${(mp * 10).roundToInt() / 10.0} MP", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
      
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("संपीड़ित चित्र", style = MaterialTheme.typography.titleMedium)
        val c = compressed
        if (c != null) {
          AsyncImage(
            model = c.bytes,
            contentDescription = "compressed",
            modifier = Modifier.size(160.dp).clickable {
              val url = dataUrl(c.bytes, "image/webp")
              uriHandler.openUri(url)
            }
          )
        }
        
        // Enhanced info display
        Card(
          modifier = Modifier.width(160.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("आकार: ${formatFileSize(compressed?.compressedSize ?: 0)}", style = MaterialTheme.typography.bodySmall)
            Text("रिज़ॉल्यूशन: ${formatDimensions(compressedDims)}", style = MaterialTheme.typography.bodySmall)
            compressedDims?.let { (w, h) ->
              val mp = (w * h) / 1_000_000.0
              Text("मेगापिक्सेल: ${(mp * 10).roundToInt() / 10.0} MP", style = MaterialTheme.typography.bodySmall)
            }
            if (compressed != null && originalBytes != null && originalBytes!!.isNotEmpty()) {
              val pct = ((1.0 - (compressed!!.compressedSize.toDouble() / originalBytes!!.size.toDouble())) * 100).toInt()
              Text("कमी: ${pct}%", 
                style = MaterialTheme.typography.bodySmall,
                color = if (pct > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
              )
            }
            
            // Show compression metadata
            compressed?.metadata?.let { metadata ->
              Text("समय: ${metadata.elapsedMillis}ms", style = MaterialTheme.typography.bodySmall)
              Text("पुनरावृत्ति: ${metadata.iterations}", style = MaterialTheme.typography.bodySmall)
              metadata.effectiveQualityPercent?.let { quality ->
                Text("गुणवत्ता: ${quality.toInt()}%", style = MaterialTheme.typography.bodySmall)
              }
              metadata.searchRange?.let { range ->
                Text("खोज सीमा: ${range.start}-${range.endInclusive}", style = MaterialTheme.typography.bodySmall)
              }
              metadata.engineUsed?.let { engine ->
                Text("इंजन: $engine", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }

    // Upload URL display
    uploadedUrl?.let { url ->
      Spacer(Modifier.height(16.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Supabase अपलोड सफल", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("बकेट:")
            Text("test")
          }
          Row(modifier = Modifier.fillMaxWidth()) {
            Text("URL: ")
            Text(
              text = url,
              style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  uriHandler.openUri(url)
                }
            )
          }
        }
      }
    }

    // Performance summary
    compressed?.metadata?.let { metadata ->
      Spacer(Modifier.height(16.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("प्रदर्शन विवरण", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("संपीड़न समय:")
            Text("${metadata.elapsedMillis}ms")
          }
          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("पुनरावृत्तियां:")
            Text("${metadata.iterations}")
          }
          metadata.estimatedQuality?.let { estimate ->
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Text("अनुमानित गुणवत्ता:")
              Text("${estimate}%")
            }
          }
          metadata.searchRange?.let { range ->
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
              Text("खोज सीमा:")
              Text("${range.start}-${range.endInclusive}")
            }
          }
          
          // Resolution change info
          if (originalDims != null && compressedDims != null) {
            val (ow, oh) = originalDims!!
            val (cw, ch) = compressedDims!!
            if (ow != cw || oh != ch) {
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("रिज़ॉल्यूशन परिवर्तन:")
                Text("${ow}×${oh} → ${cw}×${ch}")
              }
              val scaleFactor = kotlin.math.min(cw.toDouble() / ow, ch.toDouble() / oh)
              Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("स्केल फैक्टर:")
                Text("${(scaleFactor * 100).roundToInt()}%")
              }
            }
          }
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

@Composable
private fun BlurEffectTestSection() {
  var blurRadius by remember { mutableStateOf(0f) }
  val imageUrl = "https://images.unsplash.com/photo-1519125323398-675f0ddb6308?w=400" // Free Unsplash photo

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "Blur Effect Test (Modifier.blur)",
      style = MaterialTheme.typography.headlineSmall
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Blur: ${blurRadius.toInt()} px")
      Slider(
        value = blurRadius,
        onValueChange = { blurRadius = it },
        valueRange = 0f..30f,
        steps = 29
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No Blur", style = MaterialTheme.typography.bodySmall)
        Box(
          modifier = Modifier
            .size(120.dp)
            .padding(4.dp),
        ) {
          AsyncImage(
            model = imageUrl,
            contentDescription = "Original Image",
            modifier = Modifier
              .fillMaxSize()
              .clip(MaterialTheme.shapes.medium),
          )
          // Outline for visibility
          Box(Modifier.matchParentSize().border(2.dp, Color.LightGray, MaterialTheme.shapes.medium))
        }
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Blurred", style = MaterialTheme.typography.bodySmall)
        Box(
          modifier = Modifier
            .size(120.dp)
            .padding(4.dp)
            .blur(blurRadius.dp)
        ) {
          AsyncImage(
            model = imageUrl,
            contentDescription = "Blurred Image",
            modifier = Modifier
              .fillMaxSize()
              .clip(MaterialTheme.shapes.medium),
          )
          // Outline for visibility
          Box(Modifier.matchParentSize().border(2.dp, Color.LightGray, MaterialTheme.shapes.medium))
        }
      }
    }
    Text(
      "आपकी प्लेटफ़ॉर्म/ब्राउज़र/OS में ऊपर का Blur सही दिखता है? (Should look blurred if blur is supported)",
      style = MaterialTheme.typography.bodySmall,
      color = Color.Gray
    )
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
