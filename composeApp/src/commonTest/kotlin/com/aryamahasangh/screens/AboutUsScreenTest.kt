package com.aryamahasangh.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aryamahasangh.features.about_us.domain.repository.AboutUsRepository
import com.aryamahasangh.features.about_us.domain.repository.OrganisationName
import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationByNameUseCase
import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationNamesUseCase
import com.aryamahasangh.features.about_us.ui.AboutUs
import com.aryamahasangh.features.about_us.ui.AboutUsViewModel
import com.aryamahasangh.navigation.Screen
import com.aryamahasangh.test.UiTest
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.flow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertTrue
import com.aryamahasangh.components.OrganisationDetail as OrganisationDetailView

/**
 * Common UI test for verifying the About Us screen across all platforms
 */
@OptIn(ExperimentalTestApi::class)
@UiTest
class AboutUsScreenTest {
  @Test
  fun aboutUsScreenDisplaysText() =
    runUiTest {
      // Use a Koin test module so tests use the same DI wiring as the app
      val testModule = module {
        single<AboutUsRepository> { FakeAboutUsRepository() }
        single { GetOrganisationByNameUseCase(get()) }
        single { GetOrganisationNamesUseCase(get()) }
        single { AboutUsViewModel(get(), get()) }
      }

      val koinApp = startKoin { modules(testModule) }
      try {
        val viewModel = koinApp.koin.get<AboutUsViewModel>()

        // Set up the content with the AboutUs composable wrapped in TestWrapper
        setContent {
          TestWrapper {
            AboutUs(
              showDetailedAboutUs = {},
              viewModel = viewModel
            )
          }
        }
      } finally {
        stopKoin()
      }

      // Wait for the content to load
      waitForIdle()

      // Verify that the logo is displayed
      onNodeWithContentDescription("logo आर्य महासंघ").assertIsDisplayed()

      // Verify that the text is displayed
      onNodeWithText("सनातन धर्म का साक्षात् प्रतिनिधि", substring = true).assertIsDisplayed()
    }

