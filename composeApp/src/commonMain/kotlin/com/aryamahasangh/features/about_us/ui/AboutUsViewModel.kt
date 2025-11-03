package com.aryamahasangh.features.about_us.ui

import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.ErrorHandler
import com.aryamahasangh.domain.error.getUserMessage
import com.aryamahasangh.domain.error.toAppError
import com.aryamahasangh.features.about_us.domain.repository.OrganisationName
import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationByNameUseCase
import com.aryamahasangh.features.about_us.domain.usecase.GetOrganisationNamesUseCase
import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.util.NetworkUtils
import com.aryamahasangh.util.Result
import com.aryamahasangh.viewmodel.BaseViewModel

/**
 * UI state for the About Us screens
 */
data class AboutUsUiState(
  val organisation: OrganisationDetail? = null,
  val organisationNames: List<OrganisationName> = emptyList(),
  val isLoading: Boolean = false,
  val isLoadingOrganisationNames: Boolean = false,
  val error: String? = null,
  val appError: AppError? = null
)

/**
 * ViewModel for the About Us and Detailed About Us screens
 */
class AboutUsViewModel(
  private val getOrganisationByName: GetOrganisationByNameUseCase,
  private val getOrganisationNames: GetOrganisationNamesUseCase
) : BaseViewModel<AboutUsUiState>(AboutUsUiState()) {
  init {
    loadOrganisationDetails("आर्य महासंघ")
    loadOrganisationNames()
  }

  /**
   * Load organisation details by name
   */
  fun loadOrganisationDetails(name: String) {
    launch {
      updateState { it.copy(isLoading = true, error = null, appError = null) }

      getOrganisationByName(name).collect { result ->
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
   * Load organisation names
   */
  fun loadOrganisationNames() {
    launch {
      updateState { it.copy(isLoadingOrganisationNames = true, error = null, appError = null) }

      getOrganisationNames().collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoadingOrganisationNames = true, error = null, appError = null) }
          }
          is Result.Success -> {
            updateState {
              it.copy(
                organisationNames = result.data,
                isLoadingOrganisationNames = false,
                error = null,
                appError = null
              )
            }
          }
          is Result.Error -> {
            val appError =
              when {
                NetworkUtils.isLikelyNetworkIssue(result.message, result.exception) ->
                  AppError.NetworkError.NoConnection

                result.exception != null ->
                  result.exception.toAppError()

                else ->
                  AppError.UnknownError(result.message)
              }

            ErrorHandler.logError(appError, "AboutUsViewModel.loadOrganisationNames")

            updateState {
              it.copy(
                isLoadingOrganisationNames = false,
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
