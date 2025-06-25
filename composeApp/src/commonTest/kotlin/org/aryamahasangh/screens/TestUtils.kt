package org.aryamahasangh.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.aryamahasangh.navigation.LocalSnackbarHostState
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * A test wrapper that provides all necessary CompositionLocals for UI tests.
 * This ensures tests don't fail due to missing dependencies.
 */
@Composable
fun TestWrapper(content: @Composable () -> Unit) {
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
 * Simple test runner that uses runComposeUiTest
 */
@OptIn(ExperimentalTestApi::class)
inline fun runUiTest(crossinline testBody: ComposeUiTest.() -> Unit) {
  runComposeUiTest {
    testBody()
  }
}
