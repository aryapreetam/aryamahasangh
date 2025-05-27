package org.aryamahasangh.features.activities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.components.ActivityListItem
import org.aryamahasangh.navigation.Screen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivitiesScreen(
  navController: NavHostController, 
  onNavigateToActivityDetails: (String) -> Unit,
  viewModel: ActivitiesViewModel
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current

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

  // Display activities
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp)
      .verticalScroll(rememberScrollState())
  ) {
    FlowRow(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      uiState.activities.forEach { activity ->
        ActivityListItem(
          activity = activity,
          handleOnClick = {
            onNavigateToActivityDetails(activity.id)
            navController.navigate(Screen.ActivityDetails(activity.id))
          }
        ) {
          // Delete activity
          viewModel.deleteActivity(activity.id)
          scope.launch {
            snackbarHostState.showSnackbar("Activity deleted successfully")
          }
        }
      }
    }
  }
}
