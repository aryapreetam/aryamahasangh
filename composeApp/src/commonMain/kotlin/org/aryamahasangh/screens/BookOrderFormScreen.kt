package org.aryamahasangh.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.qr_code
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import org.aryamahasangh.components.PhotoItem
import org.aryamahasangh.type.BookOrderInput
import org.aryamahasangh.viewmodel.BookOrderViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BookOrderFormScreen(viewModel: BookOrderViewModel) {
  val scrollState = rememberScrollState()
  val createBookOrderState by viewModel.createBookOrderState.collectAsState()

  Column(Modifier.padding(12.dp).verticalScroll(scrollState),) {
    AsyncImage(
      model = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//ved_sanhita_order_form.webp",
      contentDescription = "ved sanhita order form",
      contentScale = ContentScale.Inside,
    )
    Text(
      text = "वेद संहिता - Order form",
      style = MaterialTheme.typography.headlineSmall,
      modifier = Modifier.padding(vertical = 8.dp)
    )
    Text(
      text = "सभी आर्य /आर्याओं नमस्ते, आप सब को सादर सूचित किया जाता है की आर्य महासंघ के तत्वाधान में आर्ष ग्रन्थ प्रकाशन विभाग द्वारा चारों वेदों (ऋग्वेद , यजुर्वेद , सामवेद , अथर्ववेद ) की मूल संहिता प्रकाशित की गयी है। इसका उद्देश्य ईश्वर प्रदत्त संविधान अर्थात् वेद ज्ञान की पुस्तक प्रत्येक आर्य परिवार में पहुँचाना है। जिसका बिक्री मूल्य ००० rs.  है। जो भी आर्य / आर्या वेद संहिता को प्राप्त करना चाहते हैं उनसे निवेदन है कि इस फॉर्म  को भरकर सबमिट करे और निचे दिए हुए बैंक अकाउंट नंबर/UPI ID में भुगतान करके स्क्रीनशॉट साथ में जोड़ें। धन्यवाद।"
    )
    // form
    OrderForm(viewModel)

    // Show success message if order was created successfully
    createBookOrderState?.let { state ->
      if (state.isLoading) {
        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
      } else if (state.error != null) {
        Text(
          text = "Error: ${state.error}",
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(16.dp)
        )
      } else if (state.createdBookOrder != null) {
        Text(
          text = "Order submitted successfully!",
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(16.dp)
        )
      }
    }
  }
}

