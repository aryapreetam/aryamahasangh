package com.aryamahasangh.features.gurukul.domain.usecase

import com.aryamahasangh.RegisterForCourseMutation
import com.aryamahasangh.features.gurukul.data.CourseRegistrationRepository
import com.aryamahasangh.features.gurukul.data.ImageUploadRepository
import com.aryamahasangh.features.gurukul.domain.models.CourseRegistrationFormData
import com.aryamahasangh.type.CourseRegistrationsInsertInput
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class RegisterForCourseUseCase(
  private val courseRegistrationRepository: CourseRegistrationRepository,
  private val imageUploadRepository: ImageUploadRepository
) {
  suspend fun execute(formData: CourseRegistrationFormData): Result<Unit> {
    // Step 1: Upload image to Supabase
    val imageResult = imageUploadRepository.uploadReceipt(
      formData.imageBytes ?: return Result.failure(Exception("No image provided")),
      formData.imageFilename ?: "receipt.jpg"
    )
    if (imageResult.isFailure) {
      return Result.failure(Exception("रसीद अपलोड विफल: ${imageResult.exceptionOrNull()?.message ?: "अज्ञात"}"))
    }
    val receiptUrl = imageResult.getOrNull() ?: return Result.failure(Exception("रसीद अपलोड विफल"))

    // Step 2: Build insert input
    val satrDateInstant = try {
      // Convert LocalDate string to Instant for database storage
      val localDate = LocalDate.parse(formData.satrDate)
      // Get start of day as Instant in UTC, then add 12 hours to get noon time
      localDate.atStartOfDayIn(TimeZone.UTC).plus(12, DateTimeUnit.HOUR, TimeZone.UTC)
    } catch (e: Exception) {
      null
    }
    val input = CourseRegistrationsInsertInput.Builder()
      .activityId(formData.activityId)
      .name(formData.name)
      .satrDate(satrDateInstant)
      .satrPlace(formData.satrPlace)
      .recommendation(formData.recommendation)
      .paymentReceiptUrl(receiptUrl)
      .build()
    // Step 3: Build mutation
    val mutation = RegisterForCourseMutation(input)
    val mutationResult = courseRegistrationRepository.registerForCourse(mutation)
    if (mutationResult.isFailure) {
      return Result.failure(Exception("पंजीकरण विफल: ${mutationResult.exceptionOrNull()?.message ?: "अज्ञात"}"))
    }
    return Result.success(Unit)
  }
}
