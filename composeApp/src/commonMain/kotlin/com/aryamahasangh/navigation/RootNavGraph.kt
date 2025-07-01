package com.aryamahasangh.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.features.activities.*
import com.aryamahasangh.features.admin.*
import com.aryamahasangh.features.admin.data.AryaSamajViewModel
import com.aryamahasangh.features.arya_nirman.AryaNirmanHomeScreen
import com.aryamahasangh.features.arya_nirman.AryaNirmanViewModel
import com.aryamahasangh.features.arya_nirman.SatraRegistrationFormScreen
import com.aryamahasangh.features.arya_nirman.SatraRegistrationViewModel
import com.aryamahasangh.features.organisations.NewOrganisationFormScreen
import com.aryamahasangh.features.organisations.OrgDetailScreen
import com.aryamahasangh.features.organisations.OrganisationsViewModel
import com.aryamahasangh.features.organisations.OrgsScreen
import com.aryamahasangh.screens.*
import com.aryamahasangh.viewmodel.*
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
          viewModel = viewModel,
          navigateToScreen = { screen ->
            navController.navigate(screen)
          }
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
                Member(
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
          allMembers =
            adminUiState.members.map { memberShort ->
              Member(
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
    navigation<Screen.AryaPariwarSection>(startDestination = Screen.AryaPariwarHome) {
      composable<Screen.AryaPariwarHome> {
        val viewModel = koinInject<FamilyViewModel>()
        AryaPariwarScreen(viewModel)
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
    navigation<Screen.AdminSection>(startDestination = Screen.AdminContainer(0)) {
      composable<Screen.AdminContainer> {
        val viewModel = koinInject<AdminViewModel>()
        val aryaSamajViewModel = koinInject<AryaSamajViewModel>()
        val familyViewModel = koinInject<FamilyViewModel>()

        // Extract the tab id from route
        val route = it.toRoute<Screen.AdminContainer>()
        val initialTabIndex = route.id

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
          aryaSamajViewModel = aryaSamajViewModel,
          familyViewModel = familyViewModel,
          initialTabIndex = initialTabIndex,
          onNavigateToMemberDetail = { memberId ->
            navController.navigate(Screen.MemberDetail(memberId))
          },
          onNavigateToAddMember = {
            navController.navigate(Screen.AddMemberForm)
          },
          onNavigateToEditMember = { memberId ->
            navController.navigate(Screen.EditMemberForm(memberId))
          },
          onNavigateToAddAryaSamaj = {
            navController.navigate(Screen.AddAryaSamajForm)
          },
          onNavigateToAryaSamajDetail = { aryaSamajId ->
            navController.navigate(Screen.AryaSamajDetail(aryaSamajId))
          },
          onEditAryaSamaj = { aryaSamajId ->
            navController.navigate(Screen.EditAryaSamajForm(aryaSamajId))
          },
          onNavigateToCreateFamily = {
            navController.navigate(Screen.CreateFamilyForm)
          },
          onNavigateToFamilyDetail = { familyId ->
            navController.navigate(Screen.FamilyDetail(familyId))
          },
          onEditFamily = { familyId ->
            navController.navigate(Screen.EditFamilyForm(familyId))
          },
          onDeleteFamily = { familyId ->
            familyViewModel.deleteFamily(familyId)
          }
        )
      }
      composable<Screen.AddMemberForm> {
        val viewModel = koinInject<AdminViewModel>()

        AddMemberFormScreen(
          viewModel = viewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          onNavigateToMemberDetail = { memberId ->
            navController.navigate(Screen.MemberDetail(memberId)) {
              // Clear the AddMemberForm from back stack, AdminContainer will preserve its tab state
              popUpTo<Screen.AdminContainer> {
                inclusive = false
              }
            }
          },
        )
      }
      composable<Screen.EditMemberForm> {
        val viewModel = koinInject<AdminViewModel>()
        val route = it.toRoute<Screen.EditMemberForm>()
        val memberId = route.memberId

        AddMemberFormScreen(
          viewModel = viewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          memberId = memberId,
          onNavigateToMemberDetail = { memberId ->
            navController.navigate(Screen.MemberDetail(memberId)) {
              // Clear the EditMemberForm from back stack, AdminContainer will preserve its tab state
              popUpTo<Screen.AdminContainer> {
                inclusive = false
              }
            }
          },
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
          onNavigateBack = {
            navController.popBackStack()
          },
          onNavigateToEdit = { memberId ->
            navController.navigate(Screen.EditMemberForm(memberId))
          }
        )
      }
      composable<Screen.CreateFamilyForm> {
        val familyViewModel = koinInject<FamilyViewModel>()
        CreateAryaParivarFormScreen(
          viewModel = familyViewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          onFamilyCreated = { familyId ->
            navController.navigate(Screen.FamilyDetail(familyId)) {
              popUpTo(Screen.AdminContainer(0))
            }
          }
        )
      }
      composable<Screen.EditFamilyForm> {
        val familyId = it.toRoute<Screen.EditFamilyForm>().familyId
        val familyViewModel = koinInject<FamilyViewModel>()
        CreateAryaParivarFormScreen(
          viewModel = familyViewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          onFamilyCreated = { familyId ->
            navController.navigate(Screen.FamilyDetail(familyId)) {
              popUpTo(Screen.AdminContainer(0))
            }
          },
          editingFamilyId = familyId
        )
      }
      composable<Screen.FamilyDetail> {
        val familyId = it.toRoute<Screen.FamilyDetail>().familyId
        val familyViewModel = koinInject<FamilyViewModel>()

        // Navigate to AboutSection if user logs out
        LaunchedEffect(isLoggedIn) {
          if (!isLoggedIn) {
            navController.navigate(Screen.AboutSection) {
              // Clear the back stack so user can't navigate back to admin
              popUpTo(0) { inclusive = true }
            }
          }
        }

        FamilyDetailScreen(
          familyId = familyId,
          viewModel = familyViewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          onEditFamily = {
            navController.navigate(Screen.EditFamilyForm(familyId))
          }
        )
      }
    }
    navigation<Screen.AryaNirmanSection>(startDestination = Screen.AryaNirmanHome) {
      composable<Screen.AryaNirmanHome> {
        val viewModel = koinInject<AryaNirmanViewModel>()
        AryaNirmanHomeScreen(
          viewModel,
          onNavigateToRegistrationForm = { id, capacity ->
            navController.navigate(Screen.AryaNirmanRegistrationForm(id, capacity))
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
          onNavigateBack = {
            navController.popBackStack()
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
          onNavigateToCreateOverview = { activityId, existingOverview, existingMediaUrls ->
            // Navigate to create/edit overview form
            navController.navigate(Screen.CreateOverviewForm(activityId, existingOverview, existingMediaUrls))
          },
          viewModel = viewModel
        )
      }
      composable<Screen.CreateOverviewForm> {
        val args = it.toRoute<Screen.CreateOverviewForm>()
        val viewModel = koinInject<ActivitiesViewModel>()
        CreateOverviewFormScreen(
          activityId = args.activityId,
          existingOverview = args.existingOverview,
          existingMediaUrls = args.existingMediaUrls,
          onNavigateBack = {
            navController.popBackStack()
          },
          onSuccess = {
            // Navigate back to activity details after successful save
            navController.navigate(Screen.ActivityDetails(args.activityId)) {
              popUpTo(Screen.ActivityDetails(args.activityId)) { inclusive = true }
            }
          },
          viewModel = viewModel
        )
      }
    }
    navigation<Screen.OrgsSection>(startDestination = Screen.Orgs) {
      composable<Screen.Orgs> {
        val viewModel = koinInject<OrganisationsViewModel>()
        OrgsScreen(
          onNavigateToOrgDetails = {
            navController.navigate(Screen.OrgDetails(it))
          },
          onNavigateToCreateOrganisation = {
            navController.navigate(Screen.NewOrganisationForm(it))
          },
          viewModel
        )
      }
      composable<Screen.OrgDetails> {
        val orgId = it.toRoute<Screen.OrgDetails>().organisationId
        val orgViewModel = koinInject<OrganisationsViewModel>()
        val adminViewModel = koinInject<AdminViewModel>()

        // Load members when screen is accessed
        LaunchedEffect(Unit) {
          adminViewModel.loadMembers()
        }

        // Collect admin state for search results
        val adminUiState by adminViewModel.membersUiState.collectAsState()

        OrgDetailScreen(
          id = orgId,
          viewModel = orgViewModel,
          searchMembers = { query ->
            // Use the search results from AdminViewModel
            // This will contain fresh results from server
            if (query.isNotBlank()) {
              adminUiState.searchResults.map { memberShort ->
                Member(
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
          allMembers =
            adminUiState.members.map { memberShort ->
              Member(
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
      composable<Screen.NewOrganisationForm> {
        val priority = it.toRoute<Screen.NewOrganisationForm>().priority
        val viewModel = koinInject<OrganisationsViewModel>()
        val adminViewModel = koinInject<AdminViewModel>()

        // Load members when screen is accessed
        LaunchedEffect(Unit) {
          adminViewModel.loadMembers()
        }

        // Collect admin state for search results
        val adminUiState by adminViewModel.membersUiState.collectAsState()

        NewOrganisationFormScreen(
          priority = priority,
          viewModel = viewModel,
          onOrganisationCreated = { organisationId ->
            // Navigate to organisation details after successful creation
            navController.navigate(Screen.OrgDetails(organisationId)) {
              popUpTo(Screen.Orgs)
            }
          },
          onCancel = {
            navController.popBackStack()
          },
          searchMembers = { query ->
            // Use the search results from AdminViewModel
            if (query.isNotBlank()) {
              adminUiState.searchResults.map { memberShort ->
                Member(
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
          allMembers =
            adminUiState.members.map { memberShort ->
              Member(
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
    navigation<Screen.AryaSamajSection>(startDestination = Screen.AryaSamajHome) {
      composable<Screen.AryaSamajHome> {
        val aryaSamajViewModel = koinInject<AryaSamajViewModel>()
        AryaSamajHomeScreen(
          viewModel = aryaSamajViewModel,
          onNavigateToDetail = { aryaSamajId ->
            navController.navigate(Screen.AryaSamajDetail(aryaSamajId))
          }
        )
      }
      composable<Screen.AddAryaSamajForm> {
        val viewModel = koinInject<AryaSamajViewModel>()
        val adminViewModel = koinInject<AdminViewModel>()

        // Load members when screen is accessed
        LaunchedEffect(Unit) {
          adminViewModel.loadMembers()
        }

        // Collect admin state for search results
        val adminUiState by adminViewModel.membersUiState.collectAsState()

        AddAryaSamajFormScreen(
          viewModel = viewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          searchMembers = { query ->
            if (query.isNotBlank()) {
              adminUiState.searchResults.map { memberShort ->
                Member(
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
          allMembers =
            adminUiState.members.map { memberShort ->
              Member(
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
      composable<Screen.AryaSamajDetail> {
        val aryaSamajId = it.toRoute<Screen.AryaSamajDetail>().aryaSamajId
        val viewModel = koinInject<AryaSamajViewModel>()
        val adminViewModel = koinInject<AdminViewModel>()

        // Load members when screen is accessed
        LaunchedEffect(Unit) {
          adminViewModel.loadMembers()
        }

        // Collect admin state for search results
        val adminUiState by adminViewModel.membersUiState.collectAsState()

        AryaSamajDetailScreen(
          aryaSamajId = aryaSamajId,
          viewModel = viewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          searchMembers = { query ->
            if (query.isNotBlank()) {
              adminUiState.searchResults.map { memberShort ->
                Member(
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
          allMembers =
            adminUiState.members.map { memberShort ->
              Member(
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
      composable<Screen.EditAryaSamajForm> {
        val aryaSamajId = it.toRoute<Screen.EditAryaSamajForm>().aryaSamajId
        val viewModel = koinInject<AryaSamajViewModel>()
        val adminViewModel = koinInject<AdminViewModel>()

        // Load members when screen is accessed
        LaunchedEffect(Unit) {
          adminViewModel.loadMembers()
        }

        // Collect admin state for search results
        val adminUiState by adminViewModel.membersUiState.collectAsState()

        AddAryaSamajFormScreen(
          viewModel = viewModel,
          onNavigateBack = {
            navController.popBackStack()
          },
          searchMembers = { query ->
            if (query.isNotBlank()) {
              adminUiState.searchResults.map { memberShort ->
                Member(
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
          allMembers =
            adminUiState.members.map { memberShort ->
              Member(
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
          },
          isEditMode = true,
          aryaSamajId = aryaSamajId
        )
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
    navigation<Screen.KshatraTrainingSection>(startDestination = Screen.KshatraTrainingHome) {
      composable<Screen.KshatraTrainingHome> {
        PhysicalTrainingForm(gender = Gender.BOY, onBack = {})
      }
    }
    navigation<Screen.ChatraTrainingSection>(startDestination = Screen.ChatraTrainingHome) {
      composable<Screen.ChatraTrainingHome> {
        PhysicalTrainingForm(gender = Gender.GIRL, onBack = {})
      }
    }
  }
}

@Composable
fun PhysicalTrainingForm(
  gender: Gender,
  onBack: (Map<String, String>) -> Unit
) {
  // Implementation of PhysicalTrainingForm
}
