package com.aryamahasangh

import androidx.compose.ui.test.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CrossPlatformUiSmokeTest {
  @Test
  fun smoke_counter_increments_once() = runComposeUiTest {
    val viewModel = CounterViewModel()

    setContent { CounterScreen(viewModel) }

    onNodeWithTag("counterText").assertTextEquals("0")
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("counterText").assertTextEquals("1")
  }

  @Test
  fun smoke_counter_increments_twice() = runComposeUiTest {
    val viewModel = CounterViewModel()

    setContent { CounterScreen(viewModel) }

    onNodeWithTag("counterText").assertTextEquals("0")
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("counterText").assertTextEquals("2")
  }

  @Test
  fun smoke_counter_increments_thrice() = runComposeUiTest {
    val viewModel = CounterViewModel()

    setContent { CounterScreen(viewModel) }

    onNodeWithTag("counterText").assertTextEquals("0")
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("counterText").assertTextEquals("3")
  }

  @Test
  fun smoke_counter_increments_forth() = runComposeUiTest {
    val viewModel = CounterViewModel()

    setContent { CounterScreen(viewModel) }

    onNodeWithTag("counterText").assertTextEquals("0")
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("counterText").assertTextEquals("4")
  }

  @Test
  fun smoke_counter_increments_fifth() = runComposeUiTest {
    val viewModel = CounterViewModel()

    setContent { CounterScreen(viewModel) }

    onNodeWithTag("counterText").assertTextEquals("0")
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("incrementButton").performClick()
    onNodeWithTag("counterText").assertTextEquals("5")
  }

//  @Test
//  fun smoke_counter_increments_fifty() = runComposeUiTest {
//    val viewModel = CounterViewModel()
//
//    setContent { CounterScreen(viewModel) }
//
//    onNodeWithTag("counterText").assertTextEquals("0")
//    for(i in 1..50) onNodeWithTag("incrementButton").performClick()
//
//    onNodeWithTag("counterText").assertTextEquals("50")
//  }
}
