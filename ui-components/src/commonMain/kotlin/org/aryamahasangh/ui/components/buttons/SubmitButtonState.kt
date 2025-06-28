package org.aryamahasangh.ui.components.buttons

/**
 * Represents the different states of a submit button
 */
enum class SubmitButtonState {
  /**
   * Initial state - button is ready to be clicked
   */
  INITIAL,

  /**
   * Submitting state - operation is in progress
   */
  SUBMITTING,

  /**
   * Success state - operation completed successfully
   */
  SUCCESS,

  /**
   * Error state - operation failed
   */
  ERROR
}
