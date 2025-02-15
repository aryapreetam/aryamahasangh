package org.aryamahasangh.utils

import kotlinx.datetime.LocalDateTime

fun LocalDateTime.toHumanReadable(): String {
  val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
  val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

  val dayOfWeek = daysOfWeek[this.dayOfWeek.ordinal]
  val month = months[this.monthNumber - 1]
  val day = this.dayOfMonth
  val year = this.year
  val hour = this.hour % 12
  val minute = this.minute.toString().padStart(2, '0')
  val amPm = if (this.hour < 12) "AM" else "PM"

  return "$dayOfWeek, $month $day, $year at ${if (hour == 0) 12 else hour}:$minute $amPm"
}

fun LocalDateTime.toShortHumanReadable(): String {
  val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

  val month = months[this.monthNumber - 1]
  val day = this.dayOfMonth
  val year = this.year
  val hour = this.hour % 12
  val minute = this.minute.toString().padStart(2, '0')
  val amPm = if (this.hour < 12) "AM" else "PM"

  return "$month $day, $year ${if (hour == 0) 12 else hour}:$minute $amPm"
}

fun format(dateTime: Any): String {
  return LocalDateTime.parse(dateTime as String).toHumanReadable()
}
