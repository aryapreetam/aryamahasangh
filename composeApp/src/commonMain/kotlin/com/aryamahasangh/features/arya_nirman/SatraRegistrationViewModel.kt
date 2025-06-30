package com.aryamahasangh.features.arya_nirman

import com.aryamahasangh.util.Result
import com.aryamahasangh.viewmodel.BaseViewModel

// Make `data` nullable to represent an event that can be consumed
data class RegistrationUiState(
  val isLoading: Boolean = false,
  val error: String? = null,
  val registrationResult: Boolean? = null, // Renamed for clarity: true for success, false for error, null for idle/consumed
  val isCapacityFull: Boolean = false // New field to indicate if capacity is full
)

class SatraRegistrationViewModel(
  private val aryaNirmanRepository: AryaNirmanRepository
) : BaseViewModel<RegistrationUiState>(RegistrationUiState()) {
  fun createRegistration(
    activityId: String,
    data: RegistrationData,
    activityCapacity: Int
  ) {
    launch {
      updateState {
        it.copy(
          isLoading = true,
          error = null,
          registrationResult = null,
          isCapacityFull = false
        )
      } // Reset before new attempt

      // Check current registration count before attempting to register
      val currentCount = aryaNirmanRepository.getRegistrationCountByActivityId(activityId)

      if (currentCount != null && currentCount >= activityCapacity) {
        // Capacity is full, don't proceed with registration
        updateState {
          it.copy(
            isLoading = false,
            error = "पंजीकरण क्षमता पूर्ण हो चुकी है। अब पंजीकरण स्वीकार नहीं किया जा रहा है।",
            registrationResult = false,
            isCapacityFull = true
          )
        }
        return@launch
      }

      // Proceed with registration if capacity is not full
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
    updateState { it.copy(registrationResult = null, error = null, isCapacityFull = false) } // Reset the event state
  }
}
