package com.aryamahasangh.screens

import AppTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.aryamahasangh.utils.TBD
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class Gender(
  val studentTerm: String, // छात्र / छात्रा
  val verbEnding: String // रहा / रहीं
) {
  BOY("छात्र", "रहा"), // Keeping BOY for internal logic, studentTerm for UI
  GIRL("छात्रा", "रहीं") // Keeping GIRL for internal logic, studentTerm for UI
}

data class EventDetails(
  val id: String,
  val genderTarget: Gender,
  val eventName: String,
  val place: String,
  val dateRange: String,
  val organiserName: String
) {
  // This will be shown in the dropdown
  override fun toString(): String = "$eventName - $place ($dateRange) | आयोजक: $organiserName"
}

// Dummy event data
val allEvents =
  listOf(
    EventDetails("b1", Gender.BOY, "बालक शौर्य प्रशिक्षण", "दिल्ली", "15 जून - 17 जून 2024", "आर्य वीर दल"),
    EventDetails("b2", Gender.BOY, "युवा संस्कार शिविर", "जयपुर", "01 जुलाई - 03 जुलाई 2024", "आर्य युवक परिषद"),
    EventDetails(
      "g1",
      Gender.GIRL,
      "कन्या आत्मरक्षा कार्यशाला",
      "मुंबई",
      "10 अगस्त - 12 अगस्त 2024",
      "आर्यांगना समिति"
    ),
    EventDetails("g2", Gender.GIRL, "तेजस्विनी बालिका शिविर", "पुणे", "05 सितम्बर - 07 सितम्बर 2024", "महिला आर्य समाज")
  )

// Your EventDetails and Gender enum would be imported here
// from EventData.kt

