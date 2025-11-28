package com.aryamahasangh.features.activities

// Wrapper class to handle both new files and existing URLs
// No MediaFile sealed class needed - we'll track existing and new files separately

// Removed MediaDocumentGrid - using DocumentGrid component instead

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aryamahasangh.components.*
import com.aryamahasangh.isDesktop
import com.aryamahasangh.navigation.LocalSetBackHandler
import com.aryamahasangh.network.bucket
import com.aryamahasangh.type.ActivityType
import com.aryamahasangh.ui.components.buttons.*
import com.aryamahasangh.util.GlobalMessageManager
import com.aryamahasangh.util.ImageCompressionService
import com.aryamahasangh.util.Result
import com.aryamahasangh.utils.FileUploadUtils
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.Clock.System
import org.koin.compose.koinInject

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

  Column(modifier = modifier) {
    // NOTE: Removed remembered snapshot & key() usage to prevent crash (removeLast on empty list) due to layout mutation.
    // We derive a simple immutable list each recomposition. Also guard FlowRow when empty.
    val selectedSnapshot = selectedOptions.toList()
    if (selectedSnapshot.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        for (option in selectedSnapshot) {
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
      Spacer(Modifier.height(4.dp))
    }

    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded }
    ) {
      OutlinedTextField(
        readOnly = true,
        value = selectedOptions.joinToString(", ") { it.name },
        onValueChange = {},
        label = { Text(label) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier.fillMaxWidth().menuAnchor(PrimaryNotEditable, true),
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
              val newSet = if (option in selectedOptions) selectedOptions - option else selectedOptions + option
              onSelectionChanged(newSet)
              // Keep menu open for multi-select.
            },
            trailingIcon = if (option in selectedOptions) {
              { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(20.dp)) }
            } else null
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityTypeSelector(
  selectedType: ActivityType?,
  onSelect: (ActivityType?) -> Unit,
  showError: Boolean
) {
  Column(modifier = Modifier.testTag("activityTypeSelector")) {
    Text(text = "प्रकार :", style = MaterialTheme.typography.bodyMedium)
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
      ActivityType.knownEntries.forEach { type ->
        FilterChip(
          selected = selectedType == type,
          onClick = { onSelect(if (selectedType == type) null else type) },
          label = { Text(type.toDisplayName()) },
          leadingIcon = if (selectedType == type) { { Icon(Icons.Filled.Done, contentDescription = null) } } else null,
          border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            enabled = true,
            selected = selectedType == type
          )
        )
      }
    }
    if (showError) {
      Text(
        text = "Type is required",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp)
      )
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


@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMaterial3Api
@Composable
fun CreateActivityScreen(
  viewModel: ActivitiesViewModel = koinInject(),
  editingActivityId: String? = null,
  onActivitySaved: (String) -> Unit,
  onCancel: () -> Unit = {}
) {
  // Remove incorrect section tracking - this screen is part of Activities section
  // ActivitiesPageState.enterActivitiesSection()

  BoxWithConstraints {
    // Consider screens smaller than 600dp as mobile-sized, but exclude desktop
    val isSmallScreen = maxWidth < 600.dp && !isDesktop()

    CreateActivityScreenContent(
      viewModel = viewModel,
      editingActivityId = editingActivityId,
      onActivitySaved = onActivitySaved,
      onCancel = onCancel,
      isSmallScreen = isSmallScreen
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMaterial3Api
@Composable
private fun CreateActivityScreenContent(
  viewModel: ActivitiesViewModel,
  editingActivityId: String? = null,
  onActivitySaved: (String) -> Unit = {},
  onCancel: () -> Unit = {},
  isSmallScreen: Boolean
) {
  // --- State ---
  var organisations by remember { mutableStateOf(emptyList<Organisation>()) }
  var isLoadingActivity by remember { mutableStateOf(false) }
  var editingActivity by remember { mutableStateOf<OrganisationalActivity?>(null) }

  // Form fields
  var name by remember { mutableStateOf("") }
  var nameError by remember { mutableStateOf(false) }
  var selectedType by remember { mutableStateOf<ActivityType?>(null) }
  var typesError by remember { mutableStateOf(false) }
  var shortDescription by remember { mutableStateOf("") }
  var shortDescriptionError by remember { mutableStateOf(false) }
  var description by remember { mutableStateOf("") }
  var descriptionError by remember { mutableStateOf(false) }
  var associatedOrganisations by remember { mutableStateOf(emptySet<Organisation>()) }
  var associatedOrganisationsError by remember { mutableStateOf(false) }
  var includeAddress by remember { mutableStateOf(true) }
  var addressData by remember { mutableStateOf(AddressData()) }
  var addressError by remember { mutableStateOf(false) }
  var triggerAddressValidation by remember { mutableStateOf(false) }
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
  var imagePickerState by remember { mutableStateOf(ImagePickerState()) }
  var triggerImageValidation by remember { mutableStateOf(false) }
  var imagePickerValid by remember { mutableStateOf(true) }
  var membersState by remember { mutableStateOf(MembersState()) }
  var contactPeopleError by remember { mutableStateOf(false) }
  var additionalInstructions by remember { mutableStateOf("") }
  var eventCapacity by remember { mutableStateOf("100") }
  var capacityError by remember { mutableStateOf<String?>(null) }
  var genderAllowed by remember { mutableStateOf<Gender?>(Gender.ANY) }
  var genderAllowedError by remember { mutableStateOf<String?>(null) }
  val capacityFocusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  var initialFormValues by remember { mutableStateOf<Map<String, Any>?>(null) }
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }

  val organisationsAndMembersState by viewModel.organisationsAndMembersState.collectAsState()
  val formSubmissionState by viewModel.activityFormSubmissionState.collectAsState()
  val createdActivityId by viewModel.createdActivityId.collectAsState()
  val scrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  // Remove unused keyboardController & lastFocusedFieldOffset
  // val keyboardController = LocalSoftwareKeyboardController.current // removed
  // var lastFocusedFieldOffset by remember { mutableStateOf(0f) } // removed

  // Date & time picker dialog states (restored)
  val openStartDateDialog = remember { mutableStateOf(false) }
  val openEndDateDialog = remember { mutableStateOf(false) }
  val openStartTimeDialog = remember { mutableStateOf(false) }
  val openEndTimeDialog = remember { mutableStateOf(false) }

  // Date/time formatters defined BEFORE any LaunchedEffect usage
  val dateFormatter: (LocalDate) -> String = { d -> "${d.dayOfMonth.toString().padStart(2,'0')}/${d.monthNumber.toString().padStart(2,'0')}/${d.year}" }
  val timeFormatter: (LocalTime) -> String = { t -> "${t.hour.toString().padStart(2,'0')}:${t.minute.toString().padStart(2,'0')}" }
  // Wrapper functions to satisfy existing calls referencing formatDate/formatTime
  fun formatDate(d: LocalDate) = dateFormatter(d)
  fun formatTime(t: LocalTime) = timeFormatter(t)

  // --- Effects ---
  LaunchedEffect(organisationsAndMembersState) {
    organisations = organisationsAndMembersState.organisations
    // Prune any previously selected organisations that no longer exist
    if (associatedOrganisations.isNotEmpty()) {
      val pruned = associatedOrganisations.filter { sel -> organisations.any { it.id == sel.id } }.toSet()
      if (pruned.size != associatedOrganisations.size) {
        associatedOrganisations = pruned
        associatedOrganisationsError = pruned.isEmpty()
      }
    }
  }
  LaunchedEffect(Unit) { viewModel.loadOrganisationsAndMembers() }
  LaunchedEffect(editingActivityId) {
    if (editingActivityId != null) {
      isLoadingActivity = true
      viewModel.loadActivityDetail(editingActivityId)
    }
  }
  LaunchedEffect(viewModel.activityDetailUiState.collectAsState().value) {
    if (editingActivityId != null) {
      val act = viewModel.activityDetailUiState.value.activity
      if (act != null) {
        editingActivity = act
        isLoadingActivity = false
        name = act.name
        selectedType = act.type
        includeAddress = if (act.type == ActivityType.CAMPAIGN) (act.addressId != null && act.addressId.isNotEmpty() && act.state.isNotEmpty()) else true
        shortDescription = act.shortDescription
        description = act.longDescription
        associatedOrganisations = act.associatedOrganisations.map { it.organisation }.toSet()
        addressData = AddressData(
          location = act.latitude?.let { lat -> act.longitude?.let { lon -> LatLng(lat, lon) } },
          address = act.address,
          state = act.state,
          district = act.district,
          vidhansabha = "",
          pincode = ""
        )
        startDate = act.startDatetime.date
        startTime = act.startDatetime.time
        startDateText = TextFieldValue(formatDate(startDate!!))
        startTimeText = TextFieldValue(formatTime(startTime!!))
        endDate = act.endDatetime.date
        endTime = act.endDatetime.time
        endDateText = TextFieldValue(formatDate(endDate!!))
        endTimeText = TextFieldValue(formatTime(endTime!!))
        membersState = MembersState(act.contactPeople.associate { it.member to (it.post to it.priority) })
        additionalInstructions = act.additionalInstructions
        eventCapacity = act.capacity.toString()
        genderAllowed = Gender.valueOf(act.allowedGender)
        imagePickerState = ImagePickerState(existingImageUrls = act.mediaFiles)
        initialFormValues = mapOf("name" to name)
      }
    }
  }
  LaunchedEffect(selectedType) {
    if (selectedType == ActivityType.CAMPAIGN && editingActivity == null) {
      includeAddress = false
      addressData = AddressData()
      addressError = false
    } else if (selectedType != null) includeAddress = true
  }

  // --- Helpers ---
  fun validateForm(): Boolean {
    nameError = name.isBlank()
    typesError = selectedType == null
    shortDescriptionError = shortDescription.isBlank()
    descriptionError = description.isBlank()
    associatedOrganisationsError = associatedOrganisations.isEmpty()
    if (includeAddress || selectedType != ActivityType.CAMPAIGN) {
      triggerAddressValidation = !triggerAddressValidation
    } else addressError = false
    startDateError = startDate == null
    startTimeError = startTime == null
    endDateError = endDate == null
    endTimeError = endTime == null
    capacityError = when {
      eventCapacity.isBlank() -> "क्षमता आवश्यक है."
      eventCapacity.toIntOrNull() == null -> "कृपया मान्य संख्या दर्ज करें."
      eventCapacity.toInt() <= 0 -> "क्षमता 0 से अधिक होनी चाहिए."
      else -> null
    }
    val validDates = !startDateError && !endDateError && !startTimeError && !endTimeError
    if (validDates) {
      val now = System.now().toLocalDateTime(TimeZone.currentSystemDefault())
      val sd = startDate!!.atTime(startTime!!)
      val ed = endDate!!.atTime(endTime!!)
      if (!(sd < ed && sd > now && ed > now)) {
        when {
          sd >= ed -> { startDateError = true; startTimeError = true; endDateError = true; endTimeError = true; startDateTimeErrorMessage = "Start datetime should be before end"; endDateTimeErrorMessage = startDateTimeErrorMessage }
          sd <= now -> { startDateError = true; startTimeError = true; startDateTimeErrorMessage = "Start must be future" }
          ed <= now -> { endDateError = true; endTimeError = true; endDateTimeErrorMessage = "End must be future" }
        }
      } else { startDateTimeErrorMessage = ""; endDateTimeErrorMessage = "" }
    }
    triggerImageValidation = !triggerImageValidation
    val eventDetailsValid = capacityError == null
    return !(nameError || typesError || shortDescriptionError || descriptionError || associatedOrganisationsError || addressError || startDateError || startTimeError || endDateError || endTimeError || contactPeopleError || !eventDetailsValid || !imagePickerValid)
  }

  suspend fun processMediaFiles(): List<String> {
    val attached = mutableListOf<String>()
    // Include existing (non-deleted) URLs first
    attached += imagePickerState.getActiveImageUrls()
    val newImages = imagePickerState.newImages.toList()
    if (newImages.isEmpty()) {
      // Handle deletions only
      if (imagePickerState.deletedImageUrls.isNotEmpty()) {
        try { bucket.delete(imagePickerState.deletedImageUrls.map { it.substringAfterLast('/') }) } catch (_: Exception) {}
      }
      return attached.distinct()
    }

    var failure: String? = null

    suspend fun attemptUpload(
      file: PlatformFile,
      baseIndex: Int,
      targetKbSequence: List<Int>
    ): Boolean {
      val ext = file.name.substringAfterLast('.', "").lowercase()
      if (ext !in listOf("jpg", "jpeg", "png", "webp")) {
        failure = "असमर्थित चित्र प्रकार"
        return false
      }
      // Progressive compression attempts
      var compressed: ByteArray = ByteArray(0)
      for ((i, target) in targetKbSequence.withIndex()) {
        compressed = ImageCompressionService.compressGeneral(file, targetKb = target, maxLongEdge = 1024)
        if (compressed.isEmpty()) continue
        if (compressed.size <= (target * 1024) || i == targetKbSequence.lastIndex) break
      }
      if (compressed.isEmpty()) {
        failure = "चित्र संसाधित नहीं हुआ"
        return false
      }
      // Hard cap guard – re-compress harsher if still above 140KB
      if (compressed.size > 140 * 1024) {
        compressed = ImageCompressionService.compressGeneral(file, targetKb = 80, maxLongEdge = 1024)
      }
      if (compressed.size > 160 * 1024) {
        failure = "चित्र बहुत बड़ा है"
        return false
      }

      val timestamp = System.now().epochSeconds
      val randomSuffix = (1000..9999).random()
      val path = "activity_${timestamp}_${randomSuffix}_${baseIndex}.webp"

      // Upload with one retry on Darwin EMSGSIZE
      var attempt = 0
      var lastError: String? = null
      while (attempt < 2) {
        when (val uploadResult = FileUploadUtils.uploadBytes(path, compressed)) {
          is Result.Success -> {
            attached += uploadResult.data
            return true
          }
          is Result.Error -> {
            val msg = uploadResult.message ?: "त्रुटि"
            // Detect Darwin EMSGSIZE scenario heuristically
            if (msg.contains("Message too long", ignoreCase = true) && attempt == 0) {
              // Re-compress smaller and retry once
              compressed = ImageCompressionService.compressGeneral(file, targetKb = 60, maxLongEdge = 1024)
              attempt++
              continue
            } else {
              lastError = msg
              failure = "चित्र अपलोड विफल: $msg"
              return false
            }
          }
          else -> {
            lastError = "अज्ञात अपलोड त्रुटि"
            failure = lastError
            return false
          }
        }
      }
      if (lastError != null) failure = lastError
      return false
    }

    newImages.forEachIndexed { index, file ->
      if (failure != null) return@forEachIndexed
      try {
        // Try sequence: start at 100KB, then 90, 80, 70 for refinement
        val ok = attemptUpload(file, index, listOf(100, 90, 80, 70))
        if (!ok && failure == null) failure = "चित्र अपलोड विफल"
      } catch (e: Exception) {
        val msg = e.message ?: "चित्र संसाधन त्रुटि"
        failure = msg
      }
    }

    // If there was a failure, show error and return empty list instead of throwing
    if (failure != null) {
      GlobalMessageManager.showError(failure!!)
      return emptyList()
    }

    // Perform deletions after successful new uploads
    if (imagePickerState.deletedImageUrls.isNotEmpty()) {
      try { bucket.delete(imagePickerState.deletedImageUrls.map { it.substringAfterLast('/') }) } catch (_: Exception) {}
    }
    return attached.distinct()
  }

  // Back handling
  val setBackHandler = LocalSetBackHandler.current
  DisposableEffect(Unit) {
    val handler: () -> Unit = { if (initialFormValues != null && name != initialFormValues!!["name"]) showUnsavedChangesDialog = true else onCancel() }
    setBackHandler?.invoke(handler)
    onDispose { setBackHandler?.invoke(null) }
  }

  if (isLoadingActivity) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Text("गतिविधि विवरण लोड हो रहा है...") } }
    return
  }

  Box(Modifier.fillMaxSize()) {
    Column(
      Modifier
        .fillMaxSize()
        .padding(12.dp)
        .verticalScroll(scrollState)
        .let { if (isSmallScreen) it.imePadding() else it }
    ) {
      // Header
      Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(if (editingActivityId != null) "गतिविधि संपादित करें" else "नई गतिविधि बनाएं", style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = { if (initialFormValues != null && name != initialFormValues!!["name"]) showUnsavedChangesDialog = true else onCancel() }) { Text("निरस्त करें") }
      }
      // Name
      OutlinedTextField(name, { name = it; nameError = false }, label = { Text("नाम") }, modifier = Modifier.width(500.dp), isError = nameError, supportingText = { if (nameError) Text("Name is required") }, maxLines = 1, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
      // Type selector
      ActivityTypeSelector(selectedType = selectedType, onSelect = { t -> selectedType = t; typesError = (t == null) }, showError = typesError)
      // Short description
      OutlinedTextField(shortDescription, { if (it.length <= 100) { shortDescription = it; shortDescriptionError = false } }, label = { Text("संक्षिप्त विवरण") }, modifier = Modifier.width(500.dp), isError = shortDescriptionError, supportingText = { if (shortDescriptionError) Text("Short Description is required") }, maxLines = 1, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
      // Description
      OutlinedTextField(description, { if (it.length <= 1000) { description = it; descriptionError = false } }, label = { Text("विस्तृत विवरण") }, modifier = Modifier.width(500.dp), minLines = 3, maxLines = 30, isError = descriptionError, supportingText = { if (descriptionError) Text("Description is required") })
      // Organisations
      MultiSelectDropdown(
        Modifier.width(500.dp),
        label = "संबधित संस्थाएं",
        options = organisations,
        selectedOptions = associatedOrganisations,
        onSelectionChanged = { set ->
          associatedOrganisations = set
          associatedOrganisationsError = set.isEmpty()
        },
        isError = associatedOrganisationsError,
        supportingText = { if (associatedOrganisationsError) Text("Associated Organisation is required") }
      )
      // Address toggle
      if (selectedType == ActivityType.CAMPAIGN) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Checkbox(includeAddress, { includeAddress = it }, modifier = Modifier.testTag("AddAddressCheckbox")); Text("स्थान जोड़ें", modifier = Modifier.testTag("AddAddressCheckboxLabel")) }
      if (includeAddress || selectedType != ActivityType.CAMPAIGN) AddressComponent(
        addressData = addressData,
        onAddressChange = { addressData = it },
        fieldsConfig = AddressFieldsConfig(showLocation = true, showAddress = true, showState = true, showDistrict = true, showVidhansabha = false, showPincode = false, mandatoryLocation = true, mandatoryAddress = true, mandatoryState = true, mandatoryDistrict = true, mandatoryVidhansabha = false, mandatoryPincode = false),
        validateFields = triggerAddressValidation,
        onValidationResult = { valid -> addressError = !valid },
        modifier = Modifier.testTag("AddressFieldsGroup"),
        onFieldFocused = if (isSmallScreen) { ofs -> scope.launch { scrollState.animateScrollTo(ofs.toInt()) } } else null,
        isSmallScreen = isSmallScreen
      )
      // Date-Time row
      FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
          Text("प्रारंभ:")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CustomTextField(startDateText, "दिनांक", Modifier.width(180.dp), readOnly = true, onClick = { openStartDateDialog.value = true }, isError = startDateError, supportingText = { if (startDateError) Text(if (startDateTimeErrorMessage.isEmpty()) "Start date is required" else startDateTimeErrorMessage) })
            CustomTextField(startTimeText, "समय", Modifier.width(180.dp), readOnly = true, onClick = { openStartTimeDialog.value = true }, isError = startTimeError, supportingText = { if (startTimeError) Text(if (startTimeError) "Start time is required" else startDateTimeErrorMessage) })
          }
        }
        Column {
          Text("समाप्ति:")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CustomTextField(endDateText, "दिनांक", Modifier.width(180.dp), readOnly = true, onClick = { openEndDateDialog.value = true }, isError = endDateError, supportingText = { if (endDateError) Text(if (endDateTimeErrorMessage.isEmpty()) "End date is required" else endDateTimeErrorMessage) })
            CustomTextField(endTimeText, "समय", Modifier.width(180.dp), readOnly = true, onClick = { openEndTimeDialog.value = true }, isError = endTimeError, supportingText = { if (endTimeError) Text(if (endTimeError) "End time is required" else endDateTimeErrorMessage) })
          }
        }
      }
      // Event details
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
        GenderDropdown(genderAllowed, { g -> genderAllowed = g; genderAllowedError = null }, label = "सत्र में प्रवेश", modifier = Modifier.width(150.dp), isError = genderAllowedError != null, supportingText = { genderAllowedError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }, customDisplayNames = mapOf(Gender.MALE to GenderAllowed.MALE.toDisplayName(), Gender.FEMALE to GenderAllowed.FEMALE.toDisplayName(), Gender.ANY to GenderAllowed.ANY.toDisplayName()))
        OutlinedTextField(eventCapacity, { if (it.isEmpty() || it.all { c -> c.isDigit() }) { eventCapacity = it; capacityError = null } }, label = { Text("क्षमता") }, modifier = Modifier.width(150.dp).focusRequester(capacityFocusRequester), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }), isError = capacityError != null, supportingText = { capacityError?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
      }
      // Images
      ImagePickerComponent(state = imagePickerState, onStateChange = { imagePickerState = it }, config = ImagePickerConfig(label = "गतिविधि चित्र", allowMultiple = true, maxImages = 10, type = ImagePickerType.IMAGE, isMandatory = false, enableBackgroundCompression = true, compressionTargetKb = 100, showCompressionProgress = true), validateFields = triggerImageValidation, onValidationResult = { v -> imagePickerValid = v })
      Spacer(Modifier.height(24.dp))
      // Members
      MembersComponent(state = membersState, onStateChange = { membersState = it; contactPeopleError = it.members.isEmpty() }, config = MembersConfig(label = "संपर्क सूत्र", addButtonText = "संपर्क व्यक्ति जोड़ें", postLabel = "भूमिका", postPlaceholder = "संयोजक, कोषाध्यक्ष इत्यादि", isMandatory = true, editMode = MembersEditMode.GROUPED, enableReordering = true), error = if (contactPeopleError) "कम से कम एक संपर्क व्यक्ति आवश्यक है" else null)
      // Instructions
      OutlinedTextField(additionalInstructions, { if (it.length <= 1000) additionalInstructions = it }, label = { Text("अतिरिक्त निर्देश") }, modifier = Modifier.width(500.dp), minLines = 3, maxLines = 10)
      Spacer(Modifier.height(24.dp))
      SubmitButtonOld(
        text = if (editingActivityId != null) "अद्यतन करें" else "गतिविधि बनाएं",
        onSubmit = {
          try {
            val media = processMediaFiles()
            val data = ActivityInputData(
              name = name,
              shortDescription = shortDescription,
              longDescription = description,
              type = selectedType!!,
              addressId = if (!includeAddress && selectedType == ActivityType.CAMPAIGN) null else editingActivity?.addressId,
              address = if (includeAddress || selectedType != ActivityType.CAMPAIGN) addressData.address else "",
              state = if (includeAddress || selectedType != ActivityType.CAMPAIGN) addressData.state else "",
              district = if (includeAddress || selectedType != ActivityType.CAMPAIGN) addressData.district else "",
              associatedOrganisations = associatedOrganisations.toList(),
              startDatetime = startDate!!.atTime(startTime!!),
              endDatetime = endDate!!.atTime(endTime!!),
              mediaFiles = media,
              contactPeople = membersState.members.map { (m, rp) -> ActivityMember(m.id, rp.first, m, rp.second) },
              additionalInstructions = additionalInstructions,
              capacity = eventCapacity.toIntOrNull() ?: 0,
              allowedGender = when (genderAllowed) { Gender.ANY -> GenderAllowed.ANY; Gender.MALE -> GenderAllowed.MALE; Gender.FEMALE -> GenderAllowed.FEMALE; null -> GenderAllowed.ANY },
              latitude = if (includeAddress || selectedType != ActivityType.CAMPAIGN) addressData.location?.latitude else null,
              longitude = if (includeAddress || selectedType != ActivityType.CAMPAIGN) addressData.location?.longitude else null
            )
            if (editingActivityId != null) viewModel.updateActivitySmart(editingActivityId, data) else viewModel.createActivity(data)
            ActivitiesPageState.markForRefresh()
          } catch (e: Exception) {
            GlobalMessageManager.showError(e.message ?: "चित्र अपलोड विफल")
          }
        },
        config = SubmitButtonConfig(fillMaxWidth = false, validator = { if (!validateForm()) SubmissionError.ValidationFailed else null }, texts = SubmitButtonTexts(submittingText = if (editingActivityId != null) "अद्यतन हो रही है..." else "बनाई जा रही है...", successText = if (editingActivityId != null) "अद्यतन सफल!" else "सफल!")),
        callbacks = object : SubmitCallbacks {
          override fun onSuccess() { val id = editingActivityId ?: viewModel.createdActivityId.value ?: return; onActivitySaved(id) }
          override fun onError(error: SubmissionError) { }
        }
      )
      LaunchedEffect(formSubmissionState.isSuccess) { if (formSubmissionState.isSuccess) { val id = editingActivityId ?: createdActivityId ?: return@LaunchedEffect; onActivitySaved(id) } }
      if (showUnsavedChangesDialog) AlertDialog(onDismissRequest = { showUnsavedChangesDialog = false }, title = { Text("असंचयिक परिवर्तन") }, text = { Text("आपके परिवर्तन संचयित नहीं किए गए हैं। क्या आप इसे त्यागना चाहते हैं?") }, confirmButton = { TextButton(onClick = { showUnsavedChangesDialog = false; onCancel() }) { Text("जी हाँ") } }, dismissButton = { TextButton(onClick = { showUnsavedChangesDialog = false }) { Text("नहीं") } })
      if (openStartDateDialog.value) CustomDatePickerDialog(onDateSelected = { d -> startDate = d; startDateText = TextFieldValue(formatDate(d)); startDateError = false; openStartDateDialog.value = false }, onDismissRequest = { openStartDateDialog.value = false })
      if (openEndDateDialog.value) CustomDatePickerDialog(onDateSelected = { d -> endDate = d; endDateText = TextFieldValue(formatDate(d)); endDateError = false; openEndDateDialog.value = false }, onDismissRequest = { openEndDateDialog.value = false })
      if (openStartTimeDialog.value) TimePickerDialog(onTimeSelected = { t -> startTime = t; startTimeText = TextFieldValue(formatTime(t)); startTimeError = false; openStartTimeDialog.value = false }, onDismissRequest = { openStartTimeDialog.value = false })
      if (openEndTimeDialog.value) TimePickerDialog(onTimeSelected = { t -> endTime = t; endTimeText = TextFieldValue(formatTime(t)); endTimeError = false; openEndTimeDialog.value = false }, onDismissRequest = { openEndTimeDialog.value = false })
    }
  }
}


// --- Preview ---
// Preview removed since we inlined the components
