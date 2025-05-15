package org.aryamahasangh.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality for all ViewModels
 */
abstract class BaseViewModel<S>(initialState: S) : ViewModel() {

  // Private backing field for UI state
  private val _uiState = MutableStateFlow(initialState)

  // Public immutable StateFlow exposed to the UI
  val uiState: StateFlow<S> = _uiState.asStateFlow()

  /**
   * Updates the UI state using the provided update function
   */
  protected fun updateState(update: (S) -> S) {
    _uiState.value = update(_uiState.value)
  }

  /**
   * Launches a coroutine in the ViewModel scope
   */
  protected fun launch(block: suspend CoroutineScope.() -> Unit) {
    viewModelScope.launch {
      block()
    }
  }
}