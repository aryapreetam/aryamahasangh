package org.aryamahasangh.features.arya_nirman

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

  // Track the real-time subscription job
  private var registrationCountsJob: Job? = null

  fun loadUpComingSessions() {
    launch {
      aryaNirmanRepository.getUpcomingActivities().collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null, data = emptyList()) }
          }
          is Result.Success -> {
            updateState {
              it.copy(
                isLoading = false,
                error = null,
                data = result.data
              )
            }
            // Start listening to registration counts after activities are loaded
            startListeningToRegistrationCounts()
          }
          is Result.Error -> {
            updateState {
              it.copy(
                isLoading = false,
                error = result.message,
                data = emptyList()
              )
            }
          }
        }
      }
    }
  }

  /**
   * Start listening to real-time registration count updates
   */
  private fun startListeningToRegistrationCounts() {
    // Cancel any existing subscription
    stopListeningToRegistrationCounts()

    registrationCountsJob =
      viewModelScope.launch {
        try {
          aryaNirmanRepository.getRegistrationCounts().collect { result ->
            _registrationCounts.value = result
          }
        } catch (e: Exception) {
          println("Error in registration counts listener: ${e.message}")
        }
      }
  }

  /**
   * Stop listening to real-time registration count updates
   */
  fun stopListeningToRegistrationCounts() {
    registrationCountsJob?.cancel()
    registrationCountsJob = null
  }

  override fun onCleared() {
    stopListeningToRegistrationCounts()
    super.onCleared()
  }
}
