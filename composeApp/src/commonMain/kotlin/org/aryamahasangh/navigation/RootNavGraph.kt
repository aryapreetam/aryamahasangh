package org.aryamahasangh.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import org.aryamahasangh.SettingKeys
import org.aryamahasangh.screens.AboutUs
import org.aryamahasangh.screens.ActivitiesContainer
import org.aryamahasangh.screens.ActivitiesScreen
import org.aryamahasangh.screens.ActivityDetailScreen
import org.aryamahasangh.screens.AdmissionScreen
import org.aryamahasangh.screens.AryaNirmanHomeScreen
import org.aryamahasangh.screens.AryaSamajHomeScreen
import org.aryamahasangh.screens.BookOrderFormScreen
import org.aryamahasangh.screens.DetailedAboutUs
import org.aryamahasangh.screens.GurukulCollegeHomeScreen
import org.aryamahasangh.screens.JoinUsScreen
import org.aryamahasangh.screens.LearningScreen
import org.aryamahasangh.screens.OrgDetailScreen
import org.aryamahasangh.screens.OrgsScreen
import org.aryamahasangh.screens.RegistrationForm
import org.aryamahasangh.screens.VideoDetailsScreen

@Composable
fun RootNavGraph(navController: NavHostController) {
  NavHost(navController = navController, startDestination = Screen.AboutSection){
    navigation<Screen.AboutSection>(startDestination = Screen.AboutUs){
      composable<Screen.AboutUs> {
        AboutUs(
          showDetailedAboutUs = {
            navController.navigate(Screen.AboutUsDetails)
          }
        )
      }
      composable<Screen.AboutUsDetails> {
        DetailedAboutUs()
      }
    }
    navigation<Screen.ActivitiesSection>(startDestination = Screen.Activities){
      composable<Screen.Activities> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        if(isLoggedIn){
          ActivitiesContainer(navController, {  })
        }else{
          ActivitiesScreen(navController, {  })
        }
      }
      composable<Screen.ActivityDetails> {
        val id = it.toRoute<Screen.ActivityDetails>().id
        ActivityDetailScreen(id)
      }
    }
    navigation<Screen.OrgsSection>(startDestination = Screen.Orgs){
      composable<Screen.Orgs> {
        OrgsScreen(navController, {})
      }
      composable<Screen.OrgDetails>{
        val orgId = it.toRoute<Screen.OrgDetails>().name
        OrgDetailScreen(orgId, navController)
      }
    }
    composable<Screen.JoinUs> {
      JoinUsScreen()
    }
    navigation<Screen.LearningSection>(startDestination = Screen.Learning){
      composable<Screen.Learning> {
        LearningScreen(navController, {  })
      }
      composable<Screen.VideoDetails> {
        val id = it.toRoute<Screen.VideoDetails>().learningItemId
        VideoDetailsScreen(id)
      }
    }
    navigation<Screen.BookSection>(startDestination = Screen.BookOrderForm){
      composable<Screen.BookOrderForm> {
        BookOrderFormScreen()
      }
    }
    navigation<Screen.AryaNirmanSection>(startDestination = Screen.AryaNirmanHome){
      composable<Screen.AryaNirmanHome> {
        AryaNirmanHomeScreen()
      }
    }
    navigation<Screen.AryaSamajSection>(startDestination = Screen.AryaSamajHome){
      composable<Screen.AryaSamajHome> {
        AryaSamajHomeScreen()
      }
    }

    navigation<Screen.GurukulSection>(startDestination = Screen.GurukulCollege){
      composable<Screen.GurukulCollege> {
        GurukulCollegeHomeScreen(
          navigateToAdmissionForm = {
            navController.navigate(Screen.AdmissionForm)
          }
        )
      }
      composable<Screen.AdmissionForm> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        if(isLoggedIn){
          AdmissionScreen()
        }else{
          RegistrationForm()
        }
      }
    }
  }
}