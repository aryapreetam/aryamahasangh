package com.aryamahasangh.features.gurukul.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationViewModel
import com.aryamahasangh.ui.components.buttons.SubmitButton
import com.aryamahasangh.ui.components.buttons.SubmitButtonConfig
import com.aryamahasangh.components.DatePickerField
import com.aryamahasangh.components.ImagePickerComponent
import com.aryamahasangh.components.ImagePickerConfig
import com.aryamahasangh.components.ImagePickerType
import com.aryamahasangh.ui.components.buttons.SubmissionError
import com.aryamahasangh.ui.components.buttons.SubmitCallbacks
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

@Composable
fun CourseRegistrationFormScreen(
  activityId: String,
  onNavigateBack: () -> Unit,
  viewModel: CourseRegistrationViewModel
) {
  val uiState by viewModel.uiState.collectAsState()
  val openDialog = uiState.showUnsavedExitDialog
  val submitSuccess = uiState.submitSuccess
  if (submitSuccess) {
    // Immediate navigation pattern
    LaunchedEffect(submitSuccess) {
      viewModel.resetSubmitState()
      onNavigateBack()
    }
  }
  if (openDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.dismissUnsavedExitDialog() },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.confirmUnsavedExit()
            onNavigateBack()
          },
          modifier = Modifier.semantics { testTag = "dialog_confirm_exit" }
        ) { Text("हाँ, बाहर जाएँ") }
      },
      dismissButton = {
        TextButton(
          onClick = { viewModel.dismissUnsavedExitDialog() },
          modifier = Modifier.semantics { testTag = "dialog_dismiss_exit" }
        ) { Text("रुकें") }
      },
      title = { Text("आपके द्वारा भरे गए विवरण सुरक्षित नहीं हुए हैं, क्या आप निश्चित रूप से बाहर निकलना चाहते हैं?") },
      modifier = Modifier.semantics { testTag = "unsaved_exit_dialog" }
    )
  }
  Column(
    modifier = Modifier.padding(24.dp).semantics { testTag = "course_registration_form" },
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    var nameFieldValue by remember { mutableStateOf(TextFieldValue(uiState.name, TextRange(uiState.name.length))) }
    LaunchedEffect(uiState.name) {
      if (nameFieldValue.text != uiState.name) {
        nameFieldValue = TextFieldValue(uiState.name, TextRange(uiState.name.length))
      }
    }
    OutlinedTextField(
      value = nameFieldValue,
      onValueChange = {
        nameFieldValue = it
        viewModel.onFieldChange(name = it.text)
      },
      label = { Text("नाम") },
      modifier = Modifier.semantics { testTag = "name_field" },
      isError = uiState.submitErrorMessage != null && nameFieldValue.text.isBlank(),
      supportingText = {
        if (uiState.submitErrorMessage != null && nameFieldValue.text.isBlank()) {
          Text("कृपया नाम दर्ज करें", color = MaterialTheme.colorScheme.error)
        }
      },
      singleLine = true,
      enabled = !uiState.isSubmitting
    )
    var selectedDate by remember {
      mutableStateOf(
        try {
          uiState.satrDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
          null
        }
      )
    }
    LaunchedEffect(uiState.satrDate) {
      val date = try {
        uiState.satrDate.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
      } catch (e: Exception) {
        null
      }
      if (selectedDate != date) selectedDate = date
    }
    DatePickerField(
      value = selectedDate,
      onValueChange = { date ->
        selectedDate = date
        viewModel.onFieldChange(satrDate = date?.toString() ?: "")
      },
      label = "सत्र दिनांक",
      modifier = Modifier.semantics { testTag = "satr_date_field" },
      isError = uiState.submitErrorMessage != null && selectedDate == null,
      supportingText = {
        if (uiState.submitErrorMessage != null && selectedDate == null) {
          Text("कृपया सत्र दिनांक चुनें", color = MaterialTheme.colorScheme.error)
        }
      },
      required = true,
      enabled = !uiState.isSubmitting
    )
    var placeFieldValue by remember {
      mutableStateOf(
        TextFieldValue(
          uiState.satrPlace,
          TextRange(uiState.satrPlace.length)
        )
      )
    }
    LaunchedEffect(uiState.satrPlace) {
      if (placeFieldValue.text != uiState.satrPlace) {
        placeFieldValue = TextFieldValue(uiState.satrPlace, TextRange(uiState.satrPlace.length))
      }
    }
    OutlinedTextField(
      value = placeFieldValue,
      onValueChange = {
        placeFieldValue = it
        viewModel.onFieldChange(satrPlace = it.text)
      },
      label = { Text("सत्र स्थान") },
      modifier = Modifier.semantics { testTag = "satr_place_field" },
      isError = uiState.submitErrorMessage != null && placeFieldValue.text.isBlank(),
      supportingText = {
        if (uiState.submitErrorMessage != null && placeFieldValue.text.isBlank()) {
          Text("कृपया सत्र स्थान भरें", color = MaterialTheme.colorScheme.error)
        }
      },
      singleLine = true,
      enabled = !uiState.isSubmitting
    )
    var recommendationFieldValue by remember {
      mutableStateOf(
        TextFieldValue(
          uiState.recommendation,
          TextRange(uiState.recommendation.length)
        )
      )
    }
    LaunchedEffect(uiState.recommendation) {
      if (recommendationFieldValue.text != uiState.recommendation) {
        recommendationFieldValue = TextFieldValue(uiState.recommendation, TextRange(uiState.recommendation.length))
      }
    }
    OutlinedTextField(
      value = recommendationFieldValue,
      onValueChange = {
        recommendationFieldValue = it
        viewModel.onFieldChange(recommendation = it.text)
      },
      label = { Text("संरक्षक की अनुशंसा") },
      modifier = Modifier.semantics { testTag = "recommendation_field" },
      isError = uiState.submitErrorMessage != null && recommendationFieldValue.text.isBlank(),
      supportingText = {
        if (uiState.submitErrorMessage != null && recommendationFieldValue.text.isBlank()) {
          Text("कृपया संरक्षक की अनुशंसा भरें", color = MaterialTheme.colorScheme.error)
        }
      },
      minLines = 3,
      maxLines = 8,
      enabled = !uiState.isSubmitting
    )
    ImagePickerComponent(
      state = uiState.imagePickerState,
      onStateChange = { pickerState -> viewModel.onFieldChange(imagePickerState = pickerState) },
      modifier = Modifier.semantics { testTag = "receipt_picker" },
      config = ImagePickerConfig(
        label = "भुगतान रसीद जोड़ें",
        type = ImagePickerType.IMAGE,
        allowMultiple = false,
        isMandatory = true,
        minImages = 1,
        maxImages = 1
      ),
      error = if (uiState.submitErrorMessage != null && uiState.imagePickerState.newImages.isEmpty()) "कृपया भुगतान रसीद अपलोड करें" else null
    )
    Spacer(Modifier.height(24.dp))
    SubmitButton(
      text = "पंजीकरण प्रस्तुत करें",
      onSubmit = {
        viewModel.onSubmit()
      },
      config = SubmitButtonConfig(
        validator = {
          if(!uiState.isValid) SubmissionError.ValidationFailed else null
//          uiState.submitErrorMessage?.let {
//            com.aryamahasangh.ui.components.buttons.SubmissionError.Custom(
//              message = it
//            )
//          }
        },
      ),
      modifier = Modifier.semantics { testTag = "submit_button" },
      callbacks = object : com.aryamahasangh.ui.components.buttons.SubmitCallbacks {
        override fun onError(error: com.aryamahasangh.ui.components.buttons.SubmissionError) {
          // No navigation on error—UI shows error state, snackbar handled by ViewModel
        }
        override fun onSuccess() {
          // Success: navigation handled by LaunchedEffect(submitSuccess)
        }
      }
    )
  }
}
