package com.aryamahasangh.features.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.ActivityListItem
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.utils.WithTooltip
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivitiesScreen(
  onNavigateToActivityDetails: (String) -> Unit,
  onNavigateToEditActivity: (String) -> Unit,
  onNavigateToCreateOrganisation: (Int) -> Unit,
  viewModel: ActivitiesViewModel
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  val isLoggedIn = LocalIsAuthenticated.current
  // Collect UI state from ViewModel
  val uiState by viewModel.uiState.collectAsState()

  // Handle loading state
  if (uiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      LinearProgressIndicator()
    }
    return
  }

  // Handle error state
  uiState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Retry"
      )
    }
  }

  // Handle empty state
  if (uiState.activities.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("No activities have been planned")
    }
    return
  }

  Box {
    // Display activities
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(8.dp)
          .verticalScroll(rememberScrollState())
    ) {
      FlowRow(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = if (isLoggedIn) 80.dp else 0.dp) // Add padding when FAB is visible
      ) {
        // Sort activities by status: ONGOING -> UPCOMING -> PAST
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val sortedActivities =
          uiState.activities.sortedWith(
            compareBy { activity ->
              when {
                currentTime >= activity.startDatetime.toLocalDateTime() && currentTime <= activity.endDatetime.toLocalDateTime() -> 0 // ONGOING
                currentTime < activity.startDatetime.toLocalDateTime() -> 1 // UPCOMING
                else -> 2 // PAST
              }
            }
          )

        sortedActivities.forEach { activity ->
          ActivityListItem(
            activity = activity,
            handleOnClick = {
              onNavigateToActivityDetails(activity.id)
            },
            handleDeleteActivity = {
              // Delete activity
              viewModel.deleteActivity(activity.id)
              scope.launch {
                snackbarHostState.showSnackbar("गतिविधि सफलतापूर्वक हटा दी गई")
              }
            },
            handleEditActivity = {
              onNavigateToEditActivity(activity.id)
            }
          )
        }
      }
    }
    // Add Organisation FAB - only visible to logged in users
    if (isLoggedIn) {
      FloatingActionButton(
        onClick = { onNavigateToCreateOrganisation(uiState.activities.size + 1) },
        modifier =
          Modifier
            .align(Alignment.BottomEnd).padding(16.dp)
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
