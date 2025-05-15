package org.aryamahasangh.viewmodel

import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.repository.JoinUsRepository
import org.aryamahasangh.type.ActivityFilterInput
import org.aryamahasangh.util.Result

/**
 * UI state for the Join Us screen
 */
data class JoinUsUiState(
  val activities: List<OrganisationalActivitiesQuery.OrganisationalActivity>? = null,
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * ViewModel for the Join Us screen
 */
class JoinUsViewModel(
  private val joinUsRepository: JoinUsRepository
) : BaseViewModel<JoinUsUiState>(JoinUsUiState()) {

  /**
   * Load filtered activities
   */
  fun loadFilteredActivities(filter: ActivityFilterInput) {
    launch {
      updateState { it.copy(isLoading = true, error = null) }
      
      joinUsRepository.getFilteredActivities(filter).collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null) }
          }
          is Result.Success -> {
            updateState { it.copy(
              activities = result.data,
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