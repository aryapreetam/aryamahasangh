package com.aryamahasangh.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.*
import kotlinx.datetime.format.char
import kotlinx.serialization.Serializable

// --- Data Models ---

enum class DatePickerType {
  DATE_OF_BIRTH, // Cannot select future dates
  FUTURE_EVENT, // Cannot select past dates (can select today and after)
  PAST_EVENT // Cannot select future dates
}

enum class Gender {
  MALE,
  FEMALE,
  ANY;

  fun toDisplayName(): String =
    when (this) {
      MALE -> "पुरुष"
      FEMALE -> "महिला"
      ANY -> "अन्य"
    }

  companion object {
    fun fromDisplayName(displayName: String): Gender? = entries.find { it.toDisplayName() == displayName }
  }
}

enum class FamilyRelation {
  SELF,
  FATHER,
  MOTHER,
  HUSBAND,
  WIFE,
  SON,
  DAUGHTER,
  BROTHER,
  SISTER,
  GRANDFATHER,
  GRANDMOTHER,
  GRANDSON,
  GRANDDAUGHTER,
  UNCLE,
  AUNT,
  COUSIN,
  NEPHEW,
  NIECE,
  GUARDIAN,
  RELATIVE,
  OTHER;

  fun toDisplayName(): String =
    when (this) {
      SELF -> "स्वयं"
      FATHER -> "पिता"
      MOTHER -> "माता"
      HUSBAND -> "पति"
      WIFE -> "पत्नी"
      SON -> "पुत्र"
      DAUGHTER -> "पुत्री"
      BROTHER -> "भाई"
      SISTER -> "बहिन"
      GRANDFATHER -> "पितामह"
      GRANDMOTHER -> "पितामही"
      GRANDSON -> "पौत्र"
      GRANDDAUGHTER -> "पौत्री"
      UNCLE -> "चाचा"
      AUNT -> "चाची"
      COUSIN -> "चचेरा भाई/बहिन"
      NEPHEW -> "भतीजा"
      NIECE -> "भतीजी"
      GUARDIAN -> "अभिभावक"
      RELATIVE -> "संबंधी"
      OTHER -> "अन्य"
    }

  companion object {
    fun fromDisplayName(displayName: String): FamilyRelation? = entries.find { it.toDisplayName() == displayName }
  }
}

// Placeholder data model for AryaSamaj
@Serializable
data class AryaSamaj(
  val id: String,
  val name: String,
  val address: String,
  val district: String
)

