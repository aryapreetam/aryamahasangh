package org.aryamahasangh.features.organisations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.aryamahasangh.LocalIsAuthenticated
import org.aryamahasangh.components.*
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.navigation.LocalSetBackHandler
import org.aryamahasangh.navigation.LocalSnackbarHostState

data class OrganisationFormData(
  val name: String = "",
  val description: String = "",
  val logo: ImagePickerState = ImagePickerState(),
  val members: MembersState = MembersState()
)

data class OrganisationFormErrors(
  val nameError: String? = null,
  val descriptionError: String? = null,
  val logoError: String? = null,
  val membersError: String? = null
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewOrganisationFormScreen(
  priority: Int,
  viewModel: OrganisationsViewModel,
  onOrganisationCreated: (String) -> Unit = {},
  onCancel: () -> Unit = {},
  searchMembers: (String) -> List<Member> = { emptyList() },
  allMembers: List<Member> = emptyList(),
  onTriggerSearch: (String) -> Unit = {}
) {
  var formData by remember { mutableStateOf(OrganisationFormData()) }
  var errors by remember { mutableStateOf(OrganisationFormErrors()) }
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }

  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  val isLoggedIn = LocalIsAuthenticated.current

  // Initial form values for unsaved changes detection
  val initialFormData by remember { mutableStateOf(OrganisationFormData()) }

  fun hasUnsavedChanges(): Boolean {
    return formData.name != initialFormData.name ||
      formData.description != initialFormData.description ||
      formData.logo != initialFormData.logo ||
      formData.members != initialFormData.members
  }

  // Handle back button with unsaved changes
  val setBackHandler = LocalSetBackHandler.current
  DisposableEffect(Unit) {
    val handler = {
      if (hasUnsavedChanges()) {
        showUnsavedChangesDialog = true
      } else {
        onCancel()
      }
    }
    setBackHandler?.invoke(handler)
    onDispose {
      setBackHandler?.invoke(null)
    }
  }

  fun validateForm(): Boolean {
    val nameError = when {
      formData.name.isBlank() -> "संस्था का नाम आवश्यक है"
      formData.name.length > 100 -> "संस्था का नाम 100 अक्षरों से अधिक नहीं हो सकता"
      else -> null
    }

    val descriptionError = when {
      formData.description.isBlank() -> "विवरण आवश्यक है"
      formData.description.length > 1500 -> "विवरण 1500 अक्षरों से अधिक नहीं हो सकता"
      else -> null
    }

    val logoConfig = ImagePickerConfig(
      type = ImagePickerType.PROFILE_PHOTO,
      isMandatory = true,
      allowMultiple = false
    )
    val logoError = validateImagePickerState(formData.logo, logoConfig)

    val membersConfig = MembersConfig(
      isMandatory = true,
      isPostMandatory = true
    )
    val membersError = validateMembers(formData.members, membersConfig)

    errors = OrganisationFormErrors(
      nameError = nameError,
      descriptionError = descriptionError,
      logoError = logoError,
      membersError = membersError
    )

    return nameError == null && descriptionError == null && logoError == null && membersError == null
  }

  fun submitForm() {
    if (!validateForm()) {
      // Scroll to the first error field or show specific guidance
      scope.launch {
        if (!formData.members.hasMembers) {
          snackbarHostState.showSnackbar(
            message = "कृपया न्यूनतम एक पदाधिकारी जोड़ें",
            actionLabel = "ठीक है"
          )
        }
      }
      return
    }

    scope.launch {
      try {
        // Upload logo first if there's a new image
        val logoUrl = if (formData.logo.newImages.isNotEmpty()) {
          val file = formData.logo.newImages.first()
          val uploadResponse = org.aryamahasangh.network.bucket.upload(
            path = "org_logo_${Clock.System.now().epochSeconds}.jpg",
            data = file.readBytes()
          )
          org.aryamahasangh.network.bucket.publicUrl(uploadResponse.path)
        } else {
          // Use existing URL if available
          formData.logo.existingImageUrls.firstOrNull() ?: ""
        }

        // Prepare members list with proper priority assignment
        val membersList = formData.members.members.map { (member, pair) ->
          Triple(member, pair.first, pair.second)
        }

        // Call ViewModel to create organisation
        viewModel.createOrganisation(
          name = formData.name,
          description = formData.description,
          logoUrl = logoUrl,
          priority = priority,
          members = membersList
        )
      } catch (e: Exception) {
        snackbarHostState.showSnackbar(
          message = "त्रुटि: ${e.message ?: "अज्ञात त्रुटि"}",
          actionLabel = "ठीक है"
        )
      }
    }
  }

  val vmCreateState by viewModel.createOrganisationState.collectAsState()

  // Handle ViewModel state changes
  LaunchedEffect(vmCreateState) {
    when {
      vmCreateState.isSuccess && vmCreateState.createdOrganisationId != null -> {
        snackbarHostState.showSnackbar("संस्था सफलतापूर्वक बनाई गई")
        onOrganisationCreated(vmCreateState.createdOrganisationId!!)
      }
    }
  }

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
        text = "नयी संस्था जोड़ें",
        style = MaterialTheme.typography.headlineSmall
      )
      TextButton(
        onClick = {
          if (hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
          } else {
            onCancel()
          }
        }
      ) {
        Text("निरस्त करें")
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Logo Section - Simplified
    Text(
      text = "संस्था का चिह्न",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    ImagePickerComponent(
      state = formData.logo,
      onStateChange = { formData = formData.copy(logo = it) },
      config = ImagePickerConfig(
        label = "चित्र चुनिए",
        type = ImagePickerType.PROFILE_PHOTO,
        isMandatory = true,
        allowMultiple = false
      ),
      error = errors.logoError,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Name Section
    OutlinedTextField(
      value = formData.name,
      onValueChange = {
        if (it.length <= 100) {
          formData = formData.copy(name = it)
        }
      },
      label = { Text("संस्था का नाम") },
      modifier = Modifier.fillMaxWidth(),
      isError = errors.nameError != null,
      supportingText = {
        errors.nameError?.let {
          Text(it, color = MaterialTheme.colorScheme.error)
        } ?: Text("${formData.name.length}/100")
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Description Section
    OutlinedTextField(
      value = formData.description,
      onValueChange = {
        if (it.length <= 1500) {
          formData = formData.copy(description = it)
        }
      },
      label = { Text("संस्था का विवरण") },
      modifier = Modifier.fillMaxWidth(),
      minLines = 4,
      maxLines = 8,
      isError = errors.descriptionError != null,
      supportingText = {
        errors.descriptionError?.let {
          Text(it, color = MaterialTheme.colorScheme.error)
        } ?: Text("${formData.description.length}/1500")
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Members Section - Using MembersComponent
    Column(
      modifier = Modifier.fillMaxWidth()
    ) {

      MembersComponent(
        modifier = Modifier.fillMaxWidth(),
        state = formData.members,
        onStateChange = { formData = formData.copy(members = it) },
        config = MembersConfig(
          isMandatory = true,
          isPostMandatory = true
        ),
        error = errors.membersError,
        searchMembers = searchMembers,
        allMembers = allMembers,
        onTriggerSearch = onTriggerSearch
      )

      // Error message
      errors.membersError?.let {
        Text(
          text = it,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Submit Button
    val canSubmit = formData.name.isNotBlank() &&
      formData.description.isNotBlank() &&
      formData.logo.newImages.isNotEmpty() &&
      formData.members.hasMembers

    Button(
      onClick = { submitForm() },
      enabled = !vmCreateState.isCreating && canSubmit
    ) {
      if (vmCreateState.isCreating) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("संस्था बनाई जा रही है...")
      } else {
        Text(
          text = if (canSubmit) "संस्था बनाएं" else when {
            !formData.members.hasMembers -> "पहले पदाधिकारी जोड़ें"
            formData.name.isBlank() -> "संस्था का नाम भरें"
            formData.description.isBlank() -> "विवरण भरें"
            formData.logo.newImages.isEmpty() -> "चिह्न चुनें"
            else -> "संस्था बनाएं"
          },
          modifier = Modifier.padding(horizontal = 24.dp)
        )
      }
    }

    // Helper text below submit button
    if (!canSubmit && !vmCreateState.isCreating) {
      Text(
        text = when {
          !formData.members.hasMembers -> "⚠️ न्यूनतम एक पदाधिकारी जोड़ना आवश्यक है"
          formData.name.isBlank() || formData.description.isBlank() || formData.logo.newImages.isEmpty() ->
            "⚠️ सभी आवश्यक क्षेत्र भरें"

          else -> ""
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp)
      )
    }

    // Show error if any
    vmCreateState.error?.let { error ->
      Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
      )
    }
  }

  // Unsaved Changes Dialog
  if (showUnsavedChangesDialog) {
    AlertDialog(
      onDismissRequest = { showUnsavedChangesDialog = false },
      title = { Text("असंरक्षित परिवर्तन") },
      text = { Text("आपके परिवर्तन संरक्षित नहीं हैं। क्या आप इन्हें त्यागने चाहते हैं?") },
      confirmButton = {
        TextButton(
          onClick = {
            showUnsavedChangesDialog = false
            onCancel()
          }
        ) {
          Text("जी हाँ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUnsavedChangesDialog = false }) {
          Text("नहीं")
        }
      }
    )
  }
}
