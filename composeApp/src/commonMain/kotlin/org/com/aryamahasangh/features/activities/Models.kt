package com.aryamahasangh.features.activities

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import com.aryamahasangh.type.GenderFilter as ApolloGenderFilter // Alias for clarity

@Immutable // For better Compose performance if data doesn't change after creation
data class UserProfile(
  val id: String, // Not visible, but good to have in the model
  val gender: String,
  val mobile: String,
  val address: String,
  val fullname: String
)

// Map of English digits to Devanagari digits
private val devanagariDigitMap: Map<Char, Char> =
  mapOf(
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

@Serializable
enum class GenderAllowed {
  ANY,
  MALE,
  FEMALE;

  /**
   * Returns the display name for the gender allowed option. i.e. सभी के लिए / केवल पुरुष / केवल महिला
   */
  fun toDisplayName(): String {
    return when (this) {
      ANY -> "सभी के लिए"
      MALE -> "केवल पुरुष"
      FEMALE -> "केवल महिला"
    }
  }

  /**
   * Returns the short display name for the gender allowed option. i.e. अन्य / पुरुष / महिला
   */
  fun toDisplayNameShort(): String {
    return when (this) {
      ANY -> "अन्य"
      MALE -> "पुरुष"
      FEMALE -> "महिला"
    }
  }

  companion object {
    fun fromDisplayName(displayName: String): GenderAllowed? {
      return entries.find { it.toDisplayName() == displayName }
    }
  }
}

fun ApolloGenderFilter?.toDomain(): GenderAllowed {
  return when (this) {
    ApolloGenderFilter.MALE -> GenderAllowed.MALE
    ApolloGenderFilter.FEMALE -> GenderAllowed.FEMALE
    ApolloGenderFilter.ANY -> GenderAllowed.ANY
    // Important: Handle UNKNOWN__ if your schema might add new enum values
    ApolloGenderFilter.UNKNOWN__ -> GenderAllowed.ANY // Or throw, or a specific unknown domain state
    null -> GenderAllowed.ANY // Or your preferred default for null
  }
}

fun GenderAllowed.toApollo(): ApolloGenderFilter {
  return when (this) {
    GenderAllowed.MALE -> ApolloGenderFilter.MALE
    GenderAllowed.FEMALE -> ApolloGenderFilter.FEMALE
    GenderAllowed.ANY -> ApolloGenderFilter.ANY
  }
}
