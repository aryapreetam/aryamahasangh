package com.aryamahasangh.features.gurukul.ui

// Added import for IntrinsicSize to allow Row to size itself to tallest child,
// enabling the vertical divider to fill available height
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.arya_gurukul_qr
import com.aryamahasangh.components.DatePickerField
import com.aryamahasangh.components.ImagePickerComponent
import com.aryamahasangh.components.ImagePickerConfig
import com.aryamahasangh.components.ImagePickerType
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationViewModel
import com.aryamahasangh.ui.components.buttons.SubmissionError
import com.aryamahasangh.ui.components.buttons.SubmitButton
import com.aryamahasangh.ui.components.buttons.SubmitButtonConfig
import com.aryamahasangh.util.GlobalMessageManager
import kotlinx.datetime.LocalDate
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
  val lastUiEffect = remember { mutableStateOf<com.aryamahasangh.features.gurukul.viewmodel.UiEffect?>(null) }
  LaunchedEffect(uiState.uiEffect) {
    if (uiState.uiEffect != com.aryamahasangh.features.gurukul.viewmodel.UiEffect.None && uiState.uiEffect != lastUiEffect.value) {
      lastUiEffect.value = uiState.uiEffect
      when (val effect = uiState.uiEffect) {
        is com.aryamahasangh.features.gurukul.viewmodel.UiEffect.ShowSnackbar -> {
          GlobalMessageManager.showSuccess(effect.message)
        }

        com.aryamahasangh.features.gurukul.viewmodel.UiEffect.None -> {}
      }
      // Optionally: reset effect so it's consumed only once
      // Not strictly needed here since effect will typically update on next state transition
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
      modifier = Modifier.width(500.dp).testTag("registrationFormSatrDateField").semantics { testTag = "satr_date_field" },
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
      modifier = Modifier.width(500.dp).testTag("registrationFormSatrPlaceField").semantics { testTag = "satr_place_field" },
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
    Column {
      Text(
        text = "निचे संरक्षक की संस्तुति के साथ उनका का नाम, व्यवसाय, योग्यता भी लिखे",
        style = MaterialTheme.typography.labelMedium
      )
      OutlinedTextField(
        value = recommendationFieldValue,
        onValueChange = {
          recommendationFieldValue = it
          viewModel.onFieldChange(recommendation = it.text)
        },
        label = { Text("संरक्षक की संस्तुति") },
        modifier = Modifier.width(500.dp).testTag("registrationFormRecommendationField")
          .semantics { testTag = "recommendation_field" },
        isError = uiState.submitErrorMessage != null && recommendationFieldValue.text.isBlank(),
        supportingText = {
          if (uiState.submitErrorMessage != null && recommendationFieldValue.text.isBlank()) {
            Text("कृपया संरक्षक की संस्तुति भरें", color = MaterialTheme.colorScheme.error)
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
