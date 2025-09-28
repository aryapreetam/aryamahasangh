package com.aryamahasangh.features.gurukul.domain.usecase

import com.aryamahasangh.RegisterForCourseMutation
import com.aryamahasangh.features.gurukul.data.CourseRegistrationRepository
import com.aryamahasangh.features.gurukul.data.ImageUploadRepository
import com.aryamahasangh.features.gurukul.domain.models.CourseRegistrationFormData
import com.aryamahasangh.type.CourseRegistrationsInsertInput
import kotlinx.datetime.Instant

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
      Instant.parse(formData.satrDate)
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
