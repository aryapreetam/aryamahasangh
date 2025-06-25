package org.aryamahasangh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.aryamahasangh.domain.error.AppError
import org.aryamahasangh.domain.error.ErrorHandler
import org.aryamahasangh.domain.error.toAppError
import org.aryamahasangh.util.Result

/**
 * Base ViewModel class with standardized error handling
 */
abstract class BaseViewModel<S>(initialState: S) : ViewModel() {
  // Private backing field for UI state
  private val _uiState = MutableStateFlow(initialState)

  // Public immutable StateFlow exposed to the UI
  val uiState: StateFlow<S> = _uiState.asStateFlow()

  /**
   * Exception handler for coroutines
   */
  private val exceptionHandler =
    CoroutineExceptionHandler { _, exception ->
      val appError = exception.toAppError()
      ErrorHandler.logError(appError, this::class.simpleName ?: "BaseViewModel")
    }

  /**
   * Updates the UI state using the provided update function
   */
  protected fun updateState(update: (S) -> S) {
    _uiState.value = update(_uiState.value)
  }

  /**
   * Launches a coroutine in the ViewModel scope with proper error handling
   */
  protected fun launch(block: suspend () -> Unit) {
    viewModelScope.launch(exceptionHandler) {
      try {
        block()
      } catch (exception: Exception) {
        val appError = exception.toAppError()
        ErrorHandler.logError(appError, this@BaseViewModel::class.simpleName ?: "BaseViewModel")
      }
    }
  }

  /**
   * Get current state value
   */
  protected val currentState: S
    get() = _uiState.value
}

/**
 * Extension functions for handling Result with proper error conversion
 */
fun <T> Result<T>.handleResult(
  onLoading: () -> Unit = {},
  onSuccess: (T) -> Unit,
  onError: (AppError) -> Unit
) {
  when (this) {
    is Result.Loading -> onLoading()
    is Result.Success -> onSuccess(data)
    is Result.Error -> {
      val appError = exception?.toAppError() ?: AppError.UnknownError(message)
      onError(appError)
    }
  }
}

/**
 * Common UI state interface that includes error handling
 */
interface ErrorState {
  val error: String?
  val appError: AppError?
  val isLoading: Boolean
}

/**
 * Extension function to handle errors in UI state
 */
fun <T : ErrorState> T.withError(appError: AppError): T {
  // This is a workaround since we can't modify sealed classes directly
  // Each implementing class should override this
  return this
}

/**
 * Extension function to clear errors in UI state
 */
fun <T : ErrorState> T.clearError(): T {
  // This is a workaround since we can't modify sealed classes directly
  // Each implementing class should override this
  return this
}
