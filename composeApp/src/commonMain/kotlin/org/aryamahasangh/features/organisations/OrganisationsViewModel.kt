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

data class OrganisationLogoState(
  val isUpdating: Boolean = false,
  val error: String? = null,
  val successMessage: String? = null
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

  // Separate state for organisation description editing
  private val _organisationDescriptionState = MutableStateFlow(OrganisationDescriptionState())
  val organisationDescriptionState: StateFlow<OrganisationDescriptionState> =
    _organisationDescriptionState.asStateFlow()

  // Separate state for organisation logo update
  private val _organisationLogoState = MutableStateFlow(OrganisationLogoState())
  val organisationLogoState: StateFlow<OrganisationLogoState> = _organisationLogoState.asStateFlow()

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
            updateState {
              it.copy(
                organisations = result.data,
                isLoading = false,
                error = null
              )
            }
          }

          is Result.Error -> {
            updateState {
              it.copy(
                isLoading = false,
                error = result.message
              )
            }
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
            _organisationDetailUiState.value =
              OrganisationDetailUiState(
                organisation = result.data,
                isLoading = false,
                error = null
              )
            // Initialize description state with current description
            _organisationDescriptionState.value = OrganisationDescriptionState(
              description = result.data?.description ?: "",
              isUpdating = false,
              error = null,
              editMode = false
            )
          }

          is Result.Error -> {
            _organisationDetailUiState.value =
              OrganisationDetailUiState(
                isLoading = false,
                error = result.message
              )
          }
        }
      }
    }
  }

  /**
   * Set edit mode for organization description
   */
  fun setDescriptionEditMode(editMode: Boolean) {
    _organisationDescriptionState.value = _organisationDescriptionState.value.copy(
      editMode = editMode,
      error = null
    )
  }

  /**
   * Clear logo update success message
   */
  fun clearLogoSuccessMessage() {
    _organisationLogoState.value = _organisationLogoState.value.copy(
      successMessage = null
    )
  }

  /**
   * Update the logo of an organisation
   * @param orgId The ID of the organisation to update
   * @param name The name of the organisation (used to reload the details after update)
   * @param imageUrl The URL of the new logo image
   */
  fun updateOrganisationLogo(
    orgId: String,
    name: String,
    imageUrl: String
  ) {
    launch {
      // Set updating state for logo only
      _organisationLogoState.value = _organisationLogoState.value.copy(
        isUpdating = true,
        error = null,
        successMessage = null
      )

      // Call the repository to update the logo
      organisationsRepository.updateOrganisationLogo(orgId, imageUrl).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set updating state above
          }

          is Result.Success -> {
            if (result.data) {
              // If update was successful, update the current organisation logo in state
              val currentOrg = _organisationDetailUiState.value.organisation
              if (currentOrg != null) {
                _organisationDetailUiState.value = _organisationDetailUiState.value.copy(
                  organisation = currentOrg.copy(logo = imageUrl)
                )
              }
              _organisationLogoState.value = _organisationLogoState.value.copy(
                isUpdating = false,
                successMessage = "✅ Logo updated successfully"
              )
            } else {
              // If update failed but didn't throw an exception
              _organisationLogoState.value = _organisationLogoState.value.copy(
                isUpdating = false,
                error = "Failed to update organisation logo"
              )
            }
          }

          is Result.Error -> {
            // If there was an error, update the logo state
            _organisationLogoState.value = _organisationLogoState.value.copy(
              isUpdating = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun updateOrganisationDescription(
    orgId: String,
    description: String
  ) {
    launch {
      // Set updating state for description only
      _organisationDescriptionState.value = _organisationDescriptionState.value.copy(
        isUpdating = true,
        error = null
      )

      // Call the repository to update the description
      organisationsRepository.updateOrganisationDescription(orgId = orgId, description = description)
        .collect { result ->
          when (result) {
            is Result.Loading -> {
              // Already set updating state above
            }

            is Result.Success -> {
              if (result.data) {
                // If update was successful, update the current organisation in state
                val currentOrg = _organisationDetailUiState.value.organisation
                if (currentOrg != null) {
                  _organisationDetailUiState.value = _organisationDetailUiState.value.copy(
                    organisation = currentOrg.copy(description = description)
                  )
                }
                // Reset description state
                _organisationDescriptionState.value = _organisationDescriptionState.value.copy(
                  description = description,
                  isUpdating = false,
                  editMode = false,
                  error = null
                )
              } else {
                // If update failed but didn't throw an exception
                _organisationDescriptionState.value = _organisationDescriptionState.value.copy(
                  isUpdating = false,
                  error = "Failed to update organisation description"
                )
              }
            }

            is Result.Error -> {
              // If there was an error, update the description state
              _organisationDescriptionState.value = _organisationDescriptionState.value.copy(
                isUpdating = false,
                error = result.message
              )
            }
          }
        }
    }
  }
}
