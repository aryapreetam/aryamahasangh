package com.aryamahasangh.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.aryamahasangh.navigation.LocalSnackbarHostState
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * A test wrapper that provides all necessary CompositionLocals for UI tests.
 * This ensures tests don't fail due to missing dependencies.
 */
@Composable
fun TestWrapper(
  vararg koinModules: org.koin.core.module.Module = arrayOf(),
  content: @Composable () -> Unit
) {
  // Create instances of all required dependencies
  val snackbarHostState = SnackbarHostState()

  // Provide a simple LifecycleOwner so components relying on LocalLifecycleOwner (NavController, etc.) work in tests
  val lifecycleOwner = remember {
    object : androidx.lifecycle.LifecycleOwner {
      override val lifecycle: androidx.lifecycle.Lifecycle = androidx.lifecycle.LifecycleRegistry(this).apply {
        handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
      }
    }
  }

  // Provide CompositionLocals first so anything inside composables can read them
  // Tests should start/stop Koin globally when they need DI; avoid starting Koin inside TestWrapper
  CompositionLocalProvider(
    LocalSnackbarHostState provides snackbarHostState,
    androidx.lifecycle.compose.LocalLifecycleOwner provides lifecycleOwner
  ) {
    content()
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
