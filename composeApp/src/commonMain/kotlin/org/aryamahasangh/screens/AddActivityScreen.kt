package org.aryamahasangh.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.aryamahasangh.OrganisationalActivityDetailQuery.*
import org.aryamahasangh.type.ActivityType
import org.jetbrains.compose.ui.tooling.preview.Preview


//Mock Data
val availableOrganisations = listOf(
  "385933f4-2eca-46bb-bb31-50277d08d873" to "Organisation A",
  "36f4e51f-1517-4f1e-8395-d1d83b7c76ea" to "Organisation B",
  "fbf6537d-6361-4bf0-95a3-4afcc814a6f9" to "Organisation C",
  "some-other-org-id" to "Organisation D"
)

val availableContactPeople = listOf(
  ContactPeople(Member("John Doe", "", "123-456-7890"), "Manager", 1),
  ContactPeople(Member("Jane Smith", "", "987-654-3210"), "Coordinator", 2),
  ContactPeople(Member("Alice Johnson", "", "555-123-4567"), "Volunteer", 3)
)

@Composable
fun OrganisationalActivityForm(activity: OrganisationalActivity) {
  var nameState = remember { mutableStateOf(TextFieldValue(activity.name)) }
  var descriptionState = remember { mutableStateOf(TextFieldValue(activity.description)) }
  var selectedOrganisations = remember { mutableStateOf(activity.associatedOrganisation) }
  var activityTypeState = remember { mutableStateOf(activity.activityType) }
  var placeState = remember { mutableStateOf(TextFieldValue(activity.place)) }
  var startDateState = remember { mutableStateOf(activity.startDateTime) }
  var endDateState = remember { mutableStateOf(activity.endDateTime) }
  var additionalInstructionsState = remember { mutableStateOf(TextFieldValue(activity.additionalInstructions)) }
  var selectedContactPeople = remember { mutableStateOf(activity.contactPeople) }
  val organisationIds = activity.associatedOrganisation

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      text = "Activity Details",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    OutlinedTextField(
      value = nameState.value,
      onValueChange = { nameState.value = it },
      label = { Text("Name") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )

    OutlinedTextField(
      value = descriptionState.value,
      onValueChange = { descriptionState.value = it },
      label = { Text("Description") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      minLines = 3
    )

    // Associated Organisations (Multiple Choice Dropdown)
    MultiChoiceDropdown(
      label = "Associated Organisations",
      options = availableOrganisations.toMap(),  // Convert list to map
      selectedOptions = selectedOrganisations.value as List<String>,
      onSelectionChanged = { selected -> selectedOrganisations.value = selected }
    )

    // Activity Type (Chips)
    Text(
      text = "Activity Type",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    ActivityTypeChips(
      selectedType = activityTypeState.value.toString(),
      onTypeSelected = { activityTypeState.value = ActivityType.safeValueOf(it) }
    )

    // Place (Text Area + Map Option - Placeholder for Map)
    OutlinedTextField(
      value = placeState.value,
      onValueChange = { placeState.value = it },
      label = { Text("Place") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    Button(onClick = { /*TODO: Open Map*/ }, modifier = Modifier.padding(bottom = 16.dp)) {
      Text("Choose Place from Map")
    }

    // Date Time Pickers
    DateTimeInput(
      label = "Start Date & Time",
      selectedDateTime = startDateState.value as String,
      onDateTimeChanged = { startDateState.value = it }
    )
    DateTimeInput(
      label = "End Date & Time",
      selectedDateTime = endDateState.value as String,
      onDateTimeChanged = { endDateState.value = it }
    )

    // Media Files Upload
    Text(
      text = "Media Files",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    MediaUploadComponent()

    // Additional Instructions (Text Area)
    OutlinedTextField(
      value = additionalInstructionsState.value,
      onValueChange = { additionalInstructionsState.value = it },
      label = { Text("Additional Instructions") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      minLines = 3
    )

    // Contact People (Dropdown)
    ContactPeopleDropdown(
      label = "Contact People",
      contactPeople = availableContactPeople,
      selectedPeople = selectedContactPeople.value,
      onPeopleSelected = {selectedContactPeople.value = it}

    )

    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = { /*TODO: Save data*/ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
      Text("Save")
    }
  }
}

//Composable Functions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiChoiceDropdown(
  label: String,
  options: Map<String, String>, // Key is ID, Value is Display Text
  selectedOptions: List<String>,
  onSelectionChanged: (List<String>) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Column {
    OutlinedTextField(
      readOnly = true,
      value = selectedOptions.joinToString(", ") { options[it] ?: it }, // Display selected names
      onValueChange = { },
      label = { Text(label) },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = expanded
        )
      },
      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
      enabled = false // To visually represent it as a dropdown
    )

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth()
    ) {
      options.forEach { (id, name) ->
        val isSelected = selectedOptions.contains(id)
        DropdownMenuItem(
          text = { Text(text = name) },
          onClick = {
            val mutableList = selectedOptions.toMutableList()
            if (isSelected) {
              mutableList.remove(id)
            } else {
              mutableList.add(id)
            }
            onSelectionChanged(mutableList.toList())
            expanded = false // Close the dropdown after selection
          },
          trailingIcon = {
            if (isSelected) {
              Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
            }
          }
        )
      }
    }
  }
}

@Composable
fun ActivityTypeChips(selectedType: String, onTypeSelected: (String) -> Unit) {
  val activityTypes = listOf("Session", "Event", "Campaign")

  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(activityTypes.size) { index ->
      val type = activityTypes[index]
      FilterChip(
        selected = (type == selectedType),
        onClick = { onTypeSelected(type) },
        label = { Text(type) }
      )
    }
  }
}

@Composable
fun DateTimeInput(label: String, selectedDateTime: String, onDateTimeChanged: (String) -> Unit) {
  var showDatePicker by remember { mutableStateOf(false) }

  OutlinedTextField(
    value = selectedDateTime,
    onValueChange = { /* Read-only */ },
    label = { Text(label) },
    readOnly = true,
    trailingIcon = {
      IconButton(onClick = { showDatePicker = true }) {
        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
      }
    },
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
  )

  if (showDatePicker) {
    // Implement DateTime Picker Popup
    AlertDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          // TODO: Implement DateTime selection and update startDateState/endDateState

          showDatePicker = false
        }) {
          Text("Confirm")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Cancel")
        }
      },
      title = { Text("Select Date and Time") },
      text = {
        // TODO: Implement DateTime selection UI here
        // Use a calendar and time picker library for Compose Multiplatform
        Text("DateTime Picker UI Goes Here")
      }
    )
  }
}

