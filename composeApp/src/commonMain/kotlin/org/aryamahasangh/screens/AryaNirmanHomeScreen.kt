package org.aryamahasangh.screens

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
import org.aryamahasangh.type.ActivityFilterInput
import org.aryamahasangh.type.ActivityPeriod
import org.aryamahasangh.type.ActivityType
import org.aryamahasangh.viewmodel.JoinUsViewModel

@Composable
fun AryaNirmanHomeScreen(viewModel: JoinUsViewModel) {
  LaunchedEffect(Unit){
    val activityFilter = ActivityFilterInput(
      type = Optional.present(ActivityType.SESSION),
      activityPeriod = Optional.present(ActivityPeriod.FUTURE)
    )
    viewModel.loadFilteredActivities(activityFilter)
  }
  val uiState by viewModel.uiState.collectAsState()
  Column(modifier = Modifier.padding(8.dp)){
    Text("आगामी सत्र")
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
            val activityFilter = ActivityFilterInput(
              type = Optional.present(ActivityType.SESSION),
              activityPeriod = Optional.present(ActivityPeriod.FUTURE)
            )
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
            ActivitiesList(activities = it)
          }
        }
      }
    }
  }
}