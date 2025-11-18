package com.aryamahasangh.features.gurukul.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aryamahasangh.features.gurukul.viewmodel.CourseDropdownItem
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationReceivedItem
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationsReceivedUiState
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationsReceivedViewModel
import com.aryamahasangh.utils.WithTooltip

// Use a simple data class for entries

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
            Column(
              Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("courseDropdownItem_${item.id}"),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("courseDropdownItem_title_${item.id}")
              )
              if (item.shortDescription.isNotBlank()) {
                Text(
                  text = item.shortDescription,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("courseDropdownItem_desc_${item.id}")
                )
              }
              // Date & time row
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("courseDropdownItem_dates_${item.id}")
              ) {
                Icon(
                  Icons.Filled.CalendarMonth,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = item.formattedDateRange,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  color = MaterialTheme.colorScheme.onSurface,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("courseDropdownItem_date_${item.id}")
                )
                if (item.formattedTimeRange.isNotBlank()) {
                  Text(
                    text = "| ${item.formattedTimeRange}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("courseDropdownItem_time_${item.id}")
                  )
                }
              }
              // Place row
              if (item.place.isNotBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  modifier = Modifier.testTag("courseDropdownItem_place_${item.id}")
                ) {
                  Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                  )
                  Text(
                    text = item.place,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          },
          onClick = {
            onCourseSelected(item.id)
            onDropdownExpandChanged(false)
          }
        )
        // Add light divider except after the last item
        if(index != courses.lastIndex){
          HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
          )
        }
      }
    }
  }
}

@Composable
fun CourseRegistrationReceivedCard(
  item: CourseRegistrationReceivedItem,
  onReceiptClicked: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val uriHandler = LocalUriHandler.current
  Card(
    shape = RoundedCornerShape(4.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("registrationCard_${item.id}"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Top row: Photo on left, Registration info on right
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Photo on left (96x96)
        Box(
          modifier = Modifier.size(96.dp),
          contentAlignment = Alignment.Center
        ) {
          if (!item.photoUrl.isNullOrBlank()) {
            AsyncImage(
              model = item.photoUrl,
              contentDescription = "फोटो",
              modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(8.dp)),
              contentScale = ContentScale.Crop
            )
          } else {
            // Placeholder when no photo
            Box(
              modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "फोटो उपलब्ध नहीं",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Registration info on right
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Name and receipt button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = item.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f).testTag("registrationName_${item.id}")
            )
            if (!item.receiptUrl.isNullOrBlank()) {
              WithTooltip(tooltip = "भुगतान रसीद") {
                IconButton(
                  onClick = {
                    uriHandler.openUri(item.receiptUrl ?: "")
                    onReceiptClicked(item.receiptUrl ?: "")
                  },
                  modifier = Modifier.size(32.dp).testTag("registrationReceiptButton_${item.id}")
                ) {
                  Icon(
                    imageVector = Icons.Filled.ReceiptLong,
                    contentDescription = "रसीद देखने हेतु",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
              border = CardDefaults.outlinedCardBorder(),
              shape = RoundedCornerShape(4.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              if(item.dob.isEmpty()) return@Card
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "व्यक्तिगत विवरण", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(4.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  if (item.phoneNumber.isNotBlank()) {
                    Column(
                      verticalArrangement = Arrangement.spacedBy(2.dp),
                      modifier = Modifier
                        .testTag("registrationPhone_${item.id}")
                        .clickable {
                          val sanitized = item.phoneNumber.filter { it.isDigit() || it == '+' }
                          val telUri = if (sanitized.startsWith("+")) "tel:$sanitized" else "tel:+$sanitized"
                          uriHandler.openUri(telUri)
                        }
                    ) {
                      Text(text = "फोन", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(text = item.phoneNumber, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }

                  if (item.dob.isNotBlank()) {
                    Column(
                      verticalArrangement = Arrangement.spacedBy(2.dp),
                      modifier = Modifier
                        .testTag("registrationDob_${item.id}")
                    ) {
                      Text(text = "जन्मतिथि", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(text = item.dob, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }

                  if (item.qualification.isNotBlank()) {
                    Column(
                      verticalArrangement = Arrangement.spacedBy(2.dp),
                      modifier = Modifier
                        .testTag("registrationQualification_${item.id}")
                    ) {
                      Text(text = "योग्यता", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(text = item.qualification, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }

                  if (item.guardianName.isNotBlank()) {
                    Column(
                      verticalArrangement = Arrangement.spacedBy(2.dp),
                      modifier = Modifier
                        .testTag("registrationGuardian_${item.id}")
                    ) {
                      Text(text = "अभिभावक", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(text = item.guardianName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }

                  if (item.address.isNotBlank()) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                      modifier = Modifier
                        .testTag("registrationAddress_${item.id}")
                    ) {
                      Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                      Text(text = item.address, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                  }
                }
              }
            }

            if (item.date.isNotBlank() || item.place.isNotBlank()) {
              Card(
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(text = "सत्र विवरण", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                  HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                  Spacer(modifier = Modifier.height(4.dp))
                  FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                  ) {
                    if (item.date.isNotBlank()) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.testTag("registrationDate_${item.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Filled.CalendarMonth,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                          tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                          text = item.date,
                          style = MaterialTheme.typography.bodyMedium,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                    if (item.place.isNotBlank()) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.testTag("registrationPlace_${item.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Place,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                          tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                          text = item.place,
                          style = MaterialTheme.typography.bodyMedium,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
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

      // Recommendation below (full width)
      if (item.recommendation.isNotBlank()) {
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "संस्तुति:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = item.recommendation,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("registrationRecommendation_${item.id}")
          )
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
          CourseRegistrationReceivedCard(
            item = registration,
            onReceiptClicked = onReceiptClicked
          )
        }
      }
    }
  }
}