// --- Date Picker Component ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
  value: LocalDate?,
  onValueChange: (LocalDate?) -> Unit,
  label: String,
  type: DatePickerType = DatePickerType.DATE_OF_BIRTH,
  excludeToday: Boolean = false,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  supportingText: @Composable (() -> Unit)? = null,
  enabled: Boolean = true,
  required: Boolean = false
) {
  var showDatePicker by remember { mutableStateOf(false) }

  val dateFormatter =
    LocalDate.Format {
      dayOfMonth()
      char('/')
      monthNumber()
      char('/')
      year()
    }

  Box(modifier = modifier) {
    OutlinedTextField(
      value = value?.format(dateFormatter) ?: "",
      onValueChange = { },
      label = { Text(if (required) "$label *" else label) },
      readOnly = true,
      enabled = enabled,
      trailingIcon = {
        IconButton(
          onClick = { if (enabled) showDatePicker = true }
        ) {
          Icon(
            Icons.Default.DateRange,
            contentDescription = "दिनांक चुनें"
          )
        }
      },
      modifier = Modifier.fillMaxWidth(),
      isError = isError,
      supportingText = supportingText
    )

    // Invisible clickable overlay to trigger date picker
    if (enabled) {
      Box(
        modifier =
          Modifier
            .matchParentSize()
            .clickable { showDatePicker = true }
      )
    }
  }

  if (showDatePicker) {
    val currentTimeMillis = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val currentDate =
      Instant.fromEpochMilliseconds(currentTimeMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Set date validator based on type
    val dateValidator: (Long) -> Boolean = { timeInMillis ->
      val dateToCheck =
        Instant.fromEpochMilliseconds(timeInMillis)
          .toLocalDateTime(TimeZone.currentSystemDefault()).date

      when (type) {
        DatePickerType.DATE_OF_BIRTH -> if (excludeToday) dateToCheck < currentDate else dateToCheck <= currentDate
        DatePickerType.FUTURE_EVENT -> dateToCheck >= currentDate
        DatePickerType.PAST_EVENT -> if (excludeToday) dateToCheck < currentDate else dateToCheck <= currentDate
      }
    }

    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = value?.toEpochDays()?.times(24 * 60 * 60 * 1000L),
        selectableDates =
          object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
              return dateValidator(utcTimeMillis)
            }
          }
      )

    DatePickerDialog(
      onDateSelected = { selectedDate ->
        onValueChange(selectedDate)
        showDatePicker = false
      },
      onDismiss = { showDatePicker = false },
      datePickerState = datePickerState
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
  onDateSelected: (LocalDate?) -> Unit,
  onDismiss: () -> Unit,
  datePickerState: DatePickerState
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      tonalElevation = 6.dp,
      modifier =
        Modifier
          .wrapContentSize()
          .padding(12.dp)
          .widthIn(max = 360.dp) // Add max width constraint to make it compact
    ) {
      Column(
      ) {
        Text(
          text = "दिनांक चुनें",
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.padding(top = 16.dp, start = 16.dp)
        )

        DatePicker(
          state = datePickerState,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("रद्द करें")
          }
          TextButton(
            onClick = {
              val selectedDate =
                datePickerState.selectedDateMillis?.let { millis ->
                  LocalDate.fromEpochDays((millis / (24 * 60 * 60 * 1000L)).toInt())
                }
              onDateSelected(selectedDate)
            }
          ) {
            Text("चुनें")
          }
        }
      }
    }
  }
}

// --- Gender Dropdown Component ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(
  value: Gender?,
  onValueChange: (Gender) -> Unit,
  label: String = "लिंग",
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  supportingText: @Composable (() -> Unit)? = null,
  enabled: Boolean = true,
  required: Boolean = false,
  customDisplayNames: Map<Gender, String>? = null
) {
  var expanded by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current

  fun getDisplayName(gender: Gender): String {
    return customDisplayNames?.get(gender) ?: gender.toDisplayName()
  }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { if (enabled) expanded = !expanded },
    modifier = modifier
  ) {
    OutlinedTextField(
      value = value?.let { getDisplayName(it) } ?: "",
      onValueChange = { },
      readOnly = true,
      enabled = enabled,
      label = { Text(if (required) "$label *" else label) },
      placeholder = { Text("लिंग चुनें") },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      modifier = Modifier.menuAnchor(PrimaryNotEditable, true),
      isError = isError,
      supportingText = supportingText
    )

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      Gender.entries.forEach { gender ->
        DropdownMenuItem(
          text = { Text(getDisplayName(gender)) },
          onClick = {
            onValueChange(gender)
            expanded = false
            focusManager.moveFocus(FocusDirection.Next)
          }
        )
      }
    }
  }
}

// --- Family Relation Dropdown Component ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyRelationDropdown(
  value: FamilyRelation?,
  onValueChange: (FamilyRelation) -> Unit,
  label: String = "पारिवारिक सम्बन्ध",
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  supportingText: @Composable (() -> Unit)? = null,
  enabled: Boolean = true,
  required: Boolean = false
) {
  var expanded by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { if (enabled) expanded = !expanded },
    modifier = modifier
  ) {
    OutlinedTextField(
      value = value?.toDisplayName() ?: "",
      onValueChange = { },
      readOnly = true,
      enabled = enabled,
      label = { Text(if (required) "$label *" else label) },
      placeholder = { Text("सम्बन्ध चुनें") },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      modifier = Modifier.menuAnchor(PrimaryNotEditable, true),
      isError = isError,
      supportingText = supportingText
    )

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      FamilyRelation.entries.forEach { relation ->
        DropdownMenuItem(
          text = { Text(relation.toDisplayName()) },
          onClick = {
            onValueChange(relation)
            expanded = false
            focusManager.moveFocus(FocusDirection.Next)
          }
        )
      }
    }
  }
}

