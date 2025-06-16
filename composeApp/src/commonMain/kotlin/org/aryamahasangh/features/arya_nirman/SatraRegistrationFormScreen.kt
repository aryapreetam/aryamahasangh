package org.aryamahasangh.features.arya_nirman

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aryamahasangh.features.activities.GenderAllowed

// --- Data Model for Form Submission ---
data class RegistrationData(
  val fullName: String,
  val phoneNumber: String,
  val gender: GenderAllowed,
  val aadharNumber: String,
  val education: String,
  val fullAddress: String,
  val inspirationSource: String,
  val inspirationDetailName: String?, // For friend/relative name or specific source name
  val inspirationDetailPhone: String?, // For friend/relative phone
  val hasTrainedAryaInFamily: Boolean, // NEW
  val trainedAryaName: String?, // NEW
  val trainedAryaPhone: String?, // NEW
  val instructionsAcknowledged: Boolean
)

// --- Inspiration Source Options ---
enum class InspirationType(val displayName: String) {
  FRIEND_RELATIVE("मित्र / सम्बन्धी"),
  NEWSPAPER("समाचार पत्र"),
  NEWS_CHANNEL("वृत्त वाहिनी"),
  SOCIAL_MEDIA("सामाजिक माध्यम (Facebook/Youtube/Instagram/Whatsapp etc)")
  ;

  companion object {
    fun fromDisplayName(name: String?): InspirationType? = values().find { it.displayName == name }
  }
}

