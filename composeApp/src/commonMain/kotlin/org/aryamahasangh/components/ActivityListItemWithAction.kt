package org.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aryamahasangh.features.activities.GenderAllowed
import org.aryamahasangh.features.arya_nirman.UpcomingActivity
import org.aryamahasangh.features.arya_nirman.convertDates

// --- Data Structures ---

// Enum for gender restriction
enum class GenderRestriction {
  MALE,
  FEMALE,
  UNISEX
}

// Data class for event information
data class EventItemData(
  val id: String,
  val title: String,
  val dateRange: String,
  val timeRange: String,
  val place: String,
  val genderRestriction: GenderRestriction,
  val totalCapacity: Int,
  val seatsFilled: Int
) {
  val seatsLeft: Int
    get() = totalCapacity - seatsFilled

  val isFull: Boolean
    get() = seatsLeft <= 0
}

// Example UiState (simplified for this component) - You'd have this in your ViewModel
data class EventListUiState(
  val events: List<EventItemData> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

// --- Composable ---

// Define some theme-consistent colors (or use MaterialTheme.colorScheme directly)
val SeatsAvailableColor = Color(0xFF4CAF50) // A nice green
val SeatsFullColor = Color(0xFFD32F2F) // A common error red, can also use MaterialTheme.colorScheme.error
// val SubtleTextColor = MaterialTheme.colorScheme.onSurfaceVariant // Use this if you want theme-aware subtle text

fun TextStyle.asSpanStyle() =
  SpanStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    fontFamily = fontFamily,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration
  )

