package org.aryamahasangh.screens

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
import org.aryamahasangh.DeleteActivityMutation
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.components.ActivityListItem
import org.aryamahasangh.navigation.Screen
import org.aryamahasangh.network.apolloClient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivitiesScreen(navController: NavHostController, onNavigateToActivityDetails: (String) -> Unit) {
  val scope = rememberCoroutineScope()

  val snackbarHostState = LocalSnackbarHostState.current
  var isLoading by remember { mutableStateOf(false) }
  var activities by remember { mutableStateOf(emptyList<OrganisationalActivitiesQuery.OrganisationalActivity>()) }
  LaunchedEffect(Unit) {
    isLoading = true
    val res = apolloClient.query(OrganisationalActivitiesQuery()).execute()
    isLoading = false
    activities = res.data?.organisationalActivities ?: emptyList()
  }
  if(isLoading){
    Box(
      modifier = Modifier
        .fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      LinearProgressIndicator()
    }
    return
  }

  if(activities.isEmpty()){
    Box(
      modifier = Modifier
        .fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("No activities have been planned")
    }
    return
  }

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
      activities.forEach { activity ->
        ActivityListItem(
          activity = activity,
          handleOnClick = {
            onNavigateToActivityDetails(activity.id)
            navController.navigate(Screen.ActivityDetails(activity.id))
          }
        ) {
          scope.launch {
            val res = apolloClient.mutation(DeleteActivityMutation(activity.id)).execute()
            if(!res.hasErrors()){
              activities = activities.filter { it.id != activity.id  }
              snackbarHostState.showSnackbar("Activity deleted successfully")
            }else{
              snackbarHostState.showSnackbar(
                message = "Error deleting activity. Please try again",
                actionLabel = "Close"
              )
            }
          }
        }
      }
    }
  }
}
