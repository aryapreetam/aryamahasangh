package com.aryamahasangh.features.gurukul.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormEffect
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormIntent
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormUiState
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormViewModel
import com.aryamahasangh.ui.components.buttons.*
import com.aryamahasangh.ui.components.buttons.SubmissionError
import com.aryamahasangh.ui.components.buttons.SubmissionError.ValidationFailed
import io.github.jan.supabase.auth.exception.AuthErrorCode
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun CourseRegistrationFormScreen(
  activityId: String,
  modifier: Modifier = Modifier,
  onNavigateBack: () -> Unit = {}
) {

  // ViewModel provided via DI (parameterized)
  val viewModel: CourseRegistrationFormViewModel = koinInject(parameters = { parametersOf(activityId) })
  // Use the collectUiState method which properly handles Molecule
  val uiState = viewModel.collectUiState()
  // Layout: Adaptive vertical arrangement, Material3
  Column(modifier = modifier.width(500.dp).padding(16.dp)) {
    Text(
      text = "कक्षा प्रवेश पंजीकरण",
      style = MaterialTheme.typography.headlineSmall
    )

    // Use TextFieldValue with proper cursor placement
    var nameFieldValue by remember { mutableStateOf(TextFieldValue(uiState.name, TextRange(uiState.name.length))) }

    // Update TextFieldValue when the state changes to maintain cursor position
    LaunchedEffect(uiState.name) {
      // Only update if the text has changed and field doesn't have focus
      if (nameFieldValue.text != uiState.name) {
        nameFieldValue = TextFieldValue(uiState.name, TextRange(uiState.name.length))
      }
    }

    OutlinedTextField(
      value = nameFieldValue,
      onValueChange = {
        nameFieldValue = it  // Update local field state
        viewModel.sendIntent(CourseRegistrationFormIntent.NameChanged(it.text))
      },
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

    // Improved DatePickerField - ensure selection and state update works correctly
    var selectedDate by remember {
      mutableStateOf(
        try {
          uiState.satrDate.takeIf { it.isNotBlank() }?.let { kotlinx.datetime.LocalDate.parse(it) }
        } catch (e: Exception) {
          null
        }
      )
    }

    LaunchedEffect(uiState.satrDate) {
      // Sync selectedDate when the underlying state changes
      val date = try {
        uiState.satrDate.takeIf { it.isNotBlank() }?.let { kotlinx.datetime.LocalDate.parse(it) }
      } catch (e: Exception) {
        null
      }
      if (selectedDate != date) {
        selectedDate = date
      }
    }

    com.aryamahasangh.components.DatePickerField(
      value = selectedDate,
      onValueChange = { date ->
        selectedDate = date
        viewModel.sendIntent(CourseRegistrationFormIntent.SatrDateChanged(date?.toString() ?: ""))
      },
      label = "सत्र दिनांक",
      modifier = Modifier.fillMaxWidth().testTag("registrationFormSatrDateField"),
      isError = uiState.fieldErrors.containsKey(CourseRegistrationFormUiState.Field.DATE),
      supportingText = {
        uiState.fieldErrors[CourseRegistrationFormUiState.Field.DATE]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      },
      required = true,
      enabled = !uiState.isLoading
    )
    Spacer(Modifier.height(16.dp))

    // Use TextFieldValue with proper cursor placement for place field
    var placeFieldValue by remember {
      mutableStateOf(
        TextFieldValue(
          uiState.satrPlace,
          TextRange(uiState.satrPlace.length)
        )
      )
    }

    // Update TextFieldValue when the state changes
    LaunchedEffect(uiState.satrPlace) {
      if (placeFieldValue.text != uiState.satrPlace) {
        placeFieldValue = TextFieldValue(uiState.satrPlace, TextRange(uiState.satrPlace.length))
      }
    }

    OutlinedTextField(
      value = placeFieldValue,
      onValueChange = {
        placeFieldValue = it  // Update local field state
        viewModel.sendIntent(CourseRegistrationFormIntent.SatrPlaceChanged(it.text))
      },
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

    // Use TextFieldValue with proper cursor placement for recommendation field
    var recommendationFieldValue by remember {
      mutableStateOf(
        TextFieldValue(
          uiState.recommendation,
          TextRange(uiState.recommendation.length)
        )
      )
    }

    // Update TextFieldValue when the state changes
    LaunchedEffect(uiState.recommendation) {
      if (recommendationFieldValue.text != uiState.recommendation) {
        recommendationFieldValue = TextFieldValue(uiState.recommendation, TextRange(uiState.recommendation.length))
      }
    }

    OutlinedTextField(
      value = recommendationFieldValue,
      onValueChange = {
        recommendationFieldValue = it  // Update local field state
        viewModel.sendIntent(CourseRegistrationFormIntent.RecommendationChanged(it.text))
      },
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

    // SubmitButton with proper validation handling:
    // 1. Form validation happens via validator function without changing button state
    // 2. Navigation is handled via effects from ViewModel, not via callbacks
    // 3. Haptic feedback is provided automatically for validation errors

    SubmitButton(
      text = "पंजीकरण प्रस्तुत करें",
      onSubmit = {
        viewModel.sendIntent(CourseRegistrationFormIntent.Submit)
      },
      callbacks = object : SubmitCallbacks {
        override fun onSuccess() {
          // Navigate back on success
          onNavigateBack()
        }

        override fun onError(error: SubmissionError) {
          // Submit anyway even if validation fails
          viewModel.sendIntent(CourseRegistrationFormIntent.Submit)
        }
      },
      config = SubmitButtonConfig(
        fillMaxWidth = false,
        validator = {
          if(!uiState.isSubmitEnabled) SubmissionError.ValidationFailed else null
        },
        texts = SubmitButtonTexts(
          submittingText = "प्रेषित किया जा रहा है...",
          successText = "सफल!",
          errorText = "त्रुटि हुई"
        )
      ),
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
}
