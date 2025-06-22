package org.aryamahasangh.viewmodel

import org.aryamahasangh.domain.error.AppError
import org.aryamahasangh.domain.error.ErrorHandler
import org.aryamahasangh.domain.error.getUserMessage
import org.aryamahasangh.fragment.OrganisationalActivityShort
import org.aryamahasangh.repository.JoinUsRepository

/**
 * UI state for the Join Us screen
 */
data class JoinUsUiState(
  val activities: List<OrganisationalActivityShort>? = null,
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null,
  val labelState: LabelState = LabelState()
) : ErrorState

data class LabelState(
  val label: String = "आर्य महासंघ से जुड़ें",
  val isUpdating: Boolean = false,
  val error: String? = null,
  val appError: AppError? = null,
  val editMode: Boolean = false
)

/**
 * ViewModel for the Join Us screen
 */
class JoinUsViewModel(
  private val joinUsRepository: JoinUsRepository
) : BaseViewModel<JoinUsUiState>(JoinUsUiState()) {
  // Add this function to your ViewModel
  fun setEditMode(enabled: Boolean) {
    updateState { it.copy(labelState = it.labelState.copy(editMode = enabled)) }
  }

  fun loadJoinUsLabel() {
    launch {
      joinUsRepository.getJoinUsLabel().collect { result ->
        result.handleResult(
          onLoading = {
            updateState {
              it.copy(
                labelState = it.labelState.copy(isUpdating = true, error = null, appError = null)
              )
            }
          },
          onSuccess = { label ->
            updateState {
              it.copy(
                labelState = LabelState(
                  label = label,
                  isUpdating = false,
                  error = null,
                  appError = null,
                  editMode = false
                ),
                isLoading = false,
                error = null,
                appError = null
              )
            }
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "JoinUsViewModel.loadJoinUsLabel")
            updateState {
              it.copy(
                labelState = it.labelState.copy(
                  isUpdating = false,
                  error = appError.getUserMessage(),
                  appError = appError,
                  editMode = false
                )
              )
            }
          }
        )
      }
    }
  }

  fun updateJoinUsLabel(label: String) {
    launch {
      updateState { it.copy(labelState = it.labelState.copy(isUpdating = true, editMode = true)) }
      joinUsRepository.updateLabel(label).collect { result ->
        result.handleResult(
          onLoading = {
            updateState {
              it.copy(labelState = it.labelState.copy(isUpdating = true, error = null, appError = null))
            }
          },
          onSuccess = { _ ->
            updateState {
              it.copy(labelState = it.labelState.copy(isUpdating = false, editMode = false))
            }
            loadJoinUsLabel()
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "JoinUsViewModel.updateJoinUsLabel")
            updateState {
              it.copy(
                labelState = it.labelState.copy(
                  isUpdating = false,
                  error = appError.getUserMessage(),
                  appError = appError,
                  editMode = true
                )
              )
            }
          }
        )
      }
    }
  }

  /**
   * Load filtered activities
   */
  fun loadFilteredActivities(
    state: String,
    district: String
  ) {
    launch {
      updateState { it.copy(isLoading = true, error = null, appError = null) }

      joinUsRepository.getFilteredActivities(state, district).collect { result ->
        result.handleResult(
          onLoading = {
            updateState { it.copy(isLoading = true, error = null, appError = null) }
          },
          onSuccess = { activities ->
            updateState {
              it.copy(
                activities = activities,
                isLoading = false,
                error = null,
                appError = null
              )
            }
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "JoinUsViewModel.loadFilteredActivities")
            updateState {
              it.copy(
                isLoading = false,
                error = appError.getUserMessage(),
                appError = appError
              )
            }
          }
        )
      }
    }
  }
}
