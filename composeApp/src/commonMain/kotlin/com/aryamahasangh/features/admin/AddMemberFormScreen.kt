package com.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aryamahasangh.components.*
import com.aryamahasangh.features.activities.LatLng
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.navigation.LocalSetBackHandler
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.network.bucket
import com.aryamahasangh.ui.components.buttons.*
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberFormScreen(
  viewModel: AdminViewModel,
  onNavigateBack: () -> Unit,
  memberId: String? = null, // New parameter for edit mode
  onNavigateToMemberDetail: (memberId: String) -> Unit
) {
  val uiState by viewModel.memberDetailUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val setBackHandler = LocalSetBackHandler.current
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  // Edit mode state
  val isEditMode = memberId != null
  var isLoading by remember { mutableStateOf(false) }

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

  // Load member data in edit mode
  LaunchedEffect(memberId) {
    if (isEditMode) {
      isLoading = true
      viewModel.loadMember(memberId)
    }
  }

  // Update form fields with loaded member data
  LaunchedEffect(uiState.member) {
    if (isEditMode && uiState.member != null) {
      val member = uiState.member!!
      name = member.name
      phoneNumber = member.phoneNumber
      email = member.email
      dob = member.dob
      gender = member.gender
      educationalQualification = member.educationalQualification
      occupation = member.occupation
      joiningDate = member.joiningDate
      introduction = member.introduction
      uploadedImageUrl = member.profileImage

      // Set permanent address
      member.addressFields?.let { addressFields ->
        permanentAddress = AddressData(
          address = addressFields.basicAddress ?: "",
          state = addressFields.state ?: "",
          district = addressFields.district ?: "",
          pincode = addressFields.pincode ?: "",
          vidhansabha = addressFields.vidhansabha ?: "",
          location = addressFields.latitude?.let { lat ->
            addressFields.longitude?.let { lng ->
              LatLng(lat, lng)
            }
          }
        )
      }

      // Determine if there are two different addresses
      val mainAddressId = member.addressFields?.id
      val tempAddressId = member.tempAddressFields?.id

      val tempAddressHasData = member.tempAddressFields?.let { tempFields ->
        !tempFields.basicAddress.isNullOrBlank() ||
          !tempFields.state.isNullOrBlank() ||
          !tempFields.district.isNullOrBlank() ||
          !tempFields.pincode.isNullOrBlank()
      } ?: false

      isDifferentCurrentAddress = (
        mainAddressId != null &&
          tempAddressId != null &&
          mainAddressId != tempAddressId &&
          tempAddressHasData // Only check if temp address has actual data
        )

      // Set temp address if it exists and is different
      if (isDifferentCurrentAddress) {
        member.tempAddressFields?.let { tempAddressFields ->
          currentAddress = AddressData(
            address = tempAddressFields.basicAddress ?: "",
            state = tempAddressFields.state ?: "",
            district = tempAddressFields.district ?: "",
            pincode = tempAddressFields.pincode ?: "",
            vidhansabha = tempAddressFields.vidhansabha ?: "",
            location = tempAddressFields.latitude?.let { lat ->
              tempAddressFields.longitude?.let { lng ->
                LatLng(lat, lng)
              }
            }
          )
        }
      } else {
        // Reset current address to empty if not different
        currentAddress = AddressData()
      }

      // Set referrer if available
      member.referrer?.let { referrer ->
        referrerState = MembersState(
          members = mapOf(
            Member(
              id = referrer.id,
              name = referrer.name,
              profileImage = referrer.profileImage,
              phoneNumber = "",
              email = ""
            ) to Pair("रेफरर", 1)
          )
        )
      }

      isLoading = false
    }
  }

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
    nameError =
      if (name.isBlank()) {
        isValid = false
        "नाम अपेक्षित है"
      } else {
        null
      }

    // Phone validation
    phoneError =
      if (phoneNumber.isBlank()) {
        isValid = false
        "दूरभाष अपेक्षित है"
      } else if (phoneNumber.length < 10) {
        isValid = false
        "दूरभाष कम से कम 10 अंक का होना चाहिए"
      } else {
        null
      }

    // DOB validation
    dobError =
      if (dob == null) {
        isValid = false
        "जन्मतिथि अपेक्षित है"
      } else {
        null
      }

    // Gender validation
    genderError =
      if (gender == null) {
        isValid = false
        "लिंग चुनना अपेक्षित है"
      } else {
        null
      }

    // Address validation
    permanentAddressErrors =
      validateAddressData(
        permanentAddress,
        AddressFieldsConfig(),
        setOf("address", "state", "district")
      )
    if (permanentAddressErrors.addressError != null ||
      permanentAddressErrors.stateError != null ||
      permanentAddressErrors.districtError != null
    ) {
      isValid = false
    }

    // Current address validation (if different)
    if (isDifferentCurrentAddress) {
      currentAddressErrors =
        validateAddressData(
          currentAddress,
          AddressFieldsConfig(),
          setOf("address", "state", "district")
        )
      if (currentAddressErrors.addressError != null ||
        currentAddressErrors.stateError != null ||
        currentAddressErrors.districtError != null
      ) {
        isValid = false
      }
    }

    return isValid
  }

  // Save/Update member function
  fun saveMember() {
    scope.launch {
      try {
        var finalImageUrl: String? = uploadedImageUrl

        // Upload profile image if selected
        if (selectedProfileImage != null && uploadedImageUrl == null) {
          try {
            val uploadResponse =
              bucket.upload(
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

        if (isEditMode) {
          val member = uiState.member!!

          // Helper function to check if address has changed
          fun hasAddressChanged(): Boolean {
            val originalAddress = member.addressFields
            return originalAddress == null || // No original address but now we have one
              originalAddress.basicAddress != permanentAddress.address ||
              originalAddress.state != permanentAddress.state ||
              originalAddress.district != permanentAddress.district ||
              originalAddress.pincode != permanentAddress.pincode ||
              originalAddress.latitude != permanentAddress.location?.latitude ||
              originalAddress.longitude != permanentAddress.location?.longitude ||
              originalAddress.vidhansabha != permanentAddress.vidhansabha
          }

          // Helper function to check if temp address has changed
          fun hasTempAddressChanged(): Boolean {
            val originalTempAddress = member.tempAddressFields
            val currentTempAddress = if (isDifferentCurrentAddress) currentAddress else AddressData()

            return if (isDifferentCurrentAddress) {
              // User wants a different current address
              originalTempAddress == null || // No original temp address but now we want one
                originalTempAddress.basicAddress != currentTempAddress.address ||
                originalTempAddress.state != currentTempAddress.state ||
                originalTempAddress.district != currentTempAddress.district ||
                originalTempAddress.pincode != currentTempAddress.pincode ||
                originalTempAddress.latitude != currentTempAddress.location?.latitude ||
                originalTempAddress.longitude != currentTempAddress.location?.longitude ||
                originalTempAddress.vidhansabha != currentTempAddress.vidhansabha
            } else {
              // User doesn't want a different current address
              originalTempAddress != null // Had temp address before but now doesn't want it
            }
          }

          val addressChanged = hasAddressChanged()
          val tempAddressChanged = hasTempAddressChanged()

          // Check if any field has changed
          val hasAnyChanges = (
            member.name != name ||
              member.phoneNumber != phoneNumber ||
              member.email != email ||
              member.dob != dob ||
              member.gender != gender ||
              member.educationalQualification != educationalQualification ||
              member.occupation != occupation ||
              member.joiningDate != joiningDate ||
              member.introduction != introduction ||
              finalImageUrl != member.profileImage ||
              member.referrerId != referrerState.members.keys.firstOrNull()?.id ||
              member.aryaSamaj?.id != selectedAryaSamaj?.id ||
              addressChanged ||
              tempAddressChanged
            )

          if (hasAnyChanges) {
            // Only send fields that have actually changed
            viewModel.updateMember(
              memberId = memberId!!,
              name = name, // Always send current name (required field)
              phoneNumber = phoneNumber, // Always send current phone (required field)
              email = if (member.email != email) email.ifBlank { null } else null,
              dob = if (member.dob != dob) dob else null,
              gender = if (member.gender != gender) gender else null,
              educationalQualification = if (member.educationalQualification != educationalQualification) educationalQualification.ifBlank { null } else null,
              occupation = if (member.occupation != occupation) occupation.ifBlank { null } else null,
              joiningDate = if (member.joiningDate != joiningDate) joiningDate else null,
              introduction = if (member.introduction != introduction) introduction.ifBlank { null } else null,
              profileImageUrl = if (finalImageUrl != member.profileImage) finalImageUrl else null,
              addressId = if (addressChanged) member.addressFields?.id else null,
              tempAddressId = if (tempAddressChanged) member.tempAddressFields?.id else null,
              referrerId = if (member.referrerId != referrerState.members.keys.firstOrNull()?.id) referrerState.members.keys.firstOrNull()?.id else null,
              aryaSamajId = if (member.aryaSamaj?.id != selectedAryaSamaj?.id) selectedAryaSamaj?.id else null,
              // Main address fields - only send if address has changed
              basicAddress = if (addressChanged) permanentAddress.address.ifBlank { null } else null,
              state = if (addressChanged) permanentAddress.state.ifBlank { null } else null,
              district = if (addressChanged) permanentAddress.district.ifBlank { null } else null,
              pincode = if (addressChanged) permanentAddress.pincode.ifBlank { null } else null,
              latitude = if (addressChanged) permanentAddress.location?.latitude else null,
              longitude = if (addressChanged) permanentAddress.location?.longitude else null,
              vidhansabha = if (addressChanged) permanentAddress.vidhansabha.ifBlank { null } else null,
              // Temp address fields - only send if temp address has changed
              tempBasicAddress = if (tempAddressChanged && isDifferentCurrentAddress) currentAddress.address.ifBlank { null } else null,
              tempState = if (tempAddressChanged && isDifferentCurrentAddress) currentAddress.state.ifBlank { null } else null,
              tempDistrict = if (tempAddressChanged && isDifferentCurrentAddress) currentAddress.district.ifBlank { null } else null,
              tempPincode = if (tempAddressChanged && isDifferentCurrentAddress) currentAddress.pincode.ifBlank { null } else null,
              tempLatitude = if (tempAddressChanged && isDifferentCurrentAddress) currentAddress.location?.latitude else null,
              tempLongitude = if (tempAddressChanged && isDifferentCurrentAddress) currentAddress.location?.longitude else null,
              tempVidhansabha = if (tempAddressChanged && isDifferentCurrentAddress) currentAddress.vidhansabha.ifBlank { null } else null
            )
          } else {
            // No changes detected, show message and navigate back
            snackbarHostState.showSnackbar("कोई परिवर्तन नहीं किया गया")
            viewModel.resetUpdateState()
            onNavigateBack()
          }
        } else {
          // Create new member
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
            referrerId = referrerState.members.keys.firstOrNull()?.id,
            aryaSamajId = selectedAryaSamaj?.id,
            basicAddress = permanentAddress.address,
            state = permanentAddress.state,
            district = permanentAddress.district,
            pincode = permanentAddress.pincode,
            latitude = permanentAddress.location?.latitude,
            longitude = permanentAddress.location?.longitude,
            vidhansabha = permanentAddress.vidhansabha,
            tempBasicAddress = currentAddress.address,
            tempState = currentAddress.state,
            tempDistrict = currentAddress.district,
            tempPincode = currentAddress.pincode,
            tempLatitude = currentAddress.location?.latitude,
            tempLongitude = currentAddress.location?.longitude,
            tempVidhansabha = currentAddress.vidhansabha
          )
        }
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

  // Handle success: Navigate immediately (snackbar message handled globally)
  LaunchedEffect(uiState.updateSuccess) {
    if (uiState.updateSuccess) {
      viewModel.resetUpdateState()
      onNavigateToMemberDetail(if (isEditMode) memberId!! else uiState.memberId!!)
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
  val launcher =
    rememberFilePickerLauncher(
      type = PickerType.Image,
      mode = PickerMode.Single,
      title = "प्रोफ़ाइल फोटो चुनें"
    ) { file ->
      selectedProfileImage = file
      uploadedImageUrl = null
    }

  // Show loading while fetching member data
  if (isEditMode && isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
      return
    }
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
      text = {
        Text(
          "प्रोफ़ाइल फोटो अपलोड करने में असफल: $imageUploadError। क्या आप फोटो के बिना जारी रखना चाहते हैं या पुनः प्रयास करना चाहते हैं?"
        )
      },
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
      title = {
        Text(
          if (isEditMode) "सदस्य संपादित करें" else "नया सदस्य जोड़ें"
        )
      }
    )

    // Form Content
    LazyColumn(
      modifier =
        Modifier
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
          },
          existingImageUrl = if (isEditMode) uploadedImageUrl else null
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
        SubmitButton(
          text = if (isEditMode) "अपडेट करें" else "सहेजें",
          onSubmit = {
            // Execute the save member logic
            saveMember()
          },
          config = SubmitButtonConfig(
            validator = {
              if (!validateForm()) SubmissionError.ValidationFailed else null
            },
            texts = SubmitButtonTexts(
              submittingText = if (isEditMode) "अपडेट हो रहा है..." else "सहेजा जा रहा है...",
              successText = if (isEditMode) "अपडेट सफल!" else "सफल!"
            )
          ),
          callbacks = object : SubmitCallbacks {
            override fun onError(error: SubmissionError) {
              // Submission errors are handled by GlobalMessageManager in ViewModel
              // No additional action needed here
            }
          }
        )
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
