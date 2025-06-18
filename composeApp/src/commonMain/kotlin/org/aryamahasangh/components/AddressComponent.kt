package org.aryamahasangh.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.aryamahasangh.features.activities.LatLng
import org.aryamahasangh.features.activities.MapLocationPickerDialog
import org.aryamahasangh.screens.DistrictDropdown
import org.aryamahasangh.screens.StateDropdown
import org.aryamahasangh.screens.indianStatesToDistricts
import org.aryamahasangh.screens.vidhansabhaByState

/**
 * Data class representing address information
 */
data class AddressData(
  val location: LatLng? = null,
  val address: String = "",
  val state: String = "",
  val district: String = "",
  val vidhansabha: String = "",
  val pincode: String = ""
)

/**
 * Configuration for which fields to show in the address component
 */
data class AddressFieldsConfig(
  val showLocation: Boolean = true,
  val showAddress: Boolean = true,
  val showState: Boolean = true,
  val showDistrict: Boolean = true,
  val showVidhansabha: Boolean = true,
  val showPincode: Boolean = true
)

/**
 * Error states for address fields
 */
data class AddressErrors(
  val locationError: String? = null,
  val addressError: String? = null,
  val stateError: String? = null,
  val districtError: String? = null,
  val vidhansabhaError: String? = null,
  val pincodeError: String? = null
)

/**
 * Nominatim response data class for reverse geocoding
 */
@Serializable
data class NominatimAddress(
  val house_number: String? = null,
  val road: String? = null,
  val suburb: String? = null,
  val city: String? = null,
  val town: String? = null,
  val village: String? = null,
  val county: String? = null,
  val state_district: String? = null,
  val state: String? = null,
  val postcode: String? = null,
  val country: String? = null
)

@Serializable
data class NominatimResponse(
  val display_name: String? = null,
  val address: NominatimAddress? = null
)

