package org.aryamahasangh.screens

import androidx.compose.ui.test.*
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.features.organisations.OrganisationDetail
import org.aryamahasangh.repository.AboutUsRepository
import org.aryamahasangh.test.UiTest
import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.AboutUsViewModel
import kotlin.test.Test

/**
 * Common UI test for verifying the About Us screen across all platforms
 */
@OptIn(ExperimentalTestApi::class)
@UiTest
class AboutUsScreenTest {

  @Test
  fun aboutUsScreenDisplaysText() = runUiTest {
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
  fun clickingTextNavigatesToDetailedAboutUs() = runUiTest {
    // Create a fake repository that returns a predefined organization
    val fakeRepository = FakeAboutUsRepository()

    // Create the view model with the fake repository
    val viewModel = AboutUsViewModel(fakeRepository)

    // Track if navigation was triggered
    var navigationTriggered = false

    // Set up the content with the AboutUs composable wrapped in TestWrapper
    setContent {
      TestWrapper {
        AboutUs(
          showDetailedAboutUs = { navigationTriggered = true },
          viewModel = viewModel
        )
      }
    }

    // Wait for the content to load
    waitForIdle()

    // Click on the text
    onNodeWithText("सनातन धर्म का साक्षात् प्रतिनिधि", substring = true).performClick()

    // Verify navigation was triggered
    kotlin.test.assertTrue(navigationTriggered, "Navigation to DetailedAboutUs was not triggered")

    // Now set up the DetailedAboutUs screen to verify it displays correctly
    setContent {
      TestWrapper {
        DetailedAboutUs(viewModel = viewModel)
      }
    }

    // Wait for the content to load
    waitForIdle()

    // Verify that "आचार्य हनुमत प्रसाद" is displayed
    onNodeWithText("आचार्य हनुमत प्रसाद").assertIsDisplayed()
  }
}

/**
 * A fake implementation of AboutUsRepository for testing
 */
class FakeAboutUsRepository : AboutUsRepository {
  override fun getOrganisationByName(name: String) = flow {
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
}