@Composable
fun EventListItem(
  event: UpcomingActivity,
  onRegisterClick: (eventId: String) -> Unit,
  onDirectionsClick: (place: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val subtleTextColor = MaterialTheme.colorScheme.onSurfaceVariant // Get from theme

  ElevatedCard(
    modifier =
      modifier
        .widthIn(max = 500.dp)
        .fillMaxWidth(),
    shape = RoundedCornerShape(4.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseOnSurface)
  ) {
    Column(
      modifier =
        Modifier
          .padding(vertical = 16.dp, horizontal = 8.dp)
          .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Event Title
      val baseTextStyle = MaterialTheme.typography.titleLarge

//      Text(
//        text = event.title,
//        style = MaterialTheme.typography.titleLarge,
//        fontWeight = FontWeight.Bold,
//        color = MaterialTheme.colorScheme.onSurface
//      )

      Text(
        text =
          buildAnnotatedString {
            withStyle(
              style =
                baseTextStyle.asSpanStyle().copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
            ) {
              append(event.name)
            }
            append(" ")
            withStyle(
              style =
                MaterialTheme.typography.labelMedium.asSpanStyle().copy(
                  color = MaterialTheme.colorScheme.secondary
                )
            ) {
              append(
                when (event.genderAllowed) {
                  GenderAllowed.ANY -> ""
                  GenderAllowed.MALE -> "(पुरुष)"
                  GenderAllowed.FEMALE -> "(महिला)"
                }
              )
            }
          },
      )

      // Date and Time
      Text(
        text =
          buildAnnotatedString {
            val (dateRange, timeRange) = convertDates(event.startDateTime, event.endDateTime)
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

      // Place and Directions
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.LocationOn, // Updated Icon
            contentDescription = "स्थान", // "Place" in Devanagari
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = event.district,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(
          onClick = { onDirectionsClick("") },
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            imageVector = Icons.Filled.Navigation, // Updated Icon
            contentDescription = "दिशा-निर्देश", // "Directions" in Devanagari
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Capacity and Registration Status
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        if (event.isFull) {
          Text(
            text = "पंजीकरण बंद", // "Registrations closed"
            style = MaterialTheme.typography.labelLarge,
            color = SeatsFullColor,
            fontWeight = FontWeight.Bold
          )
        } else {
          Text(
            text = "पंजीकरण चालू", // "X seats left"
            style = MaterialTheme.typography.labelLarge,
            color = SeatsAvailableColor,
            fontWeight = FontWeight.Bold
          )
        }

        Button(
          onClick = { onRegisterClick(event.id) },
          enabled = !event.isFull,
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
              disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        ) {
          Text("पंजीकरण") // "Register"
        }
      }
    }
  }
}

// --- Preview Composable ---

// Dummy callback functions for preview
fun dummyRegisterCallback(eventId: String) {
  println("Register clicked for $eventId")
}

fun dummyDirectionsCallback(place: String) {
  println("Directions clicked for $place")
}

val sampleEventAvailable =
  EventItemData(
    id = "1",
    title = "आर्य प्रशिक्षण सत्र", // "Annual Tech Conference"
    dateRange = "२४-२५ मई, २०२५", // "24-25 May, 2025"
    timeRange = "प्रातः ९:०० - सायं ६:००", // "9:00am - 6:00pm"
    place = "बैंगलोर", // "Bangalore"
    genderRestriction = GenderRestriction.UNISEX,
    totalCapacity = 100,
    seatsFilled = 75
  )

val sampleEventFullFemaleOnly =
  EventItemData(
    id = "2",
    title = "आर्या प्रशिक्षण सत्र", // "Art Workshop"
    dateRange = "१८ जून, २०२५", // "18 June, 2025"
    timeRange = "प्रातः १०:०० - सायं ४:००", // "10:00am - 4:00pm"
    place = "दिल्ली", // "Delhi"
    genderRestriction = GenderRestriction.FEMALE,
    totalCapacity = 50,
    seatsFilled = 50
  )

val sampleEventMaleOnly =
  EventItemData(
    id = "3",
    title = "आर्य निर्माण सत्र", // "Entrepreneurship Summit"
    dateRange = "१२-१४ जुलाई, २०२५", // "12-14 July, 2025"
    timeRange = "प्रातः ९:३० - सायं ५:३०", // "9:30am - 5:30pm"
    place = "मुंबई", // "Mumbai"
    genderRestriction = GenderRestriction.MALE,
    totalCapacity = 200,
    seatsFilled = 150
  )

// val activityListItemsWithActions = listOf(sampleEventAvailable, sampleEventFullFemaleOnly, sampleEventMaleOnly)
//
// @OptIn(ExperimentalLayoutApi::class) // For FlowRow
// @Preview
// @Composable
// fun EventListItemCombinedPreview() {
//
//
//  MaterialTheme { // Essential for Material 3 previews
//    Surface(color = MaterialTheme.colorScheme.background) {
//      // To demonstrate inside a FlowRow (optional, shows how it might look if multiple items)
//      // Or use LazyColumn for a scrollable list
//      Column(
//        modifier = Modifier.padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//      ) {
//        Text("Events (Column Layout):", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
//        EventListItem(
//          event = sampleEventAvailable,
//          onRegisterClick = ::dummyRegisterCallback,
//          onDirectionsClick = ::dummyDirectionsCallback
//        )
//        EventListItem(
//          event = sampleEventFullFemaleOnly,
//          onRegisterClick = ::dummyRegisterCallback,
//          onDirectionsClick = ::dummyDirectionsCallback
//        )
//        EventListItem(
//          event = sampleEventMaleOnly,
//          onRegisterClick = ::dummyRegisterCallback,
//          onDirectionsClick = ::dummyDirectionsCallback
//        )
//
//        Spacer(Modifier.height(20.dp))
//        Text("Events (FlowRow Layout - if screen is wide enough):", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
//
//        BoxWithConstraints {
//          FlowRow(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//            maxItemsInEachRow = if (maxWidth > 600.dp) 2 else 1 // Example responsive FlowRow
//          ) {
//            EventListItem(
//              event = sampleEventAvailable.copy(id="flow1", title="फ्लो आइटम १"), // Flow Item 1
//              onRegisterClick = ::dummyRegisterCallback,
//              onDirectionsClick = ::dummyDirectionsCallback,
//              modifier = Modifier.weight(1f) // Example for FlowRow distribution
//            )
//            EventListItem(
//              event = sampleEventFullFemaleOnly.copy(id="flow2", title="फ्लो आइटम २"), // Flow Item 2
//              onRegisterClick = ::dummyRegisterCallback,
//              onDirectionsClick = ::dummyDirectionsCallback,
//              modifier = Modifier.weight(1f) // Example for FlowRow distribution
//            )
//          }
//        }
//      }
//    }
//  }
// }
