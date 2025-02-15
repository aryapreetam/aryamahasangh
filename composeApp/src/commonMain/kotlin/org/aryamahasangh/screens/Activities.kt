package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.datetime.LocalDateTime
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.navigation.Screen
import org.aryamahasangh.network.apolloClient
import org.aryamahasangh.utils.toShortHumanReadable

@Composable
fun Activities(navController: NavHostController, onNavigateToActivityDetails: (String) -> Unit) {
  val activities = remember { mutableStateOf(emptyList<OrganisationalActivitiesQuery.OrganisationalActivity>()) }
  LaunchedEffect(Unit) {
    val res = apolloClient.query(OrganisationalActivitiesQuery()).execute()
    activities.value = res.data?.organisationalActivities!!
  }
  Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    activities.value.forEach { activity ->
      ElevatedCard(
        onClick = {
          onNavigateToActivityDetails(activity.id)
          navController.navigate(Screen.ActivityDetails(activity.id))
        },
        shape = RoundedCornerShape(4.dp),
      ){
        Column(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
          Text(activity.name)
          Text(activity.description)
          Text("${f(activity.startDateTime)} - ${f(activity.endDateTime)}")
          Text("${activity.place}  | ${activity.activityType}")
        }
      }
    }
  }
}

fun f(dateTime: Any): String {
  return LocalDateTime.parse(dateTime as String).toShortHumanReadable()
}
