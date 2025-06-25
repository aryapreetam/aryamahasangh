package org.aryamahasangh.examples

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.aryamahasangh.components.*
import org.aryamahasangh.features.activities.Member

// Example data class for form
data class PersonFormData(
  val fullName: String = "",
  val dateOfBirth: LocalDate? = null,
  val gender: Gender? = null,
  val familyRelation: FamilyRelation? = null,
  val selectedAryaSamaj: AryaSamaj? = null,
  val referrer: Member? = null // NEW: Referrer field
)

// Mock data for AryaSamaj
private val mockAryaSamajList =
  listOf(
    AryaSamaj("1", "आर्य समाज मुख्य मंदिर", "दयानंद मार्ग, दिल्ली", "दिल्ली"),
    AryaSamaj("2", "आर्य समाज गुड़गांव", "सेक्टर 14, गुड़गांव", "गुड़गांव"),
    AryaSamaj("3", "आर्य समाज फरीदाबाद", "न्यू टाउन, फरीदाबाद", "फरीदाबाद"),
    AryaSamaj("4", "आर्य समाज नोएडा", "सेक्टर 62, नोएडा", "गौतम बुद्ध नगर"),
    AryaSamaj("5", "आर्य समाज गाज़ियाबाद", "राज नगर, गाज़ियाबाद", "गाज़ियाबाद"),
    AryaSamaj("6", "आर्य समाज मेरठ", "पल टन बाज़ार, मेरठ", "मेरठ"),
    AryaSamaj("7", "आर्य समाज आगरा", "दयानंद नगर, आगरा", "आगरा"),
    AryaSamaj("8", "आर्य समाज कानपुर", "मॉल रोड, कानपुर", "कानपुर"),
    AryaSamaj("9", "आर्य समाज लखनऊ", "गोमती नगर, लखनऊ", "लखनऊ"),
    AryaSamaj("10", "आर्य समाज वाराणसी", "लंका, वाराणसी", "वाराणसी")
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormComponentsExample() {
  // Form state with remember for config changes (no serialization needed)
  var formData by remember { mutableStateOf(PersonFormData()) }

  // Validation errors
  var nameError by remember { mutableStateOf<String?>(null) }
  var dobError by remember { mutableStateOf<String?>(null) }
  var genderError by remember { mutableStateOf<String?>(null) }
  var relationError by remember { mutableStateOf<String?>(null) }
  var aryaSamajError by remember { mutableStateOf<String?>(null) }
  var referrerError by remember { mutableStateOf<String?>(null) }

  // Search state for AryaSamaj
  var searchResults by remember { mutableStateOf(mockAryaSamajList) }
  var isSearching by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  val scrollState = rememberScrollState()
  val coroutineScope = rememberCoroutineScope()

  // Referrer using MembersComponent in SINGLE mode
  val referrerMembersState =
    remember(formData.referrer) {
      if (formData.referrer != null) {
        MembersState(members = mapOf(formData.referrer!! to Pair("", 0)))
      } else {
        MembersState()
      }
    }

  // Mock members for the referrer field
  val mockMembers =
    mockAryaSamajList.map {
      Member(
        id = it.id,
        name = it.name,
        educationalQualification = "स्नातक",
        profileImage = "",
        phoneNumber = "9876543210",
        email = "${it.name.replace(" ", ".").lowercase()}@example.com",
        address = it.address,
        district = it.district,
        state = "दिल्ली",
        pincode = "110001"
      )
    }

  // Mock search function
  fun searchAryaSamaj(query: String): List<AryaSamaj> {
    return if (query.isBlank()) {
      mockAryaSamajList
    } else {
      mockAryaSamajList.filter {
        it.name.contains(query, ignoreCase = true) ||
          it.address.contains(query, ignoreCase = true) ||
          it.district.contains(query, ignoreCase = true)
      }
    }
  }

  // Validation functions
  fun validateName(): Boolean {
    return if (formData.fullName.isBlank()) {
      nameError = "कृपया अपना नाम दर्ज करें"
      false
    } else {
      nameError = null
      true
    }
  }

  fun validateDateOfBirth(): Boolean {
    return if (formData.dateOfBirth == null) {
      dobError = "कृपया जन्म तिथि चुनें"
      false
    } else {
      dobError = null
      true
    }
  }

  fun validateGender(): Boolean {
    return if (formData.gender == null) {
      genderError = "कृपया लिंग चुनें"
      false
    } else {
      genderError = null
      true
    }
  }

  fun validateFamilyRelation(): Boolean {
    return if (formData.familyRelation == null) {
      relationError = "कृपया पारिवारिक सम्बन्ध चुनें"
      false
    } else {
      relationError = null
      true
    }
  }

  fun validateAryaSamaj(): Boolean {
    return if (formData.selectedAryaSamaj == null) {
      aryaSamajError = "कृपया आर्य समाज चुनें"
      false
    } else {
      aryaSamajError = null
      true
    }
  }

  fun validateReferrer(): Boolean {
    return if (formData.referrer == null) {
      referrerError = "कृपया संदर्भक चुनें"
      false
    } else {
      referrerError = null
      true
    }
  }

  fun validateForm(): Boolean {
    return validateName() &&
      validateDateOfBirth() &&
      validateGender() &&
      validateFamilyRelation() &&
      validateAryaSamaj() &&
      validateReferrer()
  }

  fun submitForm() {
    if (validateForm()) {
      // Form is valid - submit data
      coroutineScope.launch {
        snackbarHostState.showSnackbar(
          message = "फॉर्म सफलतापूर्वक जमा किया गया!",
          duration = SnackbarDuration.Short
        )
      }
    } else {
      coroutineScope.launch {
        snackbarHostState.showSnackbar(
          message = "कृपया सभी फ़ील्ड सही ढंग से भरें",
          duration = SnackbarDuration.Long
        )
      }
    }
  }

  fun resetForm() {
    formData = PersonFormData()
    nameError = null
    dobError = null
    genderError = null
    relationError = null
    aryaSamajError = null
    referrerError = null
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("फॉर्म कम्पोनेन्ट उदाहरण") },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .padding(paddingValues)
          .verticalScroll(scrollState)
          .padding(16.dp)
          .widthIn(max = 600.dp) // Max width for better tablet/desktop experience
    ) {
      // Form Header
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
          )
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              "व्यक्तिगत जानकारी फॉर्म",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
              "सभी कस्टम कम्पोनेन्ट का उदाहरण",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Full Name Field (regular text field for comparison)
      OutlinedTextField(
        value = formData.fullName,
        onValueChange = {
          formData = formData.copy(fullName = it)
          if (nameError != null) validateName()
        },
        label = { Text("पूरा नाम *") },
        modifier = Modifier.fillMaxWidth(),
        isError = nameError != null,
        supportingText = {
          nameError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
          }
        }
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Date of Birth using custom DatePickerField
      DatePickerField(
        value = formData.dateOfBirth,
        onValueChange = {
          formData = formData.copy(dateOfBirth = it)
          if (dobError != null) validateDateOfBirth()
        },
        label = "जन्म तिथि",
        type = DatePickerType.DATE_OF_BIRTH, // Cannot select future dates
        required = true,
        modifier = Modifier.width(200.dp), // Adaptive width
        isError = dobError != null,
        supportingText = {
          dobError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
          }
        }
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Gender and Family Relation in a row (for tablet/desktop)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Gender using custom GenderDropdown
        GenderDropdown(
          value = formData.gender,
          onValueChange = {
            formData = formData.copy(gender = it)
            if (genderError != null) validateGender()
          },
          modifier = Modifier.width(150.dp), // Adaptive width
          required = true,
          isError = genderError != null,
          supportingText = {
            genderError?.let {
              Text(it, color = MaterialTheme.colorScheme.error)
            }
          }
        )

        // Family Relation using custom FamilyRelationDropdown
        FamilyRelationDropdown(
          value = formData.familyRelation,
          onValueChange = {
            formData = formData.copy(familyRelation = it)
            if (relationError != null) validateFamilyRelation()
          },
          modifier = Modifier.width(200.dp), // Adaptive width
          required = true,
          isError = relationError != null,
          supportingText = {
            relationError?.let {
              Text(it, color = MaterialTheme.colorScheme.error)
            }
          }
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Arya Samaj using custom AryaSamajSelector
      AryaSamajSelector(
        selectedAryaSamaj = formData.selectedAryaSamaj,
        onAryaSamajSelected = {
          formData = formData.copy(selectedAryaSamaj = it)
          if (aryaSamajError != null) validateAryaSamaj()
        },
        modifier = Modifier.width(400.dp), // Adaptive width
        required = true,
        isError = aryaSamajError != null,
        supportingText = {
          aryaSamajError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
          }
        },
        searchAryaSamaj = ::searchAryaSamaj,
        allAryaSamaj = searchResults,
        onTriggerSearch = { query ->
          // Simulate network search
          isSearching = true
          // Simulate network delay
          coroutineScope.launch {
            kotlinx.coroutines.delay(500)
            searchResults = searchAryaSamaj(query)
            isSearching = false
          }
        }
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Test section for MembersComponent
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
          )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            "MembersComponent टेस्ट सेक्शन",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "यहाँ आप MembersComponent की कार्यक्षमता देख सकते हैं",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // MembersComponent in SINGLE mode
      MembersComponent(
        state = referrerMembersState,
        onStateChange = { newState ->
          val member = newState.members.keys.firstOrNull()
          formData = formData.copy(referrer = member)
          if (referrerError != null) validateReferrer()
        },
        config =
          MembersConfig(
            label = "संदर्भक (Referrer)",
            addButtonText = "संदर्भक चुनें",
            choiceType = MembersChoiceType.SINGLE,
            singleModeLabel = "संदर्भक (Referrer)",
            singleModeButtonText = "संदर्भक चुनें",
            editMode = MembersEditMode.INDIVIDUAL, // Changed to INDIVIDUAL to show button
            isMandatory = true,
            showMemberCount = false
          ),
        error = referrerError,
        searchMembers = { query ->
          // Mock search - filter mock members by name
          mockMembers.filter { it.name.contains(query, ignoreCase = true) }
        },
        allMembers = mockMembers,
        onTriggerSearch = { query ->
          // Simulate network search
        },
        modifier = Modifier.width(400.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // MembersComponent in MULTIPLE mode
      val multipleMembersState = remember { MembersState() }
      MembersComponent(
        state = multipleMembersState,
        onStateChange = { newState ->
          // This is just a test - no actual data binding needed
        },
        config =
          MembersConfig(
            label = "कई सदस्य (Multiple Members)",
            addButtonText = "सदस्य जोड़ें",
            choiceType = MembersChoiceType.MULTIPLE,
            editMode = MembersEditMode.INDIVIDUAL, // Show individual buttons
            isMandatory = false,
            showMemberCount = true
          ),
        error = null,
        searchMembers = { query ->
          // Mock search - filter mock members by name
          mockMembers.filter { it.name.contains(query, ignoreCase = true) }
        },
        allMembers = mockMembers,
        onTriggerSearch = { query ->
          // Simulate network search
        },
        modifier = Modifier.width(400.dp)
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Submit Button
        Button(
          onClick = ::submitForm,
          modifier = Modifier.weight(1f)
        ) {
          Text("जमा करें")
        }

        // Reset Button
        OutlinedButton(
          onClick = ::resetForm,
          modifier = Modifier.weight(1f)
        ) {
          Text("रीसेट करें")
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Debug Info Card (for development)
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          )
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            "फॉर्म डेटा (डिबग जानकारी)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(8.dp))

          val debugInfo =
            buildString {
              appendLine("नाम: ${formData.fullName.ifBlank { "खाली" }}")
              appendLine("जन्म तिथि: ${formData.dateOfBirth?.toString() ?: "चुना नहीं गया"}")
              appendLine("लिंग: ${formData.gender?.toDisplayName() ?: "चुना नहीं गया"}")
              appendLine("पारिवारिक सम्बन्ध: ${formData.familyRelation?.toDisplayName() ?: "चुना नहीं गया"}")
              appendLine("आर्य समाज: ${formData.selectedAryaSamaj?.name ?: "चुना नहीं गया"}")
              val selectedSamaj = formData.selectedAryaSamaj
              if (selectedSamaj != null) {
                appendLine("पता: ${selectedSamaj.address}")
                appendLine("जिला: ${selectedSamaj.district}")
              }
              appendLine("संदर्भक: ${formData.referrer?.name ?: "चुना नहीं गया"}")
              val selectedReferrer = formData.referrer
              if (selectedReferrer != null) {
                appendLine("संदर्भक पता: ${selectedReferrer.address}")
                appendLine("संदर्भक दूरभाष: ${selectedReferrer.phoneNumber}")
              }
            }

          Text(
            debugInfo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
