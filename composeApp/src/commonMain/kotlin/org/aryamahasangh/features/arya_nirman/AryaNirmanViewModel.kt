package org.aryamahasangh.features.arya_nirman

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.BaseViewModel

data class UiState(
  val isLoading: Boolean = false,
  val error: String? = null,
  val data: List<UpcomingActivity> = listOf()
)

class AryaNirmanViewModel(
  private val aryaNirmanRepository: AryaNirmanRepository
) : BaseViewModel<UiState>(UiState()) {

  private val _registrationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
  val registrationCounts: StateFlow<Map<String, Int>> = _registrationCounts.asStateFlow()

  init {
    launch {
      aryaNirmanRepository.getRegistrationCounts().collect { result ->
        _registrationCounts.value = result
      }
    }
  }

  fun loadUpComingSessions(){
    launch {
      aryaNirmanRepository.getUpcomingActivities().collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null, data = emptyList()) }
          }
          is Result.Success -> {
            updateState { it.copy(
              isLoading = false,
              error = null,
              data = result.data
            )}
          }
          is Result.Error -> {
            updateState { it.copy(
              isLoading = false,
              error = result.message,
              data = emptyList()
            )}
          }
        }
      }
    }
  }
}