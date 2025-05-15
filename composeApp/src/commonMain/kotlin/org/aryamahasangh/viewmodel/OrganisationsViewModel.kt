package org.aryamahasangh.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.aryamahasangh.OrganisationsQuery
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.repository.OrganisationsRepository
import org.aryamahasangh.util.Result

/**
 * UI state for the Organisations screen
 */
data class OrganisationsUiState(
  val organisations: List<OrganisationsQuery.Organisation> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * UI state for the Organisation Details screen
 */
data class OrganisationDetailUiState(
  val organisation: OrganisationQuery.Organisation? = null,
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * ViewModel for the Organisations and Organisation Details screens
 */
class OrganisationsViewModel(
  private val organisationsRepository: OrganisationsRepository
) : BaseViewModel<OrganisationsUiState>(OrganisationsUiState()) {

  // Separate state for organisation details
  private val _organisationDetailUiState = MutableStateFlow(OrganisationDetailUiState())
  val organisationDetailUiState: StateFlow<OrganisationDetailUiState> = _organisationDetailUiState.asStateFlow()

  init {
    loadOrganisations()
  }

  /**
   * Load all organisations
   */
  fun loadOrganisations() {
    launch {
      organisationsRepository.getOrganisations().collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null) }
          }
          is Result.Success -> {
            updateState { it.copy(
              organisations = result.data,
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

  /**
   * Load organisation details by name
   */
  fun loadOrganisationDetail(name: String) {
    launch {
      _organisationDetailUiState.value = OrganisationDetailUiState(isLoading = true)
      
      organisationsRepository.getOrganisationByName(name).collect { result ->
        when (result) {
          is Result.Loading -> {
            _organisationDetailUiState.value = OrganisationDetailUiState(isLoading = true, error = null)
          }
          is Result.Success -> {
            _organisationDetailUiState.value = OrganisationDetailUiState(
              organisation = result.data,
              isLoading = false,
              error = null
            )
          }
          is Result.Error -> {
            _organisationDetailUiState.value = OrganisationDetailUiState(
              isLoading = false,
              error = result.message
            )
          }
        }
      }
    }
  }
}