val inspirationOptions = InspirationType.values().toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatraRegistrationFormScreen(
  onRegistrationSuccess: () -> Unit = {}, // Callback for successful registration
  onRegistrationFailed: () -> Unit = {}, // Callback for failed registration (e.g. server error, not validation)
  onNavigateBack: () -> Unit = {}, // Callback for navigating back
  viewModel: SatraRegistrationViewModel,
  activityId: String,
  activityCapacity: Int
) {
  val uiState by viewModel.uiState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()
  val focusManager = LocalFocusManager.current

  // Handle activity loading error
  LaunchedEffect(uiState.error) {
    val error = uiState.error
    if (error != null && !uiState.isLoading) {
      snackbarHostState.showSnackbar(
        message = error,
        duration = SnackbarDuration.Long
      )
    }
  }

  // --- Form State with Validation Holders ---
  var fullName by remember { mutableStateOf("") }
  var fullNameError by remember { mutableStateOf<String?>(null) }
  val fullNameFocusRequester = remember { FocusRequester() }

  var phoneNumber by remember { mutableStateOf("") }
  var phoneNumberError by remember { mutableStateOf<String?>(null) }
  val phoneFocusRequester = remember { FocusRequester() }

  var selectedGender by remember { mutableStateOf<GenderAllowed>(GenderAllowed.MALE) }
  var genderError by remember { mutableStateOf<String?>(null) }
  var genderExpanded by remember { mutableStateOf(false) }

  var aadharNumber by remember { mutableStateOf("") }
  var aadharError by remember { mutableStateOf<String?>(null) }
  val aadharFocusRequester = remember { FocusRequester() }

  var education by remember { mutableStateOf("") }
  var educationError by remember { mutableStateOf<String?>(null) }
  val educationFocusRequester = remember { FocusRequester() }

  var fullAddress by remember { mutableStateOf("") }
  var fullAddressError by remember { mutableStateOf<String?>(null) }
  val addressFocusRequester = remember { FocusRequester() }

  var selectedInspirationSource by remember { mutableStateOf<InspirationType?>(null) }
  var inspirationSourceError by remember { mutableStateOf<String?>(null) }
  var inspirationSourceExpanded by remember { mutableStateOf(false) }

  var friendRelativeName by remember { mutableStateOf("") }
  var friendRelativeNameError by remember { mutableStateOf<String?>(null) }
  val friendNameFocusRequester = remember { FocusRequester() }

  var friendRelativePhone by remember { mutableStateOf("") }
  var friendRelativePhoneError by remember { mutableStateOf<String?>(null) }
  val friendPhoneFocusRequester = remember { FocusRequester() }

  var otherSourceName by remember { mutableStateOf("") }
  var otherSourceNameError by remember { mutableStateOf<String?>(null) }
  val otherSourceFocusRequester = remember { FocusRequester() }

  // --- NEW STATE FOR TRAINED ARYA ---
  var hasTrainedAryaInFamily by remember { mutableStateOf(false) }
  var trainedAryaName by remember { mutableStateOf("") }
  var trainedAryaNameError by remember { mutableStateOf<String?>(null) }
  val trainedAryaNameFocusRequester = remember { FocusRequester() }
  var trainedAryaPhone by remember { mutableStateOf("") }
  var trainedAryaPhoneError by remember { mutableStateOf<String?>(null) }
  val trainedAryaPhoneFocusRequester = remember { FocusRequester() }
  // --- END NEW STATE ---

  var instructionsAcknowledged by remember { mutableStateOf(false) }
  var acknowledgeError by remember { mutableStateOf<String?>(null) }

  // Effect to handle focus after inspiration source changes
  LaunchedEffect(selectedInspirationSource) {
    // A small delay can sometimes help ensure the UI has settled,
    // though often not strictly necessary with LaunchedEffect if the
    // target composable is immediately available after state change.
    // kotlinx.coroutines.delay(50) // Optional: uncomment if still facing issues

    when (selectedInspirationSource) {
      InspirationType.FRIEND_RELATIVE -> {
        if (friendRelativeName.isBlank()) { // Only focus if empty, or always focus
          friendNameFocusRequester.requestFocus()
        }
      }

      InspirationType.NEWSPAPER,
      InspirationType.NEWS_CHANNEL,
      InspirationType.SOCIAL_MEDIA
        -> {
        if (otherSourceName.isBlank()) { // Only focus if empty, or always focus
          otherSourceFocusRequester.requestFocus()
        }
      }

      null -> {
        // Do nothing or clear focus from these fields if needed
      }
    }
  }

  LaunchedEffect(hasTrainedAryaInFamily) { // NEW LaunchedEffect for Trained Arya
    if (hasTrainedAryaInFamily && trainedAryaName.isBlank()) {
      // kotlinx.coroutines.delay(50) // Optional delay if needed
      trainedAryaNameFocusRequester.requestFocus()
    }
  }

  // --- Validation Functions ---
  fun validateFullName(showError: Boolean = true): Boolean {
    return if (fullName.isBlank()) {
      if (showError) fullNameError = "कृपया अपना पूरा नाम दर्ज करें।"
      false
    } else {
      fullNameError = null
      true
    }
  }

  fun validatePhoneNumber(showError: Boolean = true): Boolean {
    return when {
      phoneNumber.isBlank() -> {
        if (showError) phoneNumberError = "कृपया अपना दूरभाष नंबर दर्ज करें।"
        false
      }

      !phoneNumber.all { it.isDigit() } || phoneNumber.length != 10 -> {
        if (showError) phoneNumberError = "कृपया 10 अंकों का मान्य दूरभाष नंबर दर्ज करें।"
        false
      }

      else -> {
        phoneNumberError = null
        true
      }
    }
  }

  fun validateGender(showError: Boolean = true): Boolean {
    return run {
      genderError = null
      true
    }
  }

  fun validateAadharNumber(showError: Boolean = true): Boolean {
    return when {
      aadharNumber.isBlank() -> {
        if (showError) aadharError = "कृपया अपना आधार कार्ड संख्या दर्ज करें।"
        false
      }

      !aadharNumber.all { it.isDigit() } || aadharNumber.length != 12 -> {
        if (showError) aadharError = "कृपया 12 अंकों की मान्य आधार कार्ड संख्या दर्ज करें।"
        false
      }

      else -> {
        aadharError = null
        true
      }
    }
  }

  fun validateEducation(showError: Boolean = true): Boolean {
    return if (education.isBlank()) {
      if (showError) educationError = "कृपया अपनी शैक्षणिक योग्यता दर्ज करें।"
      false
    } else {
      educationError = null
      true
    }
  }

  fun validateFullAddress(showError: Boolean = true): Boolean {
    return if (fullAddress.isBlank()) {
      if (showError) fullAddressError = "कृपया अपना सम्पूर्ण पता दर्ज करें।"
      false
    } else {
      fullAddressError = null
      true
    }
  }

  fun validateInspirationSourceDetails(showError: Boolean = true): Boolean {
    var isValid = true
    when (selectedInspirationSource) {
      InspirationType.FRIEND_RELATIVE -> {
        if (friendRelativeName.isBlank()) {
          if (showError) friendRelativeNameError = "कृपया मित्र/सम्बन्धी का नाम दर्ज करें।"
          isValid = false
        } else {
          friendRelativeNameError = null
        }

        if (friendRelativePhone.isBlank() || !friendRelativePhone.all { it.isDigit() } || friendRelativePhone.length != 10) {
          if (showError) friendRelativePhoneError = "कृपया 10 अंकों का मान्य दूरभाष नंबर दर्ज करें।"
          isValid = false
        } else {
          friendRelativePhoneError = null
        }
      }

      InspirationType.NEWSPAPER, InspirationType.NEWS_CHANNEL, InspirationType.SOCIAL_MEDIA -> {
        if (otherSourceName.isBlank()) {
          if (showError) otherSourceNameError = "कृपया स्रोत का नाम दर्ज करें।"
          isValid = false
        } else {
          otherSourceNameError = null
        }
      }

      null -> { /* Should be caught by validateInspirationSourceSelection */
      }
    }
    return isValid
  }

  fun validateInspirationSourceSelection(showError: Boolean = true): Boolean {
    return if (selectedInspirationSource == null) {
      if (showError) inspirationSourceError = "कृपया प्रेरणा का स्रोत चुनें।"
      false
    } else {
      inspirationSourceError = null
      true
    }
  }

  // NEW Trained Arya Validation
  fun validateTrainedAryaName(showError: Boolean = true): Boolean {
    if (!hasTrainedAryaInFamily) {
      trainedAryaNameError = null
      return true
    }
    return if (trainedAryaName.isBlank()) {
      if (showError) trainedAryaNameError = "कृपया प्रशिक्षित आर्य का नाम दर्ज करें."
      false
    } else {
      trainedAryaNameError = null
      true
    }
  }

  fun validateTrainedAryaPhone(showError: Boolean = true): Boolean {
    if (!hasTrainedAryaInFamily) {
      trainedAryaPhoneError = null
      return true
    }
    return when {
      trainedAryaPhone.isBlank() -> {
        if (showError) trainedAryaPhoneError = "कृपया प्रशिक्षित आर्य का दूरभाष दर्ज करें."
        false
      }

      !trainedAryaPhone.all { it.isDigit() } || trainedAryaPhone.length != 10 -> {
        if (showError) trainedAryaPhoneError = "कृपया 10 अंकों का मान्य दूरभाष नंबर दर्ज करें."
        false
      }

      else -> {
        trainedAryaPhoneError = null
        true
      }
    }
  }

  fun validateAcknowledgement(showError: Boolean = true): Boolean {
    return if (!instructionsAcknowledged) {
      if (showError) {
        acknowledgeError = "पंजीकरण के लिए कृपया निर्देशों को स्वीकार करें।"
        coroutineScope.launch {
          snackbarHostState.showSnackbar(
            message = acknowledgeError!!,
            duration = SnackbarDuration.Short
          )
        }
      }
      false
    } else {
      acknowledgeError = null
      true
    }
  }

  // Derived state for overall form validity to enable/disable button
  val isFormCompletelyValid by derivedStateOf {
    validateFullName(false) &&
      validatePhoneNumber(false) &&
      validateGender(false) &&
      validateAadharNumber(false) &&
      validateEducation(false) &&
      validateFullAddress(false) &&
      validateInspirationSourceSelection(false) &&
      (if (selectedInspirationSource != null) validateInspirationSourceDetails(false) else true) &&
      (if (hasTrainedAryaInFamily) validateTrainedAryaName(false) && validateTrainedAryaPhone(false) else true) && // NEW
      instructionsAcknowledged // Checkbox directly
  }

  fun runAllValidationsAndShowErrors(): Boolean {
    val vBase =
      validateFullName() && validatePhoneNumber() && validateGender() &&
        validateAadharNumber() && validateEducation() && validateFullAddress() &&
        validateInspirationSourceSelection() &&
        (if (selectedInspirationSource != null) validateInspirationSourceDetails() else true)
    val vTrainedArya =
      if (hasTrainedAryaInFamily) validateTrainedAryaName() && validateTrainedAryaPhone() else true // NEW
    return vBase && vTrainedArya
  }

  fun resetForm() {
    fullName = ""
    fullNameError = null
    phoneNumber = ""
    phoneNumberError = null
    selectedGender = GenderAllowed.ANY
    genderError = null
    aadharNumber = ""
    aadharError = null
    education = ""
    educationError = null
    fullAddress = ""
    fullAddressError = null
    selectedInspirationSource = null
    inspirationSourceError = null
    friendRelativeName = ""
    friendRelativeNameError = null
    friendRelativePhone = ""
    friendRelativePhoneError = null
    otherSourceName = ""
    otherSourceNameError = null
    hasTrainedAryaInFamily = false
    trainedAryaName = ""
    trainedAryaNameError = null // NEW
    trainedAryaPhone = ""
    trainedAryaPhoneError = null // NEW
    instructionsAcknowledged = false
    acknowledgeError = null
    coroutineScope.launch { scrollState.scrollTo(0) }
  }

  fun handleSubmit() {
    if (runAllValidationsAndShowErrors()) {
      val data =
        RegistrationData(
          fullName = fullName.trim(),
          phoneNumber = phoneNumber,
          gender = selectedGender,
          aadharNumber = aadharNumber,
          education = education.trim(),
          fullAddress = fullAddress.trim(),
          inspirationSource = selectedInspirationSource!!.displayName,
          inspirationDetailName = if (selectedInspirationSource == InspirationType.FRIEND_RELATIVE) friendRelativeName.trim() else otherSourceName.trim(),
          inspirationDetailPhone = if (selectedInspirationSource == InspirationType.FRIEND_RELATIVE) friendRelativePhone else null,
          hasTrainedAryaInFamily = hasTrainedAryaInFamily, // NEW
          trainedAryaName = if (hasTrainedAryaInFamily) trainedAryaName.trim() else null, // NEW
          trainedAryaPhone = if (hasTrainedAryaInFamily) trainedAryaPhone else null, // NEW
          instructionsAcknowledged = instructionsAcknowledged
        )
      viewModel.createRegistration(activityId = activityId, data = data, activityCapacity = activityCapacity)
    } else {
      coroutineScope.launch {
        snackbarHostState.showSnackbar(
          message = "पंजीकरण विफल। कृपया सभी आवश्यक फ़ील्ड सही ढंग से भरें और पुनः प्रयास करें।",
          duration = SnackbarDuration.Long
        )
      }
      onRegistrationFailed()
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .padding(paddingValues)
          .verticalScroll(scrollState)
          .padding(horizontal = 8.dp, vertical = 16.dp)
          .widthIn(max = 700.dp) // Max width for form content
          .imePadding() // Handles software keyboard overlap
    ) {
      Text(
        text = "पंजीकरण प्रपत्र", // Registration Form
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary
      )

      // --- Form Fields ---

      // Full Name
      OutlinedTextField(
        value = fullName,
        onValueChange = {
          fullName = it
          if (fullNameError != null) validateFullName()
        },
        label = { Text("सत्रार्थी का नाम") },
        modifier = Modifier.fillMaxWidth().focusRequester(fullNameFocusRequester),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
        isError = fullNameError != null,
        supportingText = { fullNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
      )
      FlowRow(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Gender
        ExposedDropdownMenuBox(
          expanded = genderExpanded,
          onExpandedChange = { genderExpanded = !genderExpanded },
          modifier = Modifier.width(130.dp)
        ) {
          OutlinedTextField(
            value = selectedGender.toDisplayNameShort(),
            onValueChange = {}, readOnly = true,
            label = { Text("लिंग") },
            placeholder = { Text("लिंग चुनें") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            isError = genderError != null,
            supportingText = { genderError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
          )
          ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
            GenderAllowed.entries.forEach { option ->
              DropdownMenuItem(
                text = { Text(option.toDisplayNameShort()) },
                onClick = {
                  selectedGender = option
                  genderExpanded = false
                  if (genderError != null) validateGender()
                  focusManager.moveFocus(FocusDirection.Next)
                }
              )
            }
          }
        }

        // Phone Number
        OutlinedTextField(
          value = phoneNumber,
          onValueChange = {
            if (it.length <= 10 && it.all { char -> char.isDigit() }) phoneNumber = it
            if (phoneNumberError != null) validatePhoneNumber()
          },
          label = { Text("दूरभाष (Mobile)") },
          modifier = Modifier.width(160.dp).focusRequester(phoneFocusRequester),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
          keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
          isError = phoneNumberError != null,
          supportingText = { phoneNumberError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        // Aadhar Card Number
        OutlinedTextField(
          value = aadharNumber,
          onValueChange = {
            if (it.length <= 12 && it.all { char -> char.isDigit() }) aadharNumber = it
            if (aadharError != null) validateAadharNumber()
          },
          label = { Text("आधार कार्ड संख्या") },
          modifier = Modifier.width(160.dp).focusRequester(aadharFocusRequester),
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
          keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
          isError = aadharError != null,
          supportingText = { aadharError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        )

        // Education Qualification
        OutlinedTextField(
          value = education,
          onValueChange = {
            education = it
            if (educationError != null) validateEducation()
          },
          label = { Text("शैक्षणिक योग्यता") },
          modifier = Modifier.width(160.dp).focusRequester(educationFocusRequester),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
          keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
          isError = educationError != null,
          supportingText = { educationError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        )
      }

      // Full Address
      OutlinedTextField(
        value = fullAddress,
        onValueChange = {
          fullAddress = it
          if (fullAddressError != null) validateFullAddress()
        },
        label = { Text("सम्पूर्ण पता") },
        modifier = Modifier.width(500.dp).defaultMinSize(minHeight = 100.dp).focusRequester(addressFocusRequester),
        minLines = 3,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        keyboardActions =
          KeyboardActions(onNext = {
            // Decide if next should go to dropdown or clear focus
            focusManager.moveFocus(FocusDirection.Next)
          }),
        isError = fullAddressError != null,
        supportingText = { fullAddressError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
      )
      Spacer(modifier = Modifier.height(8.dp))

      // Inspiration Source Dropdown
      Text(
        "सत्र में आने के लिए आपको किसने प्रेरित किया?",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp)
      )
      ExposedDropdownMenuBox(
        expanded = inspirationSourceExpanded,
        onExpandedChange = { inspirationSourceExpanded = !inspirationSourceExpanded },
        modifier = Modifier.width(500.dp)
      ) {
        OutlinedTextField(
          value = selectedInspirationSource?.displayName ?: "",
          onValueChange = {},
          readOnly = true,
          label = { Text("प्रेरणा का स्रोत चुनें") },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inspirationSourceExpanded) },
          modifier =
            Modifier.fillMaxWidth().menuAnchor().focusRequester(
              remember {
                FocusRequester()
              } // Dummy focus for dropdown
            ),
          isError = inspirationSourceError != null,
          supportingText = { inspirationSourceError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        )
        ExposedDropdownMenu(
          expanded = inspirationSourceExpanded,
          onDismissRequest = { inspirationSourceExpanded = false }
        ) {
          inspirationOptions.forEach { option ->
            DropdownMenuItem(
              text = { Text(option.displayName) },
              onClick = {
                val previousSource = selectedInspirationSource // Store previous for comparison
                selectedInspirationSource = option
                inspirationSourceExpanded = false

                // Clear previous conditional fields and their errors only if source type changes
                if (previousSource != option) { // or more specific logic if needed
                  friendRelativeName = ""
                  friendRelativeNameError = null
                  friendRelativePhone = ""
                  friendRelativePhoneError = null
                  otherSourceName = ""
                  otherSourceNameError = null
                }

                if (inspirationSourceError != null) validateInspirationSourceSelection()
              }
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))

      // Conditional Fields for Inspiration Source
      when (selectedInspirationSource) {
        InspirationType.FRIEND_RELATIVE -> {
          OutlinedTextField(
            value = friendRelativeName,
            onValueChange = {
              friendRelativeName = it
              if (friendRelativeNameError != null) validateInspirationSourceDetails()
            },
            label = { Text("मित्र/सम्बन्धी का नाम") },
            modifier = Modifier.width(500.dp).focusRequester(friendNameFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            isError = friendRelativeNameError != null,
            supportingText = { friendRelativeNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = friendRelativePhone,
            onValueChange = {
              if (it.length <= 10 && it.all { char -> char.isDigit() }) friendRelativePhone = it
              if (friendRelativePhoneError != null) validateInspirationSourceDetails()
            },
            label = { Text("मित्र/सम्बन्धी का दूरभाष (10 अंक)") },
            modifier = Modifier.width(500.dp).focusRequester(friendPhoneFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            isError = friendRelativePhoneError != null,
            supportingText = { friendRelativePhoneError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
          )
        }

        InspirationType.NEWSPAPER, InspirationType.NEWS_CHANNEL, InspirationType.SOCIAL_MEDIA -> {
          OutlinedTextField(
            value = otherSourceName,
            onValueChange = {
              otherSourceName = it
              if (otherSourceNameError != null) validateInspirationSourceDetails()
            },
            label = { Text("${selectedInspirationSource?.displayName ?: "स्रोत"} का नाम") },
            modifier = Modifier.width(500.dp).focusRequester(otherSourceFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            isError = otherSourceNameError != null,
            supportingText = { otherSourceNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
          )
        }

        null -> { /* No fields to show, or placeholder if needed */
        }
      }
      if (selectedInspirationSource != null) Spacer(modifier = Modifier.height(8.dp))

// --- NEW TRAINED ARYA SECTION ---
      Column(modifier = Modifier.widthIn(500.dp).align(Alignment.CenterHorizontally)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth().clickable {
              hasTrainedAryaInFamily = !hasTrainedAryaInFamily
              if (!hasTrainedAryaInFamily) { // Clear if unchecking
                trainedAryaName = ""
                trainedAryaNameError = null
                trainedAryaPhone = ""
                trainedAryaPhoneError = null
              } else { // If checking, validate if already filled from previous interaction
                if (trainedAryaName.isNotBlank()) validateTrainedAryaName()
                if (trainedAryaPhone.isNotBlank()) validateTrainedAryaPhone()
              }
            }
        ) {
          Checkbox(
            checked = hasTrainedAryaInFamily,
            onCheckedChange = {
              hasTrainedAryaInFamily = it
              if (!it) {
                trainedAryaName = ""
                trainedAryaNameError = null
                trainedAryaPhone = ""
                trainedAryaPhoneError = null
              } else {
                if (trainedAryaName.isNotBlank()) validateTrainedAryaName()
                if (trainedAryaPhone.isNotBlank()) validateTrainedAryaPhone()
              }
            },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
          )

          Text("क्या आपके परिवार में प्रशिक्षित आर्य है?", style = MaterialTheme.typography.bodyMedium)
        }

        AnimatedVisibility(
          visible = hasTrainedAryaInFamily,
          enter = slideInVertically { fullHeight -> fullHeight / 2 } + fadeIn(), // Adjusted animation
          exit = slideOutVertically { fullHeight -> fullHeight / 2 } + fadeOut()
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(modifier = Modifier.height(4.dp)) // Small space after checkbox
            OutlinedTextField(
              value = trainedAryaName,
              onValueChange = {
                trainedAryaName = it
                if (trainedAryaNameError != null) validateTrainedAryaName()
              },
              label = { Text("प्रशिक्षित आर्य का नाम") },
              modifier = Modifier.fillMaxWidth().focusRequester(trainedAryaNameFocusRequester),
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Text,
                  imeAction = ImeAction.Next
                ),
              keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
              isError = trainedAryaNameError != null, supportingText = { trainedAryaNameError?.let { Text(it) } }
            )
            OutlinedTextField(
              value = trainedAryaPhone,
              onValueChange = {
                if (it.length <= 10 &&
                  it.all { c ->
                    c.isDigit()
                  }
                ) {
                  trainedAryaPhone = it
                }
                if (trainedAryaPhoneError != null) validateTrainedAryaPhone()
              },
              label = { Text("प्रशिक्षित आर्य का दूरभाष (Mobile)") },
              modifier = Modifier.fillMaxWidth().focusRequester(trainedAryaPhoneFocusRequester),
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Number,
                  imeAction = ImeAction.Next
                ),
              keyboardActions =
                KeyboardActions(
                  onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
              // Or Done if it's the last
              isError = trainedAryaPhoneError != null, supportingText = { trainedAryaPhoneError?.let { Text(it) } }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Additional Instructions
      Text(
        "अतिरिक्त निर्देश:",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))
      val instructions =
        listOf(
          "सत्र में दोनों दिन उपस्थित रहना अनिवार्य है।",
          "सत्र में कोई मूल्यवान वस्तु न लाएं।",
          "सत्र में प्रश्नोत्तर शैली में विद्वान्/आचार्यों से संवाद/अध्यापन होगा।",
          "विना परिचय पत्र (Identity card) सत्र में बैठने की अनुमति नहीं दी जाएगी।"
        )
      Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(Modifier.padding(12.dp)) {
          instructions.forEach { instruction ->
            Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.Top) {
              Text(
                "•  ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))

      // Acknowledgement Checkbox
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable {
              instructionsAcknowledged = !instructionsAcknowledged
              if (acknowledgeError != null && instructionsAcknowledged) acknowledgeError = null
            }
            .padding(vertical = 8.dp)
      ) {
        Checkbox(
          checked = instructionsAcknowledged,
          onCheckedChange = {
            instructionsAcknowledged = it
            if (acknowledgeError != null && it) acknowledgeError = null
          },
          colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Text(
          text = "मैं स्वीकार करता/करती हूँ कि मैंने निर्देश पढ़ और समझ लिए हैं।",
          style = MaterialTheme.typography.bodyMedium,
          color = if (acknowledgeError != null && !instructionsAcknowledged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
      }
      if (acknowledgeError != null && !instructionsAcknowledged) {
        Text(
          acknowledgeError!!,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 48.dp) // Align with checkbox text
        )
      }
      Spacer(modifier = Modifier.height(24.dp))

      // Register Button
      Button(
        onClick = { handleSubmit() },
        modifier = Modifier.height(52.dp),
        enabled = isFormCompletelyValid && !uiState.isLoading
      ) {
        if (uiState.isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
          )
          Spacer(Modifier.width(8.dp))
          Text("पंजीकृत किया जा रहा है...")
        } else {
          Text(
            "पंजीकृत करें",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp)
          )
        }
      }

      // Effect to react to registration result
      LaunchedEffect(uiState.registrationResult, uiState.isCapacityFull) {
        when (uiState.registrationResult) {
          true -> { // Success
            coroutineScope.launch {
              snackbarHostState.showSnackbar(
                message = "आपने सफलतापूर्वक पंजीकरण करा लिया है!",
                duration = SnackbarDuration.Long
              )
            }
            resetForm()
            onRegistrationSuccess()
            viewModel.registrationEventHandled() // Consume the event
          }

          false -> { // Failure (if error is not null, it's a failure)
            if (uiState.error != null) {
              coroutineScope.launch {
                snackbarHostState.showSnackbar(
                  message = "पंजीकरण विफल: ${uiState.error}",
                  duration = SnackbarDuration.Long
                )
              }
              // If capacity is full, navigate back after showing the message
              if (uiState.isCapacityFull) {
                kotlinx.coroutines.delay(2000) // Wait 2 seconds for user to read the message
                onNavigateBack()
              }
            }
            viewModel.registrationEventHandled() // Consume the event
          }

          null -> {
            // Event has been consumed or is in initial state, do nothing
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp)) // Space at the bottom
    }
  }
}

// Note: Preview annotations are only fully supported in the Android platform.
// To use Preview annotations in this file, you need to:
// 1. Add the following import: import androidx.compose.ui.tooling.preview.Preview
// 2. Uncomment the Preview composables below
//
// @Preview
// @Composable
// fun RegistrationFormScreenPreviewMobileLight() {
//   MaterialTheme(colorScheme = lightColorScheme()) { // Use your app's light theme or a default one
//     SatraRegistrationFormScreen(
//       onRegistrationSuccess = { println("Preview: Registration Success!") },
//       onRegistrationFailed = { println("Preview: Registration Failed!") },
//     )
//   }
// }
//
// @Preview(showBackground = true, name = "Registration Form Mobile (Dark)", widthDp = 380, heightDp = 1200)
// @Composable
// fun RegistrationFormScreenPreviewMobileDark() {
//   MaterialTheme(colorScheme = darkColorScheme()) { // Use your app's dark theme or a default one
//     SatraRegistrationFormScreen(
//       onRegistrationSuccess = { println("Preview: Registration Success!") },
//       onRegistrationFailed = { println("Preview: Registration Failed!") }
//     )
//   }
// }
//
// @Preview(showBackground = true, name = "Registration Form Desktop/Tablet (Light)", widthDp = 720, heightDp = 1000)
// @Composable
// fun RegistrationFormScreenPreviewDesktopLight() {
//   MaterialTheme(colorScheme = lightColorScheme()) {
//     // Wrapping in a Box to better simulate how it might be centered or constrained on a larger screen
//     Box(
//       modifier = Modifier.fillMaxSize(), // Fill the preview area
//       contentAlignment = Alignment.TopCenter // Align the form to the top center
//     ) {
//       SatraRegistrationFormScreen(
//         onRegistrationSuccess = { println("Preview: Registration Success!") },
//         onRegistrationFailed = { println("Preview: Registration Failed!") }
//       )
//     }
//   }
// }
//
// @Preview(showBackground = true, name = "Registration Form Wide Desktop (Light)", widthDp = 1000, heightDp = 1000)
// @Composable
// fun RegistrationFormScreenPreviewWideDesktopLight() {
//   MaterialTheme(colorScheme = lightColorScheme()) {
//     Box(
//       modifier = Modifier.fillMaxSize(),
//       contentAlignment = Alignment.TopCenter
//     ) {
//       SatraRegistrationFormScreen(
//         onRegistrationSuccess = { println("Preview: Registration Success!") },
//         onRegistrationFailed = { println("Preview: Registration Failed!") }
//       )
//     }
//   }
// }
