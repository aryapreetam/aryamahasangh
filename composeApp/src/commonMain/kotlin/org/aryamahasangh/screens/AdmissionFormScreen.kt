package org.aryamahasangh.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.ui.zIndex
import kotlinx.datetime.*
import org.aryamahasangh.utils.epochToDate
import org.jetbrains.compose.ui.tooling.preview.Preview

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
  return number.length == 12 && number.all { it.isDigit() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)
  val showDatePickerDialog = remember { mutableStateOf(false) }

  OutlinedTextField(
    modifier = modifier,
    value = value,
    label = { Text("Date of birth") },
    onValueChange = { onValueChange(it) },
    trailingIcon = {
      IconButton(onClick = { showDatePickerDialog.value = true }) {
        Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
      }
    },
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
    placeholder = { Text("dd/mm/yyyy") },
    singleLine = true,
    maxLines = 1,
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
  modifier: Modifier
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
        value = selectedBloodGroup ?: "Select",
        onValueChange = { /* Do nothing, read-only */ },
        readOnly = true,
        singleLine = true,
        placeholder = { Text("Select") },
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
              textFieldSize = coordinates.size.toSize()
            }.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        label = { Text("Blood Group") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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

@Composable
fun DocumentGrid(
  documents: List<String>,
  onDocumentRemoved: (String) -> Unit
) {
  if (documents.isEmpty()) {
    Text("No documents attached.")
    return
  }
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 120.dp),
    contentPadding = PaddingValues(16.dp)
  ) {
    items(documents.size) { index ->
      val document = documents[index]
      Box(
        modifier = Modifier
          .padding(4.dp)
          .aspectRatio(1f)
      ) {
        // Attempt to load the image based on file extension
        val imageBitmap = try {
          // Load image from file
          // val bufferedImage = org.jetbrains.skia.Image.makeFromEncoded(document.readBytes()).toBitmap()
          // bufferedImage.asImageBitmap()
          null
        } catch (e: Exception) {
          e.printStackTrace()
          null // Handle loading errors
        }

        if (imageBitmap != null) {
          Image(
            bitmap = imageBitmap,
            contentDescription = "Attached Document",
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
          )
        } else {
          // Display a placeholder or error message
          Text("Unsupported File Format", textAlign = TextAlign.Center)
        }

        IconButton(
          onClick = { onDocumentRemoved(document) },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(24.dp)
            .zIndex(1f) // Ensure the button is on top
        ) {
          Icon(Icons.Filled.Close, "Remove", tint = Color.Red)
        }
      }
    }
  }
}

// Placeholder for file selection for Compose Multiplatform
// Replace this with platform-specific file selection logic
@Composable
fun rememberFilePicker(): FilePicker {
  return remember { FilePicker() }
}

@Composable
fun ButtonForFilePicker(onFileSelected: (String?) -> Unit) {
  Button(onClick = { onFileSelected("path") }) {
    Text("Select File")
  }
}

@Composable
fun StudentPhotoSection(
  studentPhoto: String?,
  onPhotoSelected: (String?) -> Unit
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Student Photo:")

    if (studentPhoto != null) {
      // TODO: Load the ImageBitmap from the file path
      val imageBitmap: ImageBitmap? = null // Replace with actual image loading logic

      if (imageBitmap != null) {
        Box(modifier = Modifier.size(150.dp)) {
          Image(
            bitmap = imageBitmap,
            contentDescription = "Student Photo",
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
          )
          IconButton(
            onClick = { onPhotoSelected(null) },
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(4.dp)
              .size(24.dp)
              .zIndex(1f)
          ) {
            Icon(Icons.Filled.Close, "Remove Photo", tint = Color.Red)
          }
        }
      } else {
        Text("Error loading image")
      }
    } else {
      Box(
        modifier = Modifier
          .size(150.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Text("No photo selected")
      }
    }

    Button(onClick = {
      // TODO: Implement photo selection (camera or gallery)
      onPhotoSelected("path")
    }) {
      Text("Select Photo")
    }
  }
}


