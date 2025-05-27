package org.aryamahasangh.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.error_profile_image
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.components.PhotoItem
import org.aryamahasangh.network.bucket
import org.aryamahasangh.utils.epochToDate
import org.aryamahasangh.viewmodel.AdmissionsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class FormData(
  val studentName: String = "",
  val aadharNo: String = "",
  val dob: String = "",
  val bloodGroup: String = "",
  val previousClass: String = "",
  val marksObtained: String = "",
  val schoolName: String = "",
  val fatherName: String = "",
  val fatherOccupation: String = "",
  val fatherQualification: String = "",
  val motherName: String = "",
  val motherOccupation: String = "",
  val motherQualification: String = "",
  val fullAddress: String = "",
  val mobileNo: String = "",
  val alternateMobileNo: String = "",
  val attachedDocuments: List<String> = emptyList(),
  val studentPhoto: String? = null,
  val studentSignature: String? = null,
  val parentSignature: String? = null
)

fun isValidAadhar(aadhar: String): Boolean {
  return aadhar.length == 12 && aadhar.all { it.isDigit() }
}

fun isValidMobileNumber(number: String): Boolean {
  return number.length == 10 && number.all { it.isDigit() }
}

fun isValidDate(date: String): Boolean {
  if (date.isBlank()) return false
  return try {
    val dateParts = date.split("/")
    if (dateParts.size != 3) return false

    val day = dateParts[0].toInt()
    val month = dateParts[1].toInt()
    val year = dateParts[2].toInt()

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val inputDate = LocalDate(year, month, day)
    inputDate <= now

  } catch (e: Exception) {
    false
  }
}


@Composable
fun AadharVisualTransformation(): VisualTransformation {
  return VisualTransformation { text ->
    val trimmed = text.text.replace(" ", "")
    var formatted = ""
    for (i in trimmed.indices) {
      formatted += trimmed[i]
      if ((i + 1) % 4 == 0 && i < trimmed.length - 1) {
        formatted += " "
      }
    }
    TransformedText(
      AnnotatedString(formatted),
      offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
          if (offset <= 4) return offset
          if (offset <= 8) return offset + 1
          if (offset <= 12) return offset + 2
          return 14
        }

        override fun transformedToOriginal(offset: Int): Int {
          if (offset <= 4) return offset
          if (offset <= 9) return offset - 1
          if (offset <= 14) return offset - 2
          return 12
        }
      })
  }
}

//class DateTransformation : VisualTransformation {
//  override fun filter(text: AnnotatedString): TransformedText {
//    println("filter text: $text, ${text.text}")
//    val formattedText = formatDate(text.text)
//    val offsetMapping = DateOffsetMapping(text.text, formattedText)
//
//    return TransformedText(
//      text = AnnotatedString(formattedText),
//      offsetMapping = offsetMapping
//    )
//  }
//
//  private fun formatDate(input: String): String {
//    val builder = StringBuilder()
//    var i = 0
//
//    if(input.length in 6..10 && input.count { it == '/' } == 2) return input
//
//    // Rule 1: When user enters 1 character and appends '/', prepend '0'
//    if (input.length == 2 && input.endsWith("/")) {
//      builder.append("0").append(input[0]).append("/")
//      i = 2
//    }
//    // Rule 2: When user enters two digits, append '/' automatically
//    else if (input.length == 2 && !input.endsWith("/")) {
//      builder.append(input).append("/")
//      i = 3
//    }
//    else if (input.length == 3) {
//      builder.append(input.substring(0, 2)).append("/").append(input.substring(2))
//      i = 3
//    }
//    else if (input.length == 4) {
//      builder.append(input.substring(0, 2)).append("/").append(input.substring(2)).append("/")
//      i = 3
//    }
//    else if (input.length >= 5) {
//      val first = input.substring(0, 2)
//      val second = input.substring(2, 4)
//      val third = input.substring(4)
//      builder.append(first).append("/").append(second).append("/").append(third)
//      i = 3
//    }
//    // Default case: Append the remaining characters
//    else {
//      builder.append(input)
//    }
//
//    return builder.toString()
//  }
//
//  private class DateOffsetMapping(
//    private val originalText: String,
//    private val formattedText: String
//  ) : OffsetMapping {
//    override fun originalToTransformed(offset: Int): Int {
//      println("originalToTransformed originalText: $originalText, formattedText: $formattedText offset: $offset")
//      // Calculate the transformed offset based on the original text
//      val toTransformOff =  when {
//        offset < 2 -> offset // First part (day)
//        offset == 2 -> offset + 1
//        offset == 3 -> offset + 1
//        offset == 4 -> offset + 2
//        offset == 5 -> offset + 2
//        offset in 6..8 -> offset + 2
//        offset in 9..10 -> offset
//        else -> offset + 2 // Third part (year)
//      }
//      println("toTransformOff: $toTransformOff")
//      return toTransformOff
//    }
//
//    override fun transformedToOriginal(offset: Int): Int {
//      println("transformedToOriginal originalText: $originalText, formattedText: $formattedText offset: $offset")
//      // Calculate the original offset based on the formatted text
//      val toOriginalOff =  when {
//        offset <= 2 -> offset // First part (day)
//        offset <= 5 -> offset - 1 // Second part (month)
//        else -> offset - 2 // Third part (year)
//      }
//      println("transformedToOriginal return $toOriginalOff")
//      return toOriginalOff
//    }
//  }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  isError: Boolean,
  errorMessage: String,
) {
  val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)
  val showDatePickerDialog = remember { mutableStateOf(false) }