@Composable
@Preview
fun OrderFormScreenPreview() {
  // In preview, we don't need a real viewModel
  // This is just a placeholder for the preview
  Text("Preview not available - requires viewModel")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderForm(viewModel: BookOrderViewModel) {
  // --- State for Form Fields ---
  var yourName by remember { mutableStateOf("") }
  var addressLine by remember { mutableStateOf("") } // Village/Colony/Sector/Apartment
  var city by remember { mutableStateOf("") }

  var address by remember { mutableStateOf("") }
  var addressError by remember { mutableStateOf(false) }

  var state by remember { mutableStateOf("") }

  var district by remember { mutableStateOf("") }

  var stateErrorMessage by remember { mutableStateOf("") }
  var districtErrorMessage by remember { mutableStateOf("") }

  var mobileNumber by remember { mutableStateOf("") }
  var pinCode by remember { mutableStateOf("") }
  var country by remember { mutableStateOf("India") } // Default or could be OutlinedTextField
  var districtOfficerName by remember { mutableStateOf("") }
  var districtOfficerNumber by remember { mutableStateOf("") }
  var paymentReceiptUrl by remember { mutableStateOf("") } // URL for the payment receipt

  // --- State for Error Handling ---
  var yourNameError by remember { mutableStateOf<String?>(null) }
  var addressLineError by remember { mutableStateOf<String?>(null) }
  var cityError by remember { mutableStateOf<String?>(null) }
  var districtError by remember { mutableStateOf<String?>(null) }
  var stateError by remember { mutableStateOf<String?>(null) }
  var mobileNumberError by remember { mutableStateOf<String?>(null) }
  var pinCodeError by remember { mutableStateOf<String?>(null) }
  var countryError by remember { mutableStateOf<String?>(null) }
  var districtOfficerNameError by remember { mutableStateOf<String?>(null) }
  var districtOfficerNumberError by remember { mutableStateOf<String?>(null) }
  // No error for payment receipt as it's not marked mandatory

  var showSubmissionMessage by remember { mutableStateOf(false) }
  var submissionStatus by remember { mutableStateOf("") }

  var studentPhoto by remember { mutableStateOf<PlatformFile?>(null) }
  var studentPhotoError by remember { mutableStateOf(false) }
  var studentPhotoErrorMessage by remember { mutableStateOf("") }

  // Collect the createBookOrderState to detect successful submissions
  val createBookOrderState by viewModel.createBookOrderState.collectAsState()

  // Reset form fields when order is successfully created
  LaunchedEffect(createBookOrderState?.createdBookOrder) {
    if (createBookOrderState?.createdBookOrder != null) {
      // Reset all form fields
      yourName = ""
      addressLine = ""
      city = ""
      address = ""
      addressError = false
      state = ""
      district = ""
      stateErrorMessage = ""
      districtErrorMessage = ""
      mobileNumber = ""
      pinCode = ""
      country = "India"
      districtOfficerName = ""
      districtOfficerNumber = ""
      paymentReceiptUrl = ""

      // Reset error states
      yourNameError = null
      addressLineError = null
      cityError = null
      districtError = null
      stateError = null
      mobileNumberError = null
      pinCodeError = null
      countryError = null
      districtOfficerNameError = null
      districtOfficerNumberError = null

      // Reset photo
      studentPhoto = null
      studentPhotoError = false
      studentPhotoErrorMessage = ""

      // Keep submission message visible
      showSubmissionMessage = true
      submissionStatus = "फॉर्म सफलतापूर्वक जमा किया गया!"
    }
  }

  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    CustomOutlinedTextField(
      value = yourName,
      onValueChange = { yourName = it; yourNameError = null },
      devnagariLabel = "आपका नाम*",
      englishPlaceholder = "Your Name",
      isError = yourNameError != null,
      errorMessage = yourNameError,
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Next
      )
    )

    CustomOutlinedTextField(
      value = addressLine,
      onValueChange = { addressLine = it; addressLineError = null },
      devnagariLabel = "गांव/कॉलोनी/सेक्टर/अपार्टमेंट*",
      englishPlaceholder = "Village/Colony/Sector/Apartment",
      isError = addressLineError != null,
      errorMessage = addressLineError,
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Next
      ),
      minLines = 2
    )



    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CustomOutlinedTextField(
        value = city,
        onValueChange = { city = it; cityError = null },
        devnagariLabel = "शहर*",
        englishPlaceholder = "City",
        isError = cityError != null,
        errorMessage = cityError,
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Words,
          imeAction = ImeAction.Next
        ),
        modifier = Modifier.width(250.dp)
      )
      StateDropdown(
        states = indianStatesToDistricts.keys.toList(),
        selectedState = state,
        onStateSelected = {
          state = it
          stateError = null
        },
        modifier = Modifier.width(160.dp),
        isError = stateError != null,
        errorMessage = if (stateError != null) stateErrorMessage else ""
      )
      // District Selection (Conditional)
      val districts = indianStatesToDistricts[state] ?: emptyList()
      DistrictDropdown(
        districts = districts,
        selectedDistrict = district,
        onDistrictSelected = {
          district = it ?: ""
          districtError = null
        },
        modifier = Modifier.width(200.dp),
        isError = districtError != null,
        errorMessage = if (districtError != null) districtErrorMessage else ""
      )
    }
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CustomOutlinedTextField(
        value = pinCode,
        onValueChange = {
          val newText = it.filter { char -> char.isDigit() }
          if (newText.length <= 6) {
            pinCode = newText
          }
          pinCodeError = null
        },
        devnagariLabel = "पिन कोड*",
        englishPlaceholder = "Pin Code",
        isError = pinCodeError != null,
        errorMessage = pinCodeError,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Number, // Use Number for PINs
          imeAction = ImeAction.Next
        ),
        modifier = Modifier.width(160.dp)
      )

      CustomOutlinedTextField(
        value = country,
        onValueChange = { country = it; countryError = null },
        devnagariLabel = "देश*",
        englishPlaceholder = "Country",
        isError = countryError != null,
        errorMessage = countryError,
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Words,
          imeAction = ImeAction.Next
        ),
        modifier = Modifier.width(250.dp)
      )

      CustomOutlinedTextField(
        value = mobileNumber,
        onValueChange = {
          // Allow only digits and limit length if needed (e.g., 10 for India)
          val newText = it.filter { char -> char.isDigit() }
          if (newText.length <= 10) { // Example length limit
            mobileNumber = newText
          }
          mobileNumberError = null
        },
        devnagariLabel = "आपका मोबाइल नंबर*",
        englishPlaceholder = "Your Mobile No.",
        isError = mobileNumberError != null,
        errorMessage = mobileNumberError,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Phone,
          imeAction = ImeAction.Next
        ),
        modifier = Modifier.width(200.dp)
      )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("अधिकारी विवरण", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth()) // Officer Details
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      CustomOutlinedTextField(
        value = districtOfficerName,
        onValueChange = { districtOfficerName = it; districtOfficerNameError = null },
        devnagariLabel = "जनपद अधिकारी नाम*",
        englishPlaceholder = "Name of District Officer",
        isError = districtOfficerNameError != null,
        errorMessage = districtOfficerNameError,
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Words,
          imeAction = ImeAction.Next
        ),
        modifier = Modifier.width(300.dp)
      )

      CustomOutlinedTextField(
        value = districtOfficerNumber,
        onValueChange = {
          val newText = it.filter { char -> char.isDigit() }
          if (newText.length <= 10) { // Example length limit
            districtOfficerNumber = newText
          }
          districtOfficerNumberError = null
        },
        devnagariLabel = "जनपद अधिकारी का नंबर*",
        englishPlaceholder = "District Officer's Number",
        isError = districtOfficerNumberError != null,
        errorMessage = districtOfficerNumberError,
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Phone,
          imeAction = ImeAction.Done
        ),
        modifier = Modifier.width(300.dp)
      )
    }

    ResponsivePaymentDetails()

    Column{
      Text(
        modifier = Modifier.padding(vertical = 8.dp),
        text = "भुगतान रसीद अपलोड करें:",
        style = MaterialTheme.typography.labelLarge
      )
      ReceiptUploadSection(
        studentPhoto = studentPhoto,
        onPhotoSelected = { file ->
          studentPhoto = file
          // In a real app, you would upload the file to a server and get a URL back
          // For now, we'll just use a placeholder URL
          paymentReceiptUrl = file?.path ?: ""
          studentPhotoError = false
          studentPhotoErrorMessage = ""
        },
        onPhotoRemoved = { photo ->
          studentPhoto = null
          paymentReceiptUrl = ""
        },
        isError = studentPhotoError,
        errorMessage = studentPhotoErrorMessage
      )
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Spacer(Modifier.height(16.dp))
    Button(
      onClick = {
        // Reset errors
        yourNameError = null; addressLineError = null; cityError = null;
        districtError = null; stateError = null; mobileNumberError = null; pinCodeError = null;
        countryError = null; districtOfficerNameError = null; districtOfficerNumberError = null;
        showSubmissionMessage = false

        var isValid = true
        if (yourName.isBlank()) { yourNameError = "कृपया आपका नाम दर्ज करें।"; isValid = false }
        if (addressLine.isBlank()) { addressLineError = "कृपया पता दर्ज करें।"; isValid = false }
        if (city.isBlank()) { cityError = "कृपया शहर दर्ज करें।"; isValid = false }

        if (state == "") { stateError = "कृपया राज्य चुनें।"; isValid = false }
        if (district == "") { districtError = "कृपया जनपद चुनें।"; isValid = false }


        if (mobileNumber.isBlank()) { mobileNumberError = "कृपया मोबाइल नंबर दर्ज करें।"; isValid = false }
        else if (mobileNumber.length < 10) { mobileNumberError = "मोबाइल नंबर कम से कम 10 अंकों का होना चाहिए।"; isValid = false } // Basic validation

        if (pinCode.isBlank()) { pinCodeError = "कृपया पिन कोड दर्ज करें।"; isValid = false }
        else if (pinCode.length != 6) { pinCodeError = "पिन कोड 6 अंकों का होना चाहिए।"; isValid = false }

        if (country.isBlank()) { countryError = "कृपया देश दर्ज करें।"; isValid = false }
        if (districtOfficerName.isBlank()) { districtOfficerNameError = "कृपया जनपद अधिकारी का नाम दर्ज करें।"; isValid = false }

        if (districtOfficerNumber.isBlank()) { districtOfficerNumberError = "कृपया जनपद अधिकारी का नंबर दर्ज करें।"; isValid = false }
        else if (districtOfficerNumber.length < 10) { districtOfficerNumberError = "अधिकारी का नंबर कम से कम 10 अंकों का होना चाहिए।"; isValid = false }


        if (isValid) {
          submissionStatus = "फॉर्म सफलतापूर्वक जमा किया गया!"
          showSubmissionMessage = true
          println("--- Form Data ---")
          println("Your Name (आपका नाम): $yourName")
          println("Address (गांव/कॉलोनी...): $addressLine")
          println("City (शहर): $city")
          println("District (ज़िला): $district")
          println("State (राज्य): $state")
          println("Mobile No. (आपका मोबाइल नंबर): $mobileNumber")
          println("Pin Code (पिन कोड): $pinCode")
          println("Country (देश): $country")
          println("District Officer Name (जनपद अधिकारी नाम): $districtOfficerName")
          println("District Officer's Number (जनपद अधिकारी का नंबर): $districtOfficerNumber")
          println("Payment Receipt: ${paymentReceiptUrl.ifEmpty { "Not uploaded" }}")

          // Create BookOrderInput and submit to the repository
          val bookOrderInput = BookOrderInput(
            fullname = yourName,
            address = addressLine,
            city = city,
            district = district,
            state = state,
            mobile = mobileNumber,
            pincode = pinCode,
            country = country,
            districtOfficerName = districtOfficerName,
            districtOfficerNumber = districtOfficerNumber,
            paymentReceiptUrl = paymentReceiptUrl.ifEmpty { "Not provided" }
          )

          // Submit the order
          viewModel.createBookOrder(bookOrderInput)
        } else {
          submissionStatus = "कृपया ऊपर दी गई त्रुटियों को सुधारें।"
          showSubmissionMessage = true
        }
      },
      modifier = Modifier.padding(vertical = 16.dp)
    ) {
      Text("Submit Order") // Submit
    }

    if (showSubmissionMessage) {
      Text(
        submissionStatus,
        color = if (submissionStatus.contains("सफलतापूर्वक")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp)
      )
    }

    Spacer(Modifier.height(32.dp)) // Extra space at the bottom
  }
}

