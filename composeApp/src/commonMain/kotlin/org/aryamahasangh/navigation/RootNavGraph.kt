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
import org.aryamahasangh.viewmodel.*
import org.koin.compose.koinInject

@ExperimentalMaterial3Api
@Composable
fun RootNavGraph(navController: NavHostController) {
  NavHost(navController = navController, startDestination = Screen.AboutSection){
    navigation<Screen.AboutSection>(startDestination = Screen.AboutUs){
      composable<Screen.AboutUs> {
        val viewModel = koinInject<AboutUsViewModel>()
        AboutUs(
          showDetailedAboutUs = {
            navController.navigate(Screen.AboutUsDetails)
          },
          viewModel = viewModel
        )
      }
      composable<Screen.AboutUsDetails> {
        val viewModel = koinInject<AboutUsViewModel>()
        DetailedAboutUs(viewModel = viewModel)
      }
    }
    navigation<Screen.ActivitiesSection>(startDestination = Screen.Activities){
      composable<Screen.Activities> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = koinInject<ActivitiesViewModel>()
        if(isLoggedIn){
          ActivitiesContainer(navController, {  }, viewModel)
        }else{
          ActivitiesScreen(navController, {  }, viewModel)
        }
      }
      composable<Screen.ActivityDetails> {
        val id = it.toRoute<Screen.ActivityDetails>().id
        val viewModel = koinInject<ActivitiesViewModel>()
        ActivityDetailScreen(id, viewModel)
      }
    }
    navigation<Screen.OrgsSection>(startDestination = Screen.Orgs){
      composable<Screen.Orgs> {
        val viewModel = koinInject<OrganisationsViewModel>()
        OrgsScreen(navController, {}, viewModel)
      }
      composable<Screen.OrgDetails>{
        val orgId = it.toRoute<Screen.OrgDetails>().name
        val viewModel = koinInject<OrganisationsViewModel>()
        OrgDetailScreen(orgId, navController, viewModel)
      }
    }
    composable<Screen.JoinUs> {
      val viewModel = koinInject<JoinUsViewModel>()
      JoinUsScreen(viewModel)
    }
    navigation<Screen.LearningSection>(startDestination = Screen.Learning){
      composable<Screen.Learning> {
        val viewModel = koinInject<LearningViewModel>()
        LearningScreen(navController, {  }, viewModel)
      }
      composable<Screen.VideoDetails> {
        val id = it.toRoute<Screen.VideoDetails>().learningItemId
        val viewModel = koinInject<LearningViewModel>()
        VideoDetailsScreen(id, viewModel)
      }
    }
    navigation<Screen.BookSection>(startDestination = Screen.BookOrderForm){

      composable<Screen.BookOrderForm> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = koinInject<BookOrderViewModel>()
        if(isLoggedIn){
          BookOrdersContainer(
            viewModel = viewModel,
            onNavigateToDetails = {
              navController.navigate(Screen.BookOrderDetails(it))
            }
          )
        }else {
          BookOrderFormScreen(viewModel = viewModel)
        }
      }
      composable<Screen.BookOrderDetails> {
        val id = it.toRoute<Screen.BookOrderDetails>().bookOrderId
        val viewModel = koinInject<BookOrderViewModel>()
        BookOrderDetailsScreen(viewModel, id, {})
      }
    }
    navigation<Screen.AryaNirmanSection>(startDestination = Screen.AryaNirmanHome){
      composable<Screen.AryaNirmanHome> {
        val viewModel = koinInject<JoinUsViewModel>()
        AryaNirmanHomeScreen(viewModel)
      }
    }
    navigation<Screen.AryaPariwarSection>(startDestination = Screen.AryaPariwarHome){
      composable<Screen.AryaPariwarHome> {
        val viewModel = koinInject<JoinUsViewModel>()
        AryaPariwarScreen(viewModel)
      }
    }
    navigation<Screen.AryaSamajSection>(startDestination = Screen.AryaSamajHome){
      composable<Screen.AryaSamajHome> {
        AryaSamajHomeScreen()
      }
    }

    navigation<Screen.AryaGurukulSection>(startDestination = Screen.AryaGurukulCollege){
      composable<Screen.AryaGurukulCollege> {
        GurukulCollegeHomeScreen(
          navigateToAdmissionForm = {
            navController.navigate(Screen.AdmissionForm)
          }
        )
      }
    }
    navigation<Screen.AryaaGurukulSection>(startDestination = Screen.AryaaGurukulCollege){
      composable<Screen.AryaaGurukulCollege> {
        AryaaGurukulHomeScreen(
          navigateToAdmissionForm = {
            navController.navigate(Screen.AdmissionForm)
          }
        )
      }
      composable<Screen.AdmissionForm> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = koinInject<AdmissionsViewModel>()
        if(isLoggedIn){
          AdmissionScreen(viewModel)
        }else{
          RegistrationForm(viewModel)
        }
      }
    }
  }
}
