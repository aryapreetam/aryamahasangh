package com.aryamahasangh.viewmodel

import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.ErrorHandler
import com.aryamahasangh.domain.error.getUserMessage
import com.aryamahasangh.domain.error.toAppError
import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.repository.AboutUsRepository
import com.aryamahasangh.util.NetworkUtils
import com.aryamahasangh.util.Result

/**
 * UI state for the About Us screens
 */
data class AboutUsUiState(
  val organisation: OrganisationDetail? = null,
  val isLoading: Boolean = false,
  val error: String? = null,
  val appError: AppError? = null
)

/**
 * ViewModel for the About Us and Detailed About Us screens
 */
class AboutUsViewModel(
  private val aboutUsRepository: AboutUsRepository
) : BaseViewModel<AboutUsUiState>(AboutUsUiState()) {
  init {
    loadOrganisationDetails("आर्य महासंघ")
  }

  /**
   * Load organisation details by name
   */
  fun loadOrganisationDetails(name: String) {
    launch {
      // Clear any previous errors first
      updateState { it.copy(isLoading = true, error = null, appError = null) }

      aboutUsRepository.getOrganisationByName(name).collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null, appError = null) }
          }
          is Result.Success -> {
            updateState {
              it.copy(
                organisation = result.data,
                isLoading = false,
                error = null,
                appError = null
              )
            }
          }
          is Result.Error -> {
            // Enhanced error handling with better network detection
            val appError =
              when {
                NetworkUtils.isLikelyNetworkIssue(result.message, result.exception) ->
                  AppError.NetworkError.NoConnection

                result.exception != null ->
                  result.exception.toAppError()

                else ->
                  AppError.UnknownError(result.message)
              }

            ErrorHandler.logError(appError, "AboutUsViewModel.loadOrganisationDetails")

            updateState {
              it.copy(
                isLoading = false,
                error = appError.getUserMessage(),
                appError = appError
              )
            }
          }
        }
      }
    }
  }

  /**
   * Clear error state
   */
  fun clearError() {
    updateState { it.copy(error = null, appError = null) }
  }
}
