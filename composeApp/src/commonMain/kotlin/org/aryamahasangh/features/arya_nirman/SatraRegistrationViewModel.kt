package org.aryamahasangh.features.arya_nirman

import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.BaseViewModel

data class RegistrationUiState(
  val isLoading: Boolean = false,
  val error: String? = null,
  val data: Boolean? = false
)

class SatraRegistrationViewModel(
  private val aryaNirmanRepository: AryaNirmanRepository
) : BaseViewModel<RegistrationUiState>(RegistrationUiState()) {
  fun createRegistration(activityId: String, data: RegistrationData) {
    launch {
      aryaNirmanRepository.registerForActivity(activityId = activityId, data = data).collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null, data = false) }
          }
          is Result.Success -> {
            updateState {
              it.copy(
                isLoading = false,
                error = null,
                data = result.data
              )
            }
          }

          is Result.Error -> {
            updateState {
              it.copy(
                isLoading = false,
                error = result.message,
                data = null,
              )
            }
          }
        }
      }
    }
  }
}
