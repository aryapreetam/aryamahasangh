package com.aryamahasangh.features.admin.member

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aryamahasangh.components.*
import com.aryamahasangh.features.activities.Member
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileImageSection(
  selectedProfileImage: PlatformFile?,
  onImageSelected: () -> Unit,
  onImageRemoved: () -> Unit,
  existingImageUrl: String? = null // Add parameter for existing image URL
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    SectionTitle("प्रोफ़ाइल फोटो")

    Box(
      modifier = Modifier.size(120.dp),
      contentAlignment = Alignment.Center
    ) {
      when {
        selectedProfileImage != null -> {
          // Show selected image with remove button
          AddMemberProfilePhotoItem(
            file = selectedProfileImage,
            onRemoveFile = onImageRemoved,
            modifier = Modifier.size(100.dp)
          )
        }

        !existingImageUrl.isNullOrEmpty() -> {
          // Show existing image from URL with replace option
          Box(modifier = Modifier.size(100.dp)) {
            AsyncImage(
              model = existingImageUrl,
              contentDescription = "Profile Image",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .clickable { onImageSelected() }
            )

            // Replace button overlay
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
              shape = CircleShape,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
            ) {
              IconButton(
                onClick = onImageSelected,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  Icons.Default.AddAPhoto,
                  modifier = Modifier.size(16.dp),
                  contentDescription = "Change Photo",
                  tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }
        }
        else -> {
          // Show placeholder with upload option
          Box(
            modifier =
              Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onImageSelected() },
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Default.AddAPhoto,
                contentDescription = "फोटो अपलोड करें",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                "फोटो जोड़ें",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun PersonalDetailsSection(
  name: String,
  onNameChange: (String) -> Unit,
  nameError: String?,
  nameFocusRequester: FocusRequester,
  phoneNumber: String,
  onPhoneNumberChange: (String) -> Unit,
  phoneError: String?,
  phoneFocusRequester: FocusRequester,
  dob: LocalDate?,
  onDobChange: (LocalDate?) -> Unit,
  dobError: String?,
  gender: Gender?,
  onGenderChange: (Gender) -> Unit,
  genderError: String?,
  email: String,
  onEmailChange: (String) -> Unit,
  emailFocusRequester: FocusRequester,
  educationalQualification: String,
  onEducationalQualificationChange: (String) -> Unit,
  educationFocusRequester: FocusRequester,
  occupation: String,
  onOccupationChange: (String) -> Unit,
  occupationFocusRequester: FocusRequester,
  focusManager: FocusManager
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SectionTitle("व्यक्तिगत विवरण")

    // Name field
    OutlinedTextField(
      value = name,
      onValueChange = onNameChange,
      label = { Text("नाम *") },
      modifier =
        Modifier
          .widthIn(max = 500.dp)
          .focusRequester(nameFocusRequester),
      isError = nameError != null,
      supportingText = { nameError?.let { Text(it) } },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
      keyboardActions =
        KeyboardActions(
          onNext = { phoneFocusRequester.requestFocus() }
        )
    )

    // Phone number field
    OutlinedTextField(
      value = phoneNumber,
      onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 15) onPhoneNumberChange(it) },
      label = { Text("दूरभाष *") },
      modifier =
        Modifier
          .widthIn(max = 500.dp)
          .focusRequester(phoneFocusRequester),
      isError = phoneError != null,
      supportingText = { phoneError?.let { Text(it) } },
      keyboardOptions =
        KeyboardOptions(
          keyboardType = KeyboardType.Phone,
          imeAction = ImeAction.Next
        ),
      keyboardActions =
        KeyboardActions(
          onNext = { focusManager.moveFocus(FocusDirection.Next) }
        )
    )

    // DOB and Gender in a row for adaptive layout
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // DOB field
      DatePickerField(
        value = dob,
        onValueChange = onDobChange,
        label = "जन्मतिथि",
        type = DatePickerType.DATE_OF_BIRTH,
        modifier = Modifier.width(200.dp),
        isError = dobError != null,
        supportingText = { dobError?.let { Text(it) } },
        required = true
      )

      // Gender field
      GenderDropdown(
        value = gender,
        onValueChange = onGenderChange,
        modifier = Modifier.width(150.dp),
        isError = genderError != null,
        supportingText = { genderError?.let { Text(it) } },
        required = true
      )
    }

    // Email field
    OutlinedTextField(
      value = email,
      onValueChange = onEmailChange,
      label = { Text("ईमेल") },
      modifier =
        Modifier
          .widthIn(max = 500.dp)
          .focusRequester(emailFocusRequester),
      keyboardOptions =
        KeyboardOptions(
          keyboardType = KeyboardType.Email,
          imeAction = ImeAction.Next
        ),
      keyboardActions =
        KeyboardActions(
          onNext = { educationFocusRequester.requestFocus() }
        )
    )

    // Education and Occupation in a row for adaptive layout
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Educational qualification field
      OutlinedTextField(
        value = educationalQualification,
        onValueChange = onEducationalQualificationChange,
        label = { Text("शैक्षणिक योग्यता") },
        modifier =
          Modifier
            .width(250.dp)
            .focusRequester(educationFocusRequester),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions =
          KeyboardActions(
            onNext = { occupationFocusRequester.requestFocus() }
          )
      )

      // Occupation field
      OutlinedTextField(
        value = occupation,
        onValueChange = onOccupationChange,
        label = { Text("व्यवसाय") },
        modifier =
          Modifier
            .width(200.dp)
            .focusRequester(occupationFocusRequester),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions =
          KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) }
          )
      )
    }
  }
}

