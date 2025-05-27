package org.aryamahasangh.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.aryamahasangh.StudentApplicationsQuery
import org.aryamahasangh.repository.AdmissionsRepository
import org.aryamahasangh.type.AdmissionFormDataInput
import org.aryamahasangh.util.Result

/**
 * Use case for getting student applications with filtering and sorting
 */
class GetStudentApplicationsUseCase(
    private val admissionsRepository: AdmissionsRepository
) {
    operator fun invoke(): Flow<Result<List<StudentApplicationsQuery.StudentsApplied>>> {
        return admissionsRepository.getStudentApplications()
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        // Apply business logic: sort by application date (newest first)
                        val sortedApplications = result.data.sortedByDescending { 
                            it.createdAt ?: ""
                        }
                        Result.Success(sortedApplications)
                    }
                    else -> result
                }
            }
            .catch { exception ->
                emit(Result.Error("Failed to load student applications: ${exception.message}", exception))
            }
    }
}

/**
 * Use case for submitting admission form with comprehensive validation
 */
class SubmitAdmissionFormUseCase(
    private val admissionsRepository: AdmissionsRepository
) {
    suspend operator fun invoke(formData: AdmissionFormDataInput): Result<Boolean> {
        return try {
            // Validate form data
            val validationResult = validateAdmissionForm(formData)
            if (validationResult != null) {
                return Result.Error(validationResult)
            }
            
            admissionsRepository.submitAdmissionForm(formData)
        } catch (exception: Exception) {
            Result.Error("Failed to submit admission form: ${exception.message}", exception)
        }
    }
    
    private fun validateAdmissionForm(formData: AdmissionFormDataInput): String? {
        return when {
            formData.name.isNullOrBlank() -> "Name is required"
            formData.name!!.length < 2 -> "Name must be at least 2 characters"
            formData.name!!.length > 50 -> "Name must not exceed 50 characters"
            !isValidName(formData.name!!) -> "Name contains invalid characters"
            
            formData.email.isNullOrBlank() -> "Email is required"
            !isValidEmail(formData.email!!) -> "Please enter a valid email address"
            
            formData.phone.isNullOrBlank() -> "Phone number is required"
            !isValidPhone(formData.phone!!) -> "Please enter a valid phone number"
            
            formData.address.isNullOrBlank() -> "Address is required"
            formData.address!!.length < 10 -> "Address must be at least 10 characters"
            formData.address!!.length > 200 -> "Address must not exceed 200 characters"
            
            formData.dateOfBirth.isNullOrBlank() -> "Date of birth is required"
            !isValidDateOfBirth(formData.dateOfBirth!!) -> "Please enter a valid date of birth"
            
            formData.qualification.isNullOrBlank() -> "Qualification is required"
            formData.qualification!!.length < 2 -> "Qualification must be at least 2 characters"
            
            formData.experience.isNullOrBlank() -> "Experience is required"
            formData.experience!!.length < 10 -> "Experience must be at least 10 characters"
            formData.experience!!.length > 500 -> "Experience must not exceed 500 characters"
            
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
    
    suspend fun submitAdmissionForm(formData: AdmissionFormDataInput) = 
        submitAdmissionFormUseCase(formData)
}