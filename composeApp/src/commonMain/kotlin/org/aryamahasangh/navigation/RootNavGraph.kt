package org.aryamahasangh.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import org.aryamahasangh.SettingKeys
import org.aryamahasangh.features.activities.ActivitiesContainer
import org.aryamahasangh.features.activities.ActivitiesScreen
import org.aryamahasangh.features.activities.ActivitiesViewModel
import org.aryamahasangh.features.activities.ActivityDetailScreen
import org.aryamahasangh.features.admin.AdminContainerScreen
import org.aryamahasangh.features.admin.AdminViewModel
import org.aryamahasangh.features.admin.MemberDetailScreen
import org.aryamahasangh.features.arya_nirman.AryaNirmanHomeScreen
import org.aryamahasangh.features.arya_nirman.AryaNirmanViewModel
import org.aryamahasangh.features.arya_nirman.SatraRegistrationFormScreen
import org.aryamahasangh.features.arya_nirman.SatraRegistrationViewModel
import org.aryamahasangh.features.organisations.OrgDetailScreen
import org.aryamahasangh.features.organisations.OrganisationsViewModel
import org.aryamahasangh.features.organisations.OrgsScreen
import org.aryamahasangh.screens.*
import org.aryamahasangh.viewmodel.*
import org.koin.compose.koinInject

