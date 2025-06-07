package org.aryamahasangh.utils

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TBD(){
  Text("निर्माणाधीन", modifier = Modifier.padding(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithTooltip(tooltip: String, content: @Composable () -> Unit) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    tooltip = { PlainTooltip { Text(tooltip) } },
    state = rememberTooltipState()
  ) {
    content()
  }
}

fun LocalDateTime.toHumanReadable(): String {
  val daysOfWeek = listOf("रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार")
  val months =
    listOf(
      "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून",
      "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

  val dayOfWeek = daysOfWeek[this.dayOfWeek.ordinal]
  val month = months[this.monthNumber - 1]
  val day = this.dayOfMonth
  val year = this.year
  val hour = this.hour % 12
  val minute = this.minute.toString().padStart(2, '0')
  val amPm =
    if (this.hour < 12) {
      "प्रातः"
    } else if (this.hour in 12..15) {
      "दोपहर"
    } else {
      "सायं"
    }

  return "$dayOfWeek, $month $day, $year $amPm ${if (hour == 0) 12 else hour}:$minute"
}

fun LocalDateTime.toShortHumanReadable(): String {
  val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

  val month = months[this.monthNumber - 1]
  val day = this.dayOfMonth
  val year = this.year
  val hour = this.hour % 12
  val minute = this.minute.toString().padStart(2, '0')
  val amPm = if (this.hour < 12) "am" else "pm"

  return "$month $day, $year ${if (hour == 0) 12 else hour}:$minute$amPm"
}

fun format(dateTime: Any): String {
  return LocalDateTime.parse(dateTime as String).toHumanReadable()
}

fun formatShort(dateTime: LocalDateTime): String {
  return dateTime.toShortHumanReadable()
}

fun formatForBook(dateTime: Any): String {
  val dateTimeStr = dateTime as String
  // Parse as Instant to handle timezone information
  val instant = Instant.parse(dateTimeStr)
  // Convert to LocalDateTime in the system's default timezone
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  return localDateTime.toHumanReadable()
}

fun formatShortForBook(dateTime: Any): String {
  val dateTimeStr = dateTime as String
  // Parse as Instant to handle timezone information
  val instant = Instant.parse(dateTimeStr)
  // Convert to LocalDateTime in the system's default timezone
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
  return localDateTime.toShortHumanReadable()
}

fun epochToDate(epochMillis: Long): String {
  // Convert epoch milliseconds to Instant
  val instant = Instant.fromEpochMilliseconds(epochMillis)

  // Convert Instant to LocalDateTime in the system's default time zone
  val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

  // Extract the date part (year, month, day)
  val date = localDateTime.date

  // Format the date as dd/mm/yyyy
  return "${date.dayOfMonth.toString().padStart(2, '0')}/" +
    "${date.monthNumber.toString().padStart(2, '0')}/" +
    date.year
}
