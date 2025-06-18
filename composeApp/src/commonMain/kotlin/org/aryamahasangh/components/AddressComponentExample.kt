package org.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Example usage of AddressComponent showing different configurations
 */
@Composable
fun AddressComponentExample() {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Example 1: Full address component with all fields
    FullAddressExample()

    HorizontalDivider()

    // Example 2: Only state, district, and vidhansabha
    MinimalAddressExample()

    HorizontalDivider()

    // Example 3: Address with validation
    AddressWithValidationExample()
  }
}

@Composable
private fun FullAddressExample() {
  var addressData by remember { mutableStateOf(AddressData()) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "पूर्ण पता प्रपत्र (सभी फ़ील्ड के साथ)",
      style = MaterialTheme.typography.headlineSmall
    )

    AddressComponent(
      addressData = addressData,
      onAddressChange = { addressData = it },
      fieldsConfig = AddressFieldsConfig(
        showLocation = true,
        showAddress = true,
        showState = true,
        showDistrict = true,
        showVidhansabha = true,
        showPincode = true
      ),
      modifier = Modifier.fillMaxWidth()
    )

    // Display collected data
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
      )
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text("एकत्रित डेटा:", style = MaterialTheme.typography.titleMedium)
        addressData.location?.let { location ->
          Text("स्थान: ${location.latitude}, ${location.longitude}")
        }
        val address = addressData.address
        if (address.isNotBlank()) {
          Text("पता: $address")
        }
        if (addressData.state.isNotBlank()) {
          Text("राज्य: ${addressData.state}")
        }
        if (addressData.district.isNotBlank()) {
          Text("जिला: ${addressData.district}")
        }
        if (addressData.vidhansabha.isNotBlank()) {
          Text("विधानसभा: ${addressData.vidhansabha}")
        }
        if (addressData.pincode.isNotBlank()) {
          Text("पिन कोड: ${addressData.pincode}")
        }
      }
    }
  }
}

@Composable
private fun MinimalAddressExample() {
  var addressData by remember { mutableStateOf(AddressData()) }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "केवल राज्य, जिला और विधानसभा",
      style = MaterialTheme.typography.headlineSmall
    )

    AddressComponent(
      addressData = addressData,
      onAddressChange = { addressData = it },
      fieldsConfig = AddressFieldsConfig(
        showLocation = false,
        showAddress = false,
        showState = true,
        showDistrict = true,
        showVidhansabha = true,
        showPincode = false
      ),
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun AddressWithValidationExample() {
  var addressData by remember { mutableStateOf(AddressData()) }
  var showErrors by remember { mutableStateOf(false) }

  // Define which fields are required
  val requiredFields = setOf("address", "state", "district", "pincode")

  // Validate on demand
  val errors = if (showErrors) {
    validateAddressData(
      addressData,
      AddressFieldsConfig(
        showLocation = false,
        showAddress = true,
        showState = true,
        showDistrict = true,
        showVidhansabha = false,
        showPincode = true
      ),
      requiredFields
    )
  } else {
    AddressErrors()
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "पता सत्यापन के साथ",
      style = MaterialTheme.typography.headlineSmall
    )

    AddressComponent(
      addressData = addressData,
      onAddressChange = {
        addressData = it
        // Clear errors as user types
        if (showErrors) {
          showErrors = false
        }
      },
      fieldsConfig = AddressFieldsConfig(
        showLocation = false,
        showAddress = true,
        showState = true,
        showDistrict = true,
        showVidhansabha = false,
        showPincode = true
      ),
      errors = errors,
      modifier = Modifier.fillMaxWidth()
    )

    Button(
      onClick = {
        showErrors = true
        // Check if all required fields are valid
        val validationErrors = validateAddressData(
          addressData,
          AddressFieldsConfig(
            showLocation = false,
            showAddress = true,
            showState = true,
            showDistrict = true,
            showVidhansabha = false,
            showPincode = true
          ),
          requiredFields
        )

        val hasErrors = listOf(
          validationErrors.addressError,
          validationErrors.stateError,
          validationErrors.districtError,
          validationErrors.pincodeError
        ).any { it != null }

        if (!hasErrors) {
          // Form is valid, proceed with submission
          println("Form submitted with data: $addressData")
        }
      },
      modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
      Text("जमा करें")
    }
  }
}

/**
 * Example of using AddressComponent in a real form like CreateActivityFormScreen
 */
@Composable
fun ActivityFormWithAddressExample() {
  var name by remember { mutableStateOf("") }
  var addressData by remember { mutableStateOf(AddressData()) }
  var showErrors by remember { mutableStateOf(false) }

  val requiredFields = setOf("location", "address", "state", "district")
  val addressErrors = if (showErrors) {
    validateAddressData(
      addressData,
      AddressFieldsConfig(),
      requiredFields
    )
  } else {
    AddressErrors()
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      "गतिविधि बनाएं",
      style = MaterialTheme.typography.headlineMedium
    )

    OutlinedTextField(
      value = name,
      onValueChange = { name = it },
      label = { Text("गतिविधि का नाम") },
      modifier = Modifier.fillMaxWidth(),
      isError = showErrors && name.isBlank(),
      supportingText = {
        if (showErrors && name.isBlank()) {
          Text("नाम आवश्यक है")
        }
      }
    )

    // Address Component integrated into the form
    AddressComponent(
      addressData = addressData,
      onAddressChange = { addressData = it },
      errors = addressErrors,
      modifier = Modifier.fillMaxWidth()
    )

    Button(
      onClick = {
        showErrors = true

        // Validate all fields including address
        val addressValidationErrors = validateAddressData(
          addressData,
          AddressFieldsConfig(),
          requiredFields
        )

        val hasAddressErrors = listOf(
          addressValidationErrors.locationError,
          addressValidationErrors.addressError,
          addressValidationErrors.stateError,
          addressValidationErrors.districtError
        ).any { it != null }

        if (name.isNotBlank() && !hasAddressErrors) {
          // All validations passed
          println("Activity created with name: $name and address: $addressData")
        }
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("गतिविधि बनाएं")
    }
  }
}

@Preview
@Composable
fun AddressComponentExamplePreview() {
  MaterialTheme {
    Surface {
      AddressComponentExample()
    }
  }
}
