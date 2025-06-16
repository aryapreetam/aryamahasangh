package org.aryamahasangh.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import org.aryamahasangh.LocalIsAuthenticated
import org.aryamahasangh.features.activities.*
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
  val isLoggedIn = LocalIsAuthenticated.current
  NavHost(navController = navController, startDestination = Screen.AboutSection) {
    navigation<Screen.AboutSection>(startDestination = Screen.AboutUs) {
      composable<Screen.AboutUs> {
        val viewModel = koinInject<AboutUsViewModel>()
        AboutUs(
          showDetailedAboutUs = { organisationId ->
            navController.navigate(Screen.AboutUsDetails(organisationId))
          },
          viewModel = viewModel
        )
      }
      composable<Screen.AboutUsDetails> {
        val orgViewModel = koinInject<OrganisationsViewModel>()
        val adminViewModel = koinInject<AdminViewModel>()

        val organisationId = it.toRoute<Screen.AboutUsDetails>().organisationId

        // Load members when screen is accessed
        LaunchedEffect(Unit) {
          adminViewModel.loadMembers()
        }

        // Collect admin state for search results
        val adminUiState by adminViewModel.membersUiState.collectAsState()

        OrgDetailScreen(
          id = organisationId,
          viewModel = orgViewModel,
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
    navigation<Screen.ActivitiesSection>(startDestination = Screen.Activities) {
      composable<Screen.Activities> {
        val viewModel = koinInject<ActivitiesViewModel>()
        val onNavigateToDetails = { id: String ->
          println("navigating to details: $id")
          navController.navigate(Screen.ActivityDetails(id))
        }
        if (isLoggedIn) {
          ActivitiesContainer(
            onNavigateToActivityDetails = onNavigateToDetails,
            viewModel = viewModel,
            onNavigateToEditActivity = { id ->
              navController.navigate(Screen.EditActivity(id))
            }
          )
        } else {
          ActivitiesScreen(
            onNavigateToActivityDetails = onNavigateToDetails,
            onNavigateToEditActivity = { id ->
              // This won't be used for non-logged in users, but we need to provide it
            },
            viewModel = viewModel
          )
        }
      }
      composable<Screen.EditActivity> {
        val id = it.toRoute<Screen.EditActivity>().id
        val viewModel = koinInject<ActivitiesViewModel>()
        CreateActivityScreen(
          viewModel = viewModel,
          editingActivityId = id,
          onActivitySaved = { activityId ->
            // Navigate to activity details after save
            navController.navigate(Screen.ActivityDetails(activityId)) {
              popUpTo(Screen.Activities)
            }
          },
          onCancel = {
            navController.popBackStack()
          }
        )
      }
      composable<Screen.ActivityDetails> {
        val id = it.toRoute<Screen.ActivityDetails>().id
        val viewModel = koinInject<ActivitiesViewModel>()
        ActivityDetailScreen(
          id = id,
          onNavigateToEdit = { activityId ->
            // Navigate to edit screen
            navController.navigate(Screen.EditActivity(activityId))
          },
          onNavigateToRegistration = { activityId, capacity ->
            // Navigate to registration form
            navController.navigate(Screen.AryaNirmanRegistrationForm(activityId, capacity))
          },
          viewModel = viewModel
        )
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
          onNavigateToRegistrationForm = { id, capacity ->
            navController.navigate(Screen.AryaNirmanRegistrationForm(activityId = id, capacity = capacity))
          }
        )
      }
      composable<Screen.AryaNirmanRegistrationForm> {
        val viewModel = koinInject<SatraRegistrationViewModel>()
        val args = it.toRoute<Screen.AryaNirmanRegistrationForm>()
        SatraRegistrationFormScreen(
          viewModel = viewModel,
          activityId = args.activityId,
          activityCapacity = args.capacity,
          onNavigateBack = { navController.popBackStack() }
        )
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
          isAddMode = memberId == "new",
          onNavigateBack = {
            if (memberId == "new") {
              // For add mode, first check if there are unsaved changes
              // The screen will handle this logic internally
              navController.popBackStack()
            } else {
              navController.popBackStack()
            }
          }
        )
      }
    }
    navigation<Screen.KshatraTrainingSection>(startDestination = Screen.KshatraTrainingHome) {
      composable<Screen.KshatraTrainingHome> {
        PhysicalTrainingForm(Gender.BOY, {})
      }
    }
    navigation<Screen.ChatraTrainingSection>(startDestination = Screen.ChatraTrainingHome) {
      composable<Screen.ChatraTrainingHome> {
        PhysicalTrainingFormGirl(Gender.GIRL, {})
      }
    }
  }
}

@Composable
fun PhysicalTrainingFormGirl(gender: Gender, onBack: () -> Unit) {
  PhysicalTrainingForm(Gender.GIRL, {})
}
