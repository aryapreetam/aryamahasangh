package com.aryamahasangh.features.gurukul.ui

// Added import for IntrinsicSize to allow Row to size itself to tallest child,
// enabling the vertical divider to fill available height
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.arya_gurukul_qr
import com.aryamahasangh.components.DatePickerField
import com.aryamahasangh.components.ImagePickerComponent
import com.aryamahasangh.components.ImagePickerConfig
import com.aryamahasangh.components.ImagePickerType
import com.aryamahasangh.features.gurukul.viewmodel.ButtonState
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationViewModel
import com.aryamahasangh.features.gurukul.viewmodel.UiEffect
import com.aryamahasangh.util.GlobalMessageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
fun CourseRegistrationFormScreen(
  activityId: String,
  onNavigateBack: () -> Unit,
  viewModel: CourseRegistrationViewModel
) {
  val uiState by viewModel.uiState.collectAsState()
  val openDialog = uiState.showUnsavedExitDialog
  val shouldNavigateBack = uiState.buttonState is ButtonState.Success
  if (shouldNavigateBack) {
    LaunchedEffect("success-nav") {
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
  // ONE collector for effects — permanent for composition lifetime
  LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is UiEffect.ShowSnackbar ->
          if (effect.isError)
            GlobalMessageManager.showError(effect.message)
          else
            GlobalMessageManager.showSuccess(effect.message)

        UiEffect.None -> Unit
      }
    }
  }

  Column(
    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()).semantics { testTag = "course_registration_form" },
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    var nameFieldValue by remember { mutableStateOf(TextFieldValue(uiState.name, TextRange(uiState.name.length))) }
    LaunchedEffect(uiState.name) {
      if (nameFieldValue.text != uiState.name) {
        nameFieldValue = TextFieldValue(uiState.name, TextRange(uiState.name.length))
      }
    }
    Text(
      text = "कक्षा प्रवेश पंजीकरण",
      style = MaterialTheme.typography.headlineSmall
    )
    Spacer(Modifier.height(16.dp))
    // Photo Upload Section
    ImagePickerComponent(
      state = uiState.photoPickerState,
      onStateChange = { newState ->
        viewModel.onFieldChange(photoPickerState = newState)
      },
      config = ImagePickerConfig(
        label = "फोटो अपलोड करें\n(Passport Size)",
        type = ImagePickerType.PROFILE_PHOTO,
        allowMultiple = false,
        maxImages = 1,
        isMandatory = true,
        showPreview = true,
        previewSize = 180, // Slightly bigger preview (default is 150dp for PROFILE_PHOTO)
        enableBackgroundCompression = true,
        compressionTargetKb = 40,
        showCompressionProgress = true
      )
    )

    OutlinedTextField(
      value = nameFieldValue,
      onValueChange = {
        nameFieldValue = it
        viewModel.onFieldChange(name = it.text)
      },
      label = { Text("नाम") },
      modifier = Modifier.width(500.dp).testTag("registrationFormNameField").semantics { testTag = "name_field" },
      isError = uiState.submitErrorMessage != null && nameFieldValue.text.isBlank(),
      supportingText = {
        if (uiState.submitErrorMessage != null && nameFieldValue.text.isBlank()) {
          Text("कृपया नाम दर्ज करें", color = MaterialTheme.colorScheme.error)
        }
      },
      singleLine = true,
      enabled = !uiState.isSubmitting
    )
    var phoneFieldValue by remember { mutableStateOf(TextFieldValue(uiState.phoneNumber, TextRange(uiState.phoneNumber.length))) }
    LaunchedEffect(uiState.phoneNumber) {
      if (phoneFieldValue.text != uiState.phoneNumber) {
        phoneFieldValue = TextFieldValue(uiState.phoneNumber, TextRange(uiState.phoneNumber.length))
      }
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      DatePickerField(
        value = uiState.dob,
        onValueChange = { date ->
          viewModel.onFieldChange(dob = date)
        },
        label = "जन्मतिथि",
        type = com.aryamahasangh.components.DatePickerType.DATE_OF_BIRTH,
        excludeToday = true,
        modifier = Modifier.width(160.dp).testTag("registrationFormDobField").semantics { testTag = "dob_field" },
        isError = uiState.submitErrorMessage != null && uiState.dob == null,
        supportingText = {
          if (uiState.submitErrorMessage != null && uiState.dob == null) {
            Text("कृपया जन्म तिथि चुनें", color = MaterialTheme.colorScheme.error)
          }
        },
        required = true,
        enabled = !uiState.isSubmitting
      )

      OutlinedTextField(
        value = phoneFieldValue,
        onValueChange = { tfv ->
          val filtered = tfv.text.filter { it.isDigit() }.take(10)
          phoneFieldValue = TextFieldValue(filtered, TextRange(filtered.length))
          viewModel.onFieldChange(phoneNumber = filtered)
        },
        label = { Text("मोबाइल नंबर") },
        modifier = Modifier.width(300.dp).testTag("registrationFormPhoneField").semantics { testTag = "phone_field" },
        isError = uiState.submitErrorMessage != null && phoneFieldValue.text.isBlank(),
        supportingText = {
          if (uiState.submitErrorMessage != null && phoneFieldValue.text.isBlank()) {
            Text("कृपया फोन नंबर दर्ज करें", color = MaterialTheme.colorScheme.error)
          }
        },
        singleLine = true,
        enabled = !uiState.isSubmitting,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
      )
    }

    var qualificationFieldValue by remember { mutableStateOf(TextFieldValue(uiState.qualification, TextRange(uiState.qualification.length))) }
    LaunchedEffect(uiState.qualification) {
      if (qualificationFieldValue.text != uiState.qualification) {
        qualificationFieldValue = TextFieldValue(uiState.qualification, TextRange(uiState.qualification.length))
      }
    }
    OutlinedTextField(
      value = qualificationFieldValue,
      onValueChange = {
        qualificationFieldValue = it
        viewModel.onFieldChange(qualification = it.text)
      },
      label = { Text("योग्यता") },
      modifier = Modifier.width(500.dp).testTag("registrationFormQualificationField").semantics { testTag = "qualification_field" },
      isError = uiState.submitErrorMessage != null && qualificationFieldValue.text.isBlank(),
      supportingText = {
        if (uiState.submitErrorMessage != null && qualificationFieldValue.text.isBlank()) {
          Text("कृपया योग्यता दर्ज करें", color = MaterialTheme.colorScheme.error)
        }
      },
      singleLine = true,
      enabled = !uiState.isSubmitting
    )


    var guardianNameFieldValue by remember { mutableStateOf(TextFieldValue(uiState.guardianName, TextRange(uiState.guardianName.length))) }
    LaunchedEffect(uiState.guardianName) {
      if (guardianNameFieldValue.text != uiState.guardianName) {
        guardianNameFieldValue = TextFieldValue(uiState.guardianName, TextRange(uiState.guardianName.length))
      }
    }
    OutlinedTextField(
      value = guardianNameFieldValue,
      onValueChange = {
        guardianNameFieldValue = it
        viewModel.onFieldChange(guardianName = it.text)
      },
      label = { Text("पिता/पति का नाम") },
      modifier = Modifier.width(500.dp).testTag("registrationFormGuardianNameField").semantics { testTag = "guardian_name_field" },
      isError = uiState.submitErrorMessage != null && guardianNameFieldValue.text.isBlank(),
      supportingText = {
        if (uiState.submitErrorMessage != null && guardianNameFieldValue.text.isBlank()) {
          Text("कृपया अभिभावक का नाम दर्ज करें", color = MaterialTheme.colorScheme.error)
        }
      },
      singleLine = true,
      enabled = !uiState.isSubmitting
    )

    var addressFieldValue by remember { mutableStateOf(TextFieldValue(uiState.address, TextRange(uiState.address.length))) }
    LaunchedEffect(uiState.address) {
      if (addressFieldValue.text != uiState.address) {
        addressFieldValue = TextFieldValue(uiState.address, TextRange(uiState.address.length))
      }
    }
    OutlinedTextField(
      value = addressFieldValue,
      onValueChange = {
        addressFieldValue = it
        viewModel.onFieldChange(address = it.text)
      },
      label = { Text("पता") },
      modifier = Modifier.width(500.dp).testTag("registrationFormAddressField").semantics { testTag = "address_field" },
      isError = uiState.submitErrorMessage != null && addressFieldValue.text.isBlank(),
      supportingText = {
        if (uiState.submitErrorMessage != null && addressFieldValue.text.isBlank()) {
          Text("कृपया पता दर्ज करें", color = MaterialTheme.colorScheme.error)
        }
      },
      minLines = 3,
      maxLines = 5,
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
    //

    Column {
      Text(
        text = "दो दिवसीय(आर्या प्रशिक्षण) सत्र कब और कहाँ किया?",
        style = MaterialTheme.typography.labelMedium
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DatePickerField(
          value = uiState.satrDate,
          onValueChange = { date ->
            viewModel.onFieldChange(satrDate = date)
          },
          label = "सत्र दिनांक",
          modifier = Modifier.width(160.dp).testTag("registrationFormSatrDateField")
            .semantics { testTag = "satr_date_field" },
          isError = uiState.submitErrorMessage != null && uiState.satrDate == null,
          supportingText = {
            if (uiState.submitErrorMessage != null && uiState.satrDate == null) {
              Text("कृपया सत्र दिनांक चुनें", color = MaterialTheme.colorScheme.error)
            }
          },
          required = true,
          enabled = !uiState.isSubmitting
        )

        OutlinedTextField(
          value = placeFieldValue,
          onValueChange = {
            placeFieldValue = it
            viewModel.onFieldChange(satrPlace = it.text)
          },
          label = { Text("सत्र स्थान") },
          modifier = Modifier.width(300.dp).testTag("registrationFormSatrPlaceField")
            .semantics { testTag = "satr_place_field" },
          isError = uiState.submitErrorMessage != null && placeFieldValue.text.isBlank(),
          supportingText = {
            if (uiState.submitErrorMessage != null && placeFieldValue.text.isBlank()) {
              Text("कृपया सत्र स्थान भरें", color = MaterialTheme.colorScheme.error)
            }
          },
          singleLine = true,
          enabled = !uiState.isSubmitting
        )
      }
    }

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
    Column {
      Text(
        text = "आर्या परिषद् के अध्यक्षा(जनपद/प्रांत) की संस्तुति(Recommendation)",
        style = MaterialTheme.typography.labelMedium
      )
      OutlinedTextField(
        value = recommendationFieldValue,
        onValueChange = {
          recommendationFieldValue = it
          viewModel.onFieldChange(recommendation = it.text)
        },
        label = { Text("संस्तुति") },
        modifier = Modifier.width(500.dp).testTag("registrationFormRecommendationField")
          .semantics { testTag = "recommendation_field" },
        isError = uiState.submitErrorMessage != null && recommendationFieldValue.text.isBlank(),
        supportingText = {
          if (uiState.submitErrorMessage != null && recommendationFieldValue.text.isBlank()) {
            Text("कृपया संस्तुति(Recommendation) भरें", color = MaterialTheme.colorScheme.error)
          }
        },
        minLines = 3,
        maxLines = 8,
        enabled = !uiState.isSubmitting
      )
    }
    ResponsivePaymentDetails()
    ImagePickerComponent(
      state = uiState.imagePickerState,
      onStateChange = { pickerState -> viewModel.onFieldChange(imagePickerState = pickerState) },
      modifier = Modifier.width(500.dp).semantics { testTag = "receipt_picker" },
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
      state = uiState.buttonState,
      onSubmit = { viewModel.onSubmit() },
      isValid = uiState.isValid,
      modifier = Modifier.semantics { testTag = "submit_button" }
    )
  }
}

