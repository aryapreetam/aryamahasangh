package com.aryamahasangh.features.gurukul.domain.usecase

import com.aryamahasangh.RegisterForCourseMutation
import com.aryamahasangh.features.gurukul.data.GurukulRepository
import com.aryamahasangh.features.gurukul.data.ImageUploadRepository
import com.aryamahasangh.features.gurukul.domain.exception.InvalidInputException
import com.aryamahasangh.features.gurukul.domain.exception.RegistrationSubmissionException
import com.aryamahasangh.features.gurukul.domain.exception.UploadFailedException
import com.aryamahasangh.features.gurukul.domain.exception.UploadType
import com.aryamahasangh.features.gurukul.domain.models.CourseRegistrationFormData
import com.aryamahasangh.type.CourseRegistrationsInsertInput

class RegisterForCourseUseCase(
  private val courseRepository: GurukulRepository,
  private val imageRepository: ImageUploadRepository
) {
  suspend operator fun invoke(form: CourseRegistrationFormData) {
    // --- Upload receipt ---
    val receiptUrl = try {
      imageRepository.uploadReceipt(
        form.imageBytes ?: throw InvalidInputException("MISSING_RECEIPT"),
        form.imageFilename ?: "receipt.jpg"
      ).getOrThrow()
    } catch (e: Exception) {
      throw UploadFailedException(
        type = UploadType.Receipt,
        message = "UPLOAD_FAILED",
        cause = e
      )
    }

    // --- Upload photo ---
    val photoUrl = try {
      imageRepository.uploadReceipt(
        form.photoBytes ?: throw InvalidInputException("MISSING_PHOTO"),
        form.photoFilename ?: "photo.jpg"
      ).getOrThrow()
    } catch (e: Exception) {
      throw UploadFailedException(
        type = UploadType.Photo,
        message = "UPLOAD_FAILED",
        cause = e
      )
    }

    // --- Build mutation input ---
    val input = CourseRegistrationsInsertInput.Builder()
      .activityId(form.activityId)
      .name(form.name)
      .satrDate(form.satrDate)
      .satrPlace(form.satrPlace)
      .recommendation(form.recommendation)
      .paymentReceiptUrl(receiptUrl)
      .photoUrl(photoUrl)
      .guardianName(form.guardianName)
      .dob(form.dob)
      .address(form.address)
      .qualification(form.qualification)
      .phoneNumber(form.phoneNumber)
      .build()

    // --- Submit mutation ---
    try {
      courseRepository.registerForCourse(RegisterForCourseMutation(input))
        .getOrThrow()
    } catch (e: Exception) {
      throw RegistrationSubmissionException("MUTATION_FAILED", e)
    }
  }
}
