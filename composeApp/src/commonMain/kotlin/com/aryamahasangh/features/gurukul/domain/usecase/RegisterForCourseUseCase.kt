package com.aryamahasangh.features.gurukul.domain.usecase

import com.aryamahasangh.RegisterForCourseMutation
import com.aryamahasangh.features.gurukul.data.GurukulRepository
import com.aryamahasangh.features.gurukul.data.ImageUploadRepository
import com.aryamahasangh.features.gurukul.domain.models.CourseRegistrationFormData
import com.aryamahasangh.type.CourseRegistrationsInsertInput
import kotlinx.datetime.*

class RegisterForCourseUseCase(
  private val courseRegistrationRepository: GurukulRepository,
  private val imageUploadRepository: ImageUploadRepository
) {
  suspend fun execute(formData: CourseRegistrationFormData): Result<Unit> {
    // Step 1: Upload receipt image to Supabase
    val imageResult = imageUploadRepository.uploadReceipt(
      formData.imageBytes ?: return Result.failure(Exception("No image provided")),
      formData.imageFilename ?: "receipt.jpg"
    )
    if (imageResult.isFailure) {
      return Result.failure(Exception("रसीद अपलोड विफल: ${imageResult.exceptionOrNull()?.message ?: "अज्ञात"}"))
    }
    val receiptUrl = imageResult.getOrNull() ?: return Result.failure(Exception("रसीद अपलोड विफल"))

    // Step 2: Upload user photo to Supabase
    val photoResult = imageUploadRepository.uploadReceipt(
      formData.photoBytes ?: return Result.failure(Exception("No photo provided")),
      formData.photoFilename ?: "photo.jpg"
    )
    if (photoResult.isFailure) {
      return Result.failure(Exception("फोटो अपलोड विफल: ${photoResult.exceptionOrNull()?.message ?: "अज्ञात"}"))
    }
    val photoUrl = photoResult.getOrNull() ?: return Result.failure(Exception("फोटो अपलोड विफल"))

    // Step 3: Build insert input
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
      .photoUrl(photoUrl)
      .build()
    // Step 4: Build mutation
    val mutation = RegisterForCourseMutation(input)
    val mutationResult = courseRegistrationRepository.registerForCourse(mutation)
    if (mutationResult.isFailure) {
      return Result.failure(Exception("पंजीकरण विफल: ${mutationResult.exceptionOrNull()?.message ?: "अज्ञात"}"))
    }
    return Result.success(Unit)
  }
}
