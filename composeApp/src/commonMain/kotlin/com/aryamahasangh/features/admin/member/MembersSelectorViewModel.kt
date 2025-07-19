package com.aryamahasangh.features.admin.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.util.Result
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the members selector with infinite scroll support
 */
data class MembersSelectorUiState(
  val recentMembers: List<Member> = emptyList(),
  val searchResults: List<Member> = emptyList(),
  val isLoadingRecent: Boolean = false,
  val isSearching: Boolean = false,
  val isLoadingMore: Boolean = false,
  val hasNextPageRecent: Boolean = true,
  val hasNextPageSearch: Boolean = true,
  val currentSearchQuery: String = "", // For display/debugging only, not for TextField
  val error: String? = null,
  val showRetryButton: Boolean = false,
  val retryCount: Int = 0
)

/**
 * ViewModel for the self-contained members selector functionality with infinite scroll
 */
@OptIn(FlowPreview::class)
class MembersSelectorViewModel(
  private val repository: MembersSelectorRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(MembersSelectorUiState())
  val uiState: StateFlow<MembersSelectorUiState> = _uiState.asStateFlow()

  // Internal search trigger - only for triggering network calls
  private val _searchTrigger = MutableStateFlow("")
  private var searchJob: Job? = null
  private var recentCursor: String? = null
  private var searchCursor: String? = null

  // Collection type for this selector instance
  private var collectionType: MemberCollectionType = MemberCollectionType.ALL_MEMBERS

  init {
    // Load recent members on initialization
    loadRecentMembers()

    // Setup debounced search with proper cancellation
    _searchTrigger
      .debounce(300) // Optimized from 500ms for better responsiveness
      .distinctUntilChanged()
      .onEach { query ->
        // Cancel previous search
        searchJob?.cancel()

        if (query.length >= 2) {
          searchMembers(query, resetPagination = true)
        } else {
          // Clear search results when query is too short
          _uiState.update {
            it.copy(
              searchResults = emptyList(),
              isSearching = false,
              hasNextPageSearch = true,
              currentSearchQuery = query,
              error = null,
              showRetryButton = false,
              retryCount = 0
            )
          }
          // Reset search cursor
          searchCursor = null
        }
      }
      .launchIn(viewModelScope)
  }

  /**
   * Load recent members (called on initialization)
   */
  private fun loadRecentMembers(resetPagination: Boolean = true) {
    if (resetPagination) {
      recentCursor = null
    }

    viewModelScope.launch {
      repository.getRecentMembers(limit = 20, cursor = recentCursor, collectionType = collectionType)
        .collect { result ->
          _uiState.update { currentState ->
            when (result) {
              is Result.Loading -> currentState.copy(
                isLoadingRecent = !resetPagination, // Don't show loading for pagination
                isLoadingMore = !resetPagination,   // Show loading more for pagination
                error = null,
                showRetryButton = false
              )

              is Result.Success -> {
                val newMembers = if (resetPagination) {
                  result.data.items
                } else {
                  currentState.recentMembers + result.data.items
                }

                recentCursor = result.data.endCursor

                currentState.copy(
                  recentMembers = newMembers,
                  isLoadingRecent = false,
                  isLoadingMore = false,
                  hasNextPageRecent = result.data.hasNextPage,
                  error = null,
                  showRetryButton = false,
                  retryCount = 0
                )
              }

              is Result.Error -> currentState.copy(
                isLoadingRecent = false,
                isLoadingMore = false,
                error = result.message,
                showRetryButton = true
              )
            }
          }
        }
    }
  }

  /**
   * Search members with proper job cancellation and pagination support
   */
  private fun searchMembers(query: String, resetPagination: Boolean = true) {
    if (resetPagination) {
      searchCursor = null
    }

    searchJob = viewModelScope.launch {
      repository.searchMembers(query, limit = 20, cursor = searchCursor, collectionType = collectionType)
        .collect { result ->
          _uiState.update { currentState ->
            when (result) {
              is Result.Loading -> currentState.copy(
                isSearching = resetPagination, // Only show searching for new search
                isLoadingMore = !resetPagination, // Show loading more for pagination
                currentSearchQuery = query,
                error = null,
                showRetryButton = false
              )

              is Result.Success -> {
                val newResults = if (resetPagination) {
                  result.data.items
                } else {
                  currentState.searchResults + result.data.items
                }

                searchCursor = result.data.endCursor

                currentState.copy(
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

              is Result.Error -> currentState.copy(
                isSearching = false,
                isLoadingMore = false,
                currentSearchQuery = query,
                error = result.message,
                showRetryButton = true
              )
            }
          }
        }
    }
  }

  /**
   * Load next page of results (either recent members or search results)
   */
  fun loadNextPage() {
    val currentState = _uiState.value

    // Don't load if already loading or no more pages
    if (currentState.isLoadingMore) return

    if (currentState.currentSearchQuery.isBlank()) {
      // Load more recent members
      if (currentState.hasNextPageRecent) {
        loadRecentMembers(resetPagination = false)
      }
    } else {
      // Load more search results  
      if (currentState.hasNextPageSearch) {
        searchMembers(currentState.currentSearchQuery, resetPagination = false)
      }
    }
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
        _uiState.update { it.copy(retryCount = attempt) }

        if (attempt >= maxRetries) {
          // Show manual retry button after max retries
          _uiState.update {
            it.copy(
              error = "कुछ प्रयासों के बाद भी त्रुटि है",
              showRetryButton = true
            )
          }
        } else {
          // Wait with exponential backoff
          delay(1000L * attempt)
        }
      }
    }
  }

  /**
   * Trigger search (called from UI with debouncing)
   * This does NOT update any UI state immediately - only triggers the search
   */
  fun triggerSearch(query: String) {
    _searchTrigger.value = query
  }

  /**
   * Set the collection type for this selector instance
   */
  fun setCollectionType(type: MemberCollectionType) {
    if (collectionType != type) {
      collectionType = type
      // Reload with new collection type
      recentCursor = null
      searchCursor = null
      loadRecentMembers(resetPagination = true)
    }
  }

  /**
   * Retry loading recent members
   */
  fun retryLoadRecentMembers() {
    viewModelScope.launch {
      retryWithBackoff(operation = {
        loadRecentMembers(resetPagination = true)
      })
    }
  }

  /**
   * Retry current search
   */
  fun retrySearch() {
    val currentQuery = _uiState.value.currentSearchQuery
    if (currentQuery.length >= 2) {
      viewModelScope.launch {
        retryWithBackoff(operation = {
          _searchTrigger.value = currentQuery
        })
      }
    }
  }

  /**
   * Clear search and return to recent members
   */
  fun clearSearch() {
    searchJob?.cancel()
    searchCursor = null
    _searchTrigger.value = ""
  }

  /**
   * Clear any error state
   */
  fun clearError() {
    _uiState.update {
      it.copy(
        error = null,
        showRetryButton = false,
        retryCount = 0
      )
    }
  }
}