/**
 * Comprehensive address component for collecting address information
 *
 * @param addressData Current address data
 * @param onAddressChange Callback when address data changes
 * @param fieldsConfig Configuration for which fields to show
 * @param errors Current error states for validation
 * @param onFieldFocused Optional callback when a field is focused (useful for keyboard handling)
 * @param isSmallScreen Whether the screen is small (for keyboard handling)
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddressComponent(
  addressData: AddressData,
  onAddressChange: (AddressData) -> Unit,
  fieldsConfig: AddressFieldsConfig = AddressFieldsConfig(),
  errors: AddressErrors = AddressErrors(),
  onFieldFocused: ((Float) -> Unit)? = null,
  isSmallScreen: Boolean = false,
  modifier: Modifier = Modifier
) {
  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()

  // State for map dialog
  var showMapDialog by remember { mutableStateOf(false) }

  // State for reverse geocoding
  var isReverseGeocoding by remember { mutableStateOf(false) }

  // Focus requesters for keyboard navigation
  val addressFocusRequester = remember { FocusRequester() }
  val pincodeFocusRequester = remember { FocusRequester() }

  // HTTP client for reverse geocoding
  val httpClient = remember {
    HttpClient {
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
          coerceInputValues = true
        })
      }
    }
  }

  // Function to perform reverse geocoding
  suspend fun reverseGeocode(lat: Double, lon: Double) {
    isReverseGeocoding = true
    try {
      println("Starting reverse geocoding for lat: $lat, lon: $lon")

      // Try with Hindi first, then fallback to English
      val languages = listOf("hi", "en")

      for (lang in languages) {
        val url = "https://nominatim.openstreetmap.org/reverse?" +
          "lat=$lat&lon=$lon&format=json&addressdetails=1&accept-language=$lang"

        println("Trying reverse geocoding with language: $lang")

        try {
          val response: HttpResponse = httpClient.get(url) {
            headers {
              append("User-Agent", "AryaMahasangh/1.0")
              // Don't send content-type header for GET requests - causes CORS issues
              // append("Accept", "application/json") // Also not needed, causes issues
            }
          }

          println("Response status: ${response.status}")

          if (response.status.value == 200) {
            val jsonResponse = response.bodyAsText()
            println("Reverse geocoding response: $jsonResponse")

            // Parse the response manually to handle potential issues
            val jsonObject = Json.parseToJsonElement(jsonResponse).jsonObject
            val address = jsonObject["address"]?.jsonObject

            if (address != null) {
              println("Address object found: $address")

              // Build address from components
              val addressParts = mutableListOf<String>()
              address["house_number"]?.jsonPrimitive?.content?.let { addressParts.add(it) }
              address["road"]?.jsonPrimitive?.content?.let { addressParts.add(it) }
              address["suburb"]?.jsonPrimitive?.content?.let { addressParts.add(it) }
              address["city"]?.jsonPrimitive?.content?.let { addressParts.add(it) }
              address["town"]?.jsonPrimitive?.content?.let { addressParts.add(it) }
              address["village"]?.jsonPrimitive?.content?.let { addressParts.add(it) }

              val fullAddress = addressParts.joinToString(", ")
              println("Full address: $fullAddress")

              // Get state
              val stateStr = address["state"]?.jsonPrimitive?.content ?: ""
              println("State from API: $stateStr")

              // Map state name to match our state list - check both Hindi and English
              val mappedState = when {
                // Hindi mappings
                stateStr.contains("उत्तर प्रदेश", ignoreCase = true) -> "उत्तर प्रदेश"
                stateStr.contains("हरियाणा", ignoreCase = true) -> "हरियाणा"
                stateStr.contains("दिल्ली", ignoreCase = true) -> "दिल्ली"
                stateStr.contains("राजस्थान", ignoreCase = true) -> "राजस्थान"
                stateStr.contains("मध्य प्रदेश", ignoreCase = true) -> "मध्य प्रदेश"
                stateStr.contains("महाराष्ट्र", ignoreCase = true) -> "महाराष्ट्र"
                stateStr.contains("गुजरात", ignoreCase = true) -> "गुजरात"
                stateStr.contains("बिहार", ignoreCase = true) -> "बिहार"
                stateStr.contains("पंजाब", ignoreCase = true) -> "पंजाब"
                // English mappings
                stateStr.contains("Uttar Pradesh", ignoreCase = true) -> "उत्तर प्रदेश"
                stateStr.contains("Haryana", ignoreCase = true) -> "हरियाणा"
                stateStr.contains("Delhi", ignoreCase = true) -> "दिल्ली"
                stateStr.contains("NCT of Delhi", ignoreCase = true) -> "दिल्ली"
                stateStr.contains("National Capital Territory", ignoreCase = true) -> "दिल्ली"
                stateStr.contains("Rajasthan", ignoreCase = true) -> "राजस्थान"
                stateStr.contains("Madhya Pradesh", ignoreCase = true) -> "मध्य प्रदेश"
                stateStr.contains("Maharashtra", ignoreCase = true) -> "महाराष्ट्र"
                stateStr.contains("Gujarat", ignoreCase = true) -> "गुजरात"
                stateStr.contains("Bihar", ignoreCase = true) -> "बिहार"
                stateStr.contains("Punjab", ignoreCase = true) -> "पंजाब"
                stateStr.contains("West Bengal", ignoreCase = true) -> "पश्चिम बंगाल"
                stateStr.contains("Tamil Nadu", ignoreCase = true) -> "तमिलनाडु"
                stateStr.contains("Karnataka", ignoreCase = true) -> "कर्नाटक"
                stateStr.contains("Kerala", ignoreCase = true) -> "केरल"
                stateStr.contains("Andhra Pradesh", ignoreCase = true) -> "आंध्र प्रदेश"
                stateStr.contains("Telangana", ignoreCase = true) -> "तेलंगाना"
                stateStr.contains("Odisha", ignoreCase = true) -> "ओडिशा"
                stateStr.contains("Orissa", ignoreCase = true) -> "ओडिशा"
                stateStr.contains("Jharkhand", ignoreCase = true) -> "झारखंड"
                stateStr.contains("Chhattisgarh", ignoreCase = true) -> "छत्तीसगढ़"
                stateStr.contains("Uttarakhand", ignoreCase = true) -> "उत्तराखंड"
                stateStr.contains("Himachal Pradesh", ignoreCase = true) -> "हिमाचल प्रदेश"
                stateStr.contains("Jammu", ignoreCase = true) -> "जम्मू और कश्मीर"
                stateStr.contains("Kashmir", ignoreCase = true) -> "जम्मू और कश्मीर"
                stateStr.contains("Assam", ignoreCase = true) -> "असम"
                stateStr.contains("Sikkim", ignoreCase = true) -> "सिक्किम"
                stateStr.contains("Meghalaya", ignoreCase = true) -> "मेघालय"
                stateStr.contains("Tripura", ignoreCase = true) -> "त्रिपुरा"
                stateStr.contains("Mizoram", ignoreCase = true) -> "मिजोरम"
                stateStr.contains("Manipur", ignoreCase = true) -> "मणिपुर"
                stateStr.contains("Nagaland", ignoreCase = true) -> "नागालैंड"
                stateStr.contains("Arunachal Pradesh", ignoreCase = true) -> "अरुणाचल प्रदेश"
                stateStr.contains("Goa", ignoreCase = true) -> "गोवा"
                else -> ""
              }

              println("Mapped state: $mappedState")

              // Get district
              val districtStr = address["state_district"]?.jsonPrimitive?.content
                ?: address["county"]?.jsonPrimitive?.content ?: ""
              println("District from API: $districtStr")

              val stateDistricts = indianStatesToDistricts[mappedState] ?: emptyList()
              val mappedDistrict = if (districtStr.isNotEmpty() && stateDistricts.isNotEmpty()) {
                stateDistricts.find { districtInList ->
                  // Check if API district contains our district or vice versa
                  districtStr.contains(districtInList, ignoreCase = true) ||
                    districtInList.contains(districtStr, ignoreCase = true) ||
                  // Also check without "district" suffix
                  districtStr.replace(" district", "", ignoreCase = true)
                    .contains(districtInList.replace(" district", "", ignoreCase = true), ignoreCase = true)
                } ?: ""
              } else {
                ""
              }

              println("Mapped district: $mappedDistrict")

              // Get pincode
              val pincode = address["postcode"]?.jsonPrimitive?.content ?: ""

              // Update address data
              val newAddressData = addressData.copy(
                address = if (fullAddress.isNotBlank()) fullAddress else addressData.address,
                state = if (mappedState.isNotBlank()) mappedState else addressData.state,
                district = if (mappedDistrict.isNotBlank()) mappedDistrict else addressData.district,
                pincode = if (pincode.isNotBlank()) pincode else addressData.pincode
              )

              println("Updating address data: $newAddressData")
              onAddressChange(newAddressData)

              // If we successfully got and mapped data, don't try other languages
              if (mappedState.isNotBlank() || fullAddress.isNotBlank()) {
                isReverseGeocoding = false
                return
              }
            }
          } else if (response.status.value == 403 || response.status.value == 429) {
            println("API rate limit or blocked: ${response.status}")
            // Don't try other languages if we're rate limited
            break
          }
        } catch (e: Exception) {
          println("Error making request for language $lang: ${e.message}")
          // Continue to next language
        }
      }

      println("Reverse geocoding completed without finding valid data")
    } catch (e: Exception) {
      println("Reverse geocoding error: ${e.message}")
      e.printStackTrace()
    } finally {
      isReverseGeocoding = false
    }
  }

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Location Section
    if (fieldsConfig.showLocation) {
      Column {
        Text(
          "स्थान चुनें (Location)",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedButton(
          onClick = { showMapDialog = true },
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Icon(
            Icons.Filled.Map,
            contentDescription = "मानचित्र से चुनें",
            modifier = Modifier.size(ButtonDefaults.IconSize)
          )
          Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          Text("मानचित्र से चुनें")
        }

        if (addressData.location != null) {
          Text(
            "चुना गया स्थान: ${addressData.location.latitude}, ${addressData.location.longitude}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        errors.locationError?.let {
          Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    }

    // Address field
    if (fieldsConfig.showAddress) {
      OutlinedTextField(
        value = addressData.address,
        onValueChange = { newAddress ->
          if (newAddress.length <= 200) {
            onAddressChange(addressData.copy(address = newAddress))
          }
        },
        label = { Text("पूर्ण पता") },
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(addressFocusRequester),
        minLines = 2,
        maxLines = 3,
        isError = errors.addressError != null,
        supportingText = { errors.addressError?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
          onNext = { focusManager.moveFocus(FocusDirection.Next) }
        )
      )
    }

    // State, District, and Vidhansabha in a flow row
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      // State Dropdown
      if (fieldsConfig.showState) {
        StateDropdown(
          states = indianStatesToDistricts.keys.toList(),
          selectedState = addressData.state,
          onStateSelected = { newState ->
            onAddressChange(
              addressData.copy(
                state = newState,
                district = "", // Reset district when state changes
                vidhansabha = "" // Reset vidhansabha when state changes
              )
            )
          },
          modifier = Modifier.width(160.dp),
          isError = errors.stateError != null,
          errorMessage = errors.stateError ?: ""
        )
      }

      // District Dropdown
      if (fieldsConfig.showDistrict && addressData.state.isNotEmpty()) {
        val districts = indianStatesToDistricts[addressData.state] ?: emptyList()
        DistrictDropdown(
          districts = districts,
          selectedDistrict = addressData.district,
          onDistrictSelected = { newDistrict ->
            onAddressChange(addressData.copy(district = newDistrict ?: ""))
          },
          modifier = Modifier.width(200.dp),
          isError = errors.districtError != null,
          errorMessage = errors.districtError ?: "",
          isMandatory = errors.districtError != null
        )
      }

      // Vidhansabha Dropdown
      if (fieldsConfig.showVidhansabha && addressData.state.isNotEmpty()) {
        val vidhansabhas = vidhansabhaByState[addressData.state] ?: emptyList()
        VidhansabhaDropdown(
          vidhansabhas = vidhansabhas,
          selectedVidhansabha = addressData.vidhansabha,
          onVidhansabhaSelected = { newVidhansabha ->
            onAddressChange(addressData.copy(vidhansabha = newVidhansabha))
          },
          modifier = Modifier.width(200.dp),
          isError = errors.vidhansabhaError != null,
          errorMessage = errors.vidhansabhaError ?: ""
        )
      }
    }

    // Pincode field
    if (fieldsConfig.showPincode) {
      OutlinedTextField(
        value = addressData.pincode,
        onValueChange = { newPincode ->
          if (newPincode.isEmpty() || (newPincode.all { it.isDigit() } && newPincode.length <= 10)) {
            onAddressChange(addressData.copy(pincode = newPincode))
          }
        },
        label = { Text("पिन कोड") },
        modifier = Modifier
          .width(150.dp)
          .focusRequester(pincodeFocusRequester),
        singleLine = true,
        isError = errors.pincodeError != null,
        supportingText = { errors.pincodeError?.let { Text(it) } },
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Number,
          imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
          onDone = { focusManager.clearFocus() }
        )
      )
    }

    // Map Location Picker Dialog
    if (showMapDialog) {
      MapLocationPickerDialog(
        onDismiss = { showMapDialog = false },
        onLocationPicked = { latLng ->
          onAddressChange(addressData.copy(location = latLng))
          showMapDialog = false

          // Perform reverse geocoding
          scope.launch {
            reverseGeocode(latLng.latitude, latLng.longitude)
          }
        }
      )
    }

    // Show loading indicator for reverse geocoding
    if (isReverseGeocoding) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.Center
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("पता प्राप्त कर रहा है...")
      }
    }
  }
}

/**
 * Vidhansabha Dropdown component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VidhansabhaDropdown(
  vidhansabhas: List<String>,
  selectedVidhansabha: String,
  onVidhansabhaSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  errorMessage: String = ""
) {
  var expanded by remember { mutableStateOf(false) }

  Column(modifier = modifier) {
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded }
    ) {
      OutlinedTextField(
        readOnly = true,
        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        value = selectedVidhansabha.ifEmpty { "विधानसभा चुनें" },
        label = { Text("विधानसभा") },
        onValueChange = {},
        placeholder = { Text("Vidhansabha") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        isError = isError,
        supportingText = { if (isError && errorMessage.isNotEmpty()) Text(errorMessage) }
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        vidhansabhas.forEach { vidhansabha ->
          DropdownMenuItem(
            text = { Text(vidhansabha) },
            onClick = {
              onVidhansabhaSelected(vidhansabha)
              expanded = false
            },
            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
          )
        }
      }
    }
  }
}

/**
 * Utility function to validate address data
 */
