package com.aryamahasangh.features.gurukul.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationsReceivedViewModel

// Use a simple data class for entries
data class CourseRegistrationReceivedItem(
  val id: String,
  val name: String,
  val satrDate: String,
  val satrPlace: String,
  val recommendation: String,
  val receiptUrl: String?
)

data class CourseDropdownItem(
  val id: String,
  val name: String,
  val shortDescription: String,
  val startDate: String, // formatted
  val endDate: String, // formatted, may be blank
  val place: String, // district/state, may be blank
)

data class CourseRegistrationsReceivedUiState(
  val isLoading: Boolean = false,
  val isError: Boolean = false,
  val courses: List<CourseDropdownItem> = emptyList(),
  val selectedCourseId: String? = null,
  val registrations: List<CourseRegistrationReceivedItem> = emptyList(),
  val errorMessage: String? = null, // message_code for snackbar
  val isDropdownExpanded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseRegistrationsReceivedScreen(
  viewModel: CourseRegistrationsReceivedViewModel,
  modifier: Modifier = Modifier
) {
  val uiState = viewModel.uiState.collectAsState().value
  val uriHandler = LocalUriHandler.current
  Column(modifier = modifier.widthIn(max = 600.dp).padding(vertical = 16.dp, horizontal = 8.dp)) {
    CourseDropdown(
      courses = uiState.courses,
      selectedCourseId = uiState.selectedCourseId,
      isDropdownExpanded = uiState.isDropdownExpanded,
      isEnabled = !uiState.isLoading,
      onDropdownExpandChanged = viewModel::onDropdownExpandChanged,
      onCourseSelected = viewModel::onCourseSelected
    )
    Spacer(modifier = Modifier.height(8.dp))
    RegistrationsList(
      uiState = uiState,
      uriHandler = uriHandler,
      onReceiptClicked = viewModel::onReceiptClicked
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDropdown(
  courses: List<CourseDropdownItem>,
  selectedCourseId: String?,
  isDropdownExpanded: Boolean,
  isEnabled: Boolean,
  onDropdownExpandChanged: (Boolean) -> Unit,
  onCourseSelected: (String) -> Unit
) {
  ExposedDropdownMenuBox(
    expanded = isDropdownExpanded,
    onExpandedChange = onDropdownExpandChanged,
    modifier = Modifier.fillMaxWidth().testTag("courseDropdownBox")
  ) {
    OutlinedTextField(
      value = selectedCourseId?.let { id ->
        courses.find { it.id == id }?.name ?: ""
      } ?: "कक्षा चुनिए",
      onValueChange = {},
      readOnly = true,
      enabled = isEnabled,
      label = { Text("कक्षा") },
      placeholder = { Text("कक्षा चुनिए") },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
      modifier = Modifier.fillMaxWidth().menuAnchor().testTag("courseDropdown")
    )
    ExposedDropdownMenu(
      expanded = isDropdownExpanded,
      onDismissRequest = { onDropdownExpandChanged(false) }
    ) {
      courses.forEachIndexed { index, item ->
        DropdownMenuItem(
          text = {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
              Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = item.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${item.startDate}${if (item.endDate.isNotBlank()) " - ${item.endDate}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (item.place.isNotBlank()) {
                Text(
                  text = item.place,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          },
          onClick = {
            onCourseSelected(item.id)
            onDropdownExpandChanged(false)
          },
          modifier = Modifier.testTag("courseDropdownItem_${item.id}")
        )
        if(index != courses.lastIndex){
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}

@Composable
fun RegistrationsList(
  uiState: CourseRegistrationsReceivedUiState,
  uriHandler: androidx.compose.ui.platform.UriHandler,
  onReceiptClicked: (String) -> Unit
) {
  when {
    uiState.isLoading -> {
      Box(modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
      }
    }

    uiState.isError -> {
      Box(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = uiState.errorMessage ?: "त्रुटि आ गई, कृपया पुनः प्रयास करें",
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.align(Alignment.Center).padding(8.dp).testTag("courseRegistrationsError")
        )
      }
    }

    uiState.selectedCourseId == null -> {
      Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Text("कृपया कक्षा चुनिए", style = MaterialTheme.typography.bodyMedium)
      }
    }

    uiState.registrations.isEmpty() -> {
      Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Text(
          text = "इस कक्षा के लिए कोई पंजीकरण प्राप्त नहीं हुए।",
          modifier = Modifier.align(Alignment.Center).testTag("courseRegistrationsEmpty")
        )
      }
    }

    else -> {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().testTag("courseRegistrationsList"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(uiState.registrations) { registration ->
          Card(
            modifier = Modifier.fillMaxWidth().testTag("registrationCard_${registration.id}")
          ) {
            Row(
              modifier = Modifier.padding(16.dp).fillMaxWidth(),
              verticalAlignment = Alignment.Top
            ) {
              Column(Modifier.weight(1f)) {
                Text(
                  text = registration.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("registrationName_${registration.id}")
                )
                if (registration.satrDate.isNotBlank() || registration.satrPlace.isNotBlank()) {
                  Text(
                    text = "सत्र दिनांक: ${registration.satrDate}${if (registration.satrPlace.isNotBlank()) " (स्थान: ${registration.satrPlace})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("registrationDatePlace_${registration.id}")
                  )
                }
                if (registration.recommendation.isNotBlank()) {
                  Text(
                    text = registration.recommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                      .testTag("registrationRecommendation_${registration.id}")
                  )
                }
              }
              if (!registration.receiptUrl.isNullOrBlank()) {
                IconButton(
                  onClick = {
                    uriHandler.openUri(registration.receiptUrl ?: ""); onReceiptClicked(
                    registration.receiptUrl ?: ""
                  )
                  },
                  modifier = Modifier.testTag("registrationReceiptButton_${registration.id}")
                ) {
                  Icon(
                    imageVector = Icons.Filled.ReceiptLong,
                    contentDescription = "रसीद देखने हेतु",
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
