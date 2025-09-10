package com.aryamahasangh.features.activities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.ActivityListItem
import com.aryamahasangh.features.activities.ui.SearchAndFilterRow
import com.aryamahasangh.features.admin.PaginatedListScreen
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.fragment.ActivityWithStatus
import com.aryamahasangh.utils.WithTooltip
import kotlinx.datetime.Clock

// Global state object for scroll persistence
internal object ActivitiesPageState {
  var activities: List<ActivityWithStatus> = emptyList()
  var paginationState: PaginationState<ActivityWithStatus> = PaginationState()
  var lastSearchQuery: String = ""
  var activeFilters: Set<ActivityFilterOption> = setOf(ActivityFilterOption.ShowAll)
  var needsRefresh: Boolean = false

  // FIX: Add explicit section tracking
  private var isInActivitiesSection = false
  private var hasInitialized = false

  fun clear() {
    activities = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    activeFilters = setOf(ActivityFilterOption.ShowAll)
    needsRefresh = false
  }

  fun saveState(
    newActivities: List<ActivityWithStatus>,
    newPaginationState: PaginationState<ActivityWithStatus>,
    searchQuery: String,
    filterOptions: Set<ActivityFilterOption>
  ) {
    activities = newActivities
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
    activeFilters = filterOptions
  }

  fun hasData(): Boolean = activities.isNotEmpty()

  fun markForRefresh() {
    needsRefresh = true
  }

  // FIX: Explicit section tracking methods
  fun enterActivitiesSection() {
    // FIX: Be more conservative about clearing state
    // Only clear if we've been away from Activities section for a significant time
    // or if this is truly external entry (not internal navigation)
    if (!isInActivitiesSection && hasInitialized) {
      // For now, don't auto-clear on section re-entry to preserve user context
      // Users can manually clear filters if they want a fresh start
      // clear()  // Commented out to preserve filters/search during internal navigation
    }
    isInActivitiesSection = true
    hasInitialized = true
  }

  fun exitActivitiesSection() {
    isInActivitiesSection = false
  }
}

