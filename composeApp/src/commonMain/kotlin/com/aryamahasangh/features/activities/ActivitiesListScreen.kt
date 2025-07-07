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
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.ActivityListItem
import com.aryamahasangh.features.admin.PaginatedListScreen
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.fragment.ActivityWithStatus
import com.aryamahasangh.fragment.OrganisationalActivityShort
import com.aryamahasangh.utils.WithTooltip
import kotlinx.datetime.Clock

// Convert ActivityWithStatus to OrganisationalActivityShort for compatibility
private fun ActivityWithStatus.toOrganisationalActivityShort(): OrganisationalActivityShort {
  return OrganisationalActivityShort(
    id = this.id ?: "",
    name = this.name ?: "",
    shortDescription = this.shortDescription ?: "",
    startDatetime = this.startDatetime ?: Clock.System.now(),
    endDatetime = this.endDatetime ?: Clock.System.now(),
    type = this.type ?: com.aryamahasangh.type.ActivityType.UNKNOWN__,
    district = this.district,
    state = this.state
  )
}

// Global state object for scroll persistence
private object ActivitiesPageState {
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

    // Preserve pagination only if not refreshing
    if (!ActivitiesPageState.needsRefresh && ActivitiesPageState.hasData() &&
      ActivitiesPageState.lastSearchQuery == uiState.searchQuery
    ) {
      viewModel.preserveActivityPagination(ActivitiesPageState.activities, ActivitiesPageState.paginationState)
    }

    // Load data: Reset pagination if refresh needed OR no existing data (initial load)
    val shouldReset = ActivitiesPageState.needsRefresh || !ActivitiesPageState.hasData()
    viewModel.loadActivitiesPaginated(pageSize = pageSize, resetPagination = shouldReset)
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
      searchPlaceholder = "गतिविधि का नाम खोजें",
      emptyStateText = "कोई गतिविधि नहीं मिली",
      endOfListText = { count -> "सभी गतिविधियां दिखाई गईं (${count.toString().toDevanagariNumerals()})" },
      addButtonText = "नयी गतिविधी", // Not used since showAddButton = false
      onAddClick = { }, // Not used since showAddButton = false
      showAddButton = false, // Hide the add button in search bar
      isCompactLayout = isCompact,
      itemsPerRow = if (isCompact) 1 else 2,
      itemContent = { activity: ActivityWithStatus ->
        ActivityListItem(
          activity = activity.toOrganisationalActivityShort(),
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
      ) {
        WithTooltip("नयी गतिविधी बनायें") {
          Icon(
            Icons.Default.Add,
            contentDescription = "नयी गतिविधी बनायें",
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    }
  }
}