  @Test
  fun clickingTextNavigatesToDetailedAboutUs() =
    runUiTest {
      // Use Koin test module for DI
      val testModule = module {
        single<AboutUsRepository> { FakeAboutUsRepository() }
        single { GetOrganisationByNameUseCase(get()) }
        single { GetOrganisationNamesUseCase(get()) }
        single { AboutUsViewModel(get(), get()) }
      }

      val koinApp = startKoin { modules(testModule) }
      try {
        val viewModel = koinApp.koin.get<AboutUsViewModel>()

        var navigationTriggered = false

        setContent {
          TestWrapper {
            AboutUs(
              showDetailedAboutUs = { navigationTriggered = true },
              viewModel = viewModel
            )
          }
        }

        // Wait for content to load
        waitForIdle()

        // Click on the description text (using the content description of the parent column)
        onNode(
          hasClickAction() and hasAnyDescendant(hasText("सनातन धर्म का साक्षात् प्रतिनिधि", substring = true)),
          useUnmergedTree = true
        ).performClick()

        assertTrue(navigationTriggered, "Navigation to organisation details was not triggered")
      } finally {
        stopKoin()
      }
    }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun aboutUsDetailedShowsExecutiveSection() = runUiTest {
    // Use Koin test module so the test uses same DI wiring as the app
    val testModule = module {
      single<AboutUsRepository> { FakeAboutUsRepository() }
      single { com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationByNameUseCase(get()) }
      single { com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationNamesUseCase(get()) }
      single { com.aryamahasangh.features.about_us.ui.AboutUsViewModel(get(), get()) }
    }

    val koinApp = startKoin { modules(testModule) }
    try {
      val viewModel = koinApp.koin.get<com.aryamahasangh.features.about_us.ui.AboutUsViewModel>()

      // Compose state to request navigation from test; using LaunchedEffect below ensures navigation runs on UI thread
      val shouldNavigate = mutableStateOf(false)

      // Provide a dummy keyPeople list so executive section is rendered regardless of login state
      val dummyKeyPeople = listOf(
        com.aryamahasangh.features.organisations.OrganisationalMember(
          id = "om-1",
          member = com.aryamahasangh.features.activities.Member(
            id = "m-1",
            name = "परीक्षण सदस्य"
          ),
          post = "अध्यक्ष",
          priority = 1
        )
      )

      setContent {
        TestWrapper {
          val navController = rememberNavController()

          // Observe the test-controlled flag and navigate from the UI/main thread when requested
          LaunchedEffect(shouldNavigate.value) {
            if (shouldNavigate.value) {
              navController.navigate(Screen.AboutUsDetails("test-id"))
            }
          }

          NavHost(navController = navController, startDestination = Screen.AboutUs) {
            composable<Screen.AboutUs> {
              AboutUs(
                showDetailedAboutUs = { organisationId ->
                  navController.navigate(Screen.AboutUsDetails(organisationId))
                },
                viewModel = viewModel
              )
            }

            composable<Screen.AboutUsDetails> {
              val organisationId = it.toRoute<Screen.AboutUsDetails>().organisationId

              OrganisationDetailView(
                id = organisationId,
                name = "आर्य महासंघ",
                logo = null,
                description = "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है।",
                keyPeople = dummyKeyPeople,
                isLoggedIn = false
              )
            }
          }
        }
      }

      waitForIdle()

      // Instead of performing a click (which can be flaky on desktop), set the flag to trigger navigation on the UI thread
      onNodeWithTag("about_intro_paragraph", useUnmergedTree = true).assertExists()

      runOnIdle { shouldNavigate.value = true }

      waitForIdle()

      // Scroll to the executive section header and verify it's displayed
      onNode(hasScrollAction())
        .performScrollToNode(hasText("कार्यकारिणी/पदाधिकारी", substring = true))
      onNodeWithText("कार्यकारिणी/पदाधिकारी", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    } finally {
      stopKoin()
    }
  }

  @Test
  fun organisationDetailComponentShowsExecutiveSection() = runUiTest {
       // Render OrganisationDetailView directly to verify the executive section header is present
       val dummyKeyPeople = listOf(
         com.aryamahasangh.features.organisations.OrganisationalMember(
           id = "om-1",
           member = com.aryamahasangh.features.activities.Member(
             id = "m-1",
             name = "परीक्षण सदस्य"
           ),
           post = "अध्यक्ष",
           priority = 1
         )
       )
       setContent {
         TestWrapper {
           OrganisationDetailView(
            id = "test-id",
            name = "आर्य महासंघ",
            logo = null,
            description = "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है।",
            keyPeople = dummyKeyPeople,
            isLoggedIn = false
          )
         }
       }

       waitForIdle()

       // Scroll to the executive section header and verify it's displayed
       onNode(hasScrollAction())
         .performScrollToNode(hasText("कार्यकारिणी/पदाधिकारी", substring = true))
       onNodeWithText("कार्यकारिणी/पदाधिकारी", substring = true, useUnmergedTree = true)
         .assertIsDisplayed()
     }
 }

/**
 * A fake implementation of AboutUsRepository for testing
 */
class FakeAboutUsRepository : AboutUsRepository {
  override fun getOrganisationByName(name: String) =
    flow {
      emit(
        Result.Success(
          com.aryamahasangh.features.organisations.OrganisationDetail(
            id = "test-id",
            name = "आर्य महासंघ",
            description = "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है।",
            logo = null,
            members = emptyList()
          )
        )
      )
    }

  override fun getOrganisationNames() =
    flow {
      emit(
        Result.Success(
          listOf(
            OrganisationName(id = "test-org-1", name = "Test Organisation 1"),
            OrganisationName(id = "test-org-2", name = "Test Organisation 2"),
            OrganisationName(id = "test-org-3", name = "Test Organisation 3")
          )
        )
      )
    }
}