// --- Arya Samaj Selection Component ---

@Composable
fun AryaSamajSelector(
  selectedAryaSamaj: AryaSamaj?,
  onAryaSamajSelected: (AryaSamaj?) -> Unit,
  label: String = "आर्य समाज",
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  supportingText: @Composable (() -> Unit)? = null,
  enabled: Boolean = true,
  required: Boolean = false,
  // Optional location for proximity-based sorting
  latitude: Double? = null,
  longitude: Double? = null
) {
  var showDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    OutlinedTextField(
      value = selectedAryaSamaj?.name ?: "",
      onValueChange = { },
      readOnly = true,
      enabled = enabled,
      label = { Text(if (required) "$label *" else label) },
      placeholder = { Text("आर्य समाज चुनें") },
      trailingIcon = {
        Row {
          if (selectedAryaSamaj != null && enabled) {
            IconButton(
              onClick = { onAryaSamajSelected(null) }
            ) {
              Icon(
                Icons.Default.Clear,
                contentDescription = "साफ़ करें"
              )
            }
          }
          IconButton(
            onClick = {
              if (enabled) {
                showDialog = true
              }
            }
          ) {
            Icon(
              Icons.Default.ArrowDropDown,
              contentDescription = "आर्य समाज चुनें"
            )
          }
        }
      },
      modifier = Modifier.fillMaxWidth(),
      isError = isError,
      supportingText = supportingText
    )

    // Invisible clickable overlay that excludes the trailing icon area
    if (enabled) {
      Box(
        modifier =
          Modifier
            .matchParentSize()
            .padding(end = if (selectedAryaSamaj != null) 96.dp else 48.dp) // Exclude trailing icons
            .clickable {
              showDialog = true
            }
      )
    }
  }

  if (showDialog) {
    AryaSamajSelectionDialog(
      onDismiss = { showDialog = false },
      onAryaSamajSelected = { selected ->
        onAryaSamajSelected(selected)
        showDialog = false
      },
      selectedAryaSamaj = selectedAryaSamaj,
      latitude = latitude,
      longitude = longitude
    )
  }
}

// --- Arya Samaj Selection Dialog ---