@Composable
fun ReceiptUploadSection(
  studentPhoto: PlatformFile?,
  onPhotoSelected: (PlatformFile?) -> Unit,
  onPhotoRemoved: (PlatformFile) -> Unit,
  isError: Boolean,
  errorMessage: String
) {
  Column(horizontalAlignment = Alignment.Start) {
    val launcher = rememberFilePickerLauncher(
      type = PickerType.Image,
      mode = PickerMode.Single,
      title = "रसीद जोड़ें",
    ) { file ->
      onPhotoSelected(file)
    }
    Box(modifier = Modifier.size(120.dp)) {
      if(studentPhoto != null){
        PhotoItem(studentPhoto, onRemoveFile = onPhotoRemoved)
      }else{
        Icon(
          imageVector = Icons.Filled.PhotoLibrary,
          contentDescription = "Selected",
          modifier = Modifier.size(96.dp).padding(16.dp),
          tint = MaterialTheme.colorScheme.outlineVariant
        )
      }
    }

    OutlinedButton(onClick = {
      launcher.launch()
    }) {
      Text("रसीद जोड़ें")
    }
    if (isError) {
      Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
    }
  }
}

@Composable
@Preview
fun ResponsivePaymentDetails() {
  // This is the top-level text, as seen in your image
  Text(
    text = "नीचे दिए गए माध्यम से आप भुगतान कर सकते है।", // "You can pay through the means given below."
    fontSize = 16.sp,
    modifier = Modifier.padding(top = 16.dp)
  )

  // The main component that handles responsiveness
  BoxWithConstraints(
    modifier = Modifier.padding(16.dp),
  ) {
    val isWideScreen = maxWidth > 600.dp // Define your breakpoint

    if (isWideScreen) {
      // Horizontal layout for wider screens
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        QrCodeSection(modifier = Modifier.padding(end = 16.dp))
        OrDivider(isHorizontalLayout = true, modifier = Modifier.padding(horizontal = 24.dp)) // Vertical divider
        BankDetailsSection(modifier = Modifier.padding(start = 16.dp))
      }
    } else {
      // Vertical layout for narrower screens
      Column(
      ) {
        QrCodeSection(modifier = Modifier.padding(bottom = 16.dp))
        OrDivider(isHorizontalLayout = false) // Horizontal divider
        BankDetailsSection(modifier = Modifier.padding(top = 16.dp))
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
      modifier = Modifier
        .size(200.dp) // Adjust size as needed
        .background(Color.LightGray), // Placeholder background
      contentAlignment = Alignment.Center
    ) {
       Image(
           painter = painterResource(Res.drawable.qr_code), // Replace with actual resource
           contentDescription = "QR Code for UPI Payment",
           modifier = Modifier.fillMaxSize(),
           contentScale = ContentScale.Fit
       )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "upi: test1234@upi",
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
    BankDetailItem("Branch:", "Sector-24 Rohini, Delhi")
    BankDetailItem("Name in Bank:", "AARSH GRANTH PRAKASHAN NIDHI")
    BankDetailItem("Account number:", "6582000100016320")
    BankDetailItem("IFSC code:", "PUNB0658200")
  }
}

@Composable
fun BankDetailItem(label: String, value: String) {
  Row {
    Text(text = label, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(150.dp)) // Adjust width for alignment
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
    modifier = modifier.then( // Apply incoming modifier first
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
          modifier = Modifier
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
          modifier = Modifier
            .weight(1f) // Bottom segment takes available space below text
            .width(1.dp)
        )
      }
    } else {
      // HORIZONTAL DIVIDER: A single Divider with Text on top (using background to "erase")
      // The Box's contentAlignment = Alignment.Center will center both Divider and Text.
      HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        modifier = Modifier
          .fillMaxWidth() // Divider fills the width of its parent Box
          .height(1.dp)
      )
      Text(
        text = dividerText,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surface) // Erase line behind text
          .padding(horizontal = textHorizontalPadding, vertical = textVerticalPadding)
      )
    }
  }
}

@Composable
fun CustomOutlinedTextField(
  value: String,
  onValueChange: (String) -> Unit,
  devnagariLabel: String,
  englishPlaceholder: String,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  errorMessage: String? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  singleLine: Boolean = false,
  minLines: Int = 1,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(devnagariLabel) },
    placeholder = { Text(englishPlaceholder) },
    modifier = modifier.width(500.dp),
    isError = isError,
    supportingText = { if (isError && errorMessage != null) Text(errorMessage) },
    keyboardOptions = keyboardOptions,
    singleLine = singleLine,
    minLines = minLines,
    maxLines = maxLines
  )
}