@Composable
fun MediaUploadComponent() {
  // Placeholder
  Button(onClick = { /*TODO: Implement Media Upload */ }) {
    Text("Upload Media Files")
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPeopleDropdown(
  label: String,
  contactPeople: List<ContactPeople>,
  selectedPeople: List<ContactPeople>,
  onPeopleSelected: (List<ContactPeople>) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Column {
    OutlinedTextField(
      readOnly = true,
      value = selectedPeople.joinToString(", ") { it.member.name },
      onValueChange = { },
      label = { Text(label) },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(
          expanded = expanded
        )
      },
      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
      enabled = false
    )

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth()
    ) {
      contactPeople.forEach { person ->
        val isSelected = selectedPeople.contains(person)
        DropdownMenuItem(
          text = { Text(text = person.member.name) },
          onClick = {
            val mutableList = selectedPeople.toMutableList()
            if (isSelected) {
              mutableList.remove(person)
            } else {
              mutableList.add(person)
            }
            onPeopleSelected(mutableList.toList())
            expanded = false
          },
          trailingIcon = {
            if (isSelected) {
              Icon(imageVector = Icons.Default.Check, contentDescription = "Selected")
            }
          }
        )
      }
    }
  }
}

@Preview
@Composable
fun PreviewOrganisationalActivityForm() {
  val sampleActivity = OrganisationalActivity(
    id = "eb179afc-aec7-4c87-8e51-9a9f192d4551",
    name = "Health Awareness Drive",
    description = "Campaign for spreading awareness about health and hygiene.",
    associatedOrganisation = listOf(
      "385933f4-2eca-46bb-bb31-50277d08d873",
      "36f4e51f-1517-4f1e-8395-d1d83b7c76ea"
    ),
    activityType = ActivityType.CAMPAIGN,
    place = "Bilāspur",
    startDateTime = "2025-02-20T15:38:26.337446",
    endDateTime = "2025-04-08T15:38:26.337446",
    mediaFiles = emptyList(),
    additionalInstructions = "Please follow COVID-19 protocols.",
    contactPeople = listOf(
      ContactPeople(
        member = Member(
          name = "Member 13",
          profileImage = "https://example.com/profile32.jpg",
          phoneNumber = "+12345678384"
        ),
        post = "Manager",
        priority = 4
      )
    )
  )

  MaterialTheme {
    OrganisationalActivityForm(activity = sampleActivity)
  }
}