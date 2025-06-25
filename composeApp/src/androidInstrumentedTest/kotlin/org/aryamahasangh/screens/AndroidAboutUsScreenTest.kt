package org.aryamahasangh.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.features.organisations.OrganisationDetail
import org.aryamahasangh.navigation.LocalSnackbarHostState
import org.aryamahasangh.repository.AboutUsRepository
import org.aryamahasangh.test.UiTest
import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.AboutUsViewModel
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * A test wrapper that provides all necessary CompositionLocals for UI tests.
 * This ensures tests don't fail due to missing dependencies.
 */
@Composable
private fun TestWrapper(content: @Composable () -> Unit) {
  // Create instances of all required dependencies
  val snackbarHostState = SnackbarHostState()

  // Wrap in KoinApplication for test isolation
  KoinApplication(application = {
    modules(
      module {
        // Add any test-specific dependencies here
      }
    )
  }) {
    // Provide all CompositionLocals that might be needed
    CompositionLocalProvider(
      LocalSnackbarHostState provides snackbarHostState
    ) {
      content()
    }
  }
}

/**
 * Android instrumented test for the About Us screen.
 * This is needed because runComposeUiTest from commonTest doesn't work with Android instrumented tests.
 */
@UiTest
class AndroidAboutUsScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun aboutUsScreenDisplaysText() {
    // Create a fake repository that returns a predefined organization
    val fakeRepository = AndroidFakeAboutUsRepository()

    // Create the view model with the fake repository
    val viewModel = AboutUsViewModel(fakeRepository)

    // Set up the content with the AboutUs composable wrapped in TestWrapper
    composeTestRule.setContent {
      TestWrapper {
        AboutUs(
          showDetailedAboutUs = {},
          viewModel = viewModel
        )
      }
    }

    // Wait for the content to load
    composeTestRule.waitForIdle()

    // Verify that the logo is displayed
    composeTestRule.onNodeWithContentDescription("logo आर्य महासंघ").assertIsDisplayed()

    // Verify that the text is displayed
    composeTestRule.onNodeWithText("सनातन धर्म का साक्षात् प्रतिनिधि", substring = true).assertIsDisplayed()
  }

  @Test
  fun aboutUsScreenNavigatesToDetailedView() {
    // Create a fake repository that returns a predefined organization
    val fakeRepository = AndroidFakeAboutUsRepository()
    val viewModel = AboutUsViewModel(fakeRepository)
    var navigationTriggered = false

    composeTestRule.setContent {
      TestWrapper {
        AboutUs(
          showDetailedAboutUs = { navigationTriggered = true },
          viewModel = viewModel
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("सनातन धर्म का साक्षात् प्रतिनिधि", substring = true).performClick()
    assert(navigationTriggered) { "Navigation callback was not triggered" }
  }
}

/**
 * Android-specific fake implementation of AboutUsRepository for testing
 */
class AndroidFakeAboutUsRepository : AboutUsRepository {
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
}
