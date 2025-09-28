// ... existing code ...
package com.aryamahasangh.features.gurukul.domain.models

data class CourseRegistrationFormData(
  val activityId: String,
  val name: String,
  val satrDate: String,
  val satrPlace: String,
  val recommendation: String,
  val imageBytes: ByteArray?, // null until image picked
  val imageFilename: String?
)
// ... existing code ...
