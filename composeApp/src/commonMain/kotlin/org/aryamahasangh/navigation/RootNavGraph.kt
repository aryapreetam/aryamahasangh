package org.aryamahasangh.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
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
import org.aryamahasangh.screens.*
import org.aryamahasangh.viewmodel.OrganisationsViewModel

@ExperimentalMaterial3Api
@Composable
fun RootNavGraph(navController: NavHostController) {
  NavHost(navController = navController, startDestination = Screen.AboutSection){
    navigation<Screen.AboutSection>(startDestination = Screen.AboutUs){
      composable<Screen.AboutUs> {
        val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.AboutUsViewModel>()
        AboutUs(
          showDetailedAboutUs = {
            navController.navigate(Screen.AboutUsDetails)
          },
          viewModel = viewModel
        )
      }
      composable<Screen.AboutUsDetails> {
        val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.AboutUsViewModel>()
        DetailedAboutUs(viewModel = viewModel)
      }
    }
    navigation<Screen.ActivitiesSection>(startDestination = Screen.Activities){
      composable<Screen.Activities> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.ActivitiesViewModel>()
        if(isLoggedIn){
          ActivitiesContainer(navController, {  }, viewModel)
        }else{
          ActivitiesScreen(navController, {  }, viewModel)
        }
      }
      composable<Screen.ActivityDetails> {
        val id = it.toRoute<Screen.ActivityDetails>().id
        val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.ActivitiesViewModel>()
        ActivityDetailScreen(id, viewModel)
      }
    }
    navigation<Screen.OrgsSection>(startDestination = Screen.Orgs){
      composable<Screen.Orgs> {
        val viewModel = org.koin.compose.koinInject<OrganisationsViewModel>()
        OrgsScreen(navController, {}, viewModel)
      }
      composable<Screen.OrgDetails>{
        val orgId = it.toRoute<Screen.OrgDetails>().name
        val viewModel = org.koin.compose.koinInject<OrganisationsViewModel>()
        OrgDetailScreen(orgId, navController, viewModel)
      }
    }
    composable<Screen.JoinUs> {
      val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.JoinUsViewModel>()
      JoinUsScreen(viewModel)
    }
    navigation<Screen.LearningSection>(startDestination = Screen.Learning){
      composable<Screen.Learning> {
        val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.LearningViewModel>()
        LearningScreen(navController, {  }, viewModel)
      }
      composable<Screen.VideoDetails> {
        val id = it.toRoute<Screen.VideoDetails>().learningItemId
        val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.LearningViewModel>()
        VideoDetailsScreen(id, viewModel)
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
        val viewModel = org.koin.compose.koinInject<org.aryamahasangh.viewmodel.AdmissionsViewModel>()
        if(isLoggedIn){
          AdmissionScreen(viewModel)
        }else{
          RegistrationForm(viewModel)
        }
      }
    }
  }
}