data class SubmitButtonStrings(
  val idle: String = "पंजीकरण प्रस्तुत करें",
  val loading: String = "प्रेषित किया जा रहा है…",
  val success: String = "सफल!",
  val errorPrefix: String = "विफल"
)

@Composable
fun SubmitButton(
  state: ButtonState,
  strings: SubmitButtonStrings = SubmitButtonStrings(),
  isValid: Boolean,
  validationMessage: String = "कृपया सभी आवश्यक फील्ड भरें",
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val shake = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()

  // Local UI-only validation block
  var showValidationFeedback by remember { mutableStateOf(false) }

  // --- RUN SHAKE ---
  suspend fun runShake() {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    val pattern = listOf(12f, -10f, 8f, -6f, 4f, -2f, 0f)
    for (v in pattern) shake.animateTo(v, tween(50))
  }

  // Backend error triggers shake
  LaunchedEffect(state) {
    if (state is ButtonState.Error) runShake()
  }

  // Button Colors (unchanged)
  val backgroundColor = when (state) {
    ButtonState.Idle -> MaterialTheme.colorScheme.primary
    ButtonState.Loading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
    ButtonState.Success -> Color(0xFF059669)
    is ButtonState.Error -> Color(0xFFDC2626)
  }

  val contentColor = when (state) {
    ButtonState.Idle, ButtonState.Loading -> MaterialTheme.colorScheme.onPrimary
    ButtonState.Success -> Color.White
    is ButtonState.Error -> Color.White
  }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {

    // ===== BUTTON =====
    Button(
      onClick = {
        if (state is ButtonState.Loading) return@Button

        // VALIDATION FAILED → SHAKE + SHOW ERROR CARD
        if (!isValid) {
          scope.launch { runShake() }
          showValidationFeedback = true

          // auto-hide
          scope.launch {
            delay(2000)
            showValidationFeedback = false
          }

          return@Button
        }

        // VALIDATION PASSED → SUBMIT
        onSubmit()
      },
      enabled = true,
      modifier = modifier
        .height(56.dp)
        .graphicsLayer { translationX = shake.value },
      colors = ButtonDefaults.buttonColors(
        containerColor = backgroundColor,
        contentColor = contentColor
      ),
      contentPadding = PaddingValues(0.dp)
    ) {

      // SAME INTERNAL CONTENT AS BEFORE
      Box(
        modifier = Modifier
          .widthIn(min = 160.dp)
          .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
      ) {
        AnimatedContent(
          targetState = state,
          transitionSpec = {
            fadeIn(tween(250)) togetherWith fadeOut(tween(250))
          },
          label = "SubmitButtonAnimation"
        ) { s ->

          when (s) {

            ButtonState.Idle -> {
              Text(strings.idle, style = MaterialTheme.typography.labelLarge)
            }

            ButtonState.Loading -> {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = contentColor
                )
                Text(strings.loading, style = MaterialTheme.typography.labelLarge)
              }
            }

            ButtonState.Success -> {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Check, null, tint = contentColor, modifier = Modifier.size(20.dp))
                Text(strings.success, style = MaterialTheme.typography.labelLarge)
              }
            }

            is ButtonState.Error -> {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Refresh, null, tint = contentColor, modifier = Modifier.size(18.dp))
                Text(
                  // TODO: think about adding -> + " " + s.message.take(20) + "…"
                  strings.errorPrefix ,
                  style = MaterialTheme.typography.labelLarge
                )
              }
            }
          }
        }
      }
    }

    // ===== VALIDATION ERROR CARD BELOW BUTTON =====
    Box(
      modifier = Modifier.height(48.dp), // Reserved space to prevent jumping
      contentAlignment = Alignment.Center
    ) {
      // only use androidx.compose.animation.AnimatedVisibility here(not androidx.compose.foundation.layout.ColumnScope.AnimatedVisibility)
      androidx.compose.animation.AnimatedVisibility(
        visible = showValidationFeedback,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150))
      ) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE4E4)
          )
        ) {
          Text(
            text = validationMessage,
            color = Color(0xFFAA0000),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
          )
        }
      }
    }
  }
}

