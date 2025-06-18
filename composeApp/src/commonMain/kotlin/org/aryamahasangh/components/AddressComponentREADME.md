# AddressComponent Documentation

A comprehensive and flexible address input component for collecting location and address information with built-in
support for Indian states, districts, vidhansabhas, and reverse geocoding.

## Features

- **Location Selection**: Interactive map picker with automatic reverse geocoding
- **Auto-fill**: When a location is selected from the map, address fields are automatically populated using reverse
  geocoding
- **Hindi/Devanagari Support**: All labels and error messages are in Hindi
- **Flexible Field Configuration**: Show/hide any combination of fields based on your needs
- **Built-in Validation**: Comprehensive validation with Hindi error messages
- **Keyboard Navigation**: Proper focus management and keyboard actions
- **Mobile-Friendly**: Supports small screen keyboard handling

## Basic Usage

```kotlin
import org.aryamahasangh.components.*

@Composable
fun MyScreen() {
    var addressData by remember { mutableStateOf(AddressData()) }
    
    AddressComponent(
        addressData = addressData,
        onAddressChange = { addressData = it }
    )
}
```

## Field Configuration

Control which fields are displayed using `AddressFieldsConfig`:

```kotlin
AddressComponent(
    addressData = addressData,
    onAddressChange = { addressData = it },
    fieldsConfig = AddressFieldsConfig(
        showLocation = true,    // Map location picker
        showAddress = true,     // Full address text field
        showState = true,       // State dropdown
        showDistrict = true,    // District dropdown (loads based on state)
        showVidhansabha = true, // Vidhansabha dropdown (loads based on state)
        showPincode = true      // Pincode input field
    )
)
```

### Example: Only State, District, and Vidhansabha

```kotlin
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
    )
)
```

## Validation

Use the `validateAddressData` utility function to validate address fields:

```kotlin
var showErrors by remember { mutableStateOf(false) }

// Define which fields are required
val requiredFields = setOf("address", "state", "district", "pincode")

// Validate on demand
val errors = if (showErrors) {
    validateAddressData(
        addressData,
        fieldsConfig,
        requiredFields
    )
} else {
    AddressErrors()
}

// Pass errors to the component
AddressComponent(
    addressData = addressData,
    onAddressChange = { addressData = it },
    errors = errors
)

// In your submit button
Button(onClick = {
    showErrors = true
    val validationErrors = validateAddressData(addressData, fieldsConfig, requiredFields)
    
    if (/* no errors */) {
        // Proceed with form submission
    }
}) {
    Text("Submit")
}
```

## Data Model

### AddressData

```kotlin
data class AddressData(
    val location: LatLng? = null,      // Latitude and longitude
    val address: String = "",          // Full address text
    val state: String = "",            // Selected state
    val district: String = "",         // Selected district
    val vidhansabha: String = "",      // Selected vidhansabha
    val pincode: String = ""           // 6-10 digit pincode
)
```

### AddressErrors

```kotlin
data class AddressErrors(
    val locationError: String? = null,
    val addressError: String? = null,
    val stateError: String? = null,
    val districtError: String? = null,
    val vidhansabhaError: String? = null,
    val pincodeError: String? = null
)
```

## Advanced Features

### Reverse Geocoding

When a user selects a location from the map, the component automatically:

1. Fetches address details using OpenStreetMap's Nominatim API
2. Populates the address field with the street address
3. Attempts to match and select the appropriate state and district
4. Fills the pincode if available

### Keyboard Handling for Mobile

For small screens, you can enable special keyboard handling:

```kotlin
AddressComponent(
    addressData = addressData,
    onAddressChange = { addressData = it },
    isSmallScreen = maxWidth < 600.dp && !isDesktop(),
    onFieldFocused = { offset ->
        // Scroll to show the focused field above keyboard
        scrollToFocusedField(offset)
    }
)
```

## Integration Example with Activity Form

```kotlin
@Composable
fun CreateActivityScreen() {
    var activityName by remember { mutableStateOf("") }
    var addressData by remember { mutableStateOf(AddressData()) }
    var showErrors by remember { mutableStateOf(false) }
    
    val requiredAddressFields = setOf("location", "address", "state", "district")
    val addressErrors = if (showErrors) {
        validateAddressData(addressData, AddressFieldsConfig(), requiredAddressFields)
    } else {
        AddressErrors()
    }
    
    Column {
        // Other activity fields...
        
        // Address section
        AddressComponent(
            addressData = addressData,
            onAddressChange = { addressData = it },
            errors = addressErrors
        )
        
        Button(onClick = {
            showErrors = true
            // Validate all fields including address
            if (/* all validations pass */) {
                // Create activity with:
                // - location: addressData.location
                // - address: addressData.address
                // - state: addressData.state
                // - district: addressData.district
                // - latitude: addressData.location?.latitude
                // - longitude: addressData.location?.longitude
            }
        }) {
            Text("Create Activity")
        }
    }
}
```

## Supported States and Districts

The component includes comprehensive data for all Indian states and union territories with their respective districts.
Vidhansabha data is currently available for:

- हरियाणा (Haryana)
- दिल्ली (Delhi)

## Error Messages

All error messages are in Hindi:

- Location: "स्थान चुनना आवश्यक है"
- Address: "पता आवश्यक है"
- State: "राज्य चुनना आवश्यक है"
- District: "जिला चुनना आवश्यक है"
- Vidhansabha: "विधानसभा चुनना आवश्यक है"
- Pincode: "पिन कोड आवश्यक है" / "पिन कोड कम से कम 6 अंक का होना चाहिए"

## Notes

- District and Vidhansabha dropdowns are automatically populated based on the selected state
- When state changes, district and vidhansabha selections are cleared
- Pincode accepts 6-10 digits only
- The map location picker uses OpenStreetMap with India bounds
- Reverse geocoding attempts to match states and districts to the predefined lists for consistency
