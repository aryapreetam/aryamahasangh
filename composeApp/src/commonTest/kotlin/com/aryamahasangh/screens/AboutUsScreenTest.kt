package com.aryamahasangh.screens

import androidx.compose.ui.test.*
import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.repository.AboutUsRepository
import com.aryamahasangh.repository.OrganisationName
import com.aryamahasangh.test.UiTest
import com.aryamahasangh.util.Result
import com.aryamahasangh.viewmodel.AboutUsViewModel
import kotlinx.coroutines.flow.flow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Common UI test for verifying the About Us screen across all platforms
 */
@OptIn(ExperimentalTestApi::class)
@UiTest
class AboutUsScreenTest {
  @Test
  fun aboutUsScreenDisplaysText() =
    runUiTest {
      // Create a fake repository that returns a predefined organization
      val fakeRepository = FakeAboutUsRepository()

      // Create the view model with the fake repository
      val viewModel = AboutUsViewModel(fakeRepository)

      // Set up the content with the AboutUs composable wrapped in TestWrapper
      setContent {
        TestWrapper {
          AboutUs(
            showDetailedAboutUs = {},
            viewModel = viewModel
          )
        }
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
      val fakeRepository = FakeAboutUsRepository()
      val viewModel = AboutUsViewModel(fakeRepository)

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
      onNode(hasClickAction() and hasAnyDescendant(hasText("सनातन धर्म का साक्षात् प्रतिनिधि", substring = true)))
        .performClick()

      assertTrue(navigationTriggered, "Navigation to organisation details was not triggered")
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
          OrganisationDetail(
            id = "test-id",
            name = "आर्य महासंघ",
            description = "Test description",
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
