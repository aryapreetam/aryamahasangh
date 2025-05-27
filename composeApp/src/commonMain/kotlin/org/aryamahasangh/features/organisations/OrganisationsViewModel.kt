package org.aryamahasangh.features.organisations

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.BaseViewModel

/**
 * UI state for the Organisations screen
 */
data class OrganisationsUiState(
  val organisations: List<OrganisationWithDescription> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * UI state for the Organisation Details screen
 */
data class OrganisationDetailUiState(
  val organisation: OrganisationDetail? = null,
  val isLoading: Boolean = false,
  val error: String? = null
)

data class OrganisationDescriptionState(
  val description: String = "",
  val isUpdating: Boolean = false,
  val error: String? = null,
  val editMode: Boolean = false
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
  fun loadOrganisationDetail(id: String) {
    launch {
      _organisationDetailUiState.value = OrganisationDetailUiState(isLoading = true)

      organisationsRepository.getOrganisationById(id = id).collect { result ->
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

  /**
   * Update the logo of an organisation
   * @param orgId The ID of the organisation to update
   * @param name The name of the organisation (used to reload the details after update)
   * @param imageUrl The URL of the new logo image
   */
  fun updateOrganisationLogo(orgId: String, name: String, imageUrl: String) {
    launch {
      // Set loading state
      // FIXME This is not the correct way to do this, but it works for now
      _organisationDetailUiState.value = _organisationDetailUiState.value.copy(isLoading = true, error = null)

      // Call the repository to update the logo
      organisationsRepository.updateOrganisationLogo(orgId, imageUrl).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set loading state above, no need to do anything here
          }
          is Result.Success -> {
            if (result.data) {
              // If update was successful, load the organisation details
              loadOrganisationDetail(name)
            } else {
              // If update failed but didn't throw an exception
              _organisationDetailUiState.value = _organisationDetailUiState.value.copy(
                isLoading = false,
                error = "Failed to update organisation logo"
              )
            }
          }
          is Result.Error -> {
            // If there was an error, update the UI state
            _organisationDetailUiState.value = _organisationDetailUiState.value.copy(
              isLoading = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun updateOrganisationDescription(orgId: String, description: String){
    launch {
      // Set loading state
      // FIXME This is not the correct way to do this, but it works for now
      _organisationDetailUiState.value = _organisationDetailUiState.value.copy(isLoading = true, error = null)

      // Call the repository to update the logo
      organisationsRepository.updateOrganisationDescription(orgId = orgId, description =  description).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set loading state above, no need to do anything here
          }
          is Result.Success -> {
            if (result.data) {
              // If update was successful, load the organisation details
              loadOrganisationDetail(orgId)
            } else {
              // If update failed but didn't throw an exception
              _organisationDetailUiState.value = _organisationDetailUiState.value.copy(
                isLoading = false,
                error = "Failed to update organisation logo"
              )
            }
          }
          is Result.Error -> {
            // If there was an error, update the UI state
            _organisationDetailUiState.value = _organisationDetailUiState.value.copy(
              isLoading = false,
              error = result.message
            )
          }
        }
      }
    }
  }
}