@Composable
fun OrganizationDetailsSection(
  joiningDate: LocalDate?,
  onJoiningDateChange: (LocalDate?) -> Unit,
  referrerState: MembersState,
  onReferrerStateChange: (MembersState) -> Unit,
  referrerError: String?,
  selectedAryaSamaj: AryaSamaj?,
  onAryaSamajSelected: (AryaSamaj?) -> Unit
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SectionTitle("संगठन से जुड़ाव")

    // Joining Date
    Text(
      "किस तिथि को संगठन के साथ जुड़े?",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    DatePickerField(
      value = joiningDate,
      onValueChange = onJoiningDateChange,
      label = "जुड़ने की तिथि",
      type = DatePickerType.PAST_EVENT,
      modifier = Modifier.width(200.dp)
    )

    // Referrer
    Text(
      "किसकी प्रेरणा से जुड़े?",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    MembersComponent(
      state = referrerState,
      onStateChange = onReferrerStateChange,
      config = MembersConfig(
        label = "संदर्भक (प्रेरक)",
        addButtonText = "संदर्भक चुनें",
        choiceType = MembersChoiceType.SINGLE,
        singleModeLabel = "संदर्भक",
        singleModeButtonText = "संदर्भक चुनें",
        editMode = MembersEditMode.INDIVIDUAL,
        isMandatory = false,
        showMemberCount = false
      ),
      error = referrerError,
      modifier = Modifier.width(400.dp)
    )

    // Arya Samaj
    Text(
      "आप किस आर्य समाज के साथ जुड़े हो?",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    AryaSamajSelector(
      selectedAryaSamaj = selectedAryaSamaj,
      onAryaSamajSelected = onAryaSamajSelected,
      label = "सम्बंधित आर्य समाज",
      modifier = Modifier.width(300.dp),
      // Pass user's current location if available from address
      latitude = null, // TODO: Extract from user's selected address if needed
      longitude = null // TODO: Extract from user's selected address if needed
    )
  }
}

@Composable
fun AddressSection(
  permanentAddress: AddressData,
  onPermanentAddressChange: (AddressData) -> Unit,
  permanentAddressErrors: AddressErrors,
  isDifferentCurrentAddress: Boolean,
  onIsDifferentCurrentAddressChange: (Boolean) -> Unit,
  currentAddress: AddressData,
  onCurrentAddressChange: (AddressData) -> Unit,
  currentAddressErrors: AddressErrors
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    SectionTitle("पता")

    // Permanent Address
    Text(
      "स्थायी पता *",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Medium
    )

    AddressComponent(
      addressData = permanentAddress,
      onAddressChange = onPermanentAddressChange,
      errors = permanentAddressErrors
    )

    // Current Address Checkbox
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 8.dp)
    ) {
      Checkbox(
        checked = isDifferentCurrentAddress,
        onCheckedChange = onIsDifferentCurrentAddressChange
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text("वर्तमान पता स्थायी पते से भिन्न है")
    }

    // Current Address (if different)
    if (isDifferentCurrentAddress) {
      Text(
        "वर्तमान पता (यदि स्थायी पते से भिन्न हो तो)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
      )

      AddressComponent(
        addressData = currentAddress,
        onAddressChange = onCurrentAddressChange,
        errors = currentAddressErrors
      )
    }
  }
}

@Composable
fun IntroductionSection(
  introduction: String,
  onIntroductionChange: (String) -> Unit,
  introductionFocusRequester: FocusRequester
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    SectionTitle("संक्षिप्त परिचय")

    OutlinedTextField(
      value = introduction,
      onValueChange = { if (it.length <= 500) onIntroductionChange(it) },
      label = { Text("संक्षिप्त परिचय") },
      placeholder = { Text("अपने बारे में कुछ लिखें...") },
      modifier =
        Modifier
          .width(500.dp)
          .focusRequester(introductionFocusRequester),
      minLines = 3,
      maxLines = 5,
      supportingText = {
        Text("${introduction.length}/500 characters")
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions =
        KeyboardActions(
          onDone = { /* Focus manager will handle clearing focus */ }
        )
    )
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(bottom = 8.dp)
  )
}

@Composable
private fun AddMemberProfilePhotoItem(
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

  Box(modifier = modifier) {
    Column(modifier = Modifier.padding(6.dp)) {
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

    // Remove button positioned in top-right
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
