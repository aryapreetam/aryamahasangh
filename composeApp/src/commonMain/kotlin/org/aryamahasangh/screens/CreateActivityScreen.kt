package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.jetbrains.compose.ui.tooling.preview.Preview

// Data classes defined directly in Kotlin
data class Organisation(val id: String, val name: String)
data class Member(val id: String, val name: String, val phoneNumber: String, val profileImage: String)
data class FormData1(val organisations: List<Organisation>, val members: List<Member>)

// Mock JSON data converted to Kotlin object
val mockFormData1 = FormData1(
  organisations = listOf(
    Organisation("31e8e401-fed7-4ab9-a7fb-b031488ae3a5", "राष्ट्रीय आर्य निर्मात्री सभा"),
    Organisation("531c09ef-996f-47f0-9c88-88a9f7fa5380", "राष्ट्रीय आर्य क्षत्रिय सभा"),
    Organisation("221465d0-ba28-4e43-85a0-dfe166003b48", "राष्ट्रीय आर्य संरक्षिणी सभा"),
    Organisation("b10b4fec-0e39-439b-b8cd-747e4ac30035", "राष्ट्रीय आर्य संवर्धिनी सभा"),
    Organisation("d9be2e8e-ce2e-4c10-ac74-e3f8eb1198b8", "राष्ट्रीय आर्य दलितोद्धारिणी सभा"),
    Organisation("3d0d3f88-fbc9-415a-9182-833fe08454e6", "आर्य गुरुकुल महाविद्यालय"),
    Organisation("ac0a79c1-e9aa-474c-8ba5-d9342c56a6bc", "आर्या गुरुकुल महाविद्यालय"),
    Organisation("47790da8-8ec4-44e6-b0cc-39f0f95ee6b4", "आर्या परिषद्"),
    Organisation("1bc501ee-283c-4b64-92e8-a067f08ef39d", "वानप्रस्थ आयोग"),
    Organisation("f8e82b92-0e9e-4f59-976f-650f6b11774e", "राष्ट्रीय आर्य संचार परिषद"),
    Organisation("a5a834db-10c6-4de8-b173-c72dfdb2cec8", "आर्य महासंघ")
  ),
  members = listOf(
    Member("1e4c75f8-4271-4e20-b00e-82a5162af9e3", "आचार्य जितेन्द्र आर्य", "9416201731", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp"),
    Member("98072b81-4c63-4ffb-9643-90174107449e", "आचार्य डॉ० महेशचन्द्र आर्य", "9813377510", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_mahesh.webp"),
    Member("6c6d0a7b-ef6a-4308-8bd0-2f66e1cff8c0", "डॉ० महेश आर्य", "9810485231", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/dr_mahesh_arya.webp"),
    Member("e8f81733-2c37-410e-900c-6132529dcd93", "उपाचार्य जसबीर आर्य", "9871092222", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp"),
    Member("ec0b342e-6433-440b-bf0c-17530bf3131d", "सौम्य आर्य", "9466944880", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp"),
    Member("6541d219-e860-4998-a022-4efda1e56cff", "आर्य प्रवेश 'प्रघोष'", "7419002189", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_pravesh_ji.webp"),
    Member("5584e2ca-fc87-49f8-a110-cdbdf7f4b343", "आचार्य संजीव आर्य", "9045353309", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp"),
    Member("8c419841-6dde-464e-b257-e8b93fbab5dc", "आर्य सुशील", "9410473224", ""),
    Member("965e76fc-d7cc-49b0-a514-ae99745fd261", "आचार्य वर्चस्पति", "9053347826", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_varchaspati.webp"),
    Member("951b33a0-c5d5-41c1-b136-4c4e3f56358c", "आर्य वेदप्रकाश", "8168491108", ""),
    Member("cf298218-90ad-46dc-8eff-2c8f3f31b6cd", "सुखविंदर आर्य", "8529616314", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/anil_arya.webp"),
    Member("75235ee0-a6cd-4472-87f3-1c1e13b16f9f", "आर्य धर्मबीर शास्त्री", "9812428391", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp"),
    Member("661a0db7-12da-4179-8611-b94c2c10072b", "आर्य संदीप शास्त्री", "9812492102", ""),
    Member("b0dd89d1-3a79-4669-a0e0-4ff65a74386a", "अश्विनी आर्य", "9719375460", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_ashvani.webp"),
    Member("f9d0aa23-d579-4915-a7b1-0f976a28cdb0", "आचार्या इन्द्रा", "9868912128", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_indra.webp"),
    Member("994a668a-cae7-4eac-b7df-3f72fedb9176", "आचार्या डॉ० सुशीला", "9355690824", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_suman.webp"),
    Member("1609b32d-0595-4716-b807-dc0155106790", "आर्या रेनु", "9999999999", ""),
    Member("0663ea2e-6bfc-47fe-8cb7-b3b33fd78891", "पंडित लोकनाथजी आर्य", "7015563934", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_loknath.webp"),
    Member("d29b0b9a-9737-4743-9a94-7836016c0601", "श्री शिवनारायणजी आर्य", "9466140987", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_shivnarayan.webp"),
    Member("c2cce78b-7817-4e47-bccb-a4a130952ef6", "अनिल आर्य", "9416037102", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/anil_arya.webp"),
    Member("fea837b1-b7ed-4737-8ff9-7d9f41e6f82b", "आचार्य हनुमत् प्रसाद", "9868792232", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_hanumat_prasad.webp"),
    Member("9b2ae964-e646-4518-8a92-3c451019882a", "आचार्य सतीश", "9350945482", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_satish.webp"),
    Member("51fd156f-abd2-46c0-84bb-074850496de1", "आर्य जसबीर सिंह", "9717647455", "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp")
  )
)

//fun parseJson(jsonString: String): FormData {  //No longer needed
//    val json = Json {
//        ignoreUnknownKeys = true
//    }
//    val parsed = json.decodeFromString<Data>(jsonString)
//    return FormData(parsed.data.organisations, parsed.data.members)
//}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityForm(formData1: FormData1) { // Take FormData object directly

  // State variables for form fields
  var name by remember { mutableStateOf("") }
  var nameError by remember { mutableStateOf(false) }

  val types = remember { listOf("कक्षा", "कार्यक्रम", "अभियान", "सत्र") }
  var selectedType by remember { mutableStateOf<String?>(null) } // Only one can be selected

  var typesError by remember { mutableStateOf(false) }

  var shortDescription by remember { mutableStateOf("") }
  var shortDescriptionError by remember { mutableStateOf(false) }

  var description by remember { mutableStateOf("") }
  var descriptionError by remember { mutableStateOf(false) }

  var associatedOrganisations by remember { mutableStateOf(emptySet<String>()) }
  var associatedOrganisationsError by remember { mutableStateOf(false) }

  var address by remember { mutableStateOf("") }
  var addressError by remember { mutableStateOf(false) }

  var state by remember { mutableStateOf("") }
  var stateError by remember { mutableStateOf(false) }

  var district by remember { mutableStateOf("") }
  var districtError by remember { mutableStateOf(false) }

  val states = remember { listOf("Haryana", "Punjab", "Delhi") } //Dummy data
  val districts = remember {
    mutableStateMapOf(
      "Haryana" to listOf("Gurugram", "Faridabad"),
      "Punjab" to listOf("Ludhiana", "Amritsar"),
      "Delhi" to listOf("New Delhi", "South Delhi")
    )
  }

  var pincode by remember { mutableStateOf("") }
  var pincodeError by remember { mutableStateOf(false) }

  var startDate by remember { mutableStateOf<LocalDate?>(null) }
  var startTime by remember { mutableStateOf<LocalTime?>(null) }
  var endDate by remember { mutableStateOf<LocalDate?>(null) }
  var endTime by remember { mutableStateOf<LocalTime?>(null) }
  var startDateText by remember { mutableStateOf(TextFieldValue("")) }
  var endDateText by remember { mutableStateOf(TextFieldValue("")) }
  var startTimeText by remember { mutableStateOf(TextFieldValue("")) }
  var endTimeText by remember { mutableStateOf(TextFieldValue("")) }
  var startDateError by remember { mutableStateOf(false) }
  var startTimeError by remember { mutableStateOf(false) }
  var endDateError by remember { mutableStateOf(false) }
  var endTimeError by remember { mutableStateOf(false) }

  var attachedDocuments by remember { mutableStateOf(emptyList<PlatformFile>()) }
  var attachedDocumentsError by remember { mutableStateOf(false) }
  var attachedDocumentsErrorMessage by remember { mutableStateOf("") }


  var contactPeople by remember { mutableStateOf(emptySet<String>()) }
  var contactPeopleError by remember { mutableStateOf(false) }

  var additionalInstructions by remember { mutableStateOf("") }

  var isSubmitting by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()
  val organisations = remember { formData1.organisations }
  val members = remember { formData1.members }

  // Date Picker Dialog State
  val openStartDateDialog = remember { mutableStateOf(false) }
  val openEndDateDialog = remember { mutableStateOf(false) }

  val openStartTimeDialog = remember { mutableStateOf(false) }
  val openEndTimeDialog = remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  // Date format
  val dateFormatter: (LocalDate) -> String = { date ->
    "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
  }

  // Time format
  val timeFormatter: (LocalTime) -> String = { time ->
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
  }

  fun validateForm(): Boolean {
    nameError = name.isEmpty()
    typesError = selectedType == null
    shortDescriptionError = shortDescription.isEmpty()
    descriptionError = description.isEmpty()
    associatedOrganisationsError = associatedOrganisations.isEmpty()
    addressError = address.isEmpty()
    stateError = state.isEmpty()
    districtError = district.isEmpty()
    pincodeError = pincode.isEmpty() || pincode.length > 8
    startDateError = startDate == null
    startTimeError = startTime == null
    endDateError = endDate == null
    endTimeError = endTime == null
    contactPeopleError = contactPeople.isEmpty()

    return !(nameError || typesError || shortDescriptionError || descriptionError || associatedOrganisationsError
        || addressError || stateError || districtError || pincodeError || startDateError || startTimeError
        || endDateError || endTimeError || contactPeopleError)
  }

  fun submitForm() {

    if (validateForm()) {
      isSubmitting = true
      scope.launch {
        delay(2000)
        isSubmitting = false
        println("Form submitted with data:")
        println("Name: $name")
        println("Types: $selectedType")
        println("Short Description: $shortDescription")
        println("Description: $description")
        println("Associated Organisations: $associatedOrganisations")
        println("Address: $address")
        println("State: $state")
        println("District: $district")
        println("Pincode: $pincode")
        println("Start Date: $startDate")
        println("Start Time: $startTime")
        println("End Date: $endDate")
        println("End Time: $endTime")
        println("Contact People: $contactPeople")
        println("Additional Instructions: $additionalInstructions")

        // Reset form
        name = ""
        selectedType = ""
        shortDescription = ""
        description = ""
        associatedOrganisations = emptySet()
        address = ""
        state = ""
        district = ""
        pincode = ""
        startDate = null
        startTime = null
        endDate = null
        endTime = null
        contactPeople = emptySet()
        additionalInstructions = ""

        startDateText = TextFieldValue("")
        endDateText = TextFieldValue("")
        startTimeText = TextFieldValue("")
        endTimeText = TextFieldValue("")

        // Clear all error states
        nameError = false
        typesError = false
        shortDescriptionError = false
        descriptionError = false
        associatedOrganisationsError = false
        addressError = false
        stateError = false
        districtError = false
        pincodeError = false
        startDateError = false
        startTimeError = false
        endDateError = false
        endTimeError = false
        contactPeopleError = false
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp)
      .verticalScroll(scrollState),
  ) {
    // Name
    OutlinedTextField(
      value = name,
      onValueChange = { name = it },
      label = { Text("नाम") },
      modifier = Modifier.width(500.dp),
      isError = nameError,
      supportingText = {
        if (nameError) {
          Text("Name is required")
        }
      },
      maxLines = 1,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    Column{
      // Type (Filter Chips)
      Text(text = "प्रकार :", style = MaterialTheme.typography.bodyMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { type ->
          FilterChip(
            selected = (selectedType == type),  // Check for equality, not `in`
            onClick = {
              selectedType = if (selectedType == type) {
                null // Unselect if already selected
              } else {
                type   // Select new type and unselect the older
              }
              typesError = selectedType == null // Update error status
            },
            label = { Text(type) },
            leadingIcon = if (selectedType == type) {
              {
                Icon(
                  imageVector = Icons.Filled.Done,
                  contentDescription = "Selected",
                  modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
              }
            } else {
              null
            },
            border = FilterChipDefaults.filterChipBorder(
              borderColor = MaterialTheme.colorScheme.outline,
              selectedBorderColor = MaterialTheme.colorScheme.primary,
              enabled = selectedType == type,
              selected = selectedType == type
            ),
          )
        }
      }
      if (typesError) {
        Text(
          text = "Type is required",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(start = 16.dp)
        )
      }
    }

    // Short Description
    OutlinedTextField(
      value = shortDescription,
      onValueChange = {
        if (it.length <= 100) {
          shortDescription = it
          shortDescriptionError = false // Clear error on valid input
        }
      },
      label = { Text("संक्षिप्त विवरण") },
      modifier = Modifier.width(500.dp),
      isError = shortDescriptionError,
      supportingText = { if (shortDescriptionError) Text("Short Description is required") },
      maxLines = 1,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    // Description
    OutlinedTextField(
      value = description,
      onValueChange = {
        if (it.length <= 1000) {
          description = it
          descriptionError = false // Clear error on valid input
        }
      },
      label = { Text("विस्तृत विवरण") },
      modifier = Modifier.width(500.dp),
      minLines = 3,
      maxLines = 30,
      isError = descriptionError,
      supportingText = { if (descriptionError) Text("Description is required") },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

    // Associated Organisations
    MultiSelectDropdown(
      modifier = Modifier.width(500.dp),
      label = "संबधित संस्थाएं",
      options = organisations.map { it.name },
      selectedOptions = associatedOrganisations,
      onSelectionChanged = {
        associatedOrganisations = it
        associatedOrganisationsError = it.isEmpty()// Update error on selection change
      },
      isError = associatedOrganisationsError,
      supportingText = { if (associatedOrganisationsError) Text("Associated Organisation is required") }
    )

    // Address
    OutlinedTextField(
      value = address,
      onValueChange = {
        if (it.length <= 100) {
          address = it
          addressError = false
        }
      },
      label = { Text("पूर्ण पता") },
      modifier = Modifier.width(500.dp),
      minLines = 2,
      maxLines = 3,
      isError = addressError,
      supportingText = { if (addressError) Text("Address is required") },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )

//    // State
//    Dropdown(
//      label = "State *",
//      options = states,
//      selectedValue = state,
//      onValueChanged = {
//        state = it
//        district = ""
//        stateError = false
//      },
//      isError = stateError,
//      supportingText = { if (stateError) Text("State is required") }
//    )
//
//    // District
//    Dropdown(
//      label = "District *",
//      options = districts[state] ?: emptyList(),
//      selectedValue = district,
//      onValueChanged = {
//        district = it
//        districtError = false
//      },
//      isError = districtError,
//      supportingText = { if (districtError) Text("District is required") }
//    )

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      StateDropdown(
        states = indianStatesToDistricts.keys.toList(),
        selectedState = state,
        onStateSelected = { state = it },
        modifier = Modifier.width( 160.dp)
      )
      // District Selection (Conditional)
      val districts = indianStatesToDistricts[state] ?: emptyList()
      DistrictDropdown(
        districts = districts,
        selectedDistrict = district,
        onDistrictSelected = { district = it ?: "" },
        modifier = Modifier.width(200.dp)
      )
      // Pincode
      OutlinedTextField(
        value = pincode,
        onValueChange = {
          if (it.length <= 8 && it.all { char -> char.isDigit() }) {
            pincode = it
            pincodeError = false
          }
        },
        label = { Text("पिनकोड") },
        modifier = Modifier.width(110.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        isError = pincodeError,
        supportingText = {
          if (pincodeError) {
            Text("Pincode must be numeric and no more than 8 digits")
          }
        }
      )
    }
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ){
      Column{
        Text(text = "प्रारंभ:", style = MaterialTheme.typography.bodyMedium)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CustomTextField(
            value = startDateText,
            label = "दिनांक",
            modifier = Modifier.width(180.dp),
            readOnly = true,
            onClick = { openStartDateDialog.value = true },
            isError = startDateError,
            supportingText = {if (startDateError) Text("Start date is required")}
          )

          CustomTextField(
            value = startTimeText,
            label = "समय",
            modifier = Modifier.width(180.dp),
            readOnly = true,
            onClick = { openStartTimeDialog.value = true },
            isError = startTimeError,
            supportingText = {if (startTimeError) Text("Start time is required")}
          )
        }
      }

      Column{
        Text(text = "समाप्ति:", style = MaterialTheme.typography.bodyMedium)
        // End Date and Time
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CustomTextField(
            value = endDateText,
            label = "दिनांक",
            modifier = Modifier.width(180.dp),
            readOnly = true,
            onClick = { openEndDateDialog.value = true },
            isError = endDateError,
            supportingText = {if (endDateError) Text("End date is required")}
          )

          CustomTextField(
            value = endTimeText,
            label = "समय",
            modifier = Modifier.width(180.dp),
            readOnly = true,
            onClick = { openEndTimeDialog.value = true },
            isError = endTimeError,
            supportingText = {if (endTimeError) Text("End time is required")}
          )
        }
      }
    }

    Text(
      modifier = Modifier.padding(top = 8.dp),
      text = "संबधित चित्र एवं पत्रिकाएं:",
      style = MaterialTheme.typography.labelLarge
    )
    DocumentGrid(
      documents = attachedDocuments,
      onDocumentRemoved = { documentToRemove ->
        attachedDocuments = attachedDocuments.toMutableList().apply {
          remove(documentToRemove)
        }.toList()
      },
      isError = attachedDocumentsError,
      errorMessage = attachedDocumentsErrorMessage
    )

    ButtonForFilePicker("चित्र/पत्रिकाएं जोड़ें", onFilesSelected = { filePath ->
      if (filePath != null) {
        attachedDocumentsError = false
        attachedDocuments = (attachedDocuments + filePath).distinct()
      }
    })

    // Contact People
    ContactPeopleDropdown(
      modifier = Modifier.width(500.dp),
      label = "संपर्क सूत्र",
      members = members,
      selectedMembers = contactPeople,
      onSelectionChanged = {
        contactPeople = it
        contactPeopleError = it.isEmpty()
      },
      isError = contactPeopleError,
      supportingText = { if (contactPeopleError) Text("At least one contact person is required") }
    )

    // Additional Instructions
    OutlinedTextField(
      value = additionalInstructions,
      onValueChange = {
        if (it.length <= 1000) {
          additionalInstructions = it
        }
      },
      label = { Text("अतिरिक्त निर्देश") },
      modifier = Modifier.width(500.dp),
      minLines = 3,
      maxLines = 10,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )

    // Create Activity Button
    Button(
      modifier = Modifier.padding(vertical = 16.dp),
      onClick = { submitForm() },
      enabled = !isSubmitting,
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      )
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text("गतिविधि बनाएं")
      }
    }

    // Date and Time Picker Dialogs
    if (openStartDateDialog.value) {
      CustomDatePickerDialog(
        onDateSelected = { selectedDate ->
          startDate = selectedDate
          startDateText = TextFieldValue(dateFormatter(selectedDate))
          startDateError = false
          openStartDateDialog.value = false
        },
        onDismissRequest = { openStartDateDialog.value = false }
      )
    }

    if (openEndDateDialog.value) {
      CustomDatePickerDialog(
        onDateSelected = { selectedDate ->
          endDate = selectedDate
          endDateText = TextFieldValue(dateFormatter(selectedDate))
          endDateError = false
          openEndDateDialog.value = false
        },
        onDismissRequest = { openEndDateDialog.value = false }
      )
    }
    if (openStartTimeDialog.value) {
      TimePickerDialog(
        onTimeSelected = { selectedTime ->
          startTime = selectedTime
          startTimeText = TextFieldValue(timeFormatter(selectedTime))
          startTimeError = false
          openStartTimeDialog.value = false
        },
        onDismissRequest = { openStartTimeDialog.value = false }
      )
    }

    if (openEndTimeDialog.value) {
      TimePickerDialog(
        onTimeSelected = { selectedTime ->
          endTime = selectedTime
          endTimeText = TextFieldValue(timeFormatter(selectedTime))
          endTimeError = false
          openEndTimeDialog.value = false
        },
        onDismissRequest = { openEndTimeDialog.value = false }
      )
    }

  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
  onDateSelected: (LocalDate) -> Unit,
  onDismissRequest: () -> Unit
) {
  val datePickerState = rememberDatePickerState()
  val selectedDate = datePickerState.selectedDateMillis?.let {
    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
  }

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(
      dismissOnBackPress = true,
      dismissOnClickOutside = true
    )
  ) {
    Surface(shape = MaterialTheme.shapes.extraLarge) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        DatePicker(
          state = datePickerState,
          title = null,
          headline = null,
          showModeToggle = false,
          colors = DatePickerDefaults.colors(),
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          TextButton(onClick = onDismissRequest) {
            Text(text = "Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(onClick = {
            selectedDate?.let { onDateSelected(it) }
            onDismissRequest()
          },
            enabled = selectedDate != null
          ) {
            Text(text = "OK")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MultiSelectDropdown(
  modifier: Modifier,
  label: String,
  options: List<String>,
  selectedOptions: Set<String>,
  onSelectionChanged: (Set<String>) -> Unit,
  isError: Boolean = false,
  supportingText: @Composable () -> Unit = {}
) {
  var expanded by remember { mutableStateOf(false) }

  Column(modifier = modifier) { // Wrap the InputChip area in a Column
    // Display Selected Options as Input Chips
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(-12.dp)
    ) {
      selectedOptions.forEach { option ->
        InputChip(
          selected = true,
          onClick = { onSelectionChanged(selectedOptions - option) },
          label = {
            Text(
              text = option,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          },
          trailingIcon = {
            Icon(
              Icons.Default.Close,
              contentDescription = null,
              Modifier.size(InputChipDefaults.IconSize)
            )
          },
          modifier = Modifier.padding(2.dp)
        )
      }
    }

    ExposedDropdownMenuBox(
      expanded = false,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        readOnly = true,
        value = selectedOptions.joinToString(", "),
        onValueChange = { },
        label = { Text(label) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        isError = isError,
        supportingText = supportingText
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        options.forEach { option ->
          DropdownMenuItem(
            text = { Text(text = option) },
            onClick = {
              onSelectionChanged(
                if (option in selectedOptions) selectedOptions - option else selectedOptions + option
              )
//              expanded = false
            },
            trailingIcon = if (option in selectedOptions) {
              {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  modifier = Modifier.size(20.dp)
                )
              }
            } else null
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(
  label: String,
  options: List<String>,
  selectedValue: String,
  onValueChanged: (String) -> Unit,
  isError: Boolean = false,
  supportingText: @Composable () -> Unit = {}
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = Modifier.fillMaxWidth()
  ) {
    OutlinedTextField(
      readOnly = true,
      value = selectedValue,
      onValueChange = { },
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = Modifier.menuAnchor().fillMaxWidth(),
      isError = isError,
      supportingText = supportingText
    )
    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      options.forEach { selectionOption ->
        DropdownMenuItem(
          text = { Text(selectionOption) },
          onClick = {
            onValueChanged(selectionOption)
            expanded = false
          }
        )
      }
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactPeopleDropdown(
  modifier: Modifier,
  label: String,
  members: List<Member>,
  selectedMembers: Set<String>,
  onSelectionChanged: (Set<String>) -> Unit,
  isError: Boolean = false,
  supportingText: @Composable () -> Unit = {}
) {
  var expanded by remember { mutableStateOf(false) }
  val context = LocalPlatformContext.current  // Needed for Coil

  Column(modifier = modifier) { // Wrap the InputChip area in a Column
    // Display Selected Members as Input Chips
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(-12.dp)
    ) {
      members.filter { it.id in selectedMembers }.forEach { member ->
        InputChip(
          selected = true,
          onClick = { onSelectionChanged(selectedMembers - member.id) },
          label = { Text(member.name) },
          //onDismiss = { onSelectionChanged(selectedMembers - member.id) },
          modifier = Modifier.padding(2.dp),
          leadingIcon = {
            if (member.profileImage.isNotEmpty()) {
              AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                  .data(member.profileImage)
                  .crossfade(true)
                  .build(),
                contentDescription = "Profile Image",
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape),
                contentScale = ContentScale.Crop
              )
            } else {
              Icon(Icons.Filled.Face, contentDescription = "Profile", tint = Color.Gray)
            }
          },
          trailingIcon = {
            Icon(
              Icons.Default.Close,
              contentDescription = null,
              Modifier.size(InputChipDefaults.IconSize)
            )
          },
        )
      }
    }

    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        readOnly = true,
        value = members.filter { it.id in selectedMembers }.joinToString(", ") { it.name },
        onValueChange = { },
        label = { Text(label) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        isError = isError,
        supportingText = supportingText
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        members.forEach { member ->
          DropdownMenuItem(
            text = { Text(text = member.name) },
            onClick = {
              onSelectionChanged(
                if (member.id in selectedMembers) selectedMembers - member.id else selectedMembers + member.id
              )
//              expanded = false
            },
            leadingIcon = {
              if (member.profileImage.isNotEmpty()) {
                AsyncImage(
                  model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(member.profileImage)
                    .crossfade(true)
                    .build(),
                  contentDescription = "Profile Image",
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(Icons.Filled.Face, contentDescription = "Profile", tint = Color.Gray)
              }
            },
            trailingIcon = if (member.id in selectedMembers) {
              {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  modifier = Modifier.size(20.dp)
                )
              }
            } else null
          )
        }
      }
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
  onTimeSelected: (LocalTime) -> Unit,
  onDismissRequest: () -> Unit
) {
  val initialTime = LocalTime(12, 0) // Set initial time to noon
  val timePickerState = rememberTimePickerState(
    initialHour = initialTime.hour,
    initialMinute = initialTime.minute,
    is24Hour = true
  )

  Dialog(onDismissRequest = onDismissRequest) {
    Surface(shape = MaterialTheme.shapes.extraLarge) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TimePicker(state = timePickerState)
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
          TextButton(onClick = onDismissRequest) {
            Text(text = "Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(onClick = {
            onTimeSelected(LocalTime(timePickerState.hour, timePickerState.minute))
            onDismissRequest()
          }) {
            Text(text = "OK")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
  value: TextFieldValue,
  label: String,
  modifier: Modifier = Modifier,
  readOnly: Boolean = false,
  onClick: () -> Unit = {},
  isError: Boolean = false,
  supportingText: @Composable () -> Unit = {}
) {


  OutlinedTextField(
    value = value,
    onValueChange = {  },
    label = { Text(label) },
    modifier = modifier,
    readOnly = readOnly,
    trailingIcon = {
      IconButton(onClick = onClick) {
        Icon(if(label.contains("दिनांक")) Icons.Filled.DateRange else Icons.Filled.Schedule, contentDescription = "Select Date")
      }
    },
    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
      .also { interactionSource ->
        LaunchedEffect(interactionSource) {
          interactionSource.interactions.collect { interaction ->
            if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
              onClick()
            }
          }
        }
      },
    isError = isError,
    supportingText = supportingText
  )


}

@Preview
@Composable
fun ActivityFormPreview() {
  ActivityForm(formData1 = mockFormData1)  //Use the pre-parsed data
}