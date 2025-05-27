package org.aryamahasangh.features.arya_nirman

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apollographql.apollo.api.Optional
import org.aryamahasangh.components.EventListItem
import org.aryamahasangh.components.activityListItemsWithActions
import org.aryamahasangh.components.dummyDirectionsCallback
import org.aryamahasangh.features.activities.ActivityType
import org.aryamahasangh.viewmodel.JoinUsViewModel

@Composable
fun AryaNirmanHomeScreen(
  viewModel: JoinUsViewModel,
  onNavigateToRegistrationForm: () -> Unit) {
  LaunchedEffect(Unit){
    viewModel.loadFilteredActivities("", "")
  }
  val uiState by viewModel.uiState.collectAsState()
  Column(modifier = Modifier.padding(8.dp)){
    Text("आगामी सत्र", modifier = Modifier.padding(bottom = 8.dp))
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
            //loadFilteredActivities(activityFilter)
          }) {
            Text("Retry")
          }
        }
      }
      return
    }

    uiState.activities.let {
      if (it != null) {
        if(it.isEmpty()){
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text("No sessions have been planned!")
          }
        }else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .weight(1f) // Limits height to remaining space
          ) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              activityListItemsWithActions.forEach {
                EventListItem(
                  event = it,
                  onRegisterClick = {
                    onNavigateToRegistrationForm()
                  },
                  onDirectionsClick = ::dummyDirectionsCallback
                )
              }
            }
          }
        }
      }
    }
  }
}