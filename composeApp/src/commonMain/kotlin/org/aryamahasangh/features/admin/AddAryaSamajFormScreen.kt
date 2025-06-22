package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aryamahasangh.LocalIsAuthenticated
import org.aryamahasangh.components.*
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.features.admin.data.AryaSamajFormData
import org.aryamahasangh.features.admin.data.AryaSamajViewModel
import org.aryamahasangh.navigation.LocalSnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAryaSamajFormScreen(
  viewModel: AryaSamajViewModel,
  onNavigateBack: () -> Unit,
  searchMembers: (String) -> List<Member> = { emptyList() },
  allMembers: List<Member> = emptyList(),
  onTriggerSearch: (String) -> Unit = {},
  isEditMode: Boolean = false, // New parameter to determine if we're editing
  aryaSamajId: String? = null // New parameter for the AryaSamaj ID when editing
) {
  val isAuthenticated = LocalIsAuthenticated.current
  val snackbarHostState = LocalSnackbarHostState.current
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
      if (isEditMode) {
        snackbarHostState.showSnackbar("आर्य समाज सफलतापूर्वक संपादित किया गया")
      } else {
        snackbarHostState.showSnackbar("आर्य समाज सफलतापूर्वक जोड़ा गया")
      }
      onNavigateBack()
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
    modifier = Modifier
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
      config = ImagePickerConfig(
        label = "आर्य समाज के चित्र",
        allowMultiple = true,
        maxImages = 10,
        isMandatory = false
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

    AddressComponent(
      addressData = currentFormData.addressData,
      onAddressChange = { newAddressData ->
        viewModel.updateFormData(currentFormData.copy(addressData = newAddressData))
      },
      fieldsConfig = AddressFieldsConfig(
        showLocation = true,
        showAddress = true,
        showState = true,
        showDistrict = true,
        showVidhansabha = true,
        showPincode = true,
      ),
      errors = AddressErrors(
        locationError = formUiState.validationErrors["location"],
        addressError = formUiState.validationErrors["address"],
        stateError = formUiState.validationErrors["state"],
        districtError = formUiState.validationErrors["district"]
      )
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Members component
    MembersComponent(
      state = currentFormData.membersState,
      onStateChange = { newMembersState ->
        viewModel.updateFormData(currentFormData.copy(membersState = newMembersState))
      },
      config = MembersConfig(
        label = "कार्यकारिणी/पदाधिकारी",
        addButtonText = "पदाधिकारी जोड़ें",
        postLabel = "पद",
        postPlaceholder = "संयोजक, कोषाध्यक्ष इत्यादि",
        isPostMandatory = false,
        isMandatory = false,
        editMode = MembersEditMode.GROUPED,
        enableReordering = true
      ),
      searchMembers = searchMembers,
      allMembers = allMembers,
      onTriggerSearch = onTriggerSearch
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Submit button
    Button(
      onClick = { viewModel.submitForm() },
      modifier = Modifier.fillMaxWidth(),
      enabled = !formUiState.isSubmitting
    ) {
      if (formUiState.isSubmitting) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("सुरक्षित हो रहा है...")
      } else {
        Text(if (isEditMode) "संपादित करें" else "आर्य समाज जोड़ें")
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
