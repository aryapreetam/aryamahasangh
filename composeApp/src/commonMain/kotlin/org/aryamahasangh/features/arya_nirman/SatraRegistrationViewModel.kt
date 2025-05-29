package org.aryamahasangh.features.arya_nirman

import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.BaseViewModel


// Make `data` nullable to represent an event that can be consumed
data class RegistrationUiState(
  val isLoading: Boolean = false,
  val error: String? = null,
  val registrationResult: Boolean? = null // Renamed for clarity: true for success, false for error, null for idle/consumed
)

class SatraRegistrationViewModel(
  private val aryaNirmanRepository: AryaNirmanRepository
) : BaseViewModel<RegistrationUiState>(RegistrationUiState()) {

  fun createRegistration(
    activityId: String,
    data: RegistrationData
  ) {
    launch {
      updateState { it.copy(isLoading = true, error = null, registrationResult = null) } // Reset before new attempt
      aryaNirmanRepository.registerForActivity(activityId = activityId, data = data).collect { result ->
        when (result) {
          is Result.Loading -> {
            // isLoading is already true
          }

          is Result.Success -> {
            updateState {
              it.copy(
                isLoading = false,
                error = null,
                registrationResult = result.data // Should be true if repository returns boolean for success
              )
            }
          }

          is Result.Error -> {
            updateState {
              it.copy(
                isLoading = false,
                error = result.message,
                registrationResult = false // Indicate failure
              )
            }
          }
        }
      }
    }
  }

  // New function to be called by the UI after handling the registration event
  fun registrationEventHandled() {
    updateState { it.copy(registrationResult = null, error = null) } // Reset the event state
  }
}
