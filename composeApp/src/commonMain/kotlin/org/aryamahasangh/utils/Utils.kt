package org.aryamahasangh.utils

import kotlinx.datetime.LocalDateTime

fun LocalDateTime.toHumanReadable(): String {
  val daysOfWeek = listOf("रविवार", "सोमवार", "मंगलवार", "बुधवार", "गुरुवार", "शुक्रवार", "शनिवार")
  val months = listOf(
    "जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून",
    "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
  )

  val dayOfWeek = daysOfWeek[this.dayOfWeek.ordinal]
  val month = months[this.monthNumber - 1]
  val day = this.dayOfMonth
  val year = this.year
  val hour = this.hour % 12
  val minute = this.minute.toString().padStart(2, '0')
  val amPm = if (this.hour < 12) "प्रातः" else if(this.hour in 12..15) "दोपहर" else "सायं"

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
fun formatShort(dateTime: Any): String {
  return LocalDateTime.parse(dateTime as String).toShortHumanReadable()
}
