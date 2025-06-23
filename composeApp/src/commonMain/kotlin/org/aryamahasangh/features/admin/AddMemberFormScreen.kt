package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.aryamahasangh.components.*
import org.aryamahasangh.navigation.LocalSetBackHandler
import org.aryamahasangh.navigation.LocalSnackbarHostState
import org.aryamahasangh.network.bucket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberFormScreen(
  viewModel: AdminViewModel,
  onNavigateBack: () -> Unit
) {
  val uiState by viewModel.memberDetailUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val setBackHandler = LocalSetBackHandler.current
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  // Focus requesters for form fields
  val nameFocusRequester = remember { FocusRequester() }
  val phoneFocusRequester = remember { FocusRequester() }
  val emailFocusRequester = remember { FocusRequester() }
  val educationFocusRequester = remember { FocusRequester() }
  val occupationFocusRequester = remember { FocusRequester() }
  val introductionFocusRequester = remember { FocusRequester() }

  // Form state
  var name by rememberSaveable { mutableStateOf("") }
  var phoneNumber by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var dob by remember { mutableStateOf<LocalDate?>(null) }
  var gender by remember { mutableStateOf<Gender?>(null) }
  var educationalQualification by rememberSaveable { mutableStateOf("") }
  var occupation by rememberSaveable { mutableStateOf("") }
  var joiningDate by remember { mutableStateOf<LocalDate?>(null) }
  var introduction by rememberSaveable { mutableStateOf("") }

  // Address state
  var permanentAddress by remember { mutableStateOf(AddressData()) }
  var currentAddress by remember { mutableStateOf(AddressData()) }
  var isDifferentCurrentAddress by rememberSaveable { mutableStateOf(false) }

  // Member selection state
  var referrerState by remember { mutableStateOf(MembersState()) }
  var selectedAryaSamaj by remember { mutableStateOf<AryaSamaj?>(null) }

  // Profile image state
  var selectedProfileImage by remember { mutableStateOf<PlatformFile?>(null) }
  var uploadedImageUrl by remember { mutableStateOf<String?>(null) }

  // Dialog states
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }
  var showImageUploadFailureDialog by remember { mutableStateOf(false) }
  var imageUploadError by remember { mutableStateOf<String?>(null) }

  // Validation errors
  var nameError by remember { mutableStateOf<String?>(null) }
  var phoneError by remember { mutableStateOf<String?>(null) }
  var dobError by remember { mutableStateOf<String?>(null) }
  var genderError by remember { mutableStateOf<String?>(null) }
  var permanentAddressErrors by remember { mutableStateOf(AddressErrors()) }
  var currentAddressErrors by remember { mutableStateOf(AddressErrors()) }
  var referrerError by remember { mutableStateOf<String?>(null) }

  // Check if there are unsaved changes
  fun hasUnsavedChanges(): Boolean {
    return name.isNotBlank() ||
      phoneNumber.isNotBlank() ||
      email.isNotBlank() ||
      dob != null ||
      gender != null ||
      educationalQualification.isNotBlank() ||
      occupation.isNotBlank() ||
      joiningDate != null ||
      introduction.isNotBlank() ||
      permanentAddress != AddressData() ||
      (isDifferentCurrentAddress && currentAddress != AddressData()) ||
      referrerState.hasMembers ||
      selectedAryaSamaj != null ||
      selectedProfileImage != null
  }

  // Handle back navigation with unsaved changes check
  fun handleBackNavigation() {
    if (hasUnsavedChanges()) {
      showUnsavedChangesDialog = true
    } else {
      onNavigateBack()
    }
  }

  // Validation function
  fun validateForm(): Boolean {
    var isValid = true

    // Name validation
    nameError = if (name.isBlank()) {
      isValid = false
      "नाम अपेक्षित है"
    } else null

    // Phone validation
    phoneError = if (phoneNumber.isBlank()) {
      isValid = false
      "दूरभाष अपेक्षित है"
    } else if (phoneNumber.length < 10) {
      isValid = false
      "दूरभाष कम से कम 10 अंक का होना चाहिए"
    } else null

    // DOB validation
    dobError = if (dob == null) {
      isValid = false
      "जन्मतिथि अपेक्षित है"
    } else null

    // Gender validation
    genderError = if (gender == null) {
      isValid = false
      "लिंग चुनना अपेक्षित है"
    } else null

    // Address validation
    permanentAddressErrors = validateAddressData(
      permanentAddress,
      AddressFieldsConfig(),
      setOf("address", "state", "district")
    )
    if (permanentAddressErrors.addressError != null ||
      permanentAddressErrors.stateError != null ||
      permanentAddressErrors.districtError != null) {
      isValid = false
    }

    // Current address validation (if different)
    if (isDifferentCurrentAddress) {
      currentAddressErrors = validateAddressData(
        currentAddress,
        AddressFieldsConfig(),
        setOf("address", "state", "district")
      )
      if (currentAddressErrors.addressError != null ||
        currentAddressErrors.stateError != null ||
        currentAddressErrors.districtError != null) {
        isValid = false
      }
    }

    return isValid
  }

  // Save member function
  fun saveMember() {
    if (!validateForm()) {
      scope.launch {
        snackbarHostState.showSnackbar("कृपया सभी आवश्यक फ़ील्ड भरें")
      }
      return
    }

    scope.launch {
      try {
        var finalImageUrl: String? = uploadedImageUrl

        // Upload profile image if selected
        if (selectedProfileImage != null && uploadedImageUrl == null) {
          try {
            val uploadResponse = bucket.upload(
              path = "profile_${Clock.System.now().epochSeconds}.jpg",
              data = selectedProfileImage!!.readBytes()
            )
            finalImageUrl = bucket.publicUrl(uploadResponse.path)
            uploadedImageUrl = finalImageUrl
          } catch (e: Exception) {
            imageUploadError = e.message
            showImageUploadFailureDialog = true
            return@launch
          }
        }

        // Create permanent address
        val permanentAddressId = viewModel.createAddress(
          basicAddress = permanentAddress.address,
          state = permanentAddress.state,
          district = permanentAddress.district,
          pincode = permanentAddress.pincode,
          latitude = permanentAddress.location?.latitude,
          longitude = permanentAddress.location?.longitude,
          vidhansabha = permanentAddress.vidhansabha
        )

        if (permanentAddressId == null) {
          snackbarHostState.showSnackbar("स्थायी पता सहेजने में त्रुटि")
          return@launch
        }

        // Create current address if different
        val currentAddressId = if (isDifferentCurrentAddress) {
          viewModel.createAddress(
            basicAddress = currentAddress.address,
            state = currentAddress.state,
            district = currentAddress.district,
            pincode = currentAddress.pincode,
            latitude = currentAddress.location?.latitude,
            longitude = currentAddress.location?.longitude,
            vidhansabha = currentAddress.vidhansabha
          )
        } else null

        if (isDifferentCurrentAddress && currentAddressId == null) {
          snackbarHostState.showSnackbar("वर्तमान पता सहेजने में त्रुटि")
          return@launch
        }

        // Create member
        viewModel.createMember(
          name = name,
          phoneNumber = phoneNumber,
          email = email.ifBlank { null },
          dob = dob,
          gender = gender,
          educationalQualification = educationalQualification.ifBlank { null },
          occupation = occupation.ifBlank { null },
          joiningDate = joiningDate,
          introduction = introduction.ifBlank { null },
          profileImageUrl = finalImageUrl,
          addressId = permanentAddressId,
          tempAddressId = currentAddressId,
          referrerId = referrerState.members.keys.firstOrNull()?.id,
          aryaSamajId = selectedAryaSamaj?.id
        )

      } catch (e: Exception) {
        snackbarHostState.showSnackbar("Error: ${e.message}")
      }
    }
  }

  // Set back handler
  DisposableEffect(Unit) {
    setBackHandler?.invoke {
      handleBackNavigation()
    }
    onDispose {
      setBackHandler?.invoke(null)
    }
  }

  // Load all members for selection
  LaunchedEffect(Unit) {
    viewModel.loadAllMembersForSelection()
    viewModel.loadAllAryaSamajsForSelection()
  }

  // Handle success
  LaunchedEffect(uiState.updateSuccess) {
    if (uiState.updateSuccess) {
      snackbarHostState.showSnackbar("सदस्य सफलतापूर्वक जोड़ा गया")
      viewModel.resetUpdateState()
      onNavigateBack()
    }
  }

  // Handle error
  LaunchedEffect(uiState.error) {
    uiState.error?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.resetUpdateState()
    }
  }

  // Profile image picker
  val launcher = rememberFilePickerLauncher(
    type = PickerType.Image,
    mode = PickerMode.Single,
    title = "प्रोफ़ाइल फोटो चुनें"
  ) { file ->
    selectedProfileImage = file
    uploadedImageUrl = null
  }

  // Unsaved changes dialog
  if (showUnsavedChangesDialog) {
    AlertDialog(
      onDismissRequest = { showUnsavedChangesDialog = false },
      title = { Text("असहेजे परिवर्तन") },
      text = { Text("आपके पास असहेजे परिवर्तन हैं। क्या आप वास्तव में उन्हें रद्द करना चाहते हैं?") },
      confirmButton = {
        TextButton(
          onClick = {
            showUnsavedChangesDialog = false
            onNavigateBack()
          }
        ) {
          Text("रद्द करें")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showUnsavedChangesDialog = false }
        ) {
          Text("वापस जाएं")
        }
      }
    )
  }

  // Image upload failure dialog
  if (showImageUploadFailureDialog) {
    AlertDialog(
      onDismissRequest = { showImageUploadFailureDialog = false },
      title = { Text("फोटो अपलोड असफल") },
      text = { Text("प्रोफ़ाइल फोटो अपलोड करने में असफल: ${imageUploadError}। क्या आप फोटो के बिना जारी रखना चाहते हैं या पुनः प्रयास करना चाहते हैं?") },
      confirmButton = {
        TextButton(
          onClick = {
            showImageUploadFailureDialog = false
            // Continue without image
            saveMember()
          }
        ) {
          Text("फोटो के बिना जारी रखें")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showImageUploadFailureDialog = false
            // Retry upload
            saveMember()
          }
        ) {
          Text("पुनः प्रयास")
        }
      }
    )
  }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    // Top App Bar
    TopAppBar(
      title = { Text("नया सदस्य जोड़ें") }
    )

    // Form Content
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      // Profile Image Section
      item {
        ProfileImageSection(
          selectedProfileImage = selectedProfileImage,
          onImageSelected = { launcher.launch() },
          onImageRemoved = { 
            selectedProfileImage = null
            uploadedImageUrl = null
          }
        )
      }

      // Personal Details Section
      item {
        PersonalDetailsSection(
          name = name,
          onNameChange = { name = it },
          nameError = nameError,
          nameFocusRequester = nameFocusRequester,
          phoneNumber = phoneNumber,
          onPhoneNumberChange = { phoneNumber = it },
          phoneError = phoneError,
          phoneFocusRequester = phoneFocusRequester,
          dob = dob,
          onDobChange = { dob = it },
          dobError = dobError,
          gender = gender,
          onGenderChange = { gender = it },
          genderError = genderError,
          email = email,
          onEmailChange = { email = it },
          emailFocusRequester = emailFocusRequester,
          educationalQualification = educationalQualification,
          onEducationalQualificationChange = { educationalQualification = it },
          educationFocusRequester = educationFocusRequester,
          occupation = occupation,
          onOccupationChange = { occupation = it },
          occupationFocusRequester = occupationFocusRequester,
          focusManager = focusManager
        )
      }

      // Address Section
      item {
        AddressSection(
          permanentAddress = permanentAddress,
          onPermanentAddressChange = { permanentAddress = it },
          permanentAddressErrors = permanentAddressErrors,
          isDifferentCurrentAddress = isDifferentCurrentAddress,
          onIsDifferentCurrentAddressChange = { isDifferentCurrentAddress = it },
          currentAddress = currentAddress,
          onCurrentAddressChange = { currentAddress = it },
          currentAddressErrors = currentAddressErrors
        )
      }

      // Organization Details Section
      item {
        OrganizationDetailsSection(
          joiningDate = joiningDate,
          onJoiningDateChange = { joiningDate = it },
          referrerState = referrerState,
          onReferrerStateChange = { referrerState = it },
          referrerError = referrerError,
          selectedAryaSamaj = selectedAryaSamaj,
          onAryaSamajSelected = { selectedAryaSamaj = it },
          searchMembers = { query -> viewModel.searchMembersForSelection(query) },
          allMembers = uiState.allMembers,
          onTriggerMemberSearch = { query -> viewModel.triggerMemberSearch(query) },
          allAryaSamajs = uiState.allAryaSamajs
        )
      }

      // Introduction Section
      item {
        IntroductionSection(
          introduction = introduction,
          onIntroductionChange = { introduction = it },
          introductionFocusRequester = introductionFocusRequester
        )
      }

      // Submit Button
      item {
        Spacer(modifier = Modifier.height(24.dp))
        Button(
          onClick = { saveMember() },
          enabled = !uiState.isUpdating,
        ) {
          if (uiState.isUpdating) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(
            text ="सहेजें",
            modifier = Modifier
              .padding(horizontal = 24.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(bottom = 8.dp)
  )
}
