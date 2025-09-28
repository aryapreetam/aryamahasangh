package com.aryamahasangh.features.gurukul.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormEffect
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormIntent
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormUiState
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormViewModel
import com.aryamahasangh.ui.components.buttons.*
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun CourseRegistrationFormScreen(
  activityId: String,
  modifier: Modifier = Modifier
) {
  // ViewModel provided via DI (parameterized)
  val viewModel: CourseRegistrationFormViewModel = koinInject(parameters = { parametersOf(activityId) })
  // Use the collectUiState method which properly handles Molecule
  val uiState = viewModel.collectUiState()

  // Listen for navigation effects
  val uiEffect = viewModel.uiEffect.collectAsState(null).value
  LaunchedEffect(uiEffect) {
    when (uiEffect) {
      is CourseRegistrationFormEffect.NavigateBack -> {
        // Here we would navigate back
        // In a real implementation this would use a Navigator abstraction
      }

      is CourseRegistrationFormEffect.Error -> {
        // Here we would show an error snackbar with the error message
      }

      is CourseRegistrationFormEffect.Success -> {
        // Here we would handle success (probably navigate away)
      }

      else -> {}
    }
  }

  // Layout: Adaptive vertical arrangement, Material3
  Column(modifier = modifier.width(500.dp).padding(16.dp)) {
    Text(
      text = "कक्षा प्रवेश पंजीकरण",
      style = MaterialTheme.typography.headlineSmall
    )
    OutlinedTextField(
      value = uiState.name,
      onValueChange = { viewModel.sendIntent(CourseRegistrationFormIntent.NameChanged(it)) },
      label = { Text("नाम") },
      modifier = Modifier.fillMaxWidth().testTag("registrationFormNameField"),
      isError = uiState.fieldErrors.containsKey(CourseRegistrationFormUiState.Field.NAME),
      supportingText = {
        uiState.fieldErrors[CourseRegistrationFormUiState.Field.NAME]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      },
      singleLine = true,
      enabled = !uiState.isLoading
    )
    Spacer(Modifier.height(16.dp))

    com.aryamahasangh.components.DatePickerField(
      value = uiState.satrDate.takeIf { it.isNotBlank() }?.let { kotlinx.datetime.LocalDate.parse(it) },
      onValueChange = { viewModel.sendIntent(CourseRegistrationFormIntent.SatrDateChanged(it?.toString() ?: "")) },
      label = "सत्र दिनांक",
      modifier = Modifier.fillMaxWidth().testTag("registrationFormSatrDateField"),
      isError = uiState.fieldErrors.containsKey(CourseRegistrationFormUiState.Field.DATE),
      supportingText = {
        uiState.fieldErrors[CourseRegistrationFormUiState.Field.DATE]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      },
      required = true
    )
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
      value = uiState.satrPlace,
      onValueChange = { viewModel.sendIntent(CourseRegistrationFormIntent.SatrPlaceChanged(it)) },
      label = { Text("सत्र स्थान") },
      modifier = Modifier.fillMaxWidth().testTag("registrationFormSatrPlaceField"),
      isError = uiState.fieldErrors.containsKey(CourseRegistrationFormUiState.Field.PLACE),
      supportingText = {
        uiState.fieldErrors[CourseRegistrationFormUiState.Field.PLACE]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      },
      singleLine = true,
      enabled = !uiState.isLoading
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = "निचे संरक्षक की संस्तुति के साथ उनका का नाम, व्यवसाय, योग्यता भी लिखे"
    )
    OutlinedTextField(
      value = uiState.recommendation,
      onValueChange = { viewModel.sendIntent(CourseRegistrationFormIntent.RecommendationChanged(it)) },
      label = { Text("संरक्षक की संस्तुति(Recommendation)") },
      modifier = Modifier.fillMaxWidth().testTag("registrationFormRecommendationField"),
      isError = uiState.fieldErrors.containsKey(CourseRegistrationFormUiState.Field.RECOMMENDATION),
      supportingText = {
        uiState.fieldErrors[CourseRegistrationFormUiState.Field.RECOMMENDATION]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      },
      minLines = 3,
      maxLines = 8,
      enabled = !uiState.isLoading
    )
    Spacer(Modifier.height(16.dp))

    // ImagePickerComponent 
    com.aryamahasangh.components.ImagePickerComponent(
      state = uiState.imagePickerState,
      onStateChange = { viewModel.sendIntent(CourseRegistrationFormIntent.ImagePickerStateChanged(it)) },
      modifier = Modifier.testTag("registrationFormImagePicker"),
      config = com.aryamahasangh.components.ImagePickerConfig(
        label = "भुगतान रसीद जोड़ें",
        type = com.aryamahasangh.components.ImagePickerType.IMAGE,
        allowMultiple = false,
        isMandatory = true,
        minImages = 1,
        maxImages = 1
      ),
      error = uiState.fieldErrors[CourseRegistrationFormUiState.Field.RECEIPT]
    )
    Spacer(Modifier.height(24.dp))

    SubmitButton(
      text = "पंजीकरण प्रस्तुत करें",
      onSubmit = {
        // This will be called when validation passes
        viewModel.sendIntent(CourseRegistrationFormIntent.Submit)
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        validator = {
          // Return null if validation passes, or a SubmissionError if it fails
          when {
            uiState.name.isBlank() -> {
              return@SubmitButtonConfig SubmissionError.ValidationFailed
            }
            uiState.satrDate.isBlank() -> {
              return@SubmitButtonConfig SubmissionError.Custom("कृपया सत्र दिवसांक चुनें")
            }
            uiState.satrPlace.isBlank() -> {
              return@SubmitButtonConfig SubmissionError.Custom("कृपया सत्र स्थान दर्ज करें")
            }
            uiState.imagePickerState.hasImages.not() -> {
              return@SubmitButtonConfig SubmissionError.Custom("कृपया भुगतान रसीद अपलोड करें")
            }
            // Add any other validation as needed
            else -> null
          }
        },
        texts = SubmitButtonTexts(
          submittingText = "प्रेषित किया जा रहा है...",
          successText = "सफल!",
          errorText = "त्रुटि हुई"
        )
      ),
      callbacks = object : SubmitCallbacks {
        override fun onSuccess() {
          // Success is handled by the Effect collector
        }

        override fun onError(error: SubmissionError) {
          // Handle specific error cases if needed
        }
      },
      modifier = Modifier.testTag("registrationFormSubmitButton")
    )

  }

  // Unsaved changes dialog
  if (uiState.showUnsavedDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.sendIntent(CourseRegistrationFormIntent.HideUnsavedDialog) },
      confirmButton = {
        TextButton(onClick = { viewModel.sendIntent(CourseRegistrationFormIntent.DiscardUnsavedConfirmed) }) {
          Text("हाँ")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.sendIntent(CourseRegistrationFormIntent.HideUnsavedDialog) }) {
          Text("नहीं")
        }
      },
      title = { Text("असुरक्षित परिवर्तन") },
      text = { Text("आपके द्वारा भरे गए विवरण सुरक्षित नहीं हुए हैं, क्या आप निश्चित रूप से बाहर निकलना चाहते हैं?") },
      modifier = Modifier.testTag("registrationFormUnsavedDialog")
    )
  }

  // Note: In Compose Multiplatform, we would implement the back press handling
  // differently for each platform. For simplicity, we've removed it here.
}
