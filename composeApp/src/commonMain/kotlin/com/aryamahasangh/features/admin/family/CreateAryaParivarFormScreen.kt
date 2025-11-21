package com.aryamahasangh.features.admin.family

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aryamahasangh.components.*
import com.aryamahasangh.features.admin.member.MemberCollectionType
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.ui.components.buttons.*
import com.aryamahasangh.utils.WithTooltip
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateAryaParivarFormScreen(
  viewModel: FamilyViewModel,
  onNavigateBack: () -> Unit,
  onFamilyCreated: (String) -> Unit = {}, // Navigate to family detail
  editingFamilyId: String? = null
) {
  val uiState by viewModel.createFamilyUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val focusManager = LocalFocusManager.current

  // Check if we're in edit mode
  val isEditMode = editingFamilyId != null

  // Load family data for editing
  LaunchedEffect(editingFamilyId) {
    if (editingFamilyId != null) {
      // Load family detail for editing
      viewModel.loadFamilyForEditing(editingFamilyId)
    }
  }

  // Form validation states
  var familyNameError by rememberSaveable { mutableStateOf<String?>(null) }
  var aryaSamajError by rememberSaveable { mutableStateOf<String?>(null) }
  var membersError by rememberSaveable { mutableStateOf<String?>(null) }
  var addressError by rememberSaveable { mutableStateOf<String?>(null) }

  // Validator function for SubmitButton
  fun validateForm(): Boolean {
    var isValid = true

    // Validate family name
    if (uiState.familyName.isBlank()) {
      familyNameError = "परिवार का नाम अपेक्षित है"
      isValid = false
    } else {
      familyNameError = null
    }

    // Validate Arya Samaj selection (now mandatory)
    if (uiState.selectedAryaSamaj == null) {
      aryaSamajError = "आर्य समाज चुनना आवश्यक है"
      isValid = false
    } else {
      aryaSamajError = null
    }

    // Validate members
    if (uiState.familyMembers.isEmpty()) {
      membersError = "कम से कम एक सदस्य जोड़ना आवश्यक है"
      isValid = false
    } else {
      val hasHead = uiState.familyMembers.any { it.isHead }
      if (!hasHead) {
        membersError = "परिवार प्रमुख चुनना आवश्यक है"
        isValid = false
      } else {
        membersError = null
      }
    }

    // Validate address
    if (uiState.addressData.address.isBlank() || uiState.addressData.state.isBlank()) {
      addressError = "पूर्ण पता अपेक्षित है"
      isValid = false
    } else {
      addressError = null
    }

    return isValid
  }

  // Track if form has been modified for unsaved changes
  val hasUnsavedChanges =
    uiState.familyName.isNotBlank() ||
      uiState.selectedAryaSamaj != null ||
      uiState.imagePickerState.hasImages ||
      uiState.familyMembers.isNotEmpty() ||
      uiState.addressData.address.isNotBlank()

  // Handle success: Navigate immediately on submitSuccess (snackbar message handled globally)
  LaunchedEffect(uiState.submitSuccess) {
    if (uiState.submitSuccess) {
      viewModel.clearCreateFamilyState()
      if (isEditMode) {
        onFamilyCreated(editingFamilyId!!)
      } else {
        onFamilyCreated(uiState.familyId)
      }
    }
  }

  // Initialize data when screen loads
  LaunchedEffect(Unit) {
    // Load available AryaSamajs
    viewModel.loadAryaSamajs()
    // Load available members without family - call with empty string to get all members
    viewModel.searchMembersWithoutFamily("")
  }

  // Handle errors
  LaunchedEffect(uiState.error) {
    uiState.error?.let { error ->
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "ठीक है"
      )
      viewModel.clearError()
    }
  }

  // Calculate age for sorting members
  fun calculateAge(dob: LocalDate?): Int {
    if (dob == null) return 0
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return now.year - dob.year
  }

  // Sort family members by age (descending)
  val sortedFamilyMembers =
    uiState.familyMembers.sortedByDescending {
      calculateAge(parseDate(it.member.address)) // Assuming DOB is stored somewhere
    }


  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp)
  ) {
    // Header
    Text(
      text = if (isEditMode) "परिवार का संपादन करें" else "नया परिवार जोड़ें",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 24.dp)
    )

    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Family Name
      item {
        OutlinedTextField(
          value = uiState.familyName,
          onValueChange = {
            viewModel.updateFamilyName(it)
            familyNameError = null
          },
          label = { Text("परिवार का नाम *") },
          placeholder = { Text("शर्मा परिवार, गुप्ता परिवार इत्यादि") },
          modifier = Modifier.width(400.dp),
          singleLine = true,
          isError = familyNameError != null,
          supportingText = familyNameError?.let { { Text(it) } },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          keyboardActions =
            KeyboardActions(
              onNext = { focusManager.moveFocus(FocusDirection.Next) }
            )
        )
      }

      // Arya Samaj Selection
      item {
        Column {
          AryaSamajSelector(
            selectedAryaSamaj = uiState.selectedAryaSamaj,
            onAryaSamajSelected = { aryaSamaj ->
              viewModel.updateSelectedAryaSamaj(aryaSamaj)
              aryaSamajError = null
            },
            label = "आर्य समाज *",
            modifier = Modifier.width(400.dp),
          )

          // Show error message if validation failed
          aryaSamajError?.let { error ->
            Text(
              text = error,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
          }
        }
      }

      // Family Photos
      item {
        ImagePickerComponent(
          state = uiState.imagePickerState,
          onStateChange = viewModel::updateImagePickerState,
          config =
            ImagePickerConfig(
              label = "परिवार के चित्र",
              allowMultiple = true,
              maxImages = 5,
              showPreview = true,
              type = ImagePickerType.IMAGE,
              enableBackgroundCompression = true,
              compressionTargetKb = 100, // 100KB for family images
              showCompressionProgress = true
            ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Family Members Section
      item {
        // Convert FamilyMemberForCreation to MembersState for MembersComponent
        val membersState =
          remember(uiState.familyMembers) {
            MembersState(
              members =
                uiState.familyMembers.associate { familyMember ->
                  familyMember.member to Pair("", 0) // No posts needed for family members
                }
            )
          }

        MembersComponent(
          state = membersState,
          onStateChange = { newState ->
            // Convert MembersState back to FamilyMemberForCreation
            val newFamilyMembers =
              newState.members.keys.map { member ->
                // Find existing family member data or create new
                val existingFamilyMember = uiState.familyMembers.find { it.member.id == member.id }
                FamilyMemberForCreation(
                  member = member,
                  isHead = existingFamilyMember?.isHead ?: false,
                  relationToHead = existingFamilyMember?.relationToHead
                )
              }
            viewModel.updateFamilyMembers(newFamilyMembers)
          },
          config =
            MembersConfig(
              label = "परिवार के सदस्य *",
              addButtonText = "सदस्य जोड़ें",
              isMandatory = true,
              minMembers = 1,
              showMemberCount = true,
              editMode = MembersEditMode.FAMILY_MEMBERS,
              choiceType = MembersChoiceType.MULTIPLE,
              memberCollectionType = MemberCollectionType.MEMBERS_NOT_IN_FAMILY,
              // Family-specific callbacks
              onFamilyHeadChanged = { memberId, isHead ->
                viewModel.updateMemberHead(memberId, isHead)
              },
              onFamilyRelationChanged = { memberId, relation ->
                viewModel.updateMemberRelation(memberId, relation)
              },
              getFamilyHead = { memberId ->
                uiState.familyMembers.find { it.member.id == memberId }?.isHead ?: false
              },
              getFamilyRelation = { memberId ->
                uiState.familyMembers.find { it.member.id == memberId }?.relationToHead
              }
            ),
          error = membersError
        )
      }

      // Address Selection Section
      item {
        AddressSelectionSection(
          memberAddresses = uiState.memberAddresses,
          selectedAddressIndex = uiState.selectedAddressIndex,
          addressData = uiState.addressData,
          onAddressIndexSelected = viewModel::updateSelectedAddressIndex,
          onAddressDataChange = viewModel::updateAddressData,
          addressError = addressError
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Submit Button
    SubmitButtonOld(
      text = if (isEditMode) "परिवार अद्यतन करें" else "परिवार बनाएं",
      onSubmit = {
        // Execute family creation/update
        if (isEditMode) {
          viewModel.updateFamily(editingFamilyId!!)
        } else {
          viewModel.createFamily()
        }
      },
      config = SubmitButtonConfig(
        validator = {
          if (!validateForm()) SubmissionError.ValidationFailed else null
        },
        texts = SubmitButtonTexts(
          submittingText = "परिवार बनाया जा रहा है...",
          successText = if (isEditMode) "अद्यतन सफल!" else "सफल!"
        )
      ),
      callbacks = object : SubmitCallbacks {
        override fun onError(error: SubmissionError) {
          // Submission errors are handled by GlobalMessageManager in ViewModel
          // No additional action needed here
        }
      }
    )
  }
}

@Composable
private fun AddressSelectionItem(
  address: AddressWithMemberId?,
  isSelected: Boolean,
  onSelect: () -> Unit,
  isNewAddressOption: Boolean = false
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable { onSelect() }
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = isSelected,
      onClick = onSelect
    )

    Spacer(modifier = Modifier.width(8.dp))

    if (isNewAddressOption) {
      Text(
        text = "इनमे से कोई नहीं (पता नए से लिखना है)",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
      )
    } else {
      Column {
        Text(
          text = address?.addressData?.address ?: "",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
        Text(
          text = "${address?.addressData?.district ?: ""}, ${address?.addressData?.state ?: ""}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (address != null && address.memberIds.isNotEmpty()) {
          Text(
            text = "सदस्य: ${address.memberIds.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }
    }
  }
}

@Composable
private fun FamilyMembersDisplay(
  familyMembers: List<FamilyMemberForCreation>,
  onUpdateMemberHead: (String, Boolean) -> Unit,
  onUpdateMemberRelation: (String, FamilyRelation?) -> Unit,
  onRemoveMember: (FamilyMemberForCreation) -> Unit
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    familyMembers.forEach { familyMember ->
      FamilyMemberItem(
        familyMember = familyMember,
        onUpdateHead = { isHead -> onUpdateMemberHead(familyMember.member.id, isHead) },
        onUpdateRelation = { relation -> onUpdateMemberRelation(familyMember.member.id, relation) },
        onRemove = { onRemoveMember(familyMember) }
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyMemberItem(
  familyMember: FamilyMemberForCreation,
  onUpdateHead: (Boolean) -> Unit,
  onUpdateRelation: (FamilyRelation?) -> Unit,
  onRemove: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(16.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Profile Image
      if (familyMember.member.profileImage.isNotEmpty()) {
        AsyncImage(
          model =
            ImageRequest.Builder(LocalPlatformContext.current)
              .data(familyMember.member.profileImage)
              .crossfade(true)
              .build(),
          contentDescription = "Profile Image",
          modifier = Modifier.size(48.dp).clip(CircleShape),
          contentScale = ContentScale.Crop
        )
      } else {
        Surface(
          modifier = Modifier.size(48.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Icon(
              Icons.Default.Person,
              contentDescription = "Profile",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      // Member info and controls
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = familyMember.member.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )

        if (familyMember.member.address.isNotEmpty()) {
          Text(
            text = familyMember.member.address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Head selection
          WithTooltip(tooltip = "परिवार प्रमुख चुनें") {
            Row(
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = familyMember.isHead,
                onClick = { onUpdateHead(!familyMember.isHead) }
              )
              Text(
                text = "परिवार प्रमुख",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onUpdateHead(!familyMember.isHead) }
              )
            }
          }

          // Relation dropdown
          WithTooltip(tooltip = "परिवार प्रमुख के साथ संबंध") {
            FamilyRelationDropdown(
              value = familyMember.relationToHead,
              onValueChange = onUpdateRelation,
              label = "संबंध",
              modifier = Modifier.width(180.dp),
              enabled = !familyMember.isHead // Disable if this person is head
            )
          }
        }
      }

      // Remove button
      WithTooltip(tooltip = "सदस्य हटाएँ") {
        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            Icons.Default.Close,
            contentDescription = "हटाएं",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.error
          )
        }
      }
    }
  }
}

@Composable
private fun AddressSelectionSection(
  memberAddresses: List<AddressWithMemberId>,
  selectedAddressIndex: Int?,
  addressData: AddressData,
  onAddressIndexSelected: (Int?) -> Unit,
  onAddressDataChange: (AddressData) -> Unit,
  addressError: String?
) {
  Column {
    Text(
      text = "पता चुनें *",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    if (memberAddresses.isNotEmpty()) {
      // Note about address selection
      Text(
        text = "पता चुनें (परिवार के सारे सदस्यों का स्थायी पता यही होगा)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      // Address options from members
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        memberAddresses.forEachIndexed { index, addressWithMembers ->
          AddressSelectionItem(
            address = addressWithMembers,
            isSelected = selectedAddressIndex == index,
            onSelect = { onAddressIndexSelected(index) }
          )
        }

        // Option for new address
        AddressSelectionItem(
          address = null,
          isSelected = selectedAddressIndex == -1,
          onSelect = { onAddressIndexSelected(-1) },
          isNewAddressOption = true
        )
      }

      // Note about temporary address
      Text(
        text = "यदि वर्तमान पता भिन्न है तो सदस्य विवरण में अद्यतन करें।",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))
    }

    // Show address component if no member addresses or "new address" is selected
    if (memberAddresses.isEmpty() || selectedAddressIndex == -1) {
      AddressComponent(
        addressData = addressData,
        onAddressChange = onAddressDataChange,
        fieldsConfig =
          AddressFieldsConfig(
            showLocation = true,
            showAddress = true,
            showState = true,
            showDistrict = true,
            showVidhansabha = true,
            showPincode = true
          ),
        errors =
          AddressErrors(
            addressError = addressError
          )
      )
    }
  }
}

// Helper function to parse date from string (placeholder implementation)
private fun parseDate(dateString: String): LocalDate? {
  return try {
    // This is a placeholder - you'd implement actual date parsing here
    LocalDate.parse("2000-01-01") // Default date for testing
  } catch (e: Exception) {
    null
  }
}

@Composable
fun FamilyRelationDropdown(
  value: FamilyRelation?,
  onValueChange: (FamilyRelation?) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  // Placeholder implementation of the FamilyRelationDropdown
  // This should be replaced with a proper dropdown implementation
  OutlinedTextField(
    value = value?.name ?: "",
    onValueChange = { },
    label = { Text(label) },
    modifier = modifier,
    enabled = enabled,
    readOnly = true
  )
}
