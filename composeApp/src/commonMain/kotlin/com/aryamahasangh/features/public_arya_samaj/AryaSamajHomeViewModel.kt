package com.aryamahasangh.features.public_arya_samaj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.features.admin.PaginationResult
import com.aryamahasangh.util.GlobalMessageManager
import com.aryamahasangh.util.GlobalMessageDuration
import com.aryamahasangh.util.Result
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * ViewModel for the AryaSamaj home screen
 * Handles pagination, search, and filtering functionality using PaginatedRepository pattern
 */
class AryaSamajHomeViewModel(
  private val repository: AryaSamajHomeRepository
) : ViewModel() {

  companion object {
    private const val PAGE_SIZE = 20
    private const val SEARCH_DEBOUNCE_MS = 500L
  }

  private val _uiState = MutableStateFlow(AryaSamajHomeUiState())
  val uiState: StateFlow<AryaSamajHomeUiState> = _uiState.asStateFlow()

  // Use PaginationState for proper caching and scroll persistence
  private val _paginationState = MutableStateFlow(PaginationState<AryaSamajHomeListItem>())
  val paginationState: StateFlow<PaginationState<AryaSamajHomeListItem>> = _paginationState.asStateFlow()

  private var loadJob: Job? = null
  private var searchJob: Job? = null

  @OptIn(FlowPreview::class)
  private val searchQuery = MutableStateFlow("")

  @OptIn(FlowPreview::class)
  private val debouncedSearchQuery = searchQuery
    .debounce(SEARCH_DEBOUNCE_MS)
    .distinctUntilChanged()

  init {
    println("ViewModel Debug: AryaSamajHomeViewModel initialized")

    // Listen to debounced search query changes
    viewModelScope.launch {
      debouncedSearchQuery.collect { query ->
        println("ViewModel Debug: Debounced search query changed: '$query'")
        if (_uiState.value.filterState.searchQuery != query) {
          updateFilterState(searchQuery = query)
          loadItemsPaginated(resetPagination = true)
        }
      }
    }
  }

  /**
   * Load the initial list of AryaSamaj items
   */
  fun loadInitialList() {
    println("ViewModel Debug: loadInitialList called")
    loadItemsPaginated(resetPagination = true)
  }

  /**
   * Refresh the entire list
   */
  fun refreshList() {
    println("ViewModel Debug: refreshList called")
    loadItemsPaginated(resetPagination = true)
  }

  /**
   * Load next page of items
   */
  fun loadNextPage() {
    println("ViewModel Debug: loadNextPage called")
    if (!_paginationState.value.hasNextPage || _paginationState.value.isLoadingNextPage) {
      println("ViewModel Debug: loadNextPage skipped - hasNextPage: ${_paginationState.value.hasNextPage}, isLoadingNextPage: ${_paginationState.value.isLoadingNextPage}")
      return
    }
    loadItemsPaginated(resetPagination = false)
  }

  /**
   * Load items with pagination support
   */
  fun loadItemsPaginated(resetPagination: Boolean = false) {
    println("ViewModel Debug: loadItemsPaginated called - resetPagination: $resetPagination")
    val currentFilter = _uiState.value.filterState

    if (resetPagination) {
      println("ViewModel Debug: Setting initial loading state")
      _paginationState.value = PaginationState(isInitialLoading = true)
    } else {
      println("ViewModel Debug: Setting next page loading state")
      _paginationState.value = _paginationState.value.copy(isLoadingNextPage = true)
    }

    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      val cursor = if (resetPagination) null else _paginationState.value.endCursor
      println("ViewModel Debug: Starting repository call with cursor: $cursor")

      try {
        when {
          // Combined name and address search
          currentFilter.searchQuery.isNotBlank() && (
            currentFilter.selectedState.isNotBlank() ||
              currentFilter.selectedDistrict.isNotBlank() ||
              currentFilter.selectedVidhansabha.isNotBlank()
            ) -> {
            println("ViewModel Debug: Using combined search")
            repository.searchAryaSamajsByNameAndAddress(
              searchTerm = currentFilter.searchQuery,
              state = currentFilter.selectedState.ifBlank { null },
              district = currentFilter.selectedDistrict.ifBlank { null },
              vidhansabha = currentFilter.selectedVidhansabha.ifBlank { null },
              pageSize = PAGE_SIZE,
              cursor = cursor
            ).collect { result ->
              println("ViewModel Debug: Combined search result received: $result")
              handlePaginationResult(result, resetPagination)
            }
          }

          // Name search only
          currentFilter.searchQuery.isNotBlank() -> {
            println("ViewModel Debug: Using name search")
            repository.searchItemsPaginated(
              searchTerm = currentFilter.searchQuery,
              pageSize = PAGE_SIZE,
              cursor = cursor
            ).collect { result ->
              println("ViewModel Debug: Name search result received: $result")
              handlePaginationResult(result, resetPagination)
            }
          }

          // Address search only
          currentFilter.selectedState.isNotBlank() ||
            currentFilter.selectedDistrict.isNotBlank() ||
            currentFilter.selectedVidhansabha.isNotBlank() -> {
            println("ViewModel Debug: Using address search")
            repository.searchAryaSamajsByAddress(
              state = currentFilter.selectedState.ifBlank { null },
              district = currentFilter.selectedDistrict.ifBlank { null },
              vidhansabha = currentFilter.selectedVidhansabha.ifBlank { null },
              pageSize = PAGE_SIZE,
              cursor = cursor
            ).collect { result ->
              println("ViewModel Debug: Address search result received: $result")
              handlePaginationResult(result, resetPagination)
            }
          }

          // No filters - load all
          else -> {
            println("ViewModel Debug: Using getItemsPaginated (no filters)")
            repository.getItemsPaginated(
              pageSize = PAGE_SIZE,
              cursor = cursor
            ).collect { result ->
              println("ViewModel Debug: GetItemsPaginated result received: $result")
              handlePaginationResult(result, resetPagination)
            }
          }
        }
      } catch (e: Exception) {
        println("ViewModel Debug: Exception in loadItemsPaginated - ${e.message}")
        e.printStackTrace()
        val errorMessage = "सूची लोड करने में त्रुटि हुई"
        _paginationState.value = if (resetPagination) {
          PaginationState(error = errorMessage, showRetryButton = true)
        } else {
          _paginationState.value.copy(
            isLoadingNextPage = false,
            nextPageError = errorMessage
          )
        }
        showError(errorMessage)
      }
    }
  }

  /**
   * Handle pagination result from repository
   */
  private fun handlePaginationResult(
    result: PaginationResult<AryaSamajHomeListItem>,
    resetPagination: Boolean
  ) {
    println("ViewModel Debug: handlePaginationResult called - result type: ${result::class.simpleName}, resetPagination: $resetPagination")
    when (result) {
      is PaginationResult.Success -> {
        println("ViewModel Debug: Success result - items count: ${result.data.size}, hasNextPage: ${result.hasNextPage}")
        val currentItems = if (resetPagination) emptyList() else _paginationState.value.items
        _paginationState.value = _paginationState.value.copy(
          items = currentItems + result.data,
          isInitialLoading = false,
          isLoadingNextPage = false,
          isSearching = false,
          hasNextPage = result.hasNextPage,
          endCursor = result.endCursor,
          error = null,
          nextPageError = null,
          hasReachedEnd = !result.hasNextPage
        )

        // Update UI state for convenience
        updateUiStateFromPagination()
        println("ViewModel Debug: State updated - total items: ${_paginationState.value.items.size}")
      }

      is PaginationResult.Error -> {
        println("ViewModel Debug: Error result - ${result.message}")
        val errorMessage = result.message
        _paginationState.value = if (resetPagination) {
          PaginationState(error = errorMessage, showRetryButton = true)
        } else {
          _paginationState.value.copy(
            isLoadingNextPage = false,
            nextPageError = errorMessage
          )
        }
        showError(errorMessage)
      }

      is PaginationResult.Loading -> {
        println("ViewModel Debug: Loading result received")
        // Loading states are already handled above
      }
    }
  }

  /**
   * Update search query (will trigger debounced search)
   */
  fun updateSearchQuery(query: String) {
    println("ViewModel Debug: updateSearchQuery called: '$query'")
    searchQuery.value = query
    _paginationState.value = _paginationState.value.copy(isSearching = true)
  }

  /**
   * Update filter state
   */
  fun updateFilterState(
    searchQuery: String = _uiState.value.filterState.searchQuery,
    selectedState: String = _uiState.value.filterState.selectedState,
    selectedDistrict: String = _uiState.value.filterState.selectedDistrict,
    selectedVidhansabha: String = _uiState.value.filterState.selectedVidhansabha
  ) {
    println("ViewModel Debug: updateFilterState called")
    val newFilterState = AryaSamajHomeFilterState(
      searchQuery = searchQuery,
      selectedState = selectedState,
      selectedDistrict = selectedDistrict,
      selectedVidhansabha = selectedVidhansabha
    )

    _uiState.update { currentState ->
      currentState.copy(filterState = newFilterState)
    }
  }

  /**
   * Clear all filters and refresh list
   */
  fun clearFilters() {
    println("ViewModel Debug: clearFilters called")
    searchQuery.value = ""
    updateFilterState(
      searchQuery = "",
      selectedState = "",
      selectedDistrict = "",
      selectedVidhansabha = ""
    )
    loadItemsPaginated(resetPagination = true)
  }

  /**
   * Retry loading after error
   */
  fun retryLoad() {
    println("ViewModel Debug: retryLoad called")
    loadItemsPaginated(resetPagination = true)
  }

  /**
   * Load total count
   */
  fun loadTotalCount() {
    println("ViewModel Debug: loadTotalCount called")
    viewModelScope.launch {
      repository.getAryaSamajCount().collect { result ->
        when (result) {
          is Result.Success -> {
            println("ViewModel Debug: Total count loaded: ${result.data}")
            _uiState.update { it.copy(totalCount = result.data) }
          }
          is Result.Error -> {
            println("ViewModel Debug: Total count error: ${result.message}")
            // Ignore count errors
          }
          is Result.Loading -> {
            println("ViewModel Debug: Total count loading")
            // Loading handled elsewhere
          }
        }
      }
    }
  }

  /**
   * Update UI state from pagination state for convenience
   */
  private fun updateUiStateFromPagination() {
    val pagination = _paginationState.value
    _uiState.update { currentState ->
      currentState.copy(
        pageState = AryaSamajHomePageState(
          isLoading = pagination.isInitialLoading,
          error = pagination.error,
          items = pagination.items,
          hasNextPage = pagination.hasNextPage,
          endCursor = pagination.endCursor
        )
      )
    }
  }

  /**
   * Show error message
   */
  private fun showError(message: String) {
    GlobalMessageManager.showError(
      message = message,
      duration = GlobalMessageDuration.LONG
    )
  }

  override fun onCleared() {
    super.onCleared()
    println("ViewModel Debug: onCleared called")
    loadJob?.cancel()
    searchJob?.cancel()
  }
}
