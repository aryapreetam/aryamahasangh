// ... existing code ...
package com.aryamahasangh.features.gurukul.domain.models

data class CourseRegistrationFormData(
  val activityId: String,
  val name: String,
  val satrDate: String,
  val satrPlace: String,
  val recommendation: String,
  val imageBytes: ByteArray?, // Receipt image
  val imageFilename: String?,
  val photoBytes: ByteArray?, // User photo
  val photoFilename: String?
)
// ... existing code ...