@Composable
@Preview
fun ResponsivePaymentDetails() {
  Column {
    // This is the top-level text, as seen in your image
    Text(
      text = "नीचे दिए गए माध्यम से आप भुगतान कर सकते है।", // "You can pay through the means given below."
      fontSize = 16.sp,
      modifier = Modifier.padding(top = 16.dp)
    )
    // The main component that handles responsiveness
    BoxWithConstraints(
      modifier = Modifier.padding(16.dp)
    ) {
      val isWideScreen = maxWidth > 600.dp // Define your breakpoint

      if (isWideScreen) {
        // Horizontal layout for wider screens
        Row(
          // Ensure Row adopts the height of its tallest child so the vertical divider can fill it
          modifier = Modifier.padding(top = 16.dp).height(IntrinsicSize.Min),
          verticalAlignment = Alignment.CenterVertically
        ) {
          QrCodeSection(modifier = Modifier.padding(end = 16.dp))
          OrDivider(isHorizontalLayout = true, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) // Vertical divider
          BankDetailsSection(modifier = Modifier.padding(start = 16.dp))
        }
      } else {
        // Vertical layout for narrower screens
        Column {
          QrCodeSection(modifier = Modifier.padding(bottom = 16.dp))
          OrDivider(isHorizontalLayout = false) // Horizontal divider
          BankDetailsSection(modifier = Modifier.padding(top = 16.dp))
        }
      }
    }
  }
}

