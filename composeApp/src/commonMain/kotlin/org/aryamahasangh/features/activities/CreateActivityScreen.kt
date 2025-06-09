package org.aryamahasangh.features.activities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.Clock.System
import org.aryamahasangh.components.PhotoItem
import org.aryamahasangh.navigation.LocalSetBackHandler
import org.aryamahasangh.navigation.LocalSnackbarHostState
import org.aryamahasangh.network.bucket
import org.aryamahasangh.screens.ButtonForFilePicker
import org.aryamahasangh.screens.DistrictDropdown
import org.aryamahasangh.screens.StateDropdown
import org.aryamahasangh.screens.indianStatesToDistricts

// Wrapper class to handle both new files and existing URLs
// No MediaFile sealed class needed - we'll track existing and new files separately

// Removed MediaDocumentGrid - using DocumentGrid component instead

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
    onValueChange = { },
    label = { Text(label) },
    modifier = modifier,
    readOnly = readOnly,
    trailingIcon = {
      IconButton(onClick = onClick) {
        Icon(
          if (label.contains("दिनांक")) Icons.Filled.DateRange else Icons.Filled.Schedule,
          contentDescription = "Select Date"
        )
      }
    },
    interactionSource =
      remember { MutableInteractionSource() }
        .also { interactionSource ->
          LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
              if (interaction is PressInteraction.Release) {
                onClick()
              }
            }
          }
        },
    isError = isError,
    supportingText = supportingText
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
  onDateSelected: (LocalDate) -> Unit,
  onDismissRequest: () -> Unit,
  disablePastDates: Boolean = true
) {
  val today = System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

  val datePickerState =
    rememberDatePickerState(
      selectableDates =
        if (disablePastDates) {
          object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
              val selectedDate =
                Instant.fromEpochMilliseconds(utcTimeMillis)
                  .toLocalDateTime(TimeZone.currentSystemDefault()).date
              return selectedDate >= today // Allow only future dates
            }
          }
        } else {
          DatePickerDefaults.AllDates
        }
    )
  val selectedDate =
    datePickerState.selectedDateMillis?.let {
      Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

  DatePickerDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      TextButton(
        onClick = {
          onDismissRequest()
          val selectedDate1 =
            datePickerState.selectedDateMillis?.let {
              Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()).date
            }
          selectedDate1?.let {
            onDateSelected(it)
          }
        },
        enabled = selectedDate != null
      ) {
        Text("OK")
      }
    },
    dismissButton = {
      TextButton(onClick = {
        onDismissRequest()
      }) {
        Text("Cancel")
      }
    }
  ) {
    DatePicker(
      state = datePickerState
    )
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
      onExpandedChange = { expanded = !expanded }
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
        onDismissRequest = { expanded = false }
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
            trailingIcon =
              if (option in selectedOptions) {
                {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp)
                  )
                }
              } else {
                null
              }
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
  postMap: MutableMap<String, Pair<String, Int>>,
  onFieldFocused: ((Float) -> Unit)? = null
) {
  var expanded by remember { mutableStateOf(false) }
  val context = LocalPlatformContext.current // Needed for Coil

  Column(modifier = modifier) { // Wrap the InputChip area in a Column
    // Display Selected Members as Input Chips
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
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
              if (!member.profileImage.isNullOrEmpty()) {
                AsyncImage(
                  model =
                    ImageRequest.Builder(LocalPlatformContext.current)
                      .data(member.profileImage)
                      .crossfade(true)
                      .build(),
                  contentDescription = "Profile Image",
                  modifier =
                    Modifier
                      .size(24.dp)
                      .clip(CircleShape),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(
                  modifier = Modifier.size(24.dp),
                  imageVector = Icons.Filled.Face,
                  contentDescription = "Profile",
                  tint = Color.Gray
                )
              }
              Column {
                Text(member.name)
                var fieldCoordinates by remember { mutableStateOf(0f) }
                OutlinedTextField(
                  modifier = Modifier
                    .width(200.dp)
                    .onGloballyPositioned { coordinates ->
                      fieldCoordinates = coordinates.positionInRoot().y
                    }
                    .onFocusChanged { focusState ->
                      if (focusState.isFocused && onFieldFocused != null) {
                        // Use stored coordinates
                        onFieldFocused(fieldCoordinates)
                      }
                    },
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
                modifier =
                  Modifier.size(36.dp).clickable {
                    onSelectionChanged(selectedMembers - member)
                  },
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = null,
                  Modifier.size(24.dp)
                )
              }
            }
          },
          // onDismiss = { onSelectionChanged(selectedMembers - member.id) },
          modifier = Modifier.padding(2.dp)
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
        onDismissRequest = { expanded = false }
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
              if (!member.profileImage.isNullOrEmpty()) {
                AsyncImage(
                  model =
                    ImageRequest.Builder(LocalPlatformContext.current)
                      .data(member.profileImage)
                      .crossfade(true)
                      .build(),
                  contentDescription = "Profile Image",
                  modifier =
                    Modifier
                      .size(36.dp)
                      .clip(CircleShape),
                  contentScale = ContentScale.Crop
                )
              } else {
                Icon(
                  modifier = Modifier.size(36.dp),
                  imageVector = Icons.Filled.Face,
                  contentDescription = "Profile",
                  tint = Color.Gray
                )
              }
            },
            trailingIcon =
              if (member in selectedMembers) {
                {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp)
                  )
                }
              } else {
                null
              }
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
  val timePickerState =
    rememberTimePickerState(
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

val genderAllowedDisplayOptions = GenderAllowed.entries.map { it.toDisplayName() } // List of display names


@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMaterial3Api
@Composable
fun CreateActivityScreen(
  viewModel: ActivitiesViewModel,
  editingActivityId: String? = null,
  onActivitySaved: (String) -> Unit = {},
  onCancel: () -> Unit = {}
) {


  var organisations by remember { mutableStateOf(emptyList<Organisation>()) }
  var members by remember { mutableStateOf(emptyList<Member>()) }

  // State for editing activity
  var isLoadingActivity by remember { mutableStateOf(false) }
  var editingActivity by remember { mutableStateOf<OrganisationalActivity?>(null) }

  // Collect organizations and members from ViewModel
  val organisationsAndMembersState by viewModel.organisationsAndMembersState.collectAsState()

  // Update local state when ViewModel state changes
  LaunchedEffect(organisationsAndMembersState) {
    organisations = organisationsAndMembersState.organisations
    members = organisationsAndMembersState.members
  }

  // Load organizations and members when the screen is shown
  LaunchedEffect(Unit) {
    viewModel.loadOrganisationsAndMembers()
  }

  // Load activity details if editing
  LaunchedEffect(editingActivityId) {
    if (editingActivityId != null) {
      isLoadingActivity = true
      viewModel.loadActivityDetail(editingActivityId)
    }
  }

  // Watch for activity details when editing
  val activityDetailState by viewModel.activityDetailUiState.collectAsState()
  LaunchedEffect(activityDetailState) {
    if (editingActivityId != null && activityDetailState.activity != null) {
      editingActivity = activityDetailState.activity
      isLoadingActivity = false
    }
  }

  // State variables for form fields
  var name by remember { mutableStateOf("") }
  var nameError by remember { mutableStateOf(false) }

  var selectedType by remember { mutableStateOf<ActivityType?>(null) } // Only one can be selected

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

  var stateErrorMessage by remember { mutableStateOf("") }
  var districtErrorMessage by remember { mutableStateOf("") }

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

  var startDateTimeErrorMessage by remember { mutableStateOf("") }
  var endDateTimeErrorMessage by remember { mutableStateOf("") }

  var attachedDocuments by remember { mutableStateOf(emptyList<PlatformFile>()) }
  var attachedDocumentsError by remember { mutableStateOf(false) }
  var attachedDocumentsErrorMessage by remember { mutableStateOf("") }

  // Track URLs of existing media files to preserve during updates
  var existingMediaUrls by remember { mutableStateOf(emptyList<String>()) }
  var deletedMediaUrls by remember { mutableStateOf(emptySet<String>()) }

  var contactPeople by remember { mutableStateOf(emptySet<Member>()) }
  var contactPeopleError by remember { mutableStateOf(false) }

  var additionalInstructions by remember { mutableStateOf("") }

  var eventCapacity by remember { mutableStateOf("100") }
  var eventGenderAllowed by remember { mutableStateOf(GenderAllowed.ANY) }
  var eventLatitude by remember { mutableStateOf("") }
  var eventLongitude by remember { mutableStateOf("") }

  // Map dialog state
  var showMapDialog by remember { mutableStateOf(false) }

  // Add error states for event details
  var capacityError by remember { mutableStateOf<String?>(null) }
  var genderAllowedError by remember { mutableStateOf<String?>(null) }
  var genderAllowedExpanded by remember { mutableStateOf(false) }
  var latitudeError by remember { mutableStateOf<String?>(null) }
  var longitudeError by remember { mutableStateOf<String?>(null) }

  // Focus requesters
  val capacityFocusRequester = remember { FocusRequester() }
  val latitudeFocusRequester = remember { FocusRequester() }
  val longitudeFocusRequester = remember { FocusRequester() }

  val focusManager = LocalFocusManager.current

  // Initial form values
  var initialFormValues by remember { mutableStateOf<Map<String, Any>?>(null) }

  // Unsaved changes tracking
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }

  fun hasUnsavedChanges(): Boolean {
    if (initialFormValues == null) return false

    val currentValues = mapOf(
      "name" to name,
      "selectedType" to selectedType,
      "shortDescription" to shortDescription,
      "description" to description,
      "associatedOrganisations" to associatedOrganisations,
      "address" to address,
      "state" to state,
      "district" to district,
      "startDate" to startDate,
      "startTime" to startTime,
      "endDate" to endDate,
      "endTime" to endTime,
      "contactPeople" to contactPeople,
      "additionalInstructions" to additionalInstructions,
      "eventCapacity" to eventCapacity,
      "eventGenderAllowed" to eventGenderAllowed,
      "eventLatitude" to eventLatitude,
      "eventLongitude" to eventLongitude,
      "attachedDocuments" to attachedDocuments
    )

    return currentValues != initialFormValues
  }

  // Handle back button
  val setBackHandler = LocalSetBackHandler.current
  DisposableEffect(Unit) {
    val handler = {
      if (hasUnsavedChanges()) {
        showUnsavedChangesDialog = true
      } else {
        onCancel()
      }
    }
    setBackHandler?.invoke(handler)
    onDispose {
      setBackHandler?.invoke(null)
    }
  }

  // Collect form submission state from ViewModel
  val formSubmissionState by viewModel.activityFormSubmissionState.collectAsState()
  val isSubmitting = formSubmissionState.isSubmitting
  val createdActivityId by viewModel.createdActivityId.collectAsState()


  val scrollState = rememberScrollState()

  // Date Picker Dialog State
  val openStartDateDialog = remember { mutableStateOf(false) }
  val openEndDateDialog = remember { mutableStateOf(false) }

  val openStartTimeDialog = remember { mutableStateOf(false) }
  val openEndTimeDialog = remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  var postMap by remember { mutableStateOf<MutableMap<String, Pair<String, Int>>>(mutableMapOf()) }

  // Add keyboard-aware scrolling
  val keyboardController = LocalSoftwareKeyboardController.current
  var lastFocusedFieldOffset by remember { mutableStateOf(0f) }

  // Helper function to scroll to focused field
  fun scrollToFocusedField(offset: Float) {
    scope.launch {
      // Small delay to ensure keyboard is shown
      kotlinx.coroutines.delay(300)
      // Calculate target scroll position to show field above keyboard
      // Add extra padding (300dp) to ensure field is well above keyboard
      val keyboardPadding = 300.dp.value
      val viewportHeight = scrollState.viewportSize
      val currentScroll = scrollState.value

      // Calculate where the field should be positioned (above keyboard)
      val targetFieldPosition = viewportHeight - keyboardPadding

      // Calculate how much we need to scroll
      val fieldBottomPosition = offset + 100 // Add some height for the field itself
      val scrollNeeded = fieldBottomPosition - targetFieldPosition - currentScroll

      if (scrollNeeded > 0) {
        scrollState.animateScrollTo((currentScroll + scrollNeeded).toInt())
      }
    }
  }

  // Date format
  // Date format
  val dateFormatter: (LocalDate) -> String = { date ->
    "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
  }

  // Time format
  val timeFormatter: (LocalTime) -> String = { time ->
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
  }

  // Populate form with editing activity data
  LaunchedEffect(editingActivity) {
    if (editingActivity != null) {
      val activity = editingActivity!!
      name = activity.name
      selectedType = activity.type
      shortDescription = activity.shortDescription
      description = activity.longDescription
      associatedOrganisations = activity.associatedOrganisations.map { it.organisation }.toSet()
      address = activity.address
      state = activity.state
      district = activity.district

      // Parse dates and times
      startDate = activity.startDatetime.date
      startTime = activity.startDatetime.time
      startDateText = TextFieldValue(dateFormatter(startDate!!))
      startTimeText = TextFieldValue(timeFormatter(startTime!!))

      endDate = activity.endDatetime.date
      endTime = activity.endDatetime.time
      endDateText = TextFieldValue(dateFormatter(endDate!!))
      endTimeText = TextFieldValue(timeFormatter(endTime!!))

      // Contact people - Fix: Map the Member objects correctly
      contactPeople = activity.contactPeople.map { activityMember ->
        // Find the full member object from the members list if available
        members.find { it.id == activityMember.member.id } ?: activityMember.member
      }.toSet()

      // Clear and repopulate postMap
      postMap.clear()
      activity.contactPeople.forEach { activityMember ->
        postMap[activityMember.member.id] = Pair(activityMember.post, activityMember.priority)
      }

      additionalInstructions = activity.additionalInstructions
      eventCapacity = activity.capacity.toString()
      eventGenderAllowed = GenderAllowed.fromDisplayName(activity.allowedGender) ?: GenderAllowed.ANY
      eventLatitude = activity.latitude?.toString() ?: ""
      eventLongitude = activity.longitude?.toString() ?: ""

      // Debug: Print latitude/longitude values
      println("Edit mode - Loading lat/lng: lat=$eventLatitude, lng=$eventLongitude, activity.lat=${activity.latitude}, activity.lng=${activity.longitude}")

      // Store existing media URLs separately
      existingMediaUrls = activity.mediaFiles
      attachedDocuments = emptyList() // Start with no new files

      // Store initial values for unsaved changes detection
      initialFormValues = mapOf(
        "name" to name,
        "selectedType" to selectedType,
        "shortDescription" to shortDescription,
        "description" to description,
        "associatedOrganisations" to associatedOrganisations,
        "address" to address,
        "state" to state,
        "district" to district,
        "startDate" to startDate,
        "startTime" to startTime,
        "endDate" to endDate,
        "endTime" to endTime,
        "contactPeople" to contactPeople,
        "additionalInstructions" to additionalInstructions,
        "eventCapacity" to eventCapacity,
        "eventGenderAllowed" to eventGenderAllowed,
        "eventLatitude" to eventLatitude,
        "eventLongitude" to eventLongitude,
        "attachedDocuments" to attachedDocuments
      ) as Map<String, Any>
    }
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
    startDateError = startDate == null
    startTimeError = startTime == null
    endDateError = endDate == null
    endTimeError = endTime == null
    contactPeopleError = contactPeople.isEmpty()

    stateErrorMessage = if (stateError) "State required" else ""
    districtErrorMessage = if (districtError) "Districts required" else ""

    // Validate event details
    var eventDetailsValid = true

    // Validate capacity
    val capInt = eventCapacity.toIntOrNull()
    when {
      eventCapacity.isBlank() -> {
        capacityError = "क्षमता आवश्यक है."
        eventDetailsValid = false
      }

      capInt == null -> {
        capacityError = "कृपया मान्य संख्या दर्ज करें."
        eventDetailsValid = false
      }

      capInt <= 0 -> {
        capacityError = "क्षमता 0 से अधिक होनी चाहिए."
        eventDetailsValid = false
      }

      else -> {
        capacityError = null
      }
    }

    // Validate latitude
    val latDouble = eventLatitude.toDoubleOrNull()
    when {
      eventLatitude.isBlank() -> {
        latitudeError = "अक्षांश आवश्यक है."
        eventDetailsValid = false
      }

      latDouble == null -> {
        latitudeError = "कृपया मान्य अक्षांश दर्ज करें (उदा. 28.6139)."
        eventDetailsValid = false
      }

      latDouble < -90.0 || latDouble > 90.0 -> {
        latitudeError = "अक्षांश -90 और 90 के बीच होना चाहिए."
        eventDetailsValid = false
      }

      else -> {
        latitudeError = null
      }
    }

    // Validate longitude
    val lonDouble = eventLongitude.toDoubleOrNull()
    when {
      eventLongitude.isBlank() -> {
        longitudeError = "देशांतर आवश्यक है."
        eventDetailsValid = false
      }

      lonDouble == null -> {
        longitudeError = "कृपया मान्य देशांतर दर्ज करें (उदा. 77.2090)."
        eventDetailsValid = false
      }

      lonDouble < -180.0 || lonDouble > 180.0 -> {
        longitudeError = "देशांतर -180 और 180 के बीच होना चाहिए."
        eventDetailsValid = false
      }

      else -> {
        longitudeError = null
      }
    }

    if ((!startDateError && !startTimeError && !endDateError && !endTimeError)) {
      val currentDateTime = System.now().toLocalDateTime(TimeZone.currentSystemDefault())
      val startDateTime = startDate?.atTime(startTime!!)!!
      val endDateTime = endDate?.atTime(endTime!!)!!

      if (!(
          startDateTime < endDateTime &&
            startDateTime > currentDateTime && endDateTime > currentDateTime
          )
      ) {
        if (startDateTime >= endDateTime) {
          startDateError = true
          startTimeError = true
          endDateError = true
          endTimeError = true
          startDateTimeErrorMessage = "Start datetime should be before end datetime"
          endDateTimeErrorMessage = "Start datetime should be before end datetime"
        } else if (startDateTime <= currentDateTime) {
          startDateError = true
          startTimeError = true
          startDateTimeErrorMessage = "Start datetime should be after current datetime"
        } else if (endDateTime <= currentDateTime) {
          endDateError = true
          endTimeError = true
          endDateTimeErrorMessage = "End datetime should be after current datetime"
        } else {
          startDateError = false
          startTimeError = false
          endDateError = false
          endTimeError = false
          startDateTimeErrorMessage = ""
          endDateTimeErrorMessage = ""
        }
      } else {
        startDateError = false
        startTimeError = false
        endDateError = false
        endTimeError = false
        startDateTimeErrorMessage = ""
        endDateTimeErrorMessage = ""
      }
    }

    return !(
      nameError || typesError || shortDescriptionError || descriptionError || associatedOrganisationsError ||
        addressError || stateError || districtError || startDateError || startTimeError ||
        endDateError || endTimeError || contactPeopleError || !eventDetailsValid
      )
  }

  fun submitForm() {
    if (validateForm()) {
      scope.launch {
        val attachedImages = mutableListOf<String>()

        // Keep existing files that weren't deleted
        attachedImages.addAll(existingMediaUrls.filterNot { it in deletedMediaUrls })

        // Delete files marked for deletion from bucket
        if (deletedMediaUrls.isNotEmpty()) {
          try {
            val filesToDelete = deletedMediaUrls.map { url ->
              url.substringAfterLast("/")
            }.toList()
            bucket.delete(filesToDelete)
          } catch (e: Exception) {
            snackbarHostState.showSnackbar(
              message = "चित्र हटाने में त्रुटि: ${e.message}",
              actionLabel = "बंद करें"
            )
            // Continue with the update even if deletion fails
          }
        }

        // Upload new files
        try {
          attachedDocuments.forEach {
            val uploadResponse =
              bucket.upload(
                path = "${System.now().epochSeconds}.jpg",
                data = it.readBytes()
              )
            attachedImages.add(bucket.publicUrl(uploadResponse.path))
          }
        } catch (e: Exception) {
          snackbarHostState.showSnackbar(
            message = "चित्र अपलोड करने में त्रुटि। कृपया पुनः प्रयास करें",
            actionLabel = "बंद करें"
          )
          println("error uploading files: $e")
          return@launch
        }
        val inp =
          ActivityInputData(
            name = name,
            shortDescription = shortDescription,
            longDescription = description,
            type = selectedType!!,
            address = address,
            state = state,
            district = district,
            associatedOrganisations = associatedOrganisations.toList(),
            startDatetime = startDate?.atTime(startTime!!)!!,
            endDatetime = endDate?.atTime(endTime!!)!!,
            mediaFiles = attachedImages,
            contactPeople =
              contactPeople.map {
                val (role, priority) = postMap[it.id] ?: Pair("", 0)
                ActivityMember(it.id, role, it, priority)
              },
            additionalInstructions = additionalInstructions,
            capacity = eventCapacity.toIntOrNull() ?: 0,
            allowedGender = eventGenderAllowed,
            latitude = eventLatitude.toDoubleOrNull() ?: 0.0,
            longitude = eventLongitude.toDoubleOrNull() ?: 0.0
          )

        // Submit form using ViewModel
        if (editingActivityId != null) {
          viewModel.updateActivity(editingActivityId, inp)
        } else {
          viewModel.createActivity(inp)
        }
      }
    }
  }

  // Show loading while fetching activity details
  if (isLoadingActivity) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        CircularProgressIndicator()
        Text("गतिविधि विवरण लोड हो रहा है...")
      }
    }
    return
  }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(8.dp)
          .verticalScroll(scrollState)
          .imePadding() // Add IME padding to push content above keyboard
    ) {
      // Header with Cancel button
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (editingActivityId != null) "गतिविधि संपादित करें" else "नई गतिविधि बनाएं",
          style = MaterialTheme.typography.headlineSmall
        )
        TextButton(
          onClick = {
            if (hasUnsavedChanges()) {
              showUnsavedChangesDialog = true
            } else {
              onCancel()
            }
          }
        ) {
          Text("रद्द करें")
        }
      }
      // Name
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("नाम") },
        modifier = Modifier
          .width(500.dp)
          .onGloballyPositioned { coordinates ->
            lastFocusedFieldOffset = coordinates.positionInRoot().y
          }
          .onFocusChanged { focusState ->
            if (focusState.isFocused) {
              scrollToFocusedField(lastFocusedFieldOffset)
            }
          },
        isError = nameError,
        supportingText = {
          if (nameError) {
            Text("Name is required")
          }
        },
        maxLines = 1,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )

      Column {
        // Type (Filter Chips)
        Text(text = "प्रकार :", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ActivityType.entries.forEach { type ->
            FilterChip(
              selected = (selectedType == type), // Check for equality, not `in`
              onClick = {
                selectedType =
                  if (selectedType == type) {
                    null // Unselect if already selected
                  } else {
                    type // Select new type and unselect the older
                  }
                typesError = selectedType == null // Update error status
              },
              label = { Text(type.toDisplayName()) },
              leadingIcon =
                if (selectedType == type) {
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
              border =
                FilterChipDefaults.filterChipBorder(
                  borderColor = MaterialTheme.colorScheme.outline,
                  selectedBorderColor = MaterialTheme.colorScheme.primary,
                  enabled = selectedType == type,
                  selected = selectedType == type
                )
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
        modifier = Modifier
          .width(500.dp)
          .onGloballyPositioned { coordinates ->
            lastFocusedFieldOffset = coordinates.positionInRoot().y
          }
          .onFocusChanged { focusState ->
            if (focusState.isFocused) {
              scrollToFocusedField(lastFocusedFieldOffset)
            }
          },
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
        modifier = Modifier
          .width(500.dp)
          .onGloballyPositioned { coordinates ->
            lastFocusedFieldOffset = coordinates.positionInRoot().y
          }
          .onFocusChanged { focusState ->
            if (focusState.isFocused) {
              scrollToFocusedField(lastFocusedFieldOffset)
            }
          },
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
          associatedOrganisationsError = it.isEmpty() // Update error on selection change
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
        modifier = Modifier
          .width(500.dp)
          .onGloballyPositioned { coordinates ->
            lastFocusedFieldOffset = coordinates.positionInRoot().y
          }
          .onFocusChanged { focusState ->
            if (focusState.isFocused) {
              scrollToFocusedField(lastFocusedFieldOffset)
            }
          },
        minLines = 2,
        maxLines = 3,
        isError = addressError,
        supportingText = { if (addressError) Text("Address is required") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        StateDropdown(
          states = indianStatesToDistricts.keys.toList(),
          selectedState = state,
          onStateSelected = {
            state = it
            stateError = false
          },
          modifier = Modifier.width(160.dp),
          isError = stateError,
          errorMessage = if (stateError) stateErrorMessage else ""
        )
        // District Selection (Conditional)
        val districts = indianStatesToDistricts[state] ?: emptyList()
        DistrictDropdown(
          districts = districts,
          selectedDistrict = district,
          onDistrictSelected = {
            district = it ?: ""
            districtError = false
          },
          modifier = Modifier.width(200.dp),
          isError = districtError,
          errorMessage = if (districtError) districtErrorMessage else ""
        )
      }
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Column {
          Text(text = "प्रारंभ:", style = MaterialTheme.typography.bodyMedium)
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            CustomTextField(
              value = startDateText,
              label = "दिनांक",
              modifier = Modifier.width(180.dp),
              readOnly = true,
              onClick = { openStartDateDialog.value = true },
              isError = startDateError,
              supportingText = {
                if (startDateError) {
                  Text(
                    if (startDateTimeErrorMessage.isEmpty()) "Start date is required" else startDateTimeErrorMessage
                  )
                }
              }
            )

            CustomTextField(
              value = startTimeText,
              label = "समय",
              modifier = Modifier.width(180.dp),
              readOnly = true,
              onClick = { openStartTimeDialog.value = true },
              isError = startTimeError,
              supportingText = {
                if (startTimeError) {
                  Text(
                    if (startDateTimeErrorMessage.isEmpty()) "Start time is required" else startDateTimeErrorMessage
                  )
                }
              }
            )
          }
        }

        Column {
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
              supportingText = {
                if (endDateError) {
                  Text(
                    if (endDateTimeErrorMessage.isEmpty()) "End date is required" else endDateTimeErrorMessage
                  )
                }
              }
            )

            CustomTextField(
              value = endTimeText,
              label = "समय",
              modifier = Modifier.width(180.dp),
              readOnly = true,
              onClick = { openEndTimeDialog.value = true },
              isError = endTimeError,
              supportingText = {
                if (endTimeError) {
                  Text(
                    if (endDateTimeErrorMessage.isEmpty()) "End time is required" else endDateTimeErrorMessage
                  )
                }
              }
            )
          }
        }
      }

      // Event Details Section (inline components)
      Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Gender Allowed (Using standard ExposedDropdownMenuBox)
          ExposedDropdownMenuBox(
            expanded = genderAllowedExpanded,
            onExpandedChange = { genderAllowedExpanded = !genderAllowedExpanded },
            modifier = Modifier.width(150.dp)
          ) {
            OutlinedTextField(
              value = eventGenderAllowed.toDisplayName(),
              onValueChange = {},
              readOnly = true,
              label = { Text("सत्र में प्रवेश") },
              placeholder = { Text("लिंग अनुमति चुनें") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderAllowedExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor(),
              isError = genderAllowedError != null,
              supportingText = { genderAllowedError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            ExposedDropdownMenu(
              expanded = genderAllowedExpanded,
              onDismissRequest = { genderAllowedExpanded = false }
            ) {
              genderAllowedDisplayOptions.forEach { selectionDisplayName ->
                DropdownMenuItem(
                  text = { Text(selectionDisplayName) },
                  onClick = {
                    eventGenderAllowed = GenderAllowed.fromDisplayName(selectionDisplayName) ?: GenderAllowed.ANY
                    genderAllowedExpanded = false
                  },
                  contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
              }
            }
          }

          // Capacity
          OutlinedTextField(
            value = eventCapacity,
            onValueChange = {
              if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                eventCapacity = it
                capacityError = null
              }
            },
            label = { Text("क्षमता") },
            modifier = Modifier.width(150.dp).focusRequester(capacityFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            isError = capacityError != null,
            supportingText = { capacityError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
          )
        }

        // Location Section
        Text("स्थान चुनें (Location)", style = MaterialTheme.typography.titleMedium)
        // button for choosing location
        OutlinedButton(
          onClick = {
            showMapDialog = true
          },
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Icon(
            Icons.Filled.Map,
            contentDescription = "मानचित्र से चुनें",
            modifier = Modifier.size(ButtonDefaults.IconSize)
          )
          Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          Text("मानचित्र से चुनें")
        }
        Text(
          "या मैन्युअल रूप से दर्ज करें:",
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = eventLatitude,
            onValueChange = {
              if (it.isEmpty() || it.matches(Regex("^-?[0-9]*\\.?[0-9]*$"))) {
                eventLatitude = it
                latitudeError = null
              }
            },
            label = { Text("अक्षांश (Latitude)") },
            modifier = Modifier.width(170.dp).focusRequester(latitudeFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            isError = latitudeError != null,
            supportingText = { latitudeError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
          )

          OutlinedTextField(
            value = eventLongitude,
            onValueChange = {
              if (it.isEmpty() || it.matches(Regex("^-?[0-9]*\\.?[0-9]*$"))) {
                eventLongitude = it
                longitudeError = null
              }
            },
            label = { Text("देशांतर (Longitude)") },
            modifier = Modifier.width(170.dp).focusRequester(longitudeFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            isError = longitudeError != null,
            supportingText = { longitudeError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
          )
        }
      }

      Text(
        modifier = Modifier.padding(top = 8.dp),
        text = "संबधित चित्र एवं पत्रिकाएं:",
        style = MaterialTheme.typography.labelLarge
      )

      // Custom grid to show both existing and new files
      Column(horizontalAlignment = Alignment.Start) {
        val totalItems = (existingMediaUrls.size - deletedMediaUrls.size) + attachedDocuments.size
        if (totalItems == 0) {
          Icon(
            imageVector = Icons.Filled.PhotoLibrary,
            contentDescription = "Selected",
            modifier = Modifier.size(96.dp).padding(16.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
          )
        }
        FlowRow(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Show existing media files with PhotoItem-like styling
          for (url in existingMediaUrls.filterNot { it in deletedMediaUrls }) {
            val docName = url.substringAfterLast("/")
            Box(modifier = Modifier.width(120.dp).padding(8.dp)) {
              Column {
                Surface(
                  modifier = Modifier
                    .size(100.dp)
                    .clip(shape = MaterialTheme.shapes.medium)
                ) {
                  Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                      model = url,
                      contentDescription = docName,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                      color = MaterialTheme.colorScheme.surfaceVariant,
                      shape = CircleShape,
                      modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                      IconButton(
                        onClick = {
                          // Mark URL for deletion
                          deletedMediaUrls = deletedMediaUrls + url
                          scope.launch {
                            snackbarHostState.showSnackbar("चित्र हटाने के लिए चिह्नित किया गया")
                          }
                        },
                        modifier = Modifier.size(36.dp)
                      ) {
                        Icon(
                          Icons.Default.Close,
                          modifier = Modifier.size(22.dp),
                          contentDescription = "Remove",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
                Text(
                  text = docName.take(15),
                  maxLines = 2,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(4.dp)
                )
              }
            }
          }

          // Show new documents
          for (document in attachedDocuments) {
            val docName = document.name
            Box(modifier = Modifier.width(120.dp).padding(8.dp)) {
              Column {
                PhotoItem(document, onRemoveFile = {
                  attachedDocuments =
                    attachedDocuments.toMutableList().apply {
                      remove(document)
                    }.toList()
                })
                Text(
                  text = docName,
                  maxLines = 2,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(4.dp)
                )
              }
            }
          }
        }
        if (attachedDocumentsError) {
          Text(text = attachedDocumentsErrorMessage, color = MaterialTheme.colorScheme.error)
        }
      }

      ButtonForFilePicker("चित्र/पत्रिकाएं जोड़ें", onFilesSelected = { filePath ->
        if (filePath != null) {
          attachedDocumentsError = false
          attachedDocuments = (attachedDocuments + filePath).distinct()
        }
      })

      // Contact People
      ContactPeopleDropdown(
        modifier = Modifier
          .fillMaxWidth()
          .onGloballyPositioned { coordinates ->
            lastFocusedFieldOffset = coordinates.positionInRoot().y
          },
        label = "संपर्क सूत्र",
        members = members,
        selectedMembers = contactPeople,
        onSelectionChanged = {
          contactPeople = it
          contactPeopleError = it.isEmpty()
        },
        postMap = postMap,
        isError = contactPeopleError,
        supportingText = { if (contactPeopleError) Text("At least one contact person is required") },
        onFieldFocused = { offset -> scrollToFocusedField(offset) }
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
        modifier = Modifier
          .width(500.dp)
          .onGloballyPositioned { coordinates ->
            lastFocusedFieldOffset = coordinates.positionInRoot().y
          }
          .onFocusChanged { focusState ->
            if (focusState.isFocused) {
              scrollToFocusedField(lastFocusedFieldOffset)
            }
          },
        minLines = 3,
        maxLines = 10,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
      )

      // Add extra bottom padding for keyboard
      Spacer(modifier = Modifier.height(16.dp))

      // Create Activity Button
      Button(
        modifier = Modifier.padding(vertical = 16.dp),
        onClick = { submitForm() },
        enabled = !isSubmitting,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(if (editingActivityId != null) "सहेजें" else "गतिविधि बनाएं")
        }
      }

      // Handle form submission result
      LaunchedEffect(formSubmissionState) {
        when {
          formSubmissionState.isSuccess -> {
            if (editingActivityId != null) {
              // Navigate to activity details after successful update
              snackbarHostState.showSnackbar(
                message = "गतिविधि सफलतापूर्वक अपडेट की गई"
              )
              onActivitySaved(editingActivityId)
            } else {
              // Handle new activity creation
              createdActivityId?.let { activityId ->
                snackbarHostState.showSnackbar(
                  message = "नई गतिविधि सफलतापूर्वक बनाई गई"
                )
                onActivitySaved(activityId)
              }
            }
          }

          formSubmissionState.error != null -> {
            // Error submitting form
            val errorMessage = when {
              formSubmissionState.error!!.contains("network", ignoreCase = true) ->
                "नेटवर्क त्रुटि। कृपया अपना इंटरनेट कनेक्शन जांचें"

              formSubmissionState.error!!.contains("duplicate", ignoreCase = true) ->
                "इस नाम की गतिविधि पहले से मौजूद है"

              else -> "त्रुटि: ${formSubmissionState.error}"
            }
            snackbarHostState.showSnackbar(
              message = errorMessage,
              actionLabel = "बंद करें",
              duration = SnackbarDuration.Long
            )
          }
        }
      }

      // Unsaved changes dialog
      if (showUnsavedChangesDialog) {
        AlertDialog(
          onDismissRequest = { showUnsavedChangesDialog = false },
          title = { Text("असहेजे परिवर्तन") },
          text = { Text("आपके पास असहेजे परिवर्तन हैं। क्या आप वाकई छोड़ना चाहते हैं?") },
          confirmButton = {
            TextButton(
              onClick = {
                showUnsavedChangesDialog = false
                onCancel()
              }
            ) {
              Text("हाँ, छोड़ें")
            }
          },
          dismissButton = {
            TextButton(onClick = { showUnsavedChangesDialog = false }) {
              Text("रद्द करें")
            }
          }
        )
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

      // Map Location Picker Dialog
      if (showMapDialog) {
        MapLocationPickerDialog(
          onDismiss = {
            showMapDialog = false // Reset state to false to hide the dialog
          }
        ){ it ->
          println("Location picked: $it")
        }
      }
    }
  }
}

// --- Preview ---
// Preview removed since we inlined the components

