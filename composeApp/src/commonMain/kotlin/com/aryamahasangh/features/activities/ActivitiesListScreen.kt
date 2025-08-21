package com.aryamahasangh.features.activities

import androidx.compose.foundation.layout.Box
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
  var needsRefresh: Boolean = false

  fun clear() {
    activities = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    needsRefresh = false
  }

  fun saveState(
    newActivities: List<ActivityWithStatus>,
    newPaginationState: PaginationState<ActivityWithStatus>,
    searchQuery: String
  ) {
    activities = newActivities
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = activities.isNotEmpty()

  fun markForRefresh() {
    needsRefresh = true
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

  LaunchedEffect(refreshKey) {
    if (ActivitiesPageState.needsRefresh) {
      ActivitiesPageState.clear()
    }

    when {
      // Scenario 1: Have saved search query → restore and search with fresh results
      !ActivitiesPageState.needsRefresh && ActivitiesPageState.lastSearchQuery.isNotEmpty() -> {
        viewModel.restoreAndSearchActivities(ActivitiesPageState.lastSearchQuery)
      }

      // Scenario 2: Have saved non-search data → preserve pagination  
      !ActivitiesPageState.needsRefresh && ActivitiesPageState.hasData() -> {
        viewModel.preserveActivityPagination(ActivitiesPageState.activities, ActivitiesPageState.paginationState)
      }

      // Scenario 3: No saved data → load fresh initial data
      else -> {
        viewModel.loadActivitiesPaginated(pageSize = pageSize, resetPagination = true)
      }
    }

    ActivitiesPageState.needsRefresh = false
  }

  LaunchedEffect(uiState) {
    ActivitiesPageState.saveState(uiState.paginationState.items, uiState.paginationState, uiState.searchQuery)
  }

  Box {
    PaginatedListScreen(
      items = uiState.activities,
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

    // FAB for authenticated users
    if (isLoggedIn) {
      FloatingActionButton(
        onClick = { onNavigateToCreateOrganisation(uiState.activities.size + 1) },
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