fun validateAddressData(
  addressData: AddressData,
  fieldsConfig: AddressFieldsConfig,
  requiredFields: Set<String> = emptySet()
): AddressErrors {
  return AddressErrors(
    locationError = if (fieldsConfig.showLocation && "location" in requiredFields && addressData.location == null) {
      "स्थान चुनना अपेक्षित है"
    } else null,

    addressError = if (fieldsConfig.showAddress && "address" in requiredFields && addressData.address.isBlank()) {
      "पता अपेक्षित है"
    } else null,

    stateError = if (fieldsConfig.showState && "state" in requiredFields && addressData.state.isBlank()) {
      "राज्य चुनना अपेक्षित है"
    } else null,

    districtError = if (fieldsConfig.showDistrict && "district" in requiredFields && addressData.district.isBlank()) {
      "जिला चुनना अपेक्षित है"
    } else null,

    vidhansabhaError = if (fieldsConfig.showVidhansabha && "vidhansabha" in requiredFields && addressData.vidhansabha.isBlank()) {
      "विधानसभा चुनना अपेक्षित है"
    } else null,

    pincodeError = if (fieldsConfig.showPincode && "pincode" in requiredFields) {
      when {
        addressData.pincode.isBlank() -> "पिन कोड अपेक्षित है"
        addressData.pincode.length < 6 -> "पिन कोड कम से कम 6 अंक का होना चाहिए"
        !addressData.pincode.all { it.isDigit() } -> "पिन कोड में केवल अंक होने चाहिए"
        else -> null
      }
    } else null
  )
}
