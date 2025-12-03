package com.aryamahasangh.features.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.aryamahasangh.components.*
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.util.Result
import com.aryamahasangh.utils.FileUploadUtils
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOverviewFormScreen(
  activityId: String,
  existingOverview: String? = null,
  existingMediaUrls: List<String> = emptyList(),
  onNavigateBack: () -> Unit,
  onSuccess: () -> Unit,
  viewModel: ActivitiesViewModel = koinInject()
) {
  val fileUploadUtils = koinInject<FileUploadUtils>()
  val snackbarHostState = LocalSnackbarHostState.current ?: return
  val keyboardController = LocalSoftwareKeyboardController.current
  val scope = rememberCoroutineScope()

  // Form state
  var overviewDescription by rememberSaveable { mutableStateOf(existingOverview ?: "") }
  var imagePickerState by remember {
    mutableStateOf(
      ImagePickerState(
        existingImageUrls = existingMediaUrls
      )
    )
  }

  // Validation state
  var descriptionError by remember { mutableStateOf<String?>(null) }
  var imageError by remember { mutableStateOf<String?>(null) }
  var hasUnsavedChanges by remember { mutableStateOf(false) }

  // Loading state
  var isSubmitting by remember { mutableStateOf(false) }

  // Check for unsaved changes
  LaunchedEffect(overviewDescription, imagePickerState) {
    val hasTextChanges = overviewDescription != (existingOverview ?: "")
    val hasImageChanges = imagePickerState.hasChanges()
    hasUnsavedChanges = hasTextChanges || hasImageChanges
  }

  // Handle back navigation with unsaved changes
  fun handleBackPress() {
    if (hasUnsavedChanges) {
      scope.launch {
        val result =
          snackbarHostState.showSnackbar(
            message = "आपके परिवर्तन असंचयित है। क्या आप वापस जाना चाहते हैं?",
            actionLabel = "हां",
            withDismissAction = true
          )
        if (result == SnackbarResult.ActionPerformed) {
          onNavigateBack()
        }
      }
    } else {
      onNavigateBack()
    }
  }

  // Validation functions
  fun validateForm(): Boolean {
    var isValid = true

    // Validate description
    if (overviewDescription.isBlank()) {
      descriptionError = "अवलोकन विवरण आवश्यक है"
      isValid = false
    } else {
      descriptionError = null
    }

    // Validate images
    val imagePickerConfig =
      ImagePickerConfig(
        isMandatory = true,
        minImages = 1
      )
    val imageValidationError = validateImagePickerState(imagePickerState, imagePickerConfig)
    imageError = imageValidationError
    if (imageValidationError != null) {
      isValid = false
    }

    return isValid
  }

  // Submit function
  fun submitOverview() {
    if (!validateForm()) return

    scope.launch {
      isSubmitting = true
      keyboardController?.hide()

      try {
        // Upload new images first
        val uploadedUrls = mutableListOf<String>()

        // Add existing images that weren't deleted
        uploadedUrls.addAll(
          imagePickerState.existingImageUrls.filterNot { it in imagePickerState.deletedImageUrls }
        )

        // Upload new images
        imagePickerState.newImages.forEach { file ->
          val imageBytes = if (imagePickerState.hasCompressedData(file)) {
            imagePickerState.getCompressedBytes(file)!!
          } else {
            file.readBytes()
          }

          val uploadResult = fileUploadUtils.uploadCompressedImage(
            imageBytes = imageBytes,
            folder = "activity_overview",
            extension = "webp"
          )

          when (uploadResult) {
            is Result.Success -> uploadedUrls.add(uploadResult.data)
            is Result.Error -> {
              snackbarHostState.showSnackbar(
                message = "चित्र अपलोड करने में त्रुटि: ${uploadResult.message}"
              )
              return@launch
            }
            else -> {}
          }
        }

        // Call API to save overview
        viewModel.addActivityOverview(activityId, overviewDescription, uploadedUrls)
          .collect { result ->
            when (result) {
              is Result.Loading -> {
                // Already showing loading state
              }

              is Result.Success -> {
                if (result.data) {
                  snackbarHostState.showSnackbar(
                    message = "अवलोकन सफलतापूर्वक अद्यतन किया गया"
                  )
                  onSuccess()
                } else {
                  snackbarHostState.showSnackbar(
                    message = "अवलोकन अद्यतन करने में त्रुटि हुई"
                  )
                }
              }

              is Result.Error -> {
                snackbarHostState.showSnackbar(
                  message = "त्रुटि: ${result.message}"
                )
              }
            }
          }
      } catch (e: Exception) {
        snackbarHostState.showSnackbar(
          message = "त्रुटि: ${e.message}"
        )
      } finally {
        isSubmitting = false
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(if (existingOverview != null) "अवलोकन संपादित करें" else "अवलोकन लिखें")
        },
        navigationIcon = {
          IconButton(onClick = { handleBackPress() }) {
            Icon(Icons.Default.ArrowBack, contentDescription = "वापस")
          }
        },
        actions = {
          if (hasUnsavedChanges) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Text(
                "असंचयित परिवर्तन",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Description field
      OutlinedTextField(
        value = overviewDescription,
        onValueChange = {
          overviewDescription = it
          if (descriptionError != null) {
            descriptionError = null
          }
        },
        label = { Text("अवलोकन विवरण *") },
        placeholder = { Text("गतिविधि के बारे में विस्तृत अवलोकन लिखें...") },
        isError = descriptionError != null,
        supportingText = descriptionError?.let { { Text(it) } },
        keyboardOptions =
          KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences
          ),
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp),
        minLines = 6,
        maxLines = 10
      )

      // Image picker
      Card(
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          ImagePickerComponent(
            state = imagePickerState,
            onStateChange = { newState ->
              imagePickerState = newState
              if (imageError != null) {
                imageError = null
              }
            },
            config =
              ImagePickerConfig(
                label = "अवलोकन चित्र",
                isMandatory = true,
                minImages = 1,
                maxImages = 5,
                allowMultiple = true,
                enableBackgroundCompression = true,
                compressionTargetKb = 100, // 100KB for overview images
                showCompressionProgress = true
              ),
            error = imageError
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Submit button
      Button(
        onClick = { submitOverview() },
        enabled = !isSubmitting && overviewDescription.isNotBlank() && imagePickerState.hasImages,
        modifier = Modifier.fillMaxWidth()
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          if (isSubmitting) {
            "अद्यतन किया जा रहा है..."
          } else if (existingOverview != null) {
            "अवलोकन अद्यतन करें"
          } else {
            "अवलोकन अद्यतन करें"
          }
        )
      }

      // Help text
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
          )
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            "सहायता:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "• अवलोकन में गतिविधि की मुख्य बातें, सफलताएं, कुल उपस्थिति और सीखे गए पाठ शामिल करें\n" +
              "• न्यूनतम एक चित्र अपलोड करना आवश्यक है\n" +
              "• अधिकतम 5 चित्र अपलोड कर सकते हैं",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