//  var focusRequester by remember { FocusRequester() }

  val focusRequester = remember { FocusRequester() }


  OutlinedTextField(
    modifier = modifier
      .focusRequester(focusRequester),
    value = value,
    label = { Text("जन्म तिथि") },
    onValueChange = { onValueChange(it.take(10)) },
//    visualTransformation = DateTransformation(),
    trailingIcon = {
      IconButton(onClick = { showDatePickerDialog.value = true }) {
        Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
      }
    },
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
    placeholder = { Text("dd/mm/yyyy") },
    singleLine = true,
    maxLines = 1,
    isError = isError,
    supportingText = { if (isError) { Text(errorMessage) } }
  )

  if (showDatePickerDialog.value) {
    val confirmEnabled = remember { mutableStateOf(false) }
    DatePickerDialog(
      onDismissRequest = { showDatePickerDialog.value = false },
      confirmButton = {
        TextButton(
          onClick = {
            showDatePickerDialog.value = false
            datePickerState.selectedDateMillis?.let {
              val date = epochToDate(it)
              onValueChange(date)
            }
          },
          enabled = confirmEnabled.value
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePickerDialog.value = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(
        state = datePickerState
      )
      confirmEnabled.value = datePickerState.selectedDateMillis != null
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodGroupDropdown(
  selectedBloodGroup: String?,
  onBloodGroupSelected: (String) -> Unit,
  modifier: Modifier,
  isError: Boolean,
  errorMessage: String,
) {
  val bloodGroups = listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")
  var expanded by remember { mutableStateOf(false) }
  var textFieldSize by remember { mutableStateOf(Size.Zero) }
  Column(modifier = modifier) {
    ExposedDropdownMenuBox(
      expanded = false,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        value = selectedBloodGroup ?: "चुनें",
        onValueChange = { /* Do nothing, read-only */ },
        readOnly = true,
        singleLine = true,
        placeholder = { Text("चुनें") },
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
              textFieldSize = coordinates.size.toSize()
            }.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        label = { Text("रक्त वर्ग") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        isError = isError,
        supportingText = { if (isError) { Text(errorMessage) } }
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        bloodGroups.forEach { bloodGroup ->
          key(bloodGroup) {
            DropdownMenuItem(
              text = { Text(text = bloodGroup) },
              onClick = {
                onBloodGroupSelected(bloodGroup)
                expanded = false
              }
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentGrid(
  documents: List<PlatformFile>,
  onDocumentRemoved: (PlatformFile) -> Unit,
  isError: Boolean,
  errorMessage: String
) {
  Column(horizontalAlignment = Alignment.Start) {
    if (documents.isEmpty()) {
      Icon(
        imageVector = Icons.Filled.PhotoLibrary,
        contentDescription = "Selected",
        modifier = Modifier.size(96.dp).padding(16.dp),
        tint = MaterialTheme.colorScheme.outlineVariant
      )
//      Text("No documents attached.",
//        modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant),
//        textAlign = TextAlign.Start)
    }
    FlowRow(
      verticalArrangement =  Arrangement.spacedBy(8.dp),
      horizontalArrangement =  Arrangement.spacedBy(8.dp)
    ) {
      for (document in documents) {
        val docName = document.name
        Box(modifier = Modifier.width(120.dp).padding(8.dp)) {
          Column {
            PhotoItem(document, onRemoveFile = onDocumentRemoved)
            Text(text = docName,
              maxLines = 2,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(4.dp)
            )
          }
        }
      }
    }
    if (isError) {
      Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
    }
  }
}

@Composable
fun ButtonForFilePicker(label: String?, onFilesSelected: (List<PlatformFile>?) -> Unit) {
  val launcher = rememberFilePickerLauncher(
    type = PickerType.File(extensions = listOf("png", "jpg", "jpeg", "webp", "pdf", "docx")),
    mode = PickerMode.Multiple(),
    title = "Select documents",
  ) { files ->
    println(files)
    onFilesSelected(files)
  }
  OutlinedButton(onClick = {
    launcher.launch()
  }) {
    Text(label ?: "Select Documents")
  }
}

@Composable
fun StudentPhotoSection(
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
      title = "Select photo",
    ) { file ->
      onPhotoSelected(file)
    }
    Box(modifier = Modifier.size(120.dp)) {
      if(studentPhoto != null){
        PhotoItem(studentPhoto, onRemoveFile = onPhotoRemoved)
      }else{
        Image(
          painter = painterResource(resource = Res.drawable.error_profile_image),
          contentDescription = "Error profile image",
          contentScale = ContentScale.Crop,
        )
      }
    }

    OutlinedButton(onClick = {
      launcher.launch()
    }) {
      Text("Select Photo")
    }
    if (isError) {
      Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
    }
  }
}


@Composable
fun SignatureSection(
  signatureFile: PlatformFile?,
  onSignatureSelected: (PlatformFile?) -> Unit,
  label: String,
  onRemoveSignature: (PlatformFile) -> Unit,
  isError: Boolean,
  errorMessage: String
) {
  Column() {
    Text(
      modifier = Modifier.padding(vertical = 8.dp),
      text = "$label के हस्ताक्षर:",
      style = MaterialTheme.typography.labelLarge
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
      if (signatureFile != null) {
        Box(
          modifier = Modifier
            .size(150.dp)){
          PhotoItem(signatureFile, onRemoveFile = onRemoveSignature)
        }
      } else {
        Box(
          modifier = Modifier
            .size(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "No signature added",
            modifier = Modifier.fillMaxWidth(1f).padding(8.dp),
            textAlign = TextAlign.Center
          )
        }
      }
      Column() {
        val launcher = rememberFilePickerLauncher(
          type = PickerType.Image,
          mode = PickerMode.Single,
          title = "Select photo",
        ) { file ->
          onSignatureSelected(file)
        }
        OutlinedButton(onClick = {
          launcher.launch()
        }) {
          Text("Upload Image")
        }

        OutlinedButton(onClick = {
          //onSignatureSelected("path")
        }) {
          Text("Draw Signature")
        }
      }
    }
    if (isError) {
      Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalUuidApi::class)
@Composable
fun RegistrationForm(viewModel: AdmissionsViewModel) {
//
//  val scope = rememberCoroutineScope()
//  val snackbarHostState = LocalSnackbarHostState.current
//
//  var FormData by remember { mutableStateOf(FormData()) }
//
//  // Collect form submission state from ViewModel
//  val formSubmissionState by viewModel.admissionFormSubmissionState.collectAsState()
//  var isSubmittingData = formSubmissionState.isSubmitting
//  var formSubmittingProgress by remember { mutableStateOf("")}
//
//  // State for each field and its error status
//  var studentName by remember { mutableStateOf("") }
//  var studentNameError by remember { mutableStateOf(false) }
//  var studentNameErrorMessage by remember { mutableStateOf("") }
//
//  var aadharNo by remember { mutableStateOf("") }
//  var aadharNoError by remember { mutableStateOf(false) }
//  var aadharNoErrorMessage by remember { mutableStateOf("") }
//
//  var dob by remember { mutableStateOf("") }
//  var dobError by remember { mutableStateOf(false) }
//  var dobErrorMessage by remember { mutableStateOf("") }
//
//  var selectedBloodGroup by remember { mutableStateOf("") }
//  var bloodGroupError by remember { mutableStateOf(false) }
//  var bloodGroupErrorMessage by remember { mutableStateOf("") }
//
//  var previousClass by remember { mutableStateOf("") }
//  var previousClassError by remember { mutableStateOf(false) }
//  var previousClassErrorMessage by remember { mutableStateOf("") }
//
//  var marksObtained by remember { mutableStateOf("") }
//  var marksObtainedError by remember { mutableStateOf(false) }
//  var marksObtainedErrorMessage by remember { mutableStateOf("") }
//
//  var schoolName by remember { mutableStateOf("") }
//  var schoolNameError by remember { mutableStateOf(false) }
//  var schoolNameErrorMessage by remember { mutableStateOf("") }
//
//  var fatherName by remember { mutableStateOf("") }
//  var fatherNameError by remember { mutableStateOf(false) }
//  var fatherNameErrorMessage by remember { mutableStateOf("") }
//
//  var fatherOccupation by remember { mutableStateOf("") }
//  var fatherOccupationError by remember { mutableStateOf(false) }
//  var fatherOccupationErrorMessage by remember { mutableStateOf("") }
//
//  var fatherQualification by remember { mutableStateOf("") }
//  var fatherQualificationError by remember { mutableStateOf(false) }
//  var fatherQualificationErrorMessage by remember { mutableStateOf("") }
//
//  var motherName by remember { mutableStateOf("") }
//  var motherNameError by remember { mutableStateOf(false) }
//  var motherNameErrorMessage by remember { mutableStateOf("") }
//
//  var motherOccupation by remember { mutableStateOf("") }
//  var motherOccupationError by remember { mutableStateOf(false) }
//  var motherOccupationErrorMessage by remember { mutableStateOf("") }
//
//  var motherQualification by remember { mutableStateOf("") }
//  var motherQualificationError by remember { mutableStateOf(false) }
//  var motherQualificationErrorMessage by remember { mutableStateOf("") }
//
//  var fullAddress by remember { mutableStateOf("") }
//  var fullAddressError by remember { mutableStateOf(false) }
//  var fullAddressErrorMessage by remember { mutableStateOf("") }
//
//  var mobileNo by remember { mutableStateOf("") }
//  var mobileNoError by remember { mutableStateOf(false) }
//  var mobileNoErrorMessage by remember { mutableStateOf("") }
//
//  var alternateMobileNo by remember { mutableStateOf("") }
//  var alternateMobileNoError by remember { mutableStateOf(false) }
//  var alternateMobileNoErrorMessage by remember { mutableStateOf("") }
//
//
//  var attachedDocuments by remember { mutableStateOf(emptyList<PlatformFile>()) }
//  var attachedDocumentsError by remember { mutableStateOf(false) }
//  var attachedDocumentsErrorMessage by remember { mutableStateOf("") }
//
//  var studentPhoto by remember { mutableStateOf<PlatformFile?>(null) }
//  var studentPhotoError by remember { mutableStateOf(false) }
//  var studentPhotoErrorMessage by remember { mutableStateOf("") }
//
//  var studentSignature by remember { mutableStateOf<PlatformFile?>(null) }
//  var studentSignatureError by remember { mutableStateOf(false) }
//  var studentSignatureErrorMessage by remember { mutableStateOf("") }
//
//
//  var parentSignature by remember { mutableStateOf<PlatformFile?>(null) }
//  var parentSignatureError by remember { mutableStateOf(false) }
//  var parentSignatureErrorMessage by remember { mutableStateOf("") }
//
//  var isFormValid by remember { mutableStateOf(false) }
//
//  val scrollState = rememberScrollState()
//
//
//  //Validation function
//  fun validateForm(): Boolean {
//    var isValid = true
//
//    // Reset all errors first
//    studentNameError = studentName.isBlank()
//    aadharNoError = !isValidAadhar(aadharNo.replace(" ", ""))
//    dobError = !isValidDate(dob)
//    bloodGroupError = selectedBloodGroup.isBlank()
//    previousClassError = previousClass.isBlank()
//    marksObtainedError = marksObtained.isBlank()
//    schoolNameError = schoolName.isBlank()
//    fatherNameError = fatherName.isBlank()
//    fatherOccupationError = fatherOccupation.isBlank()
//    fatherQualificationError = fatherQualification.isBlank()
//    motherNameError = motherName.isBlank()
//    motherOccupationError = motherOccupation.isBlank()
//    motherQualificationError = motherQualification.isBlank()
//    fullAddressError = fullAddress.isBlank()
//    mobileNoError = !isValidMobileNumber(mobileNo)
//    alternateMobileNoError = !isValidMobileNumber(alternateMobileNo)
//    attachedDocumentsError = attachedDocuments.isEmpty()
//    studentPhotoError = studentPhoto == null
//    studentSignatureError = studentSignature == null
//    parentSignatureError = parentSignature == null
//
//    // Set error messages
//    studentNameErrorMessage = if (studentNameError) "Student name is required" else ""
//    aadharNoErrorMessage = if (aadharNoError) "Invalid Aadhar number" else ""
//    dobErrorMessage = if (dobError) "Invalid date" else ""
//    bloodGroupErrorMessage = if (bloodGroupError) "Please select a blood group" else ""
//    previousClassErrorMessage = if (previousClassError) "Previous class is required" else ""
//    marksObtainedErrorMessage = if (marksObtainedError) "Marks obtained are required" else ""
//    schoolNameErrorMessage = if (schoolNameError) "School name is required" else ""
//    fatherNameErrorMessage = if (fatherNameError) "Father's name is required" else ""
//    fatherOccupationErrorMessage = if (fatherOccupationError) "Father's occupation is required" else ""
//    fatherQualificationErrorMessage = if (fatherQualificationError) "Father's qualification is required" else ""
//    motherNameErrorMessage = if (motherNameError) "Mother's name is required" else ""
//    motherOccupationErrorMessage = if (motherOccupationError) "Mother's occupation is required" else ""
//    motherQualificationErrorMessage = if (motherQualificationError) "Mother's qualification is required" else ""
//    fullAddressErrorMessage = if (fullAddressError) "Full address is required" else ""
//    mobileNoErrorMessage = if (mobileNoError) "Invalid mobile number" else ""
//    alternateMobileNoErrorMessage = if (alternateMobileNoError) "Invalid alternate mobile number" else ""
//    attachedDocumentsErrorMessage = if (attachedDocumentsError) "At least one document must be attached" else ""
//    studentPhotoErrorMessage = if (studentPhotoError) "Student photo is required" else ""
//    studentSignatureErrorMessage = if (studentSignatureError) "Student signature is required" else ""
//    parentSignatureErrorMessage = if (parentSignatureError) "Parent signature is required" else ""
//
//    if (studentNameError || aadharNoError || dobError || bloodGroupError || previousClassError ||
//      marksObtainedError || schoolNameError || fatherNameError || fatherOccupationError ||
//      fatherQualificationError || motherNameError || motherOccupationError || motherQualificationError ||
//      fullAddressError || mobileNoError || alternateMobileNoError || attachedDocumentsError ||
//      studentPhotoError || studentSignatureError || parentSignatureError) {
//      isValid = false
//    }
//
//    return isValid
//  }
//
//  // Helper function to update form data and reset error
//  fun updateField(
//    value: String,
//    errorState: MutableState<Boolean>,
//    errorMessageState: MutableState<String>,
//    update: (String) -> Unit,
//    validation: (String) -> Boolean,
//    errorMessage: String
//  ) {
//    update(value)
//    if (validation(value)) {
//      errorState.value = false
//      errorMessageState.value = ""
//    } else {
//      errorState.value = true
//      errorMessageState.value = errorMessage
//    }
//  }
//
//  CompositionLocalProvider(
//  ) {
//    Column(
//      modifier = Modifier
//        .fillMaxSize()
//        .padding(16.dp)
//        .verticalScroll(scrollState),
//      verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//      Text("छात्रा प्रवेश प्रपत्र", style = MaterialTheme.typography.headlineMedium)
//
//      OutlinedTextField(
//        value = studentName,
//        onValueChange = { text ->
//          studentName = text.split(" ").joinToString(" ") { it.capitalize(Locale.current) }
//          studentNameError = studentName.isBlank()
//          FormData = FormData.copy(studentName = studentName)
//        },
//        label = { Text("छात्रा का नाम") },
//        modifier = Modifier.width(500.dp),
//        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//        maxLines = 1,
//        singleLine = true,
//        isError = studentNameError,
//        supportingText = { if(studentNameError) Text(studentNameErrorMessage) }
//      )
//
//      FlowRow(
//        horizontalArrangement = Arrangement.spacedBy(16.dp),
//      ) {
//        OutlinedTextField(
//          value = aadharNo,
//          onValueChange = { text ->
//            if (text.all { it.isDigit() } && text.length <= 12) {
//              aadharNo = text
//              aadharNoError = text.isBlank()
//              FormData = FormData.copy(aadharNo = aadharNo)
//            }
//          },
//          label = { Text("आधार संख्या") },
//          modifier = Modifier.width(150.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//          visualTransformation = AadharVisualTransformation(),
//          maxLines = 1,
//          singleLine = true,
//          isError = aadharNoError,
//          supportingText = { if(aadharNoError) Text(aadharNoErrorMessage) }
//        )
//
//        DatePickerTextField(
//          value = dob,
//          onValueChange = { text ->
//            dob = text
//            dobError = text.isBlank()
//            FormData = FormData.copy(dob = dob)
//          },
//          modifier = Modifier.width(180.dp),
//          isError = dobError,
//          errorMessage = dobErrorMessage
//        )
//
//        BloodGroupDropdown(
//          selectedBloodGroup = selectedBloodGroup,
//          onBloodGroupSelected = { group ->
//            selectedBloodGroup = group
//            bloodGroupError = false
//            FormData = FormData.copy(bloodGroup = selectedBloodGroup)
//          },
//          modifier = Modifier.width(150.dp),
//          isError = bloodGroupError,
//          errorMessage = bloodGroupErrorMessage
//        )
//      }
//
//      FlowRow(
//        horizontalArrangement = Arrangement.spacedBy(16.dp),
//      ) {
//        OutlinedTextField(
//          value = previousClass,
//          onValueChange = { text ->
//            if (text.length <= 20) {
//              previousClass = text
//              previousClassError = text.isBlank()
//              FormData = FormData.copy(previousClass = previousClass)
//            }
//          },
//          label = { Text("पिछली उत्तीर्ण कक्षा") },
//          modifier = Modifier.width(150.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = previousClassError,
//          supportingText = { if(previousClassError) Text(previousClassErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = marksObtained,
//          onValueChange = { text ->
//            if (text.length <= 10) {
//              marksObtained = text
//              marksObtainedError = text.isBlank()
//              FormData = FormData.copy(marksObtained = marksObtained)
//            }
//          },
//          label = { Text("प्राप्त अंक") },
//          modifier = Modifier.width(100.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = marksObtainedError,
//          supportingText = { if(marksObtainedError) Text(marksObtainedErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = schoolName,
//          onValueChange = { text ->
//            if (text.length <= 100) {
//              schoolName = text
//              schoolNameError = text.isBlank()
//              FormData = FormData.copy(schoolName = schoolName)
//            }
//          },
//          label = { Text("विद्यालय का नाम") },
//          modifier = Modifier.width(500.dp),
//          leadingIcon = { Icon(Icons.Filled.School, contentDescription = "Address") },
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = schoolNameError,
//          supportingText = { if(schoolNameError) Text(schoolNameErrorMessage) }
//        )
//      }
//      FlowRow(
//        horizontalArrangement = Arrangement.spacedBy(16.dp),
//      ) {
//        OutlinedTextField(
//          value = fatherName,
//          onValueChange = { text ->
//            if (text.length <= 100) {
//              fatherName = text
//              fatherNameError = text.isBlank()
//              FormData = FormData.copy(fatherName = fatherName)
//            }
//          },
//          label = { Text("पिता का नाम") },
//          modifier = Modifier.width(500.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = fatherNameError,
//          supportingText = { if(fatherNameError) Text(fatherNameErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = fatherOccupation,
//          onValueChange = { text ->
//            if (text.length <= 50) {
//              fatherOccupation = text
//              fatherOccupationError = text.isBlank()
//              FormData = FormData.copy(fatherOccupation = fatherOccupation)
//            }
//          },
//          label = { Text("व्यवसाय") },
//          modifier = Modifier.width(170.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = fatherOccupationError,
//          supportingText = { if(fatherOccupationError) Text(fatherOccupationErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = fatherQualification,
//          onValueChange = { text ->
//            if (text.length <= 100) {
//              fatherQualification = text
//              fatherQualificationError = text.isBlank()
//              FormData = FormData.copy(fatherQualification = fatherQualification)
//            }
//          },
//          label = { Text("योग्यता") },
//          modifier = Modifier.width(170.dp),
//          leadingIcon = { Icon(Icons.Filled.HistoryEdu, contentDescription = "Education history") },
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = fatherQualificationError,
//          supportingText = { if(fatherQualificationError) Text(fatherQualificationErrorMessage) }
//        )
//      }
//
//      FlowRow(
//        horizontalArrangement = Arrangement.spacedBy(16.dp),
//      ) {
//        OutlinedTextField(
//          value = motherName,
//          onValueChange = { text ->
//            if (text.length <= 100) {
//              motherName = text
//              motherNameError = text.isBlank()
//              FormData = FormData.copy(motherName = motherName)
//            }
//          },
//          label = { Text("माता का नाम") },
//          modifier = Modifier.width(500.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = motherNameError,
//          supportingText = { if(motherNameError) Text(motherNameErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = motherOccupation,
//          onValueChange = { text ->
//            if (text.length <= 50) {
//              motherOccupation = text
//              motherOccupationError = text.isBlank()
//              FormData = FormData.copy(motherOccupation = motherOccupation)
//            }
//          },
//          label = { Text("व्यवसाय") },
//          modifier = Modifier.width(170.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = motherOccupationError,
//          supportingText = { if(motherOccupationError) Text(motherOccupationErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = motherQualification,
//          onValueChange = { text ->
//            if (text.length <= 100) {
//              motherQualification = text
//              motherQualificationError = text.isBlank()
//              FormData = FormData.copy(motherQualification = motherQualification)
//            }
//          },
//          label = { Text("योग्यता") },
//          modifier = Modifier.width(170.dp),
//          leadingIcon = { Icon(Icons.Filled.HistoryEdu, contentDescription = "Education history") },
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          maxLines = 1,
//          singleLine = true,
//          isError = motherQualificationError,
//          supportingText = { if(motherQualificationError) Text(motherQualificationErrorMessage) }
//        )
//      }
//
//      FlowRow(
//        horizontalArrangement = Arrangement.spacedBy(16.dp),
//      ) {
//        OutlinedTextField(
//          value = fullAddress,
//          onValueChange = { text ->
//            if (text.length <= 300) {
//              fullAddress = text
//              fullAddressError = text.isBlank()
//              FormData = FormData.copy(fullAddress = fullAddress)
//            }
//          },
//          leadingIcon = { Icon(Icons.Filled.Place, contentDescription = "Address") },
//          label = { Text("घर का पता") },
//          modifier = Modifier.width(400.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
//          minLines = 2, // Use minLines instead of singleLine for a text area
//          maxLines = 4,
//          isError = fullAddressError,
//          supportingText = { if(fullAddressError) Text(fullAddressErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = mobileNo,
//          onValueChange = { text ->
//            if (text.all { it.isDigit() } && text.length <= 10) {
//              mobileNo = text
//              mobileNoError = text.isBlank()
//              FormData = FormData.copy(mobileNo = mobileNo)
//            }
//          },
//          label = { Text("दूरभाष संख्या") },
//          leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone") },
//          modifier = Modifier.width(220.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//          maxLines = 1,
//          singleLine = true,
//          isError = mobileNoError,
//          supportingText = { if(mobileNoError) Text(mobileNoErrorMessage) }
//        )
//
//        OutlinedTextField(
//          value = alternateMobileNo,
//          onValueChange = { text ->
//            if (text.all { it.isDigit() } && text.length <= 10) {
//              alternateMobileNo = text
//              alternateMobileNoError = text.isBlank()
//              FormData = FormData.copy(alternateMobileNo = alternateMobileNo)
//            }
//          },
//          leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone") },
//          label = { Text("वैकल्पिक दूरभाष संख्या(आपात्कालीन स्थिति के लिये)") },
//          modifier = Modifier.width(220.dp),
//          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//          maxLines = 1,
//          singleLine = true,
//          isError = alternateMobileNoError,
//          supportingText = { if(alternateMobileNoError) Text(alternateMobileNoErrorMessage) }
//        )
//      }
//
//
//      Text(
//        modifier = Modifier.padding(top = 8.dp),
//        text = "संलग्न प्रलेख(Documents):",
//        style = MaterialTheme.typography.labelLarge
//      )
//      DocumentGrid(
//        documents = attachedDocuments,
//        onDocumentRemoved = { documentToRemove ->
//          attachedDocuments = attachedDocuments.toMutableList().apply {
//            remove(documentToRemove)
//          }.toList()
//          FormData = FormData.copy(attachedDocuments = attachedDocuments.map { it.name })
//        },
//        isError = attachedDocumentsError,
//        errorMessage = attachedDocumentsErrorMessage
//      )
//
//      ButtonForFilePicker(null, onFilesSelected = { filePath ->
//        if (filePath != null) {
//          attachedDocumentsError = false
//          attachedDocuments = (attachedDocuments + filePath).distinct()
//          FormData = FormData.copy(attachedDocuments = attachedDocuments.map { it.name })
//        }
//      })
//
//      FlowRow(
//        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween
//      ) {
//        Column{
//          Text(
//            modifier = Modifier.padding(vertical = 8.dp),
//            text = "छात्रा का फोटो:",
//            style = MaterialTheme.typography.labelLarge
//          )
//          StudentPhotoSection(
//            studentPhoto = studentPhoto,
//            onPhotoSelected = { file ->
//              studentPhoto = file
//              studentPhotoError = file != null
//              FormData = FormData.copy(studentPhoto = studentPhoto?.name)
//            },
//            onPhotoRemoved = { photo ->
//              studentPhoto = null
//              FormData = FormData.copy(studentPhoto = null)
//            },
//            isError = studentPhotoError,
//            errorMessage = studentPhotoErrorMessage
//          )
//        }
//
//        SignatureSection(
//          signatureFile = studentSignature,
//          onSignatureSelected = { file ->
//            studentSignature = file
//            studentSignatureError = file != null
//            FormData = FormData.copy(studentSignature = studentSignature?.name)
//          },
//          label = "छात्रा",
//          onRemoveSignature = {
//            studentSignature = null
//          },
//          isError = studentSignatureError,
//          errorMessage = studentSignatureErrorMessage
//        )
//
//        SignatureSection(
//          signatureFile = parentSignature,
//          onSignatureSelected = { file ->
//            parentSignature = file
//            parentSignatureError = file != null
//            FormData = FormData.copy(parentSignature = parentSignature?.name)
//          },
//          label = "माता/पिता",
//          onRemoveSignature = {
//            parentSignature = null
//          },
//          isError = parentSignatureError,
//          errorMessage = parentSignatureErrorMessage
//        )
//      }
//
//
//      HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
//
//      Button(
//        onClick = {
//          if (validateForm()) {
//            isSubmittingData = true
//            FormData = FormData(
//              studentName = studentName,
//              aadharNo = aadharNo,
//              dob = dob,
//              bloodGroup = selectedBloodGroup,
//              previousClass = previousClass,
//              marksObtained = marksObtained,
//              schoolName = schoolName,
//              fatherName = fatherName,
//              fatherOccupation = fatherOccupation,
//              fatherQualification = fatherQualification,
//              motherName = motherName,
//              motherOccupation = motherOccupation,
//              motherQualification = motherQualification,
//              fullAddress = fullAddress,
//              mobileNo = mobileNo,
//              alternateMobileNo = alternateMobileNo,
//              attachedDocuments = attachedDocuments.map { it.name },
//              studentPhoto = studentPhoto?.name,
//              studentSignature = studentSignature?.name,
//              parentSignature = parentSignature?.name
//            )
//
//            scope.launch {
//
//              val filesToUpload = buildMap<String, List<PlatformFile>> {
//                put("document", attachedDocuments)
//                put("student_photo", listOf(studentPhoto!!))
//                put("student_signature", listOf(studentSignature!!))
//                put("parent_signature", listOf(parentSignature!!))
//              }
//
//              try{
//                filesToUpload.forEach {
//                  val (index, files) = it
//                  val docList = mutableListOf<String>()
//                  files.forEach { file ->
//                    val uploadResponse = bucket.upload(
//                      path = "${index}_${Clock.System.now().epochSeconds}.jpg",
//                      data = file.readBytes()
//                    )
//                    docList.add(bucket.publicUrl( uploadResponse.path))
//                    println("file upload complete :$uploadResponse")
//                  }
//                  when(index){
//                    "document" -> {
//                      FormData = FormData.copy(attachedDocuments = docList)
//                    }
//                    "student_photo" -> {
//                      FormData = FormData.copy(studentPhoto = docList[0])
//                    }
//                    "student_signature" -> {
//                      FormData = FormData.copy(studentSignature = docList[0])
//                    }
//                    "parent_signature" -> {
//                      FormData = FormData.copy(parentSignature = docList[0])
//                    }
//                  }
//                }
//              } catch (e:Exception){
//                bucket.delete()
//                snackbarHostState.showSnackbar(
//                  message = "Error uploading files ${e.message}",
//                  actionLabel = "Close"
//                )
//                return@launch
//              }
//
//              println("Form Data: $FormData") // Print form data
//
//              // Submit form using ViewModel
//              viewModel.submitAdmissionForm(
//                AdmissionFormDataInput(
//                  id = Uuid.random().toString(),
//                  studentName = FormData.studentName,
//                  aadharNo = FormData.aadharNo,
//                  dob = FormData.dob,
//                  bloodGroup = FormData.bloodGroup,
//                  previousClass = FormData.previousClass,
//                  marksObtained = FormData.marksObtained,
//                  schoolName = FormData.schoolName,
//                  fatherName = FormData.fatherName,
//                  fatherOccupation = FormData.fatherOccupation,
//                  fatherQualification = FormData.fatherQualification,
//                  motherName = FormData.motherName,
//                  motherOccupation = FormData.motherOccupation,
//                  motherQualification = FormData.motherQualification,
//                  fullAddress = FormData.fullAddress,
//                  mobileNo = FormData.mobileNo,
//                  alternateMobileNo = FormData.alternateMobileNo,
//                  attachedDocuments = FormData.attachedDocuments,
//                  studentPhoto = FormData.studentPhoto!!,
//                  studentSignature = FormData.studentSignature!!,
//                  parentSignature = FormData.parentSignature!!,
//                )
//              )
//            }
//          } else {
//            // Form is invalid, errors are already displayed, just scroll to top
//            scope.launch {
//              scrollState.animateScrollTo(0)
//            }
//            println("Form is invalid. Please check the inputs.")
//          }
//        },
//        enabled = !isSubmittingData,
//      ) {
//        if(isSubmittingData){
//          Row(verticalAlignment = Alignment.CenterVertically) {
//            CircularProgressIndicator(modifier = Modifier.size(16.dp))
//            Spacer(Modifier.width(8.dp))
//            Text("Submitting admission data...")
//          }
//        }else {
//          Text("Submit", modifier = Modifier.padding(horizontal = 24.dp))
//        }
//      }
//
//      // Handle form submission result
//      if (formSubmissionState.isSuccess) {
//
//        // Reset form fields
//        studentName = ""
//        aadharNo = ""
//        dob = ""
//        selectedBloodGroup = ""
//        previousClass = ""
//        marksObtained = ""
//        schoolName = ""
//        fatherName = ""
//        fatherOccupation = ""
//        fatherQualification = ""
//        motherName = ""
//        motherOccupation = ""
//        motherQualification = ""
//        fullAddress = ""
//        mobileNo = ""
//        alternateMobileNo = ""
//        attachedDocuments = listOf()
//        studentPhoto = null
//        studentSignature = null
//        parentSignature = null
//
//        // Reset form submission state
//        viewModel.resetFormSubmissionState()
//
//        scope.launch {
//          // student form submission successful
//          snackbarHostState.showSnackbar(
//            message = "Admission form data successfully submitted",
//          )
//        }
//      } else if (formSubmissionState.error != null) {
//        scope.launch {
//          // error submitting data
//          snackbarHostState.showSnackbar(
//            message = "Error submitting form: ${formSubmissionState.error}",
//            actionLabel = "Close"
//          )
//        }
//      }
//    }
//  }
}

@Preview
@Composable
fun PreviewRegistrationForm() {
  MaterialTheme {
    // This is just a preview, so we don't need a real ViewModel
    // In a real app, we would inject the ViewModel
    // RegistrationForm(viewModel = AdmissionsViewModel())
  }
}
