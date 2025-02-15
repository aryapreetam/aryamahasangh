@file:OptIn(ExperimentalMaterial3Api::class)

package org.aryamahasangh.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aryamahasangh.OrganisationalActivitiesQuery.OrganisationalActivity
import org.aryamahasangh.type.ActivityType
import org.aryamahasangh.utils.formatShort
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.random.Random


// Mock Data (Replace with your actual data source)
val indianStates = listOf(
  "Andhra Pradesh",
  "Karnataka",
  "Maharashtra",
  "Tamil Nadu",
  "Uttar Pradesh"
)

val districtsByState = mapOf(
  "Andhra Pradesh" to listOf("Guntur", "Krishna", "Visakhapatnam"),
  "Karnataka" to listOf("Bangalore", "Mysore", "Mangalore"),
  "Maharashtra" to listOf("Mumbai", "Pune", "Nagpur"),
  "Tamil Nadu" to listOf("Chennai", "Coimbatore", "Madurai"),
  "Uttar Pradesh" to listOf("Lucknow", "Varanasi", "Agra")
)

@Composable
@Preview
fun JoinUsScreen() {
    Column(modifier = Modifier.padding(16.dp)){
      Text("नमस्ते जी,\n" +
          "आप निर्मात्री सभा द्वारा आयोजित दो दिवसीय लघु गुरुकुल पाठ्यक्रम पूर्ण कर आर्य महासंघ से जुड़ सकते है। \n" +
          "निचे आप अपना क्षेत्र चुनकर आपके क्षेत्रों में आयोजित होने वाले सत्रों के विवरण देख सकते है। ")

      ActivityForm()
    }
}


@Composable
fun ActivityForm() {
  var selectedState by remember { mutableStateOf<String?>(null) }
  var selectedDistrict by remember { mutableStateOf<String?>(null) }
  var activities by remember { mutableStateOf<List<OrganisationalActivity>>(emptyList()) }

  // Reset district on state change
  LaunchedEffect(selectedState) {
    selectedDistrict = null
  }

  val showActivitiesEnabled = selectedState != null

  Column(modifier = Modifier.padding(top = 8.dp)) {
    // State Selection
//    Text(
//      text = "Select State",
//      style = MaterialTheme.typography.labelLarge,
//      modifier = Modifier.padding(bottom = 8.dp)
//    )
    StateDropdown(
      states = indianStates,
      selectedState = selectedState,
      onStateSelected = { selectedState = it }
    )
    Spacer(modifier = Modifier.height(4.dp))

    // District Selection (Conditional)
    if (selectedState != null) {
      val districts = districtsByState[selectedState] ?: emptyList()
//      Text(
//        text = "Select District (Optional)",
//        style = MaterialTheme.typography.labelLarge,
//        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
//      )
      DistrictDropdown(
        districts = districts,
        selectedDistrict = selectedDistrict,
        onDistrictSelected = { selectedDistrict = it }
      )
    }

    // Show Activities Button
    Button(
      onClick = {
        // Simulate fetching activities (Replace with your API call)
        activities = fetchActivities(selectedState, selectedDistrict)
      },
      enabled = showActivitiesEnabled,
      modifier = Modifier.align(Alignment.Start).padding(top = 8.dp, bottom = 8.dp)
    ) {
      Text("Show Activities")
    }

    // Activities List
    if (activities.isNotEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f) // Limits height to remaining space
      ) {
        ActivitiesList(activities = activities)
      }
    }
  }
}

