package com.aryamahasangh.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global message types for the app
 */
sealed class GlobalMessage(
  val message: String,
  val duration: GlobalMessageDuration = GlobalMessageDuration.SHORT
) {
  data class Success(
    val text: String,
    val dur: GlobalMessageDuration = GlobalMessageDuration.SHORT
  ) : GlobalMessage(text, dur)

  data class Error(
    val text: String,
    val dur: GlobalMessageDuration = GlobalMessageDuration.LONG
  ) : GlobalMessage(text, dur)

  data class Info(
    val text: String,
    val dur: GlobalMessageDuration = GlobalMessageDuration.SHORT
  ) : GlobalMessage(text, dur)
}

/**
 * Duration enum for global messages
 */
enum class GlobalMessageDuration {
  SHORT,    // ~4 seconds
  LONG,     // ~10 seconds
  INDEFINITE // Until user dismisses
}

/**
 * Global singleton manager for app-wide messages that persist across navigation.
 * This solves the UX issue where form submissions wait for snackbar before navigating.
 */
object GlobalMessageManager {
  private val _currentMessage = MutableStateFlow<GlobalMessage?>(null)
  val currentMessage: StateFlow<GlobalMessage?> = _currentMessage.asStateFlow()

  /**
   * Show a success message
   */
  fun showSuccess(
    message: String,
    duration: GlobalMessageDuration = GlobalMessageDuration.SHORT
  ) {
    _currentMessage.value = GlobalMessage.Success(message, duration)
  }

  /**
   * Show an error message
   */
  fun showError(
    message: String,
    duration: GlobalMessageDuration = GlobalMessageDuration.LONG
  ) {
    _currentMessage.value = GlobalMessage.Error(message, duration)
  }

  /**
   * Show an info message
   */
  fun showInfo(
    message: String,
    duration: GlobalMessageDuration = GlobalMessageDuration.SHORT
  ) {
    _currentMessage.value = GlobalMessage.Info(message, duration)
  }

  /**
   * Clear the current message
   */
  fun clearMessage() {
    _currentMessage.value = null
  }

  /**
   * Check if there's a current message
   */
  fun hasMessage(): Boolean = _currentMessage.value != null
}
