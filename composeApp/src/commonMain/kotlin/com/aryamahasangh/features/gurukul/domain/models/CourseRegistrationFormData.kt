// ... existing code ...
package com.aryamahasangh.features.gurukul.domain.models

import kotlinx.datetime.LocalDate

data class CourseRegistrationFormData(
  val activityId: String,
  val name: String,
  val satrDate: LocalDate,
  val satrPlace: String,
  val recommendation: String,
  val imageBytes: ByteArray?, // Receipt image
  val imageFilename: String?,
  val photoBytes: ByteArray?, // User photo
  val photoFilename: String?,
  val dob: LocalDate,
  val guardianName: String,
  val address: String,
  val qualification: String,
  val phoneNumber: String
)
// ... existing code ...