@Composable
fun StateDropdown(states: List<String>, selectedState: String?, onStateSelected: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Column {
    ExposedDropdownMenuBox(
      expanded = false,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        readOnly = true,
        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        value = selectedState ?: "Select State",
        label = { Text("State") },
        onValueChange = {
        },
        placeholder = { Text("Color") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        colors = ExposedDropdownMenuDefaults.textFieldColors(),
        enabled = false // to display as a dropdown
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        states.forEach { selectionOption ->
          key(selectionOption) {
            DropdownMenuItem(
              text = { Text(selectionOption) },
              onClick = {
                onStateSelected(selectionOption)
                expanded = false
              },
              contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
          }
        }
      }
    }
//    OutlinedTextField(
//      readOnly = true,
//      value = selectedState ?: "Select State",
//      onValueChange = { },
//      label = { Text("State") },
//      trailingIcon = {
//        ExposedDropdownMenuDefaults.TrailingIcon(
//          expanded = expanded
//        )
//      },
//      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
//      enabled = false // to display as a dropdown
//    )
//    DropdownMenu(
//      expanded = expanded,
//      onDismissRequest = { expanded = false },
//    ) {
//      states.forEach { state ->
//        DropdownMenuItem(
//          text = { Text(state) },
//          onClick = {
//            onStateSelected(state)
//            expanded = false
//          },
//          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
//        )
//      }
//    }
  }
}

@Composable
fun DistrictDropdown(districts: List<String>, selectedDistrict: String?, onDistrictSelected: (String?) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Column {
    ExposedDropdownMenuBox(
      expanded = false,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        readOnly = true,
        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        value = selectedDistrict ?: "Select District (Optional)",
        label = { Text("District") },
        onValueChange = {
        },
        placeholder = { Text("Color") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        colors = ExposedDropdownMenuDefaults.textFieldColors(),
        enabled = false // to display as a dropdown
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        DropdownMenuItem(
          text = { Text("None") },
          onClick = {
            onDistrictSelected(null)
            expanded = false
          }
        )
        districts.forEach { selectionOption ->
          key(selectionOption) {
            DropdownMenuItem(
              text = { Text(selectionOption) },
              onClick = {
                onDistrictSelected(selectionOption)
                expanded = false
              },
              contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
          }
        }
      }
    }

//    OutlinedTextField(
//      readOnly = true,
//      value = selectedDistrict ?: "Select District (Optional)",
//      onValueChange = { },
//      label = { Text("District") },
//      trailingIcon = {
//        ExposedDropdownMenuDefaults.TrailingIcon(
//          expanded = expanded
//        )
//      },
//      modifier = Modifier.fillMaxWidth().clickable { expanded = true },
//      enabled = false // To display as a dropdown
//    )

//    DropdownMenu(
//      expanded = expanded,
//      onDismissRequest = { expanded = false },
//    ) {
//      DropdownMenuItem(
//        text = { Text("None") },
//        onClick = {
//          onDistrictSelected(null)
//          expanded = false
//        }
//      )
//      districts.forEach { district ->
//        DropdownMenuItem(
//          text = { Text(district) },
//          onClick = {
//            onDistrictSelected(district)
//            expanded = false
//          }
//        )
//      }
//    }
  }
}

@Preview
@Composable
fun ActivityListPreview(){
  ActivitiesList(fetchActivities("Maharashtra", "Mumbai"))
}

@Composable
fun ActivitiesList(activities: List<OrganisationalActivity>) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    items(activities){
      ActivityListItem(it)
      VerticalDivider(modifier = Modifier.padding(vertical = 2.dp))
    }
  }
}

@Composable
fun ActivityListItem(activity: OrganisationalActivity, handleOnClick: () -> Unit = {}) {

  val startDate = formatShort(activity.startDateTime)
  val endDate = formatShort(activity.endDateTime)

  ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp),
    onClick = handleOnClick
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = activity.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(text = activity.description, style = MaterialTheme.typography.bodyMedium)
      Spacer(modifier = Modifier.height(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Date", tint = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Start: $startDate", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "End: $endDate", style = MaterialTheme.typography.bodySmall)
      }
      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Place", tint = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Place: ${activity.place}", style = MaterialTheme.typography.bodySmall)
      }
      Spacer(modifier = Modifier.height(4.dp))

      Text(text = "Type: ${activity.activityType}", style = MaterialTheme.typography.bodySmall)
    }
  }
}

// Helper Functions

fun fetchActivities(state: String?, district: String?): List<OrganisationalActivity> {
  // Simulate fetching activities from a data source
  // Replace this with your actual API call or data retrieval logic
  val filteredActivities = mutableListOf<OrganisationalActivity>()
  val allActivities = listOf(
      OrganisationalActivity(
        id = "1",
        name = "Health Camp",
        description = "Free health checkup camp",
        startDateTime = "2025-03-15T10:00:00",
        endDateTime = "2025-03-15T16:00:00",
        activityType = ActivityType.EVENT,
        place = "Local Hospital, Mumbai"
      )
    ,
      OrganisationalActivity(
        id = "2",
        name = "Tree Plantation Drive",
        description = "Planting trees in the city park",
        startDateTime = "2025-04-22T09:00:00",
        endDateTime = "2025-04-22T12:00:00",
        activityType = ActivityType.CAMPAIGN,
        place = "City Park, Chennai"
      )
    ,
      OrganisationalActivity(
        id = "3",
        name = "Awareness Session on Hygiene",
        description = "Session on maintaining personal hygiene",
        startDateTime = "2025-05-10T14:00:00",
        endDateTime = "2025-05-10T15:00:00",
        activityType = ActivityType.SESSION,
        place = "Community Hall, Bangalore"
    ),
      OrganisationalActivity(
        id = "4",
        name = "Health Awareness Drive",
        description = "Campaign for spreading awareness about health and hygiene.",
        startDateTime = "2025-02-20T15:38:26.337446",
        endDateTime = "2025-04-08T15:38:26.337446",
        activityType = ActivityType.CAMPAIGN,
        place = "Bilāspur"
    )
  )

  //Filter activities according to state
  val stateActivities = allActivities.filter {
      organisationalActivity ->
    organisationalActivity.place.contains(state ?: "", ignoreCase = true)
  }

  //Then filter further with district if needed
  if(district != null){
    stateActivities.filter {
        organisationalActivity ->
      organisationalActivity.place.contains(district, ignoreCase = true)
    }
  }
  else{
    return stateActivities;
  }


  return allActivities.take(Random.nextInt(1,allActivities.size - 1))
}

@Preview
@Composable
fun PreviewActivityForm() {
  MaterialTheme {
    ActivityForm()
  }
}