@Composable
fun ActivitiesScreen(
  onNavigateToActivityDetails: (String) -> Unit,
  onNavigateToEditActivity: (String) -> Unit,
  onNavigateToCreateOrganisation: (Int) -> Unit,
  viewModel: ActivitiesViewModel,
  onDataChanged: () -> Unit = {}
) {
  ActivitiesPageState.enterActivitiesSection()

  val uiState by viewModel.activitiesUiState.collectAsState()
  val windowInfo = currentWindowAdaptiveInfo()
  val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
  val isLoggedIn = LocalIsAuthenticated.current

  val density = LocalDensity.current
  val screenWidthDp = with(density) {
    windowInfo.windowSizeClass.windowWidthSizeClass.let {
      when (it) {
        WindowWidthSizeClass.COMPACT -> 600f
        WindowWidthSizeClass.MEDIUM -> 800f
        WindowWidthSizeClass.EXPANDED -> 1200f
        else -> 600f
      }
    }
  }
  val pageSize = viewModel.calculatePageSize(screenWidthDp)

  // Generate unique key when refresh is needed
  val refreshKey = remember(ActivitiesPageState.needsRefresh) {
    if (ActivitiesPageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
  }

  // FIX: Coordinate with navigation filter application to prevent race conditions
  val isApplyingFilter by viewModel.isApplyingInitialFilter.collectAsState()

  // FIX: Handle initial loading since ViewModel no longer auto-loads
  LaunchedEffect(Unit) {
    // Only load initially if no data exists and no contextual filter is being applied
    if (!isApplyingFilter && uiState.activities.isEmpty() && !ActivitiesPageState.hasData()) {
      viewModel.loadActivitiesWithCurrentState(resetPagination = true)
    }
  }

  LaunchedEffect(refreshKey, isApplyingFilter) {
    // Wait for initial filter application to complete if it's in progress
    if (isApplyingFilter) {
      return@LaunchedEffect
    }

    if (ActivitiesPageState.needsRefresh) {
      // FIX: Only clear data, preserve filter and search state to maintain user context
      ActivitiesPageState.activities = emptyList()
      ActivitiesPageState.paginationState = PaginationState()
      ActivitiesPageState.needsRefresh = false
      // Keep lastSearchQuery and activeFilters intact for better UX
    }

    when {
      // Scenario 1: Have saved search query → restore and search with fresh results
      !ActivitiesPageState.needsRefresh && ActivitiesPageState.lastSearchQuery.isNotEmpty() -> {
        viewModel.restoreAndSearchActivities(ActivitiesPageState.lastSearchQuery)
      }

      // Scenario 2: Have saved non-search data → preserve pagination  
      !ActivitiesPageState.needsRefresh && ActivitiesPageState.hasData() -> {
        viewModel.preserveActivityPagination(
          ActivitiesPageState.activities,
          ActivitiesPageState.paginationState,
          ActivitiesPageState.activeFilters
        )
      }

      // Scenario 3: No saved data → load fresh initial data
      // FIX: Use preserved filters from PageState if available, otherwise use current state
      else -> {
        // Check if PageState has preserved filters that should take priority
        val preservedFilters = ActivitiesPageState.activeFilters
        val hasPreservedFilters = preservedFilters.isNotEmpty() &&
          !preservedFilters.contains(ActivityFilterOption.ShowAll)

        if (hasPreservedFilters) {
          // Use preserved filters from PageState and update ViewModel state
          viewModel.applyFilters(preservedFilters)
        } else {
          // Use current ViewModel state
          viewModel.loadActivitiesWithCurrentState(resetPagination = true)
        }
      }
    }

    ActivitiesPageState.needsRefresh = false
  }

  LaunchedEffect(uiState) {
    ActivitiesPageState.saveState(
      uiState.paginationState.items,
      uiState.paginationState,
      uiState.searchQuery,
      uiState.activeFilterOptions
    )
  }

  DisposableEffect(Unit) {
    onDispose {
      ActivitiesPageState.exitActivitiesSection()
    }
  }

  Box {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // Always visible search and filter header (outside scrollable area)
      SearchAndFilterRow(
        modifier = Modifier.padding(8.dp),
        searchQuery = uiState.searchQuery,
        onSearchChange = viewModel::searchActivitiesWithDebounce,
        isSearching = uiState.paginationState.isSearching,
        selectedFilters = uiState.activeFilterOptions,
        isFilterDropdownOpen = uiState.isFilterDropdownOpen,
        onFilterButtonClick = viewModel::toggleFilterDropdown,
        onClearFilters = viewModel::clearAllFilters,
        onFilterToggle = viewModel::toggleFilterOption,
        onDismissDropdown = viewModel::closeFilterDropdown
      )

      // Scrollable content (list or empty state)
      PaginatedListScreen(
        items = uiState.paginationState.items,
        paginationState = uiState.paginationState,
        searchQuery = uiState.searchQuery,
        onSearchChange = viewModel::searchActivitiesWithDebounce,
        onLoadMore = viewModel::loadNextActivityPage,
        onRetry = viewModel::retryActivityLoad,
        searchPlaceholder = "गतिविधि खोजें",
        emptyStateText = "कोई गतिविधि नहीं मिली",
        endOfListText = { count -> "सभी गतिविधियां दिखाई गईं (${count.toString().toDevanagariNumerals()})" },
        addButtonText = "नयी गतिविधी", // Not used since showAddButton = false
        onAddClick = { }, // Not used since showAddButton = false
        showAddButton = false, // Hide the add button in search bar
        isCompactLayout = isCompact,
        itemsPerRow = if (isCompact) 1 else 2,
        modifier = Modifier.weight(1f), // Take remaining space
        showBuiltInSearchBar = false, // No built-in search bar
        headerContent = null, // No header content - it's above now
        itemContent = { activity: ActivityWithStatus ->
          ActivityListItem(
            activity = activity,
            handleOnClick = { onNavigateToActivityDetails(activity.id ?: "") },
            handleEditActivity = { onNavigateToEditActivity(activity.id ?: "") },
            handleDeleteActivity = {
              ActivitiesPageState.markForRefresh()
              viewModel.deleteActivity(activity.id ?: "") {
                onDataChanged()
              }
            }
          )
        }
      )
    }

    // FAB for authenticated users
    if (isLoggedIn) {
      FloatingActionButton(
        onClick = { onNavigateToCreateOrganisation(uiState.paginationState.items.size + 1) },
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
          .semantics { contentDescription = "create_activity_fab" }.testTag("create_activity_fab")
      ) {
        WithTooltip("नयी गतिविधी बनायें") {
          Icon(
            Icons.Default.Add,
            contentDescription = "create_activity_fab",
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    }
  }
}
