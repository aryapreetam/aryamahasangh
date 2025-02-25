package org.aryamahasangh.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.aryamahasangh.navigation.Screen.OrgDetails
import org.aryamahasangh.screens.*

@Composable
fun RootNavGraph(
  navController: NavHostController,
  onNavigateToOrgDetails: (String) -> Unit,
  onNavigateToActivityDetails: (String) -> Unit,
  onNavigateToVideoDetails: (String) -> Unit
) {
  NavHost(navController = navController, startDestination = Screen.AboutUs){
    composable<Screen.AboutUs> {
      AboutUs()
    }
    composable<Screen.Activities> {
      ActivitiesScreen(navController, onNavigateToActivityDetails)
    }
    composable<Screen.ActivityDetails> {
      val id = it.toRoute<Screen.ActivityDetails>().id
      ActivityDetailScreen(id)
    }
    composable<Screen.JoinUs> {
      JoinUsScreen()
    }
    composable<Screen.Orgs> {
      Orgs(navController, onNavigateToOrgDetails)
    }
    composable<OrgDetails>{
      val orgId = it.toRoute<OrgDetails>().name
      OrgDetailScreen(orgId, navController)
    }
    composable<Screen.Learning> {
      LearningScreen(navController, onNavigateToActivityDetails)
    }
    composable<Screen.VideoDetails> {
      val id = it.toRoute<Screen.VideoDetails>().learningItemId
      VideoDetailsScreen(id)
    }
    composable<Screen.AdmissionForm> {
      RegistrationForm()
    }
  }
}