object EventApiService {
  // Simulate fetching events based on gender
  suspend fun fetchEvents(gender: Gender): Result<List<EventDetails>> {
    delay(1500) // Simulate network delay
    return try {
      // In a real app, this would be an actual network call
      val fetchedEvents =
        when (gender) {
          Gender.BOY ->
            listOf(
              EventDetails(
                "api_b1",
                Gender.BOY,
                "आर्य छात्र प्रशिक्षण सत्र\n",
                "छात्र गुरुकुल, भाली आनंदपुर, रोहतक, हरियाणा",
                "15-17 जून",
                "राष्ट्रीय आर्य क्षत्रिय सभा"
              ),
              EventDetails(
                "api_b2",
                Gender.BOY,
                "आर्य छात्र प्रशिक्षण सत्र\n",
                "छात्र गुरुकुल, भाली आनंदपुर, रोहतक, हरियाणा",
                "01-03 जुलाई",
                "राष्ट्रीय आर्य क्षत्रिय सभा"
              )
            )
          Gender.GIRL ->
            listOf(
              EventDetails(
                "api_g1",
                Gender.GIRL,
                "एपीआई कन्या आत्मरक्षा कार्यशाला",
                "मुंबई (एपीआई)",
                "10-12 अगस्त",
                "आर्यांगना समिति"
              ),
              EventDetails(
                "api_g2",
                Gender.GIRL,
                "एपीआई तेजस्विनी बालिका शिविर",
                "पुणे (एपीआई)",
                "05-07 सितम्बर",
                "महिला आर्य समाज"
              )
            )
          // else -> emptyList() // Should not happen if gender is always provided
        }
      if (fetchedEvents.isEmpty() && gender == Gender.BOY) { // Simulate an empty list for a specific case for testing
        // Result.success(emptyList())
      }
      Result.success(fetchedEvents)
    } catch (e: Exception) {
      // Log the exception e.g. Timber.e(e, "Failed to fetch events for $gender")
      Result.failure(e) // Return a failure result
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalTrainingForm(
  initialGender: Gender?, // Gender is now an input parameter
  onSubmit: (formData: Map<String, String>) -> Unit
) {
  // FIXME
  if (true) {
    TBD()
    return
  }

  // The selectedGender is now directly derived from initialGender and not user-selectable on this screen.
  // We'll use initialGender directly for logic.
  var genderForForm by remember {
    mutableStateOf(initialGender)
  } // Store it in a state if needed for recomposition triggers

  // State for event selection (fetched from API)
  var availableEvents by remember { mutableStateOf<List<EventDetails>>(emptyList()) }
  var isLoadingEvents by remember { mutableStateOf(false) }
  var eventFetchError by remember { mutableStateOf<String?>(null) }

  var selectedEvent by remember { mutableStateOf<EventDetails?>(null) }
  var eventDropdownExpanded by remember { mutableStateOf(false) }
  var eventError by remember { mutableStateOf<String?>(null) } // Error for *selecting* an event

  // Coroutine scope for launching API calls
  val coroutineScope = rememberCoroutineScope()

  // Effect to fetch events when initialGender is provided or changes
  LaunchedEffect(genderForForm) {
    genderForForm?.let { currentGender ->
      isLoadingEvents = true
      eventFetchError = null
      availableEvents = emptyList() // Clear previous events
      selectedEvent = null // Clear selected event

      coroutineScope.launch {
        val result = EventApiService.fetchEvents(currentGender)
        result.fold(
          onSuccess = { events ->
            availableEvents = events
            isLoadingEvents = false
          },
          onFailure = { error ->
            eventFetchError = "कार्यक्रम लोड करने में विफल: ${error.message}"
            isLoadingEvents = false
          }
        )
      }
    } ?: run {
      // Handle case where initialGender is null, if necessary
      availableEvents = emptyList()
      selectedEvent = null
      // Optionally set an error or show a message if gender is required but not provided
    }
  }

  // Form field states (remain the same)
  var name by remember { mutableStateOf("") }
  var nameError by remember { mutableStateOf<String?>(null) }
  var fatherName by remember { mutableStateOf("") }
  var fatherNameError by remember { mutableStateOf<String?>(null) }
  var currentAddress by remember { mutableStateOf("") }
  var currentAddressError by remember { mutableStateOf<String?>(null) }
  var isPermanentAddressSameAsCurrent by remember { mutableStateOf(false) }
  var permanentAddress by remember { mutableStateOf("") }
  var permanentAddressError by remember { mutableStateOf<String?>(null) }
  var birthDate by remember { mutableStateOf("") }
  var birthDateError by remember { mutableStateOf<String?>(null) }
  var qualification by remember { mutableStateOf("") }
  var qualificationError by remember { mutableStateOf<String?>(null) }
  var mobileNumber by remember { mutableStateOf("") }
  var mobileNumberError by remember { mutableStateOf<String?>(null) }
  var aryaSamajName by remember { mutableStateOf("") }
  var aryaSamajNameError by remember { mutableStateOf<String?>(null) }
  var pradhanName by remember { mutableStateOf("") }
  var pradhanNameError by remember { mutableStateOf<String?>(null) }
  var sessionAcceptanceDate by remember { mutableStateOf("") }
  var sessionAcceptanceDateError by remember { mutableStateOf<String?>(null) }
  var acknowledgementChecked by remember { mutableStateOf(false) }
  var acknowledgementError by remember { mutableStateOf<String?>(null) }

  val acknowledgementText = "मैं/मेरा बच्चे की ओर से मैं ये घोषणा करता हूँ कि, आवेदन पत्र में मेरे द्वारा भरे गए प्रविष्टियाँ मेरी जानकारी के अनुसार पूरी तरह ठीक हैं। मैं यह वचन देता हूँ कि मैं प्रशिक्षण के दौरान बतलाये गए सभी नियमो व निर्देशों का पालन करूँगा। मैं यह स्वीकार करता हूँ कि प्रशिक्षण के दौरान मेरे साथ/मेरे बच्चे के साथ किसी भी प्रकार की अप्रिय घटना के लिए पूरी तरह से जिम्मेदार रहूँगा और सभी चिकित्सा खर्चों का वहन करूँगा।"

  fun validate(): Boolean {
    var isValid = true
    nameError = null
    fatherNameError = null
    // genderError no longer for selection
    eventError = null
    currentAddressError = null
    permanentAddressError = null
    birthDateError = null
    qualificationError = null
    mobileNumberError = null
    aryaSamajNameError = null
    pradhanNameError = null
    sessionAcceptanceDateError = null
    acknowledgementError = null

    if (name.isBlank()) {
      nameError = "नाम आवश्यक है"
      isValid = false
    }
    if (fatherName.isBlank()) {
      fatherNameError = "पिता का नाम आवश्यक है"
      isValid = false
    }

    // Validate if genderForForm (derived from initialGender) is present
    if (genderForForm == null) {
      // This error might be displayed differently, e.g., a general form error
      // or this form shouldn't even be reachable without a gender.
      // For now, we'll keep a logical check.
      // Consider how to display this error if it's critical.
      // For this example, we assume the form is only shown if initialGender is valid.
      // If it *can* be null and is an error, you'd need a place to show this.
      // For now, we'll assume navigation handles this.
      println("Validation Error: Gender not provided to form.") // Or set a general form error state
      isValid = false
    }

    if (selectedEvent == null && availableEvents.isNotEmpty()) { // Only require if events were loaded
      eventError = "कार्यक्रम का चयन करें"
      isValid = false
    }
    if (currentAddress.isBlank()) {
      currentAddressError = "वर्तमान पता आवश्यक है"
      isValid = false
    }
    if (!isPermanentAddressSameAsCurrent && permanentAddress.isBlank()) {
      permanentAddressError = "स्थाई पता आवश्यक है"
      isValid = false
    }
    if (birthDate.isBlank()) {
      birthDateError = "जन्म तिथि आवश्यक है"
      isValid = false
    } else if (!birthDate.matches(Regex("""^\d{2}/\d{2}/\d{4}$""")) && !birthDate.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
      birthDateError = "जन्म तिथि DD/MM/YYYY या YYYY-MM-DD प्रारूप में होनी चाहिए"
      isValid = false
    }
    if (qualification.isBlank()) {
      qualificationError = "योग्यता आवश्यक है"
      isValid = false
    }
    if (mobileNumber.isBlank()) {
      mobileNumberError = "चलभाष आवश्यक है"
      isValid = false
    } else if (!mobileNumber.matches(Regex("""^\d{10}$"""))) {
      mobileNumberError = "चलभाष 10 अंकों का होना चाहिए"
      isValid = false
    }
    if (aryaSamajName.isBlank()) {
      aryaSamajNameError = "संबंधित आर्य समाज का नाम आवश्यक है"
      isValid = false
    }
    if (pradhanName.isBlank()) {
      pradhanNameError = "प्रधान का नाम आवश्यक है"
      isValid = false
    }
    if (sessionAcceptanceDate.isBlank()) {
      sessionAcceptanceDateError = "द्विदिवसीय सत्र शिक्षा ग्रहण दिनांक/वर्ष आवश्यक है"
      isValid = false
    }
    if (!acknowledgementChecked) {
      acknowledgementError = "कृपया घोषणापत्र स्वीकार करें"
      isValid = false
    }
    return isValid
  }

  // Dynamic gender prompt based on the initialGender
  val genderPromptText =
    genderForForm?.let {
      "मैं आर्य ${it.studentTerm} प्रशिक्षण के लिए पंजीकरण कर ${it.verbEnding} हूँ।"
    } ?: "लिंग निर्दिष्ट नहीं है।" // Fallback if initialGender is somehow null

  Box(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    contentAlignment = Alignment.TopCenter
  ) {
    Column(
      modifier =
        Modifier
          .verticalScroll(rememberScrollState())
          .widthIn(max = 600.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // --- Display Gender (Not Selectable) ---
      Text(genderPromptText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
      // The FilterChips for gender selection are now REMOVED.

      // --- Event Dropdown ---
      if (isLoadingEvents) {
        CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
      } else if (eventFetchError != null) {
        Text(eventFetchError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
      } else if (genderForForm == null) {
        Text("कृपया पंजीकरण के लिए लिंग चुनें (पिछला चरण)।", style = MaterialTheme.typography.bodyMedium)
      } else { // Only show dropdown if gender is present and not loading/error
        ExposedDropdownMenuBox(
          expanded = eventDropdownExpanded,
          onExpandedChange = {
            if (availableEvents.isNotEmpty()) eventDropdownExpanded = !eventDropdownExpanded
          }, // Only expand if there are events
          modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)
        ) {
          OutlinedTextField(
            value = selectedEvent?.toString() ?: if (availableEvents.isEmpty() && !isLoadingEvents) "कोई कार्यक्रम उपलब्ध नहीं" else "",
            onValueChange = {},
            readOnly = true,
            label = { Text("प्रशिक्षण सत्र चुनें") },
            trailingIcon = {
              if (availableEvents.isNotEmpty()) {
                ExposedDropdownMenuDefaults.TrailingIcon(
                  expanded = eventDropdownExpanded
                )
              }
            },
            modifier = Modifier.menuAnchor(PrimaryNotEditable, true),
            isError = eventError != null,
            supportingText = { eventError?.let { Text(it) } },
            enabled = availableEvents.isNotEmpty() // Enable only if events are loaded
          )
          ExposedDropdownMenu(
            expanded = eventDropdownExpanded && availableEvents.isNotEmpty(),
            onDismissRequest = { eventDropdownExpanded = false }
          ) {
            if (availableEvents.isEmpty()) { // Should be caught by outer if, but for safety
              DropdownMenuItem(
                text = { Text("कोई कार्यक्रम उपलब्ध नहीं है") },
                onClick = { eventDropdownExpanded = false },
                enabled = false
              )
            } else {
              availableEvents.forEach { event ->
                DropdownMenuItem(
                  text = { Text(event.toString()) },
                  onClick = {
                    selectedEvent = event
                    eventDropdownExpanded = false
                    eventError = null
                  }
                )
              }
            }
          }
        }
      }

      // --- Form Fields (remain the same structure) ---
      CustomOutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          nameError = null
        },
        label = "नाम",
        error = nameError
      )
      // ... (fatherName, currentAddress, permanentAddress, birthDate & qualification row, mobileNumber, aryaSamajName, pradhanName, sessionAcceptanceDate)
      CustomOutlinedTextField(
        value = fatherName,
        onValueChange = {
          fatherName = it
          fatherNameError = null
        },
        label = "पिता का नाम",
        error = fatherNameError
      )

      CustomOutlinedTextField(
        value = currentAddress,
        onValueChange = {
          currentAddress = it
          currentAddressError = null
          if (isPermanentAddressSameAsCurrent) permanentAddress = it
        },
        label = "वर्तमान पता",
        error = currentAddressError,
        singleLine = false,
        maxLines = 3
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
      ) {
        Checkbox(
          checked = isPermanentAddressSameAsCurrent,
          onCheckedChange = {
            isPermanentAddressSameAsCurrent = it
            if (it) {
              permanentAddress = currentAddress
              permanentAddressError = null
            } else {
              permanentAddress = ""
            }
          }
        )
        Text("स्थाई पता एवं वर्तमान पता एक है", style = MaterialTheme.typography.bodyMedium)
      }

      CustomOutlinedTextField(
        value = permanentAddress,
        onValueChange = {
          permanentAddress = it
          permanentAddressError = null
        },
        label = "स्थाई पता",
        error = permanentAddressError,
        enabled = !isPermanentAddressSameAsCurrent,
        singleLine = false,
        maxLines = 3
      )
      Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        CustomOutlinedTextField(
          value = birthDate,
          onValueChange = {
            birthDate = it
            birthDateError = null
          },
          label = "जन्म तिथि (DD/MM/YYYY)",
          error = birthDateError,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
          modifier = Modifier.weight(1f).padding(end = 4.dp),
          trailingIcon = { Icon(Icons.Filled.CalendarToday, "जन्म तिथि") }
        )
        CustomOutlinedTextField(
          value = qualification,
          onValueChange = {
            qualification = it
            qualificationError = null
          },
          label = "योग्यता",
          error = qualificationError,
          modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
      }

      CustomOutlinedTextField(
        value = mobileNumber,
        onValueChange = {
          if (it.length <= 10 && it.all { char -> char.isDigit() }) {
            mobileNumber = it
          }
          mobileNumberError = null
        },
        label = "चलभाष (10 अंक)",
        error = mobileNumberError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        prefix = { Text("+91 ") }
      )

      CustomOutlinedTextField(
        value = aryaSamajName,
        onValueChange = {
          aryaSamajName = it
          aryaSamajNameError = null
        },
        label = "संबंधित आर्य समाज का नाम",
        error = aryaSamajNameError
      )

      CustomOutlinedTextField(
        value = pradhanName,
        onValueChange = {
          pradhanName = it
          pradhanNameError = null
        },
        label = "प्रधान",
        error = pradhanNameError
      )

      CustomOutlinedTextField(
        value = sessionAcceptanceDate,
        onValueChange = {
          sessionAcceptanceDate = it
          sessionAcceptanceDateError = null
        },
        label = "द्विदिवसीय सत्र शिक्षा ग्रहण दिनांक/वर्ष",
        error = sessionAcceptanceDateError
      )

      // --- Acknowledgement Checkbox (remains the same) ---
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp)) {
        Row(
          verticalAlignment = Alignment.Top,
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable {
                acknowledgementChecked = !acknowledgementChecked
                acknowledgementError = null
              }
              .padding(vertical = 4.dp)
        ) {
          Checkbox(
            checked = acknowledgementChecked,
            onCheckedChange = {
              acknowledgementChecked = it
              acknowledgementError = null
            },
            modifier = Modifier.padding(end = 8.dp)
          )
          Text(
            text = acknowledgementText,
            style = MaterialTheme.typography.bodySmall
          )
        }
        if (acknowledgementError != null) {
          Text(
            acknowledgementError!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 48.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      // --- Submit Button (remains the same) ---
      Button(
        onClick = {
          if (validate()) {
            val formData =
              mapOf(
                "नाम" to name,
                "पिता का नाम" to fatherName,
                "लिंग (छात्र/छात्रा)" to (genderForForm?.studentTerm ?: "अज्ञात"), // Use genderForForm
                "कार्यक्रम" to (selectedEvent?.eventName ?: ""),
                // ... other form data
                "कार्यक्रम स्थान" to (selectedEvent?.place ?: ""),
                "कार्यक्रम तिथि" to (selectedEvent?.dateRange ?: ""),
                "कार्यक्रम आयोजक" to (selectedEvent?.organiserName ?: ""),
                "वर्तमान पता" to currentAddress,
                "स्थाई पता" to permanentAddress,
                "जन्म तिथि" to birthDate,
                "योग्यता" to qualification,
                "चलभाष" to mobileNumber,
                "आर्य समाज का नाम" to aryaSamajName,
                "प्रधान" to pradhanName,
                "सत्र शिक्षा ग्रहण दिनांक/वर्ष" to sessionAcceptanceDate,
                "घोषणापत्र स्वीकृत" to acknowledgementChecked.toString()
              )
            onSubmit(formData)
          }
        },
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 300.dp).height(48.dp),
        // Disable button if events are loading or gender is not provided
        enabled = !isLoadingEvents && genderForForm != null
      ) {
        Text("जमा करें")
      }
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

// CustomOutlinedTextField remains the same
@Composable
fun CustomOutlinedTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  error: String?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  singleLine: Boolean = true,
  maxLines: Int = 1,
  prefix: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    isError = error != null,
    supportingText = { error?.let { Text(it) } },
    modifier = modifier.fillMaxWidth().widthIn(max = 500.dp),
    enabled = enabled,
    readOnly = readOnly,
    keyboardOptions = keyboardOptions,
    singleLine = singleLine,
    maxLines = maxLines,
    prefix = prefix,
    trailingIcon = trailingIcon
  )
}

@Preview // (name = "खाली फॉर्म", showBackground = true)
@Composable
fun PhysicalTrainingFormPreview() {
  AppTheme { // The form uses MaterialTheme components
    Surface { // Provides a background color from the theme
      PhysicalTrainingForm(Gender.BOY) { formData ->
        println("पूर्वावलोकन जमा किया गया: $formData")
        // In a real preview, this lambda doesn't really "submit" anywhere
        // but it's good practice to provide a no-op or logging lambda.
      }
    }
  }
}

// @Preview//(name = "भरा हुआ फॉर्म (उदाहरण)", showBackground = true)
// @Composable
// fun PhysicalTrainingFormFilledPreview() {
//  AppTheme {
//    Surface {
//      // We can't directly manipulate the internal state of PhysicalTrainingForm
//      // from here easily for a preview without refactoring the form itself
//      // to accept initial values.
//      // So, this preview will also show an empty form, but it serves as a
//      // placeholder for how you might create more complex previews if needed.
//      // For a "filled" preview, you'd typically pass initial values to your composable.
//
//      // For demonstration, let's just call the base form.
//      // To show a "filled" state, you would need to modify PhysicalTrainingForm
//      // to accept initial values for its remember states.
//      PhysicalTrainingForm { formData ->
//        println("पूर्वावलोकन (भरा हुआ) जमा किया गया: $formData")
//      }
//    }
//  }
// }
//
// @Preview//(name = "त्रुटि स्थिति के साथ फॉर्म", showBackground = true)
// @Composable
// fun PhysicalTrainingFormWithErrorPreview() {
//  AppTheme {
//    Surface {
//      // Similar to the "filled" state, directly triggering error states from
//      // outside without modification is complex.
//      // A common way is to have a wrapper Composable for preview that sets up
//      // some initial conditions if your main Composable supports it.
//
//      // This will show the form. You'd manually interact in the preview to see errors.
//      PhysicalTrainingForm { formData ->
//        println("पूर्वावलोकन (त्रुटि) जमा किया गया: $formData")
//      }
//      // To actually show an error state *statically* in a preview, you might need:
//      // 1. Modify PhysicalTrainingForm to accept initial error strings.
//      // 2. Or, create a more complex preview Composable that simulates clicks
//      //    or uses a local 'previewMode' flag to set errors.
//      // For now, this will render the form, and you can click "जमा करें"
//      // with empty fields to see the errors.
//    }
//  }
// }