@Composable
fun QrCodeSection(modifier: Modifier = Modifier) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    // Placeholder for QR Code - Replace with your actual QR code image
    Box(
      modifier =
        Modifier
          .size(200.dp) // Adjust size as needed
          .background(Color.LightGray),
      // Placeholder background
      contentAlignment = Alignment.Center
    ) {
      Image(
        painter = painterResource(Res.drawable.arya_gurukul_qr), // Replace with actual resource
        contentDescription = "QR Code for UPI Payment",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
//    Text(
//      text = "UPI: 9355690824m@pnb",
//      fontSize = 14.sp,
//      fontWeight = FontWeight.Medium
//    )
    Text(
      text = "Merchant: Aryaa Gurukul Mahavidyalaya",
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun BankDetailsSection(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Start // Align text to the start for bank details
  ) {
    Text("Bank details:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    BankDetailItem("Bank name:", "PNB Bank")
    BankDetailItem("Branch:", "Model Town , Karnal, HR-132001")
    BankDetailItem("Name in Bank:", "Aryaa Gurukul Mahavidyalaya")
    BankDetailItem("Account number:", "0286000101649186")
    BankDetailItem("IFSC code:", "PUNB0028600")
  }
}

@Composable
fun BankDetailItem(
  label: String,
  value: String
) {
  Row {
    Text(
      text = label,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.width(150.dp)
    ) // Adjust width for alignment
    Text(text = value)
  }
  Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun OrDivider(
  isHorizontalLayout: Boolean,
  modifier: Modifier = Modifier // Modifier for the Box container of OrDivider
) {
  val dividerText = "OR"
  val textHorizontalPadding = 8.dp
  val textVerticalPadding = 4.dp // Padding above/below "OR" text

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier.then( // Apply incoming modifier first
        if (isHorizontalLayout) {
          // For VERTICAL divider, we want it to attempt to fill the height of its parent Row.
          Modifier.fillMaxHeight()
        } else {
          // For HORIZONTAL divider, it takes a percentage of available width.
          Modifier.fillMaxWidth(0.7f) // Make horizontal divider a bit shorter
        }
      )
  ) {
    if (isHorizontalLayout) {
      // VERTICAL DIVIDER: Line - Text - Line
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center, // Center items within the column
        modifier = Modifier.fillMaxHeight() // Column itself fills the Box's height
      ) {
        VerticalDivider(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
          modifier =
            Modifier
              .weight(1f) // Top segment takes available space above text
              .width(1.dp)
        )
        Text(
          text = dividerText,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(vertical = textVerticalPadding)
        )
        VerticalDivider(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
          modifier =
            Modifier
              .weight(1f) // Bottom segment takes available space below text
              .width(1.dp)
        )
      }
    } else {
      // HORIZONTAL DIVIDER: A single Divider with Text on top (using background to "erase")
      // The Box's contentAlignment = Alignment.Center will center both Divider and Text.
      HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        modifier =
          Modifier
            .fillMaxWidth() // Divider fills the width of its parent Box
            .height(1.dp)
      )
      Text(
        text = dividerText,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier =
          Modifier
            .background(MaterialTheme.colorScheme.surface) // Erase line behind text
            .padding(horizontal = textHorizontalPadding, vertical = textVerticalPadding)
      )
    }
  }
}
