package com.aryamahasangh.features.arya_nirman

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import com.aryamahasangh.features.activities.GenderAllowed

data class UpcomingActivity(
  val id: String,
  val name: String,
  val genderAllowed: GenderAllowed,
  val isFull: Boolean,
  val startDateTime: LocalDateTime,
  val endDateTime: LocalDateTime,
  val district: String,
  val state: String,
  val latitude: Double,
  val longitude: Double,
  val capacity: Int
)

@Serializable
data class SatrRegistrationCount(
  val id: String,
  val activity_id: String
)

fun convertDates(
  fromDateTime: LocalDateTime,
  toDateTime: LocalDateTime
): Pair<String, String> {
  val devanagariDigits =
    mapOf(
      '0' to '०', '1' to '१', '2' to '२', '3' to '३', '4' to '४',
      '5' to '५', '6' to '६', '7' to '७', '8' to '८', '9' to '९'
    )

  fun toDevanagari(input: String): String {
    return input.map { devanagariDigits[it] ?: it }.joinToString("")
  }

  val monthNames =
    arrayOf(
      "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
      "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

  val fromDay = fromDateTime.dayOfMonth
  val toDay = toDateTime.dayOfMonth
  val fromMonth = fromDateTime.monthNumber
  val toMonth = toDateTime.monthNumber
  val fromYear = fromDateTime.year
  val toYear = toDateTime.year

  val dateStr =
    when {
      fromYear == toYear && fromMonth == toMonth && fromDay == toDay -> {
        "${toDevanagari("$fromDay")} ${monthNames[fromMonth - 1]}, ${toDevanagari("$fromYear")}"
      }

      fromYear == toYear && fromMonth == toMonth -> {
        "${
          toDevanagari(
            "$fromDay"
          )
        }-${toDevanagari("$toDay")} ${monthNames[fromMonth - 1]}, ${toDevanagari("$fromYear")}"
      }

      fromYear == toYear -> {
        "${
          toDevanagari(
            "$fromDay"
          )
        } ${monthNames[fromMonth - 1]} - ${
          toDevanagari(
            "$toDay"
          )
        } ${monthNames[toMonth - 1]}, ${toDevanagari("$fromYear")}"
      }

      else -> {
        "${
          toDevanagari(
            "$fromDay"
          )
        } ${monthNames[fromMonth - 1]}, ${
          toDevanagari(
            "$fromYear"
          )
        } - ${toDevanagari("$toDay")} ${monthNames[toMonth - 1]}, ${toDevanagari("$toYear")}"
      }
    }

  fun getTimePeriod(hour: Int): String =
    when (hour) {
      in 4 until 6 -> "भोर"
      in 6 until 12 -> "प्रातः"
      in 12 until 16 -> "अपराह्न"
      in 16 until 19 -> "सायं"
      in 19 until 22 -> "रात्रि"
      else -> "मध्यरात्रि"
    }

  fun formatTime(ldt: LocalDateTime): String {
    val hour = ldt.hour
    val minute = ldt.minute
    val period = getTimePeriod(hour)
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    val timeStr = "$hour12:${minute.toString().padStart(2, '0')}"
    return "$period ${toDevanagari(timeStr)}"
  }

  val timeStr = "${formatTime(fromDateTime)} - ${formatTime(toDateTime)}"

  return Pair(dateStr, timeStr)
}
