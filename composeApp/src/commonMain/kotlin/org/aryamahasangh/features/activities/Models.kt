package org.aryamahasangh.features.activities

import androidx.compose.runtime.Immutable


@Immutable // For better Compose performance if data doesn't change after creation
data class UserProfile(
  val id: String, // Not visible, but good to have in the model
  val gender: String,
  val mobile: String,
  val address: String,
  val fullname: String
)

// Map of English digits to Devanagari digits
private val devanagariDigitMap: Map<Char, Char> = mapOf(
  '0' to '०',
  '1' to '१',
  '2' to '२',
  '3' to '३',
  '4' to '४',
  '5' to '५',
  '6' to '६',
  '7' to '७',
  '8' to '८',
  '9' to '९'
)

/**
 * Converts a string containing English numerals to a string with Devanagari numerals.
 * Non-numeric characters are preserved.
 */
fun String.toDevanagariNumerals(): String {
  val stringBuilder = StringBuilder(this.length)
  for (char in this) {
    stringBuilder.append(devanagariDigitMap[char] ?: char)
  }
  return stringBuilder.toString()
}
