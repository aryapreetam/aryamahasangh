package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.navigation.Screen
import org.aryamahasangh.network.apolloClient

@Composable
fun Activities(navController: NavHostController, onNavigateToActivityDetails: (String) -> Unit) {
  val activities = remember { mutableStateOf(emptyList<OrganisationalActivitiesQuery.OrganisationalActivity>()) }
  LaunchedEffect(Unit) {
    val res = apolloClient.query(OrganisationalActivitiesQuery()).execute()
    activities.value = res.data?.organisationalActivities!!
  }
  Column(modifier = Modifier
    .fillMaxSize()
    .padding(8.dp)
    .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp)) {
    activities.value.forEach { activity ->
      ActivityListItem(activity){
        onNavigateToActivityDetails(activity.id)
        navController.navigate(Screen.ActivityDetails(activity.id))
      }
    }
  }
}
