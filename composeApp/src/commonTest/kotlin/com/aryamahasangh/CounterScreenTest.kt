package com.aryamahasangh

import androidx.compose.ui.test.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CounterScreenTest {
  @Test
  fun counter_increments_when_button_clicked() = runComposeUiTest {
    val viewModel = CounterViewModel()

    setContent {
      CounterScreen(viewModel = viewModel)
    }

    // Initial value
    onNodeWithTag("counterText").assertTextEquals("0")

    // Click and verify increment
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("counterText").assertTextEquals("1")
  }

  @Test
  fun counter_increments_twice_when_button_clicked_twice() = runComposeUiTest {
    val viewModel = CounterViewModel()

    setContent {
      CounterScreen(viewModel = viewModel)
    }

    // Initial value
    onNodeWithTag("counterText").assertTextEquals("0")

    // Click and verify increment
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("counterText").assertTextEquals("2")
  }
}
