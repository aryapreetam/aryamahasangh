package org.aryamahasangh.viewmodel

import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.repository.AboutUsRepository
import org.aryamahasangh.util.Result

/**
 * UI state for the About Us screens
 */
data class AboutUsUiState(
  val organisation: OrganisationQuery.Organisation? = null,
  val isLoading: Boolean = false,
  val error: String? = null
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
      aboutUsRepository.getOrganisationByName(name).collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null) }
          }
          is Result.Success -> {
            updateState { it.copy(
              organisation = result.data,
              isLoading = false,
              error = null
            )}
          }
          is Result.Error -> {
            updateState { it.copy(
              isLoading = false,
              error = result.message
            )}
          }
        }
      }
    }
  }
}