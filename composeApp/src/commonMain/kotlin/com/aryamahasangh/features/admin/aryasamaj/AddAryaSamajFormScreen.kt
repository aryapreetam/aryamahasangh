package com.aryamahasangh.features.admin.aryasamaj

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.*
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.ui.components.buttons.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAryaSamajFormScreen(
  viewModel: AryaSamajViewModel,
  onNavigateBack: () -> Unit,
  isEditMode: Boolean = false,
  aryaSamajId: String? = null,
  onNavigateToAryaSamajDetails: (String) -> Unit = {}
) {
  val isAuthenticated = LocalIsAuthenticated.current
  val snackbarHostState = LocalSnackbarHostState.current ?: return
  val formUiState by viewModel.formUiState.collectAsState()

  // Initialize form state
  LaunchedEffect(Unit) {
    viewModel.resetFormState()
    if (aryaSamajId != null) {
      viewModel.loadAryaSamajForEdit(aryaSamajId)
    }
  }

  // Handle success state
  LaunchedEffect(formUiState.submitSuccess) {
    if (formUiState.submitSuccess) {
      // Navigate immediately without waiting for snackbar
      onNavigateToAryaSamajDetails(formUiState.createdAryaSamajId ?: "")
    }
  }

  // Handle submission error
  formUiState.submitError?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(error)
    }
  }

  // Use ViewModel's form data directly instead of local state
  val currentFormData = formUiState.formData

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (isEditMode) "आर्य समाज संपादित करें" else "नया आर्य समाज जोड़ें",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )

      IconButton(onClick = onNavigateBack) {
        Icon(
          Icons.Default.Close,
          contentDescription = "बंद करें"
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Name field
    OutlinedTextField(
      value = currentFormData.name,
      onValueChange = { newName ->
        viewModel.updateFormData(currentFormData.copy(name = newName))
      },
      label = { Text("आर्य समाज का नाम") },
      modifier = Modifier.fillMaxWidth(),
      isError = formUiState.validationErrors.containsKey("name"),
      supportingText = {
        formUiState.validationErrors["name"]?.let { Text(it) }
      }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Description field
    OutlinedTextField(
      value = currentFormData.description,
      onValueChange = { newDescription ->
        viewModel.updateFormData(currentFormData.copy(description = newDescription))
      },
      label = { Text("विवरण") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
      maxLines = 5,
      isError = formUiState.validationErrors.containsKey("description"),
      supportingText = {
        formUiState.validationErrors["description"]?.let { Text(it) }
      }
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Image picker
    ImagePickerComponent(
      state = currentFormData.imagePickerState,
      onStateChange = { newImagePickerState ->
        viewModel.updateFormData(currentFormData.copy(imagePickerState = newImagePickerState))
      },
      config =
        ImagePickerConfig(
          label = "आर्य समाज के चित्र",
          allowMultiple = true,
          maxImages = 10,
          isMandatory = false,
          enableBackgroundCompression = true,
          compressionTargetKb = 100, // 100KB for AryaSamaj images
          showCompressionProgress = true
        )
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Address component
    Text(
      text = "पता",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    // Add validation trigger state for AddressComponent
    var triggerAddressValidation by remember { mutableStateOf(false) }

    AddressComponent(
      addressData = currentFormData.addressData,
      onAddressChange = { newAddressData ->
        viewModel.updateFormData(currentFormData.copy(addressData = newAddressData))
      },
      fieldsConfig =
        AddressFieldsConfig(
          showLocation = true,
          showAddress = true,
          showState = true,
          showDistrict = true,
          showVidhansabha = true,
          showPincode = true,
          // Configure mandatory fields - location is required as per ViewModel validation
          mandatoryLocation = true,
          mandatoryAddress = true,
          mandatoryState = true,
          mandatoryDistrict = true,
          mandatoryVidhansabha = false,
          mandatoryPincode = false
        ),
      // Use self-validation mode instead of external errors
      validateFields = triggerAddressValidation,
      onValidationResult = { isValid ->
        // AddressComponent handles its own validation and error display
        // We don't need to update ViewModel validation errors here
        // since the SubmitButton validator will call viewModel.isFormValid()
      }
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Members component
    MembersComponent(
      state = currentFormData.membersState,
      onStateChange = { newMembersState ->
        viewModel.updateFormData(currentFormData.copy(membersState = newMembersState))
      },
      config =
        MembersConfig(
          label = "कार्यकारिणी/पदाधिकारी",
          addButtonText = "पदाधिकारी जोड़ें",
          postLabel = "पद",
          postPlaceholder = "संयोजक, कोषाध्यक्ष इत्यादि",
          isPostMandatory = false,
          isMandatory = false,
          editMode = MembersEditMode.GROUPED,
          enableReordering = true
        ),
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(32.dp))

    Spacer(modifier = Modifier.height(24.dp))
    SubmitButtonOld(
      text = if (isEditMode) "संपादित करें" else "आर्य समाज जोड़ें",
      onSubmit = {
        // Submit the form via ViewModel - ViewModel handles validation
        viewModel.submitForm()
      },
      config = SubmitButtonConfig(
        validator = {
          // Trigger address validation so AddressComponent shows errors
          triggerAddressValidation = !triggerAddressValidation
          // Now check form validity
          if (!viewModel.isFormValid()) SubmissionError.ValidationFailed else null
        },
        texts = SubmitButtonTexts(
          submittingText = if (isEditMode) "संपादित हो रहा है..." else "सुरक्षित हो रहा है...",
          successText = if (isEditMode) "संपादित सफल!" else "सफल!"
        )
      ),
      callbacks = object : SubmitCallbacks {
        override fun onError(error: SubmissionError) {
          // Submission errors are handled by GlobalMessageManager in ViewModel
          // No additional action needed here
        }
      }
    )

    Spacer(modifier = Modifier.height(16.dp))
  }
}
