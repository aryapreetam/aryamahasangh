package org.aryamahasangh.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.aryamahasangh.domain.error.ErrorHandler
import org.aryamahasangh.repository.AdmissionsRepository
import org.aryamahasangh.util.Result

/**
 * Data class for student application
 */
data class StudentApplication(
  val id: String,
  val studentName: String,
  val email: String,
  val phoneNumber: String,
  val applicationDate: String,
  val status: String,
  val course: String
)

/**
 * Use case for getting student applications with filtering and sorting
 * Note: Currently disabled as the repository implementation is not complete
 */
class GetStudentApplicationsUseCase(
  private val admissionsRepository: AdmissionsRepository
) {
  operator fun invoke(): Flow<Result<List<StudentApplication>>> {
    // TODO: Implement when repository methods are available
    return kotlinx.coroutines.flow.flow {
      emit(Result.Error("Student applications feature is not yet implemented"))
    }.catch { exception ->
      val appError = ErrorHandler.handleException(exception)
      emit(Result.Error(appError.message, exception))
    }
  }
}

/**
 * Data class for admission form input
 */
data class AdmissionFormData(
  val name: String,
  val email: String,
  val phone: String,
  val address: String,
  val dateOfBirth: String,
  val qualification: String,
  val experience: String
)

/**
 * Use case for submitting admission form with comprehensive validation
 * Note: Currently disabled as the repository implementation is not complete
 */
class SubmitAdmissionFormUseCase(
  private val admissionsRepository: AdmissionsRepository
) {
  suspend operator fun invoke(formData: AdmissionFormData): Result<Boolean> {
    return try {
      // Validate form data
      val validationResult = validateAdmissionForm(formData)
      if (validationResult != null) {
        return Result.Error(validationResult)
      }

      // TODO: Implement when repository method is available
      Result.Error("Admission form submission is not yet implemented")
    } catch (exception: Exception) {
      val appError = ErrorHandler.handleException(exception)
      Result.Error(appError.message, exception)
    }
  }

  private fun validateAdmissionForm(formData: AdmissionFormData): String? {
    return when {
      formData.name.isBlank() -> "Name is required"
      formData.name.length < 2 -> "Name must be at least 2 characters"
      formData.name.length > 50 -> "Name must not exceed 50 characters"
      !isValidName(formData.name) -> "Name contains invalid characters"

      formData.email.isBlank() -> "Email is required"
      !isValidEmail(formData.email) -> "Please enter a valid email address"

      formData.phone.isBlank() -> "Phone number is required"
      !isValidPhone(formData.phone) -> "Please enter a valid phone number"

      formData.address.isBlank() -> "Address is required"
      formData.address.length < 10 -> "Address must be at least 10 characters"
      formData.address.length > 200 -> "Address must not exceed 200 characters"

      formData.dateOfBirth.isBlank() -> "Date of birth is required"
      !isValidDateOfBirth(formData.dateOfBirth) -> "Please enter a valid date of birth"

      formData.qualification.isBlank() -> "Qualification is required"
      formData.qualification.length < 2 -> "Qualification must be at least 2 characters"

      formData.experience.isBlank() -> "Experience is required"
      formData.experience.length < 10 -> "Experience must be at least 10 characters"
      formData.experience.length > 500 -> "Experience must not exceed 500 characters"

      else -> null
    }
  }

  private fun isValidName(name: String): Boolean {
    return name.matches(Regex("^[a-zA-Z\\s.'-]+$"))
  }

  private fun isValidEmail(email: String): Boolean {
    return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
  }

  private fun isValidPhone(phone: String): Boolean {
    // Allow various phone formats
    val cleanPhone = phone.replace(Regex("[\\s()-]"), "")
    return cleanPhone.matches(Regex("^[+]?[0-9]{10,15}$"))
  }

  private fun isValidDateOfBirth(dateOfBirth: String): Boolean {
    // Basic date format validation (YYYY-MM-DD)
    return dateOfBirth.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
  }
}

/**
 * Combined use case for admission management
 */
class AdmissionManagementUseCase(
  private val getStudentApplicationsUseCase: GetStudentApplicationsUseCase,
  private val submitAdmissionFormUseCase: SubmitAdmissionFormUseCase
) {
  fun getStudentApplications() = getStudentApplicationsUseCase()

  suspend fun submitAdmissionForm(formData: AdmissionFormData) = submitAdmissionFormUseCase(formData)
}
