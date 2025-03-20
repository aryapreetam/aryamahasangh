package org.aryamahasangh.screens

import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.aryamahasangh.AddOrganisationActivityMutation
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.OrganisationsAndMembersQuery
import org.aryamahasangh.OrganisationsAndMembersQuery.Member
import org.aryamahasangh.OrganisationsAndMembersQuery.Organisation
import org.aryamahasangh.network.apolloClient
import org.aryamahasangh.network.bucket
import org.aryamahasangh.type.ActivityType
import org.aryamahasangh.type.OrganisationActivityInput

val stringToActivityTypeMap = mapOf(
  "कक्षा" to ActivityType.COURSE,
  "कार्यक्रम" to ActivityType.EVENT,
  "अभियान" to ActivityType.CAMPAIGN,
  "सत्र" to ActivityType.SESSION,
)

val activityTypeToStringMap = mapOf(
  ActivityType.COURSE to "कक्षा",
  ActivityType.EVENT to "कार्यक्रम",
  ActivityType.CAMPAIGN to "अभियान",
  ActivityType.SESSION to "सत्र",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityForm() { // Take FormData object directly

  var organisations by remember { mutableStateOf(emptyList<OrganisationsAndMembersQuery.Organisation>()) }
  var members by remember { mutableStateOf(emptyList<OrganisationsAndMembersQuery.Member>()) }

  LaunchedEffect(Unit) {
    val orgsAndMembers = apolloClient.query(OrganisationsAndMembersQuery()).execute()
    organisations = orgsAndMembers.data?.organisations ?: emptyList()
    members = orgsAndMembers.data?.members ?: emptyList()
  }

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

  var associatedOrganisations by remember { mutableStateOf(emptySet<Organisation>()) }
  var associatedOrganisationsError by remember { mutableStateOf(false) }

  var address by remember { mutableStateOf("") }
  var addressError by remember { mutableStateOf(false) }

  var state by remember { mutableStateOf("") }
  var stateError by remember { mutableStateOf(false) }

  var district by remember { mutableStateOf("") }
  var districtError by remember { mutableStateOf(false) }

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


  var contactPeople by remember { mutableStateOf(emptySet<Member>()) }
  var contactPeopleError by remember { mutableStateOf(false) }

  var additionalInstructions by remember { mutableStateOf("") }

  var isSubmitting by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  // Date Picker Dialog State
  val openStartDateDialog = remember { mutableStateOf(false) }
  val openEndDateDialog = remember { mutableStateOf(false) }

  val openStartTimeDialog = remember { mutableStateOf(false) }
  val openEndTimeDialog = remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  var postMap by remember { mutableStateOf<MutableMap<String, Pair<String, Int>>>(mutableMapOf()) }

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

  @Serializable
  data class InsertResponse(
    val id: Int // Only the ID will be returned
  )

  fun submitForm() {

    if (validateForm()) {
      isSubmitting = true
      scope.launch {
        val attachedImages = mutableListOf<String>()
        try {
          attachedDocuments.forEach {
            val uploadResponse = bucket.upload(
              path = "${Clock.System.now().epochSeconds}.jpg",
              data = it.readBytes()
            )
            attachedImages.add(bucket.publicUrl(uploadResponse.path))
          }
        }catch (e: Exception) {
          snackbarHostState.showSnackbar(
            message = "Error uploading images. Please try again",
            actionLabel = "Close"
          )
          println("error uploading files: $e")
          return@launch
        }

        val inp =  OrganisationActivityInput(
          name = name,
          shortDescription = shortDescription,
          longDescription = description,
          activityType = stringToActivityTypeMap[selectedType!!]!!,
          address = address,
          state = state,
          district = district,
          pincode = pincode.toInt(),
          associatedOrganisations = associatedOrganisations.map { it.id },
          startDateTime = startDate?.atTime(startTime!!).toString(),
          endDateTime = endDate?.atTime(endTime!!).toString(),
          mediaFiles = attachedImages,
          contactPeople =  contactPeople.map {
            val (role, priority) = postMap[it.id] ?: Pair("", 0)
            listOf(it.id, role, priority.toString()) },
          additionalInstructions = additionalInstructions
        )
        val res = apolloClient.mutation(AddOrganisationActivityMutation(
         inp
        )).execute()

        if(!res.hasErrors()){
          snackbarHostState.showSnackbar(
            message = "A new activity has been created successfully.",
          )
        }
        println("res: ${res}")

        isSubmitting = false

        // Reset form
        name = ""
        selectedType = null
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
        postMap.clear()
        attachedDocuments = listOf()

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
      options = organisations,
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
      modifier = Modifier.fillMaxWidth(),
      label = "संपर्क सूत्र",
      members = members,
      selectedMembers = contactPeople,
      onSelectionChanged = {
        contactPeople = it
        contactPeopleError = it.isEmpty()
      },
      postMap = postMap,
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
  options: List<Organisation>,
  selectedOptions: Set<Organisation>,
  onSelectionChanged: (Set<Organisation>) -> Unit,
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
              text = option.name,
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
        value = selectedOptions.joinToString(", ") { it.name },
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
            text = { Text(text = option.name) },
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



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactPeopleDropdown(
  modifier: Modifier,
  label: String,
  members: List<Member>,
  selectedMembers: Set<Member>,
  onSelectionChanged: (Set<Member>) -> Unit,
  isError: Boolean = false,
  supportingText: @Composable () -> Unit = {},
  postMap: MutableMap<String, Pair<String, Int>>
) {
  var expanded by remember { mutableStateOf(false) }
  val context = LocalPlatformContext.current  // Needed for Coil

  Column(modifier = modifier) { // Wrap the InputChip area in a Column
    // Display Selected Members as Input Chips
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      members.filter { it in selectedMembers }.forEachIndexed { index, member ->
        var text by remember { mutableStateOf(postMap[member.id]?.first ?: "") }
        InputChip(
          selected = true,
          onClick = {
            // handled in close button
          },
          label = {
            Row(
              modifier = Modifier.padding(vertical = 12.dp),
              Arrangement.spacedBy(8.dp)
            ) {
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
              Column{
                Text(member.name)
                OutlinedTextField(
                  modifier = Modifier.width(200.dp),
                  value = text,
                  onValueChange = {
                    text = it
                    postMap[member.id] = Pair(it, index)
                  },
                  label = { Text("Role") },
                  placeholder = { Text("संयोजक, कोषाध्यक्ष इत्यादि") }
                )
              }
              Box(
                modifier = Modifier.size(36.dp).clickable {
                  onSelectionChanged(selectedMembers - member)
                },
                contentAlignment = Alignment.Center
              ){
                Icon(
                  Icons.Default.Close,
                  contentDescription = null,
                  Modifier.size(24.dp)
                )
              }
            }
          },
          //onDismiss = { onSelectionChanged(selectedMembers - member.id) },
          modifier = Modifier.padding(2.dp),
        )
      }
    }

    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
      modifier = Modifier.width(500.dp)
    ) {
      OutlinedTextField(
        readOnly = true,
        value = members.filter { it in selectedMembers }.joinToString(", ") { it.name },
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
                if (member in selectedMembers) selectedMembers - member else selectedMembers + member
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
            trailingIcon = if (member in selectedMembers) {
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