@Composable
fun SignatureSection(
  signatureFile: String?,
  onSignatureSelected: (String?) -> Unit,
  label: String
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("$label Signature:")

    if (signatureFile != null) {
      // TODO: Load the ImageBitmap from the file path
      val imageBitmap: ImageBitmap? = null // Replace with actual image loading logic

      if (imageBitmap != null) {
        Box(modifier = Modifier.size(150.dp)) {
          Image(
            bitmap = imageBitmap,
            contentDescription = "$label Signature",
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
          )
          IconButton(
            onClick = { onSignatureSelected(null) },
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(4.dp)
              .size(24.dp)
              .zIndex(1f)
          ) {
            Icon(Icons.Filled.Close, "Remove Signature", tint = Color.Red)
          }
        }
      } else {
        Text("Error loading image")
      }
    } else {
      Box(
        modifier = Modifier
          .size(150.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Text("No signature selected")
      }
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Button(onClick = {
        // TODO: Implement file upload for signature
        onSignatureSelected("path")
      }) {
        Text("Upload Image")
      }

      Button(onClick = {
        // TODO: Implement signature drawing
        onSignatureSelected("path")
      }) {
        Text("Draw Signature")
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegistrationForm() {
  var formData by remember { mutableStateOf(FormData()) }
  var studentName by remember { mutableStateOf("") }
  var aadharNo by remember { mutableStateOf("") }
  var dob by remember { mutableStateOf("") }
  var selectedBloodGroup by remember { mutableStateOf("") }
  var previousClass by remember { mutableStateOf("") }
  var marksObtained by remember { mutableStateOf("") }
  var schoolName by remember { mutableStateOf("") }
  var fatherName by remember { mutableStateOf("") }
  var fatherOccupation by remember { mutableStateOf("") }
  var fatherQualification by remember { mutableStateOf("") }
  var motherName by remember { mutableStateOf("") }
  var motherOccupation by remember { mutableStateOf("") }
  var motherQualification by remember { mutableStateOf("") }
  var fullAddress by remember { mutableStateOf("") }
  var mobileNo by remember { mutableStateOf("") }
  var alternateMobileNo by remember { mutableStateOf("") }
  var attachedDocuments by remember { mutableStateOf(listOf<String>("Test")) }
  var studentPhoto by remember { mutableStateOf<String?>("") }
  var studentSignature by remember { mutableStateOf<String?>(null) }
  var parentSignature by remember { mutableStateOf<String?>(null) }

  var isFormValid by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()
  val filePicker = rememberFilePicker()

  LaunchedEffect(
    studentName,
    aadharNo,
    dob,
    selectedBloodGroup,
    previousClass,
    marksObtained,
    schoolName,
    fatherName,
    fatherOccupation,
    fatherQualification,
    motherName,
    motherOccupation,
    motherQualification,
    fullAddress,
    mobileNo,
    alternateMobileNo,
    attachedDocuments,
    studentPhoto,
    studentSignature,
    parentSignature
  ) {
    isFormValid = studentName.isNotBlank() &&
        isValidAadhar(aadharNo.replace(" ", "")) &&
        isValidDate(dob) &&
        selectedBloodGroup.isNotBlank() &&
        previousClass.isNotBlank() &&
        marksObtained.isNotBlank() &&
        schoolName.isNotBlank() &&
        fatherName.isNotBlank() &&
        fatherOccupation.isNotBlank() &&
        fatherQualification.isNotBlank() &&
        motherName.isNotBlank() &&
        motherOccupation.isNotBlank() &&
        motherQualification.isNotBlank() &&
        fullAddress.isNotBlank() &&
        isValidMobileNumber(mobileNo) &&
        isValidMobileNumber(alternateMobileNo) &&
        attachedDocuments.isNotEmpty() &&
        studentPhoto != null
  }

  CompositionLocalProvider(
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(scrollState),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text("Student Registration Form", style = MaterialTheme.typography.headlineMedium)

      OutlinedTextField(
        value = studentName,
        onValueChange = { text ->
          studentName = text.split(" ").joinToString(" ") { it.capitalize(Locale.current) }
          formData = formData.copy(studentName = studentName)
        },
        label = { Text("Name of the Student") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = aadharNo,
        onValueChange = { text ->
          if (text.length <= 12) {
            aadharNo = text
            formData = formData.copy(aadharNo = aadharNo)
          }
        },
        label = { Text("Aadhar No") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = AadharVisualTransformation(),
        maxLines = 1,
        singleLine = true
      )

      Row(modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)){
        DatePickerTextField(
          value = dob,
          onValueChange = { text ->
            dob = text
            formData = formData.copy(dob = dob)
          },
          modifier = Modifier.weight(.5f),
        )

        BloodGroupDropdown(
          selectedBloodGroup = selectedBloodGroup,
          onBloodGroupSelected = { group ->
            selectedBloodGroup = group
            formData = formData.copy(bloodGroup = selectedBloodGroup)
          },
          modifier = Modifier.weight(.5f),
        )
      }

      OutlinedTextField(
        value = previousClass,
        onValueChange = { text ->
          if (text.length <= 50) {
            previousClass = text
            formData = formData.copy(previousClass = previousClass)
          }
        },
        label = { Text("Previous Class Passed") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = marksObtained,
        onValueChange = { text ->
          if (text.length <= 50) {
            marksObtained = text
            formData = formData.copy(marksObtained = marksObtained)
          }
        },
        label = { Text("Marks Obtained") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = schoolName,
        onValueChange = { text ->
          if (text.length <= 100) {
            schoolName = text
            formData = formData.copy(schoolName = schoolName)
          }
        },
        label = { Text("Name of School") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Filled.School, contentDescription = "Address") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = fatherName,
        onValueChange = { text ->
          if (text.length <= 100) {
            fatherName = text
            formData = formData.copy(fatherName = fatherName)
          }
        },
        label = { Text("Father's Name") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = fatherOccupation,
        onValueChange = { text ->
          if (text.length <= 50) {
            fatherOccupation = text
            formData = formData.copy(fatherOccupation = fatherOccupation)
          }
        },
        label = { Text("Occupation") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = fatherQualification,
        onValueChange = { text ->
          if (text.length <= 100) {
            fatherQualification = text
            formData = formData.copy(fatherQualification = fatherQualification)
          }
        },
        label = { Text("Qualification") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Filled.HistoryEdu, contentDescription = "Education history") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = motherName,
        onValueChange = { text ->
          if (text.length <= 100) {
            motherName = text
            formData = formData.copy(motherName = motherName)
          }
        },
        label = { Text("Mother's Name") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = motherOccupation,
        onValueChange = { text ->
          if (text.length <= 50) {
            motherOccupation = text
            formData = formData.copy(motherOccupation = motherOccupation)
          }
        },
        label = { Text("Occupation") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = motherQualification,
        onValueChange = { text ->
          if (text.length <= 100) {
            motherQualification = text
            formData = formData.copy(motherQualification = motherQualification)
          }
        },
        label = { Text("Qualification") },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Filled.HistoryEdu, contentDescription = "Education history") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = fullAddress,
        onValueChange = { text ->
          if (text.length <= 300) {
            fullAddress = text
            formData = formData.copy(fullAddress = fullAddress)
          }
        },
        leadingIcon = { Icon(Icons.Filled.Place, contentDescription = "Address") },
        label = { Text("Full Address") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        minLines = 3, // Use minLines instead of singleLine for a text area
        maxLines = 5
      )

      OutlinedTextField(
        value = mobileNo,
        onValueChange = { text ->
          if (text.length <= 12) {
            mobileNo = text
            formData = formData.copy(mobileNo = mobileNo)
          }
        },
        label = { Text("Mobile No.") },
        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        maxLines = 1,
        singleLine = true
      )

      OutlinedTextField(
        value = alternateMobileNo,
        onValueChange = { text ->
          if (text.length <= 12) {
            alternateMobileNo = text
            formData = formData.copy(alternateMobileNo = alternateMobileNo)
          }
        },
        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone") },
        label = { Text("Alternate Mobile No. (In Case of Emergency)") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        maxLines = 1,
        singleLine = true
      )

      Text("Attached Documents:")
//      DocumentGrid(
//        documents = attachedDocuments,
//        onDocumentRemoved = { documentToRemove ->
//          attachedDocuments = attachedDocuments.toMutableList().apply {
//            remove(documentToRemove)
//          }.toList()
//          formData = formData.copy(attachedDocuments = attachedDocuments)
//        }
//      )

      ButtonForFilePicker(onFileSelected = { filePath ->
        if (filePath != null) {
          attachedDocuments = (attachedDocuments + filePath).distinct()
          formData = formData.copy(attachedDocuments = attachedDocuments)
        }
      })

      StudentPhotoSection(
        studentPhoto = studentPhoto,
        onPhotoSelected = { file ->
          studentPhoto = file
          formData = formData.copy(studentPhoto = studentPhoto)
        }
      )

      SignatureSection(
        signatureFile = studentSignature,
        onSignatureSelected = { file ->
          studentSignature = file
          formData = formData.copy(studentSignature = studentSignature)
        },
        label = "Student"
      )

      SignatureSection(
        signatureFile = parentSignature,
        onSignatureSelected = { file ->
          parentSignature = file
          formData = formData.copy(parentSignature = parentSignature)
        },
        label = "Parent"
      )

      Button(
        onClick = {
          if (isFormValid) {
            formData = FormData(
              studentName = studentName,
              aadharNo = aadharNo,
              dob = dob,
              bloodGroup = selectedBloodGroup,
              previousClass = previousClass,
              marksObtained = marksObtained,
              schoolName = schoolName,
              fatherName = fatherName,
              fatherOccupation = fatherOccupation,
              fatherQualification = fatherQualification,
              motherName = motherName,
              motherOccupation = motherOccupation,
              motherQualification = motherQualification,
              fullAddress = fullAddress,
              mobileNo = mobileNo,
              alternateMobileNo = alternateMobileNo,
              attachedDocuments = attachedDocuments,
              studentPhoto = studentPhoto,
              studentSignature = studentSignature,
              parentSignature = parentSignature
            )
            println("Form Data: $formData") // Print form data
          } else {
            //TODO: show a dialog with error saying that the form is invalid
            println("Form is invalid. Please check the inputs.")
          }
        },
        enabled = isFormValid,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Submit")
      }
    }
  }
}

// all expect actual

class FilePicker {
  fun selectFiles(onFileSelected: (List<String>?) -> Unit){}
  fun selectFile(onFileSelected: (String?) -> Unit){}
}

//@Composable
//fun StudentPhotoSection(
//  studentPhoto: String?,
//  onPhotoSelected: (String?) -> Unit
//)

//@Composable
//fun SignatureSection(
//  signatureFile: String?,
//  onSignatureSelected: (String?) -> Unit,
//  label: String
//)

// Provide a dummy implementation for non-JVM platforms (e.g., JS)
fun getImageBitmap(path: String): ImageBitmap? = { } as ImageBitmap?

@Preview
@Composable
fun PreviewRegistrationForm() {
  MaterialTheme {
    RegistrationForm()
  }
}
