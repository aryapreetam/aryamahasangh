package com.aryamahasangh.features.admin.aryasamaj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.AryaSamaj
import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.ErrorHandler
import com.aryamahasangh.domain.error.getUserMessage
import com.aryamahasangh.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AryaSamajSelectorUiState(
  val recentAryaSamajs: List<AryaSamaj> = emptyList(),
  val searchResults: List<AryaSamaj> = emptyList(),
  val isLoadingRecent: Boolean = false,
  val isSearching: Boolean = false,
  val searchQuery: String = "",
  val error: String? = null,
  val showRetryButton: Boolean = false
)

class AryaSamajSelectorViewModel(
  private val repository: AryaSamajSelectorRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(AryaSamajSelectorUiState())
  val uiState: StateFlow<AryaSamajSelectorUiState> = _uiState.asStateFlow()

  private var searchJob: Job? = null

  /**
   * Load recent AryaSamajs with optional location-based sorting
   */
  fun loadRecentAryaSamajs(
    latitude: Double? = null,
    longitude: Double? = null,
    limit: Int = 10
  ) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isLoadingRecent = true,
        error = null,
        showRetryButton = false
      )

      repository.getRecentAryaSamajs(
        limit = limit,
        latitude = latitude,
        longitude = longitude
      ).collect { result ->
        when (result) {
          is Result.Loading -> {
            _uiState.value = _uiState.value.copy(isLoadingRecent = true)
          }

          is Result.Success -> {
            _uiState.value = _uiState.value.copy(
              recentAryaSamajs = result.data,
              isLoadingRecent = false,
              error = null,
              showRetryButton = false
            )
          }

          is Result.Error -> {
            val appError = result.exception as? AppError
            appError?.let { ErrorHandler.logError(it, "AryaSamajSelectorViewModel.loadRecentAryaSamajs") }

            _uiState.value = _uiState.value.copy(
              isLoadingRecent = false,
              error = appError?.getUserMessage() ?: result.exception?.message ?: "अज्ञात त्रुटि",
              showRetryButton = true
            )
          }
        }
      }
    }
  }

  /**
   * Search AryaSamajs with debounced input
   * Optimized for Devanagari/Hindi typing - triggers after 2 characters with 500ms delay
   */
  fun searchAryaSamajs(query: String) {
    // Update search query immediately for UI feedback
    _uiState.value = _uiState.value.copy(searchQuery = query)

    // Cancel previous search job
    searchJob?.cancel()

    if (query.length < 2) {
      // Clear search results if query is too short
      _uiState.value = _uiState.value.copy(
        searchResults = emptyList(),
        isSearching = false,
        error = null
      )
      return
    }

    searchJob = viewModelScope.launch {
      // Debounce: Wait 500ms for user to stop typing
      delay(500)

      _uiState.value = _uiState.value.copy(
        isSearching = true,
        error = null,
        showRetryButton = false
      )

      repository.searchAryaSamajs(
        query = query.trim(),
        limit = 20
      ).collect { result ->
        when (result) {
          is Result.Loading -> {
            _uiState.value = _uiState.value.copy(isSearching = true)
          }

          is Result.Success -> {
            _uiState.value = _uiState.value.copy(
              searchResults = result.data,
              isSearching = false,
              error = null,
              showRetryButton = false
            )
          }

          is Result.Error -> {
            val appError = result.exception as? AppError
            appError?.let { ErrorHandler.logError(it, "AryaSamajSelectorViewModel.searchAryaSamajs") }

            _uiState.value = _uiState.value.copy(
              isSearching = false,
              error = appError?.getUserMessage() ?: result.exception?.message ?: "खोज में त्रुटि",
              showRetryButton = true
            )
          }
        }
      }
    }
  }

  /**
   * Clear search query and results
   */
  fun clearSearch() {
    searchJob?.cancel()
    _uiState.value = _uiState.value.copy(
      searchQuery = "",
      searchResults = emptyList(),
      isSearching = false,
      error = null,
      showRetryButton = false
    )
  }

  /**
   * Retry failed operation
   */
  fun retry(
    latitude: Double? = null,
    longitude: Double? = null
  ) {
    val currentQuery = _uiState.value.searchQuery

    if (currentQuery.isNotBlank()) {
      // Retry search
      searchAryaSamajs(currentQuery)
    } else {
      // Retry loading recent AryaSamajs
      loadRecentAryaSamajs(latitude, longitude)
    }
  }

  /**
   * Clear error state
   */
  fun clearError() {
    _uiState.value = _uiState.value.copy(
      error = null,
      showRetryButton = false
    )
  }

  /**
   * Get current display results based on search state
   */
  fun getCurrentResults(): List<AryaSamaj> {
    val currentState = _uiState.value
    return if (currentState.searchQuery.isNotBlank()) {
      currentState.searchResults
    } else {
      currentState.recentAryaSamajs
    }
  }
}
