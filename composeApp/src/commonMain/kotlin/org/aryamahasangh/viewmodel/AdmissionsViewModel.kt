package org.aryamahasangh.viewmodel

// import org.aryamahasangh.StudentApplicationsQuery
// import org.aryamahasangh.type.AdmissionFormDataInput
import org.aryamahasangh.repository.AdmissionsRepository

/**
 * UI state for the Admissions screens
 */
data class AdmissionsUiState(
  // val studentApplications: List<StudentApplicationsQuery.StudentsApplied> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * UI state for form submission
 */
data class AdmissionFormSubmissionState(
  val isSubmitting: Boolean = false,
  val isSuccess: Boolean = false,
  val error: String? = null
)

/**
 * ViewModel for the Admissions and Received Applications screens
 */
class AdmissionsViewModel(
  private val admissionsRepository: AdmissionsRepository
) : BaseViewModel<AdmissionsUiState>(AdmissionsUiState()) {
  // Separate state for form submission
//  private val _admissionFormSubmissionState = MutableStateFlow(AdmissionFormSubmissionState())
//  val admissionFormSubmissionState: StateFlow<AdmissionFormSubmissionState> = _admissionFormSubmissionState.asStateFlow()
//
//  init {
//    loadStudentApplications()
//  }
//
//  /**
//   * Load all student applications
//   */
//  fun loadStudentApplications() {
//    launch {
//      admissionsRepository.getStudentApplications().collect { result ->
//        when (result) {
//          is Result.Loading -> {
//            updateState { it.copy(isLoading = true, error = null) }
//          }
//          is Result.Success -> {
//            updateState { it.copy(
//              studentApplications = result.data,
//              isLoading = false,
//              error = null
//            )}
//          }
//          is Result.Error -> {
//            updateState { it.copy(
//              isLoading = false,
//              error = result.message
//            )}
//          }
//        }
//      }
//    }
//  }
//
//  /**
//   * Submit admission form
//   */
//  fun submitAdmissionForm(input: AdmissionFormDataInput) {
//    launch {
//      _admissionFormSubmissionState.value = AdmissionFormSubmissionState(isSubmitting = true)
//
//      when (val result = admissionsRepository.submitAdmissionForm(input)) {
//        is Result.Success -> {
//          _admissionFormSubmissionState.value = AdmissionFormSubmissionState(
//            isSubmitting = false,
//            isSuccess = result.data,
//            error = null
//          )
//          loadStudentApplications()
//        }
//        is Result.Error -> {
//          _admissionFormSubmissionState.value = AdmissionFormSubmissionState(
//            isSubmitting = false,
//            isSuccess = false,
//            error = result.message
//          )
//        }
//        is Result.Loading -> {
//          // This shouldn't happen with the current implementation
//        }
//      }
//    }
//  }
//
//  /**
//   * Reset form submission state
//   */
//  fun resetFormSubmissionState() {
//    _admissionFormSubmissionState.value = AdmissionFormSubmissionState()
//  }
}
