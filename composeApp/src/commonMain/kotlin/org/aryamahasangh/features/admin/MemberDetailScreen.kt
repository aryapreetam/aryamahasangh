package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.aryamahasangh.features.arya_nirman.convertDates
import org.aryamahasangh.navigation.LocalSnackbarHostState
import org.aryamahasangh.network.bucket
import org.aryamahasangh.screens.DistrictDropdown
import org.aryamahasangh.screens.StateDropdown
import org.aryamahasangh.screens.indianStatesToDistricts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
  memberId: String,
  viewModel: AdminViewModel,
  onNavigateBack: () -> Unit = {}
) {
  val uiState by viewModel.memberDetailUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val scope = rememberCoroutineScope()

  // Local state for editing
  var editableName by remember { mutableStateOf("") }
  var editablePhoneNumber by remember { mutableStateOf("") }
  var editableEmail by remember { mutableStateOf("") }
  var editableEducationalQualification by remember { mutableStateOf("") }
  var editableAddress by remember { mutableStateOf("") }
  var editableState by remember { mutableStateOf("") }
  var editableDistrict by remember { mutableStateOf("") }
  var editablePincode by remember { mutableStateOf("") }

  LaunchedEffect(uiState.member) {
    uiState.member?.let { member ->
      editableName = member.name
      editablePhoneNumber = member.phoneNumber
      editableEmail = member.email
      editableEducationalQualification = member.educationalQualification
      editableAddress = member.address
      editableState = member.state
      editableDistrict = member.district
      editablePincode = member.pincode
    }
  }

  LaunchedEffect(memberId) {
    viewModel.loadMemberDetail(memberId)
  }

  LaunchedEffect(uiState.updateSuccess) {
    if (uiState.updateSuccess) {
      snackbarHostState.showSnackbar("सदस्य सफलतापूर्वक अपडेट किया गया")
      viewModel.resetUpdateState()
    }
  }

  LaunchedEffect(uiState.error) {
    uiState.error?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.resetUpdateState()
    }
  }

  if (uiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      // Profile Section
      ProfileSection(
        member = uiState.member,
        isEditing = uiState.isEditingProfile,
        onEditClick = { viewModel.setEditingProfile(true) },
        onPhotoUpdate = { photoUrl ->
          viewModel.updateMemberPhoto(memberId, photoUrl)
        }
      )
    }

    item {
      // Details Section
      DetailsSection(
        member = uiState.member,
        isEditing = uiState.isEditingDetails,
        isUpdating = uiState.isUpdating,
        editableName = editableName,
        editablePhoneNumber = editablePhoneNumber,
        editableEmail = editableEmail,
        editableEducationalQualification = editableEducationalQualification,
        editableAddress = editableAddress,
        editableState = editableState,
        editableDistrict = editableDistrict,
        editablePincode = editablePincode,
        onNameChange = { editableName = it },
        onPhoneNumberChange = { editablePhoneNumber = it },
        onEmailChange = { editableEmail = it },
        onEducationalQualificationChange = { editableEducationalQualification = it },
        onAddressChange = { editableAddress = it },
        onStateChange = {
          editableState = it
          editableDistrict = ""
        },
        onDistrictChange = { editableDistrict = it },
        onPincodeChange = { editablePincode = it },
        onEditClick = { viewModel.setEditingDetails(true) },
        onSaveClick = {
          viewModel.updateMemberDetails(
            memberId = memberId,
            name = editableName,
            phoneNumber = editablePhoneNumber,
            educationalQualification = editableEducationalQualification,
            email = editableEmail
          )
        },
        onCancelClick = {
          viewModel.setEditingDetails(false)
          // Reset to original values
          uiState.member?.let { member ->
            editableName = member.name
            editablePhoneNumber = member.phoneNumber
            editableEmail = member.email
            editableEducationalQualification = member.educationalQualification
            editableAddress = member.address
            editableState = member.state
            editableDistrict = member.district
            editablePincode = member.pincode
          }
        }
      )
    }

    // Show organisations and activities
    if (uiState.member != null) {
      item {
        OrganisationsSection(organisations = uiState.member!!.organisations)
      }

      item {
        ActivitiesSection(activities = uiState.member!!.activities)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePhotoItem(
  file: PlatformFile,
  onRemoveFile: () -> Unit,
  modifier: Modifier = Modifier
) {
  var bytes by remember(file) { mutableStateOf<ByteArray?>(null) }

  LaunchedEffect(file) {
    bytes =
      if (file.supportsStreams()) {
        val size = file.getSize()
        if (size != null && size > 0L) {
          val buffer = ByteArray(size.toInt())
          val tmpBuffer = ByteArray(1000)
          var totalBytesRead = 0
          file.getStream().use {
            while (it.hasBytesAvailable()) {
              val numRead = it.readInto(tmpBuffer, 1000)
              tmpBuffer.copyInto(
                buffer,
                destinationOffset = totalBytesRead,
                endIndex = numRead
              )
              totalBytesRead += numRead
            }
          }
          buffer
        } else {
          file.readBytes()
        }
      } else {
        file.readBytes()
      }
  }

  // Use a container that can accommodate the remove button without clipping
  Box { // Add padding to ensure button isn't clipped
    Column(modifier = modifier.padding(6.dp)) {
      bytes?.let { imageBytes ->
        AsyncImage(
          model = imageBytes,
          contentDescription = "Profile Image",
          contentScale = ContentScale.Crop,
          modifier =
            Modifier
              .fillMaxSize()
              .clip(CircleShape)
        )
      }
    }

    // Remove button positioned in top-right, with proper spacing
    Surface(
      color = MaterialTheme.colorScheme.errorContainer,
      shape = CircleShape,
      modifier =
        Modifier
          .align(Alignment.TopEnd)
          .size(28.dp)
    ) {
      IconButton(
        onClick = onRemoveFile,
        modifier = Modifier.size(28.dp)
      ) {
        Icon(
          Icons.Filled.Close,
          modifier = Modifier.size(16.dp),
          contentDescription = "Remove Photo",
          tint = MaterialTheme.colorScheme.onErrorContainer
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSection(
  member: MemberDetail?,
  isEditing: Boolean,
  onEditClick: () -> Unit,
  onPhotoUpdate: (String) -> Unit
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  val launcher =
    rememberFilePickerLauncher(
      type = PickerType.Image,
      mode = PickerMode.Single,
      title = "Select profile photo"
    ) { file ->
      if (file != null) {
        scope.launch {
          try {
            // Show immediate upload feedback
            val snackbarJob =
              launch {
                snackbarHostState.showSnackbar(
                  message = "🔄 Uploading profile photo...",
                  duration = SnackbarDuration.Indefinite
                )
              }

            val uploadResponse =
              bucket.upload(
                path = "profile_${Clock.System.now().epochSeconds}.jpg",
                data = file.readBytes()
              )
            val imageUrl = bucket.publicUrl(uploadResponse.path)

            // Cancel the upload progress snackbar
            snackbarJob.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()

            // Update the photo
            onPhotoUpdate(imageUrl)

            snackbarHostState.showSnackbar("✅ Profile photo updated successfully")
          } catch (e: Exception) {
            snackbarHostState.showSnackbar(
              message = "❌ Failed to upload photo: ${e.message}",
              actionLabel = "Close"
            )
            println("error uploading photo: $e")
          }
        }
      }
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Profile Image
      AsyncImage(
        model =
          member?.profileImage?.ifEmpty { "https://via.placeholder.com/100" }
            ?: "https://via.placeholder.com/100",
        contentDescription = "Profile Image",
        modifier =
          Modifier
            .size(100.dp)
            .clip(CircleShape)
      )

      Spacer(modifier = Modifier.width(16.dp))

      // Member Info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = member?.name ?: "Unknown",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        if (member != null) {
          val address =
            buildString {
              if (member.address.isNotEmpty()) append(member.address)
              if (member.district.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(member.district)
              }
              if (member.state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(member.state)
              }
              if (member.pincode.isNotEmpty()) {
                if (isNotEmpty()) append(" - ")
                append(member.pincode)
              }
            }
          if (address.isNotEmpty()) {
            Text(
              text = address,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }

      // Edit/Remove/Add photo button - positioned to the right of profile image
      if (!isEditing) {
        TooltipBox(
          positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
          tooltip = {
            PlainTooltip {
              Text("फोटो बदलें")
            }
          },
          state = rememberTooltipState()
        ) {
          IconButton(onClick = { launcher.launch() }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Photo")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSection(
  member: MemberDetail?,
  isEditing: Boolean,
  isUpdating: Boolean,
  editableName: String,
  editablePhoneNumber: String,
  editableEmail: String,
  editableEducationalQualification: String,
  editableAddress: String,
  editableState: String,
  editableDistrict: String,
  editablePincode: String,
  onNameChange: (String) -> Unit,
  onPhoneNumberChange: (String) -> Unit,
  onEmailChange: (String) -> Unit,
  onEducationalQualificationChange: (String) -> Unit,
  onAddressChange: (String) -> Unit,
  onStateChange: (String) -> Unit,
  onDistrictChange: (String) -> Unit,
  onPincodeChange: (String) -> Unit,
  onEditClick: () -> Unit,
  onSaveClick: () -> Unit,
  onCancelClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "विवरण",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        if (!isEditing) {
          IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Details")
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (isEditing) {
        // Editable fields
        OutlinedTextField(
          value = editableName,
          onValueChange = onNameChange,
          label = { Text("नाम*") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = editablePhoneNumber,
          onValueChange = onPhoneNumberChange,
          label = { Text("दूरभाष*") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = editableEmail,
          onValueChange = onEmailChange,
          label = { Text("ईमेल") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = editableEducationalQualification,
          onValueChange = onEducationalQualificationChange,
          label = { Text("शैक्षणिक योग्यता") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = editableAddress,
          onValueChange = onAddressChange,
          label = { Text("पता") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          StateDropdown(
            states = indianStatesToDistricts.keys.toList(),
            selectedState = editableState.ifEmpty { null },
            onStateSelected = onStateChange,
            modifier = Modifier.weight(1f)
          )

          val districts = indianStatesToDistricts[editableState] ?: emptyList()
          DistrictDropdown(
            districts = districts,
            selectedDistrict = editableDistrict.ifEmpty { null },
            onDistrictSelected = { onDistrictChange(it ?: "") },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = editablePincode,
          onValueChange = onPincodeChange,
          label = { Text("पिनकोड") },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
          TextButton(
            onClick = onCancelClick,
            enabled = !isUpdating
          ) {
            Text("Cancel")
          }

          Button(
            onClick = onSaveClick,
            enabled = !isUpdating && editableName.isNotBlank() && editablePhoneNumber.isNotBlank()
          ) {
            if (isUpdating) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Save")
          }
        }
      } else {
        // Display-only fields
        member?.let { memberData ->
          DetailItem("नाम", memberData.name)
          DetailItem("दूरभाष", memberData.phoneNumber)
          DetailItem("ईमेल", memberData.email)
          DetailItem("शैक्षणिक योग्यता", memberData.educationalQualification)
          DetailItem("पता", memberData.address)
          DetailItem("राज्य", memberData.state)
          DetailItem("जिला", memberData.district)
          DetailItem("पिनकोड", memberData.pincode)
        }
      }
    }
  }
}

@Composable
private fun DetailItem(
  label: String,
  value: String
) {
  if (value.isNotEmpty()) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge
      )
    }
  }
}

@Composable
private fun OrganisationsSection(organisations: List<OrganisationInfo>) {
  if (organisations.isNotEmpty()) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(4.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
      ) {
        Text(
          text = "संबधित संस्थाएं",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        organisations.forEach { org ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = org.logo.ifEmpty { "https://via.placeholder.com/40" },
              contentDescription = "Organisation Logo",
              modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = org.name, style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    }
  }
}

@Composable
private fun ActivitiesSection(activities: List<ActivityInfo>) {
  if (activities.isNotEmpty()) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(4.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
      ) {
        Text(
          text = "संबधित गतिविधियाँ",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        activities.forEach { activity ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(4.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
              Text(
                text = activity.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                text =
                  buildAnnotatedString {
                    val subtleTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val (dateRange, timeRange) = convertDates(activity.startDatetime, activity.endDatetime)
                    withStyle(
                      style =
                        SpanStyle(
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 16.sp,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                      append(dateRange)
                    }
                    append(" | ")
                    withStyle(style = SpanStyle(fontSize = 13.sp, color = subtleTextColor)) {
                      append(timeRange)
                    }
                  },
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = "${activity.district}, ${activity.state}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
          }
        }
      }
    }
  }
}
