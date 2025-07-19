package com.aryamahasangh.features.admin.aryasamaj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.AryaSamaj
import com.aryamahasangh.domain.error.AppError
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
  val isLoadingMore: Boolean = false,
  val hasNextPageRecent: Boolean = true,
  val hasNextPageSearch: Boolean = true,
  val currentSearchQuery: String = "", // For display/debugging only, not for TextField state
  val error: String? = null,
  val showRetryButton: Boolean = false,
  val retryCount: Int = 0
)

class AryaSamajSelectorViewModel(
  private val repository: AryaSamajSelectorRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(AryaSamajSelectorUiState())
  val uiState: StateFlow<AryaSamajSelectorUiState> = _uiState.asStateFlow()

  // Internal search trigger - only for triggering network calls
  private val _searchTrigger = MutableStateFlow("")
  private var searchJob: Job? = null
  private var recentCursor: String? = null
  private var searchCursor: String? = null

  /**
   * Load recent AryaSamajs with pagination support
   * @param latitude Optional latitude for proximity sorting
   * @param longitude Optional longitude for proximity sorting
   * @param resetPagination Whether to reset pagination (true for initial load)
   */
  fun loadRecentAryaSamajs(
    latitude: Double? = null,
    longitude: Double? = null,
    resetPagination: Boolean = true
  ) {
    if (resetPagination) {
      recentCursor = null
    }

    viewModelScope.launch {
      repository.getRecentAryaSamajs(
        limit = 20,
        cursor = recentCursor,
        latitude = latitude,
        longitude = longitude
      ).collect { result ->
        _uiState.value = when (result) {
          is Result.Loading -> _uiState.value.copy(
            isLoadingRecent = resetPagination, // Only show loading for initial load
            isLoadingMore = !resetPagination,  // Show loading more for pagination
            error = null,
            showRetryButton = false
          )

          is Result.Success -> {
            val newAryaSamajs = if (resetPagination) {
              result.data.items
            } else {
              _uiState.value.recentAryaSamajs + result.data.items
            }

            recentCursor = result.data.endCursor

            _uiState.value.copy(
              recentAryaSamajs = newAryaSamajs,
              isLoadingRecent = false,
              isLoadingMore = false,
              hasNextPageRecent = result.data.hasNextPage,
              error = null,
              showRetryButton = false,
              retryCount = 0
            )
          }

          is Result.Error -> {
            val appError = result.exception as? AppError
            _uiState.value.copy(
              isLoadingRecent = false,
              isLoadingMore = false,
              error = appError?.getUserMessage() ?: result.exception?.message ?: "आर्य समाज लोड करने में त्रुटि",
              showRetryButton = true
            )
          }
        }
      }
    }
  }

  /**
   * Search AryaSamajs with pagination support
   * @param query Search term
   * @param resetPagination Whether to reset pagination (true for new search)
   */
  private fun searchAryaSamajs(query: String, resetPagination: Boolean = true) {
    if (resetPagination) {
      searchCursor = null
    }

    searchJob = viewModelScope.launch {
      _uiState.value = _uiState.value.copy(
        isSearching = resetPagination,
        isLoadingMore = !resetPagination,
        currentSearchQuery = query,
        error = null,
        showRetryButton = false
      )

      repository.searchAryaSamajs(
        query = query,
        limit = 20,
        cursor = searchCursor
      ).collect { result ->
        _uiState.value = when (result) {
          is Result.Loading -> _uiState.value.copy(
            isSearching = resetPagination,
            isLoadingMore = !resetPagination,
            currentSearchQuery = query,
            error = null,
            showRetryButton = false
          )

          is Result.Success -> {
            val newResults = if (resetPagination) {
              result.data.items
            } else {
              _uiState.value.searchResults + result.data.items
            }

            searchCursor = result.data.endCursor

            _uiState.value.copy(
              searchResults = newResults,
              isSearching = false,
              isLoadingMore = false,
              hasNextPageSearch = result.data.hasNextPage,
              currentSearchQuery = query,
              error = null,
              showRetryButton = false,
              retryCount = 0
            )
          }

          is Result.Error -> {
            val appError = result.exception as? AppError

            _uiState.value.copy(
              isSearching = false,
              isLoadingMore = false,
              currentSearchQuery = query,
              error = appError?.getUserMessage() ?: result.exception?.message ?: "खोज में त्रुटि",
              showRetryButton = true
            )
          }
        }
      }
    }
  }

  /**
   * Load next page of results (either recent or search results)
   */
  fun loadNextPage() {
    val currentState = _uiState.value

    // Don't load if already loading or no more pages
    if (currentState.isLoadingMore) return

    if (currentState.currentSearchQuery.isBlank()) {
      // Load more recent AryaSamajs
      if (currentState.hasNextPageRecent) {
        loadRecentAryaSamajs(resetPagination = false)
      }
    } else {
      // Load more search results
      if (currentState.hasNextPageSearch) {
        searchAryaSamajs(currentState.currentSearchQuery, resetPagination = false)
      }
    }
  }

  /**
   * Initialize search handling with optimized debouncing
   */
  fun initializeSearch() {
    viewModelScope.launch {
      _searchTrigger.collect { query ->
        // Cancel previous search
        searchJob?.cancel()

        if (query.length >= 2) {
          // Debounce: Wait 300ms for user to stop typing (optimized from 500ms)
          delay(300)
          searchAryaSamajs(query, resetPagination = true)
        } else {
          // Clear search results when query is too short
          searchCursor = null
          _uiState.value = _uiState.value.copy(
            searchResults = emptyList(),
            isSearching = false,
            hasNextPageSearch = true,
            currentSearchQuery = query,
            error = null,
            showRetryButton = false,
            retryCount = 0
          )
        }
      }
    }
  }

  /**
   * Trigger search (called from UI with debouncing)
   */
  fun triggerSearch(query: String) {
    _searchTrigger.value = query
  }

  /**
   * Auto-retry with exponential backoff (up to 3 times)
   */
  private suspend fun retryWithBackoff(operation: suspend () -> Unit, maxRetries: Int = 3) {
    val currentState = _uiState.value
    var attempt = currentState.retryCount

    while (attempt < maxRetries) {
      try {
        operation()
        break
      } catch (e: Exception) {
        attempt++
        _uiState.value = _uiState.value.copy(retryCount = attempt)

        if (attempt >= maxRetries) {
          // Show manual retry button after max retries
          _uiState.value = _uiState.value.copy(
            error = "कुछ प्रयासों के बाद भी त्रुटि है",
            showRetryButton = true
          )
        } else {
          // Wait with exponential backoff
          delay(1000L * attempt)
        }
      }
    }
  }

  /**
   * Clear search query and results
   */
  fun clearSearch() {
    searchJob?.cancel()
    searchCursor = null
    _searchTrigger.value = ""
  }

  /**
   * Retry loading with optional location parameters
   * @param latitude Optional latitude for proximity sorting
   * @param longitude Optional longitude for proximity sorting
   */
  fun retryLoading(
    latitude: Double? = null,
    longitude: Double? = null
  ) {
    val currentQuery = _uiState.value.currentSearchQuery

    if (currentQuery.isNotBlank()) {
      // Retry search
      viewModelScope.launch {
        retryWithBackoff(operation = {
          searchAryaSamajs(currentQuery, resetPagination = true)
        })
      }
    } else {
      // Retry loading recent AryaSamajs
      viewModelScope.launch {
        retryWithBackoff(operation = {
          loadRecentAryaSamajs(latitude, longitude, resetPagination = true)
        })
      }
    }
  }

  /**
   * Clear error state
   */
  fun clearError() {
    _uiState.value = _uiState.value.copy(
      error = null,
      showRetryButton = false,
      retryCount = 0
    )
  }

  /**
   * Get current results (recent or search) for external usage
   */
  fun getCurrentResults(): List<AryaSamaj> {
    val currentState = _uiState.value
    return if (currentState.currentSearchQuery.isNotBlank()) {
      currentState.searchResults
    } else {
      currentState.recentAryaSamajs
    }
  }
}
