package org.aryamahasangh.features.arya_nirman

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aryamahasangh.components.EventListItem

@Composable
fun AryaNirmanHomeScreen(
  viewModel: AryaNirmanViewModel,
  onNavigateToRegistrationForm: (id: String, capacity: Int) -> Unit
) {
  LaunchedEffect(Unit) {
    viewModel.loadUpComingSessions()
  }

  // Stop real-time subscriptions when screen is disposed
  DisposableEffect(Unit) {
    onDispose {
      viewModel.stopListeningToRegistrationCounts()
    }
  }

  val uiState by viewModel.uiState.collectAsState()
  val registrationCounts by viewModel.registrationCounts.collectAsState()

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
//        snackbarHostState.showSnackbar(
//          message = error,
//          actionLabel = "Retry"
//        )
    }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Failed to load activities")
        Button(onClick = {
          // loadFilteredActivities(activityFilter)
        }) {
          Text("Retry")
        }
      }
    }
    return
  }

  Column(
    modifier = Modifier
      .padding(8.dp)
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    Text("आगामी सत्र", modifier = Modifier.padding(bottom = 8.dp))

    if (uiState.data.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No sessions have been planned!")
      }
    } else {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        uiState.data.forEach { activity ->
          val updatedActivity =
            activity.copy(
              isFull = (registrationCounts[activity.id] ?: 0) >= activity.capacity
            )
          EventListItem(
            event = updatedActivity,
            onRegisterClick = {
              onNavigateToRegistrationForm(activity.id, activity.capacity)
            }
          )
        }
      }
    }
  }
}