@Composable
private fun AryaSamajSelectionDialog(
  onDismiss: () -> Unit,
  onAryaSamajSelected: (AryaSamaj) -> Unit,
  selectedAryaSamaj: AryaSamaj? = null,
  latitude: Double? = null,
  longitude: Double? = null
) {
  // Get ViewModel from DI
  val viewModel: com.aryamahasangh.features.admin.aryasamaj.AryaSamajSelectorViewModel =
    org.koin.compose.koinInject()

  val uiState by viewModel.uiState.collectAsState()

  // Local TextField state - prevents cursor position issues
  var localSearchQuery by remember { mutableStateOf("") }

  // Initialize search handling when dialog opens
  LaunchedEffect(Unit) {
    viewModel.initializeSearch()
    viewModel.loadRecentAryaSamajs(latitude = latitude, longitude = longitude)
  }

  // Debounced search trigger - only affects network calls, not TextField
  LaunchedEffect(localSearchQuery) {
    kotlinx.coroutines.delay(300) // Local debounce for network calls
    viewModel.triggerSearch(localSearchQuery)
  }

  // Clear search when dialog is dismissed
  DisposableEffect(Unit) {
    onDispose {
      viewModel.clearSearch()
    }
  }

  val displayResults = if (localSearchQuery.isBlank()) {
    uiState.recentAryaSamajs
  } else {
    uiState.searchResults
  }

  val isLoading = if (localSearchQuery.isBlank()) {
    uiState.isLoadingRecent
  } else {
    uiState.isSearching
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      tonalElevation = 6.dp,
      modifier =
        Modifier
          .fillMaxWidth(0.9f)
          .fillMaxHeight(0.8f)
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        // Header
        Text(
          text = "आर्य समाज चुनें",
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search field - uses local state to prevent cursor issues
        OutlinedTextField(
          value = localSearchQuery, // Local state only
          onValueChange = { localSearchQuery = it }, // Updates local state only
          label = { Text("खोजें") },
          placeholder = { Text("आर्य समाज का नाम") },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "खोजें")
          },
          trailingIcon = {
            if (localSearchQuery.isNotEmpty()) {
              IconButton(
                onClick = { localSearchQuery = "" }
              ) {
                Icon(Icons.Default.Clear, contentDescription = "साफ़ करें")
              }
            } else if (uiState.isSearching) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
            }
          },
          modifier = Modifier.fillMaxWidth(),
          keyboardOptions =
            KeyboardOptions(
              keyboardType = KeyboardType.Text,
              imeAction = ImeAction.Search
            ),
          keyboardActions =
            KeyboardActions(
              onSearch = { viewModel.triggerSearch(localSearchQuery) }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Results list
        LazyColumn {
          if (isLoading && displayResults.isEmpty()) {
            item {
              Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                CircularProgressIndicator()
                Text(
                  text = if (localSearchQuery.isBlank()) "लोड हो रहा है..." else "खोजा जा रहा है...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          } else if (localSearchQuery.length == 1) {
            // Show minimum character requirement for single character
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "न्यूनतम २ अक्षर आवश्यक",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            items(displayResults.size) { index ->
              val aryaSamaj = displayResults[index]
              AryaSamajItem(
                aryaSamaj = aryaSamaj,
                isSelected = aryaSamaj == selectedAryaSamaj,
                onClick = {
                  onAryaSamajSelected(aryaSamaj)
                  onDismiss()
                }
              )

              // Trigger pagination when near end (last 5 items)
              if (index >= displayResults.size - 5) {
                val hasNextPage = if (localSearchQuery.isBlank()) {
                  uiState.hasNextPageRecent
                } else {
                  uiState.hasNextPageSearch
                }

                if (hasNextPage && !uiState.isLoadingMore) {
                  LaunchedEffect(key1 = displayResults.size) {
                    viewModel.loadNextPage()
                  }
                }
              }
            }

            if (displayResults.isEmpty() && !isLoading && localSearchQuery.length >= 2) {
              item {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  if (uiState.showRetryButton) {
                    Button(onClick = { viewModel.retryLoading() }) {
                      Text("पुनः प्रयास करें")
                    }
                  } else {
                    Text(
                      text = if (localSearchQuery.isBlank()) {
                        "कोई आर्य समाज उपलब्ध नहीं"
                      } else {
                        "कोई आर्य समाज नहीं मिला"
                      },
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      textAlign = TextAlign.Center
                    )
                  }
                }
              }
            } else if (displayResults.isEmpty() && !isLoading && localSearchQuery.isBlank()) {
              // Show message for empty recent list
              item {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "आर्य समाज सूची उपलब्ध नहीं",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }

            // Show loading indicator at bottom during pagination
            val hasNextPage = if (localSearchQuery.isBlank()) {
              uiState.hasNextPageRecent
            } else {
              uiState.hasNextPageSearch
            }

            if (hasNextPage && displayResults.isNotEmpty()) {
              item {
                Box(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  contentAlignment = Alignment.Center
                ) {
                  if (uiState.isLoadingMore) {
                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      CircularProgressIndicator(modifier = Modifier.size(24.dp))
                      Text(
                        "लोड हो रहा है...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Cancel button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("रद्द करें")
          }
        }
      }
    }
  }
}

@Composable
private fun AryaSamajItem(
  aryaSamaj: AryaSamaj,
  isSelected: Boolean = false,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surface
          }
      ),
    border =
      if (isSelected) {
        null
      } else {
        CardDefaults.outlinedCardBorder()
      },
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Text(
        text = aryaSamaj.name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color =
          if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSurface
          }
      )

      Text(
        text = aryaSamaj.address,
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.padding(top = 4.dp)
      )

      Text(
        text = "जिला: ${aryaSamaj.district}",
        style = MaterialTheme.typography.bodySmall,
        color =
          if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}