@ExperimentalMaterial3Api
@Composable
fun RootNavGraph(navController: NavHostController) {
  NavHost(navController = navController, startDestination = Screen.AboutSection) {
    navigation<Screen.AboutSection>(startDestination = Screen.AboutUs) {
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
    navigation<Screen.ActivitiesSection>(startDestination = Screen.Activities) {
      composable<Screen.Activities> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = koinInject<ActivitiesViewModel>()
        val onNavigateToDetails = { id: String ->
          println("navigating to details: $id")
          navController.navigate(Screen.ActivityDetails(id))
        }
        if (isLoggedIn) {
          ActivitiesContainer(onNavigateToDetails, viewModel)
        } else {
          ActivitiesScreen(onNavigateToDetails, viewModel)
        }
      }
      composable<Screen.ActivityDetails> {
        val id = it.toRoute<Screen.ActivityDetails>().id
        val viewModel = koinInject<ActivitiesViewModel>()
        ActivityDetailScreen(id, viewModel)
      }
    }
    navigation<Screen.OrgsSection>(startDestination = Screen.Orgs) {
      composable<Screen.Orgs> {
        val viewModel = koinInject<OrganisationsViewModel>()
        OrgsScreen(onNavigateToOrgDetails = {
          navController.navigate(Screen.OrgDetails(it))
        }, viewModel)
      }
      composable<Screen.OrgDetails> {
        val orgId = it.toRoute<Screen.OrgDetails>().organisationId
        val viewModel = koinInject<OrganisationsViewModel>()
        val adminViewModel = koinInject<AdminViewModel>()

        // Load members when screen is accessed
        LaunchedEffect(Unit) {
          adminViewModel.loadMembers()
        }

        // Collect admin state for search results
        val adminUiState by adminViewModel.membersUiState.collectAsState()

        OrgDetailScreen(
          id = orgId,
          viewModel = viewModel,
          searchMembers = { query ->
            // Use the search results from AdminViewModel
            // This will contain fresh results from server
            if (query.isNotBlank()) {
              adminUiState.searchResults.map { memberShort ->
                org.aryamahasangh.features.activities.Member(
                  id = memberShort.id,
                  name = memberShort.name,
                  profileImage = memberShort.profileImage,
                  phoneNumber = "", // Not available in MemberShort
                  email = "" // Not available in MemberShort
                )
              }
            } else {
              emptyList()
            }
          },
          allMembers = adminUiState.members.map { memberShort ->
            org.aryamahasangh.features.activities.Member(
              id = memberShort.id,
              name = memberShort.name,
              profileImage = memberShort.profileImage,
              phoneNumber = "", // Not available in MemberShort
              email = "" // Not available in MemberShort
            )
          },
          onTriggerSearch = { query ->
            // Trigger the server search in AdminViewModel
            adminViewModel.searchMembers(query)
          }
        )
      }
    }
    composable<Screen.JoinUs> {
      val viewModel = koinInject<JoinUsViewModel>()
      JoinUsScreen(viewModel)
    }
    navigation<Screen.LearningSection>(startDestination = Screen.Learning) {
      composable<Screen.Learning> {
        val viewModel = koinInject<LearningViewModel>()
        LearningScreen(navController, { }, viewModel)
      }
      composable<Screen.VideoDetails> {
        val id = it.toRoute<Screen.VideoDetails>().learningItemId
        val viewModel = koinInject<LearningViewModel>()
        VideoDetailsScreen(id, viewModel)
      }
    }
    navigation<Screen.BookSection>(startDestination = Screen.BookOrderForm) {
      composable<Screen.BookOrderForm> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = koinInject<BookOrderViewModel>()
        if (isLoggedIn) {
          BookOrdersContainer(
            viewModel = viewModel,
            onNavigateToDetails = {
              navController.navigate(Screen.BookOrderDetails(it))
            }
          )
        } else {
          BookOrderFormScreen(viewModel = viewModel)
        }
      }
      composable<Screen.BookOrderDetails> {
        val id = it.toRoute<Screen.BookOrderDetails>().bookOrderId
        val viewModel = koinInject<BookOrderViewModel>()
        BookOrderDetailsScreen(viewModel, id, {})
      }
    }
    navigation<Screen.AryaNirmanSection>(startDestination = Screen.AryaNirmanHome) {
      composable<Screen.AryaNirmanHome> {
        val viewModel = koinInject<AryaNirmanViewModel>()
        AryaNirmanHomeScreen(
          viewModel,
          onNavigateToRegistrationForm = {
            navController.navigate(Screen.AryaNirmanRegistrationForm(activityId = it))
          }
        )
      }
      composable<Screen.AryaNirmanRegistrationForm> {
        val viewModel = koinInject<SatraRegistrationViewModel>()
        val id = it.toRoute<Screen.AryaNirmanRegistrationForm>().activityId
        SatraRegistrationFormScreen(viewModel = viewModel, activityId = id)
      }
    }
    navigation<Screen.AryaPariwarSection>(startDestination = Screen.AryaPariwarHome) {
      composable<Screen.AryaPariwarHome> {
        val viewModel = koinInject<JoinUsViewModel>()
        AryaPariwarScreen(viewModel)
      }
    }
    navigation<Screen.AryaSamajSection>(startDestination = Screen.AryaSamajHome) {
      composable<Screen.AryaSamajHome> {
        AryaSamajHomeScreen()
      }
    }

    navigation<Screen.AryaGurukulSection>(startDestination = Screen.AryaGurukulCollege) {
      composable<Screen.AryaGurukulCollege> {
        GurukulCollegeHomeScreen(
          navigateToAdmissionForm = {
            navController.navigate(Screen.AdmissionForm)
          }
        )
      }
    }
    navigation<Screen.AryaaGurukulSection>(startDestination = Screen.AryaaGurukulCollege) {
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
        if (isLoggedIn) {
          AdmissionScreen(viewModel)
        } else {
          RegistrationForm(viewModel)
        }
      }
    }
    navigation<Screen.AdminSection>(startDestination = Screen.AdminContainer) {
      composable<Screen.AdminContainer> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = koinInject<AdminViewModel>()

        // Navigate to AboutSection if user logs out
        LaunchedEffect(isLoggedIn) {
          if (!isLoggedIn) {
            navController.navigate(Screen.AboutSection) {
              // Clear the back stack so user can't navigate back to admin
              popUpTo(0) { inclusive = true }
            }
          }
        }

        AdminContainerScreen(
          viewModel = viewModel,
          onNavigateToMemberDetail = { memberId ->
            navController.navigate(Screen.MemberDetail(memberId))
          },
          onNavigateToAddMember = {
            navController.navigate(Screen.MemberDetail("new"))
          }
        )
      }
      composable<Screen.MemberDetail> {
        var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
        val viewModel = koinInject<AdminViewModel>()
        val memberId = it.toRoute<Screen.MemberDetail>().memberId

        // Navigate to AboutSection if user logs out
        LaunchedEffect(isLoggedIn) {
          if (!isLoggedIn) {
            navController.navigate(Screen.AboutSection) {
              // Clear the back stack so user can't navigate back to admin
              popUpTo(0) { inclusive = true }
            }
          }
        }

        MemberDetailScreen(
          memberId = memberId,
          viewModel = viewModel,
          isAddMode = memberId == "new"
        )
      }
    }
  }
}
