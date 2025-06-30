package com.aryamahasangh.features.organisations

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.util.Result
import com.aryamahasangh.viewmodel.BaseViewModel

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

data class MemberManagementState(
  val isRemovingMember: Boolean = false,
  val isUpdatingPost: Boolean = false,
  val isAddingMember: Boolean = false,
  val removeError: String? = null,
  val updatePostError: String? = null,
  val addMemberError: String? = null,
  val successMessage: String? = null
)

data class CreateOrganisationState(
  val isCreating: Boolean = false,
  val isSuccess: Boolean = false,
  val error: String? = null,
  val createdOrganisationId: String? = null
)

data class DeleteOrganisationState(
  val isDeleting: Boolean = false,
  val isSuccess: Boolean = false,
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

  // Separate state for organisation description editing
  private val _organisationDescriptionState = MutableStateFlow(OrganisationDescriptionState())
  val organisationDescriptionState: StateFlow<OrganisationDescriptionState> =
    _organisationDescriptionState.asStateFlow()

  // Separate state for organisation logo update
  private val _organisationLogoState = MutableStateFlow(OrganisationLogoState())
  val organisationLogoState: StateFlow<OrganisationLogoState> = _organisationLogoState.asStateFlow()

  // Separate state for member management
  private val _memberManagementState = MutableStateFlow(MemberManagementState())
  val memberManagementState: StateFlow<MemberManagementState> = _memberManagementState.asStateFlow()

  // Separate state for creating an organisation
  private val _createOrganisationState = MutableStateFlow(CreateOrganisationState())
  val createOrganisationState: StateFlow<CreateOrganisationState> = _createOrganisationState.asStateFlow()

  // Separate state for deleting an organisation
  private val _deleteOrganisationState = MutableStateFlow(DeleteOrganisationState())
  val deleteOrganisationState: StateFlow<DeleteOrganisationState> = _deleteOrganisationState.asStateFlow()

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
            _organisationDescriptionState.value =
              OrganisationDescriptionState(
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
    _organisationDescriptionState.value =
      _organisationDescriptionState.value.copy(
        editMode = editMode,
        error = null
      )
  }

  /**
   * Clear logo update success message
   */
  fun clearLogoSuccessMessage() {
    _organisationLogoState.value =
      _organisationLogoState.value.copy(
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
      _organisationLogoState.value =
        _organisationLogoState.value.copy(
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
                _organisationDetailUiState.value =
                  _organisationDetailUiState.value.copy(
                    organisation = currentOrg.copy(logo = imageUrl)
                  )
              }
              _organisationLogoState.value =
                _organisationLogoState.value.copy(
                  isUpdating = false,
                  successMessage = "✅ Logo updated successfully"
                )
            } else {
              // If update failed but didn't throw an exception
              _organisationLogoState.value =
                _organisationLogoState.value.copy(
                  isUpdating = false,
                  error = "Failed to update organisation logo"
                )
            }
          }

          is Result.Error -> {
            // If there was an error, update the logo state
            _organisationLogoState.value =
              _organisationLogoState.value.copy(
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
      _organisationDescriptionState.value =
        _organisationDescriptionState.value.copy(
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
                  _organisationDetailUiState.value =
                    _organisationDetailUiState.value.copy(
                      organisation = currentOrg.copy(description = description)
                    )
                }
                // Reset description state
                _organisationDescriptionState.value =
                  _organisationDescriptionState.value.copy(
                    description = description,
                    isUpdating = false,
                    editMode = false,
                    error = null
                  )
              } else {
                // If update failed but didn't throw an exception
                _organisationDescriptionState.value =
                  _organisationDescriptionState.value.copy(
                    isUpdating = false,
                    error = "Failed to update organisation description"
                  )
              }
            }

            is Result.Error -> {
              // If there was an error, update the description state
              _organisationDescriptionState.value =
                _organisationDescriptionState.value.copy(
                  isUpdating = false,
                  error = result.message
                )
            }
          }
        }
    }
  }

  /**
   * Remove a member from the organisation
   */
  fun removeMemberFromOrganisation(
    organisationalMemberId: String,
    memberName: String,
    orgId: String
  ) {
    launch {
      _memberManagementState.value =
        _memberManagementState.value.copy(
          isRemovingMember = true,
          removeError = null,
          successMessage = null
        )

      organisationsRepository.removeMemberFromOrganisation(organisationalMemberId).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set loading state above
          }

          is Result.Success -> {
            if (result.data) {
              _memberManagementState.value =
                _memberManagementState.value.copy(
                  isRemovingMember = false,
                  successMessage = "User removed successfully"
                )
              // Reload organisation details to refresh the member list
              loadOrganisationDetail(orgId)
            } else {
              _memberManagementState.value =
                _memberManagementState.value.copy(
                  isRemovingMember = false,
                  removeError = "Failed to remove member from organisation"
                )
            }
          }

          is Result.Error -> {
            _memberManagementState.value =
              _memberManagementState.value.copy(
                isRemovingMember = false,
                removeError = result.message
              )
          }
        }
      }
    }
  }

  /**
   * Update a member's post in an organisation
   */
  fun updateMemberPost(
    organisationalMemberId: String,
    post: String,
    organisationId: String
  ) {
    launch {
      _memberManagementState.value = _memberManagementState.value.copy(isUpdatingPost = true)

      organisationsRepository.updateMemberPost(organisationalMemberId, post).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set loading state above
          }

          is Result.Success -> {
            _memberManagementState.value =
              _memberManagementState.value.copy(
                isUpdatingPost = false,
                successMessage = "पद सफलतापूर्वक अद्यतन किया गया"
              )
            // Reload organisation details to reflect changes
            loadOrganisationDetail(organisationId)
          }

          is Result.Error -> {
            _memberManagementState.value =
              _memberManagementState.value.copy(
                isUpdatingPost = false,
                updatePostError = result.message
              )
          }
        }
      }
    }
  }

  /**
   * Update a member's priority in an organisation
   */
  fun updateMemberPriority(
    organisationalMemberId: String,
    priority: Int
  ) {
    launch {
      organisationsRepository.updateMemberPriority(organisationalMemberId, priority).collect { result ->
        when (result) {
          is Result.Loading -> {
            // No need to show loading state for priority updates as they should be seamless
          }

          is Result.Success -> {
            // Reload organisation details to reflect the new order
            _organisationDetailUiState.value.organisation?.let { org ->
              loadOrganisationDetail(org.id)
            }
          }

          is Result.Error -> {
            _memberManagementState.value =
              _memberManagementState.value.copy(
                updatePostError = "प्राथमिकता अद्यतन करने में त्रुटि: ${result.message}"
              )
          }
        }
      }
    }
  }

  /**
   * Update multiple member priorities at once (for drag and drop reordering)
   */
  fun updateMemberPriorities(memberPriorities: List<Pair<String, Int>>) {
    launch {
      organisationsRepository.updateMemberPriorities(memberPriorities).collect { result ->
        when (result) {
          is Result.Loading -> {
            // No need to show loading state for priority updates as they should be seamless
          }

          is Result.Success -> {
            // Reload organisation details to reflect the new order
            _organisationDetailUiState.value.organisation?.let { org ->
              loadOrganisationDetail(org.id)
            }
          }

          is Result.Error -> {
            _memberManagementState.value =
              _memberManagementState.value.copy(
                updatePostError = "प्राथमिकताएं अद्यतन करने में त्रुटि: ${result.message}"
              )
          }
        }
      }
    }
  }

  /**
   * Clear member management success message
   */
  fun clearMemberManagementMessage() {
    _memberManagementState.value =
      _memberManagementState.value.copy(
        successMessage = null,
        removeError = null,
        updatePostError = null,
        addMemberError = null
      )
  }

  /**
   * Add a member to the organisation
   */
  fun addMemberToOrganisation(
    memberId: String,
    post: String,
    orgId: String,
    priority: Int
  ) {
    launch {
      _memberManagementState.value =
        _memberManagementState.value.copy(
          isAddingMember = true,
          addMemberError = null,
          successMessage = null
        )

      organisationsRepository.addMemberToOrganisation(memberId, orgId, post, priority).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set loading state above
          }

          is Result.Success -> {
            if (result.data) {
              _memberManagementState.value =
                _memberManagementState.value.copy(
                  isAddingMember = false,
                  successMessage = "Member added successfully"
                )
              // Reload organisation details to refresh the member list
              loadOrganisationDetail(orgId)
            } else {
              _memberManagementState.value =
                _memberManagementState.value.copy(
                  isAddingMember = false,
                  addMemberError = "Failed to add member to organisation"
                )
            }
          }

          is Result.Error -> {
            _memberManagementState.value =
              _memberManagementState.value.copy(
                isAddingMember = false,
                addMemberError = result.message
              )
          }
        }
      }
    }
  }

  /**
   * Create a new organisation
   * @param name The name of the organisation
   * @param description The description of the organisation
   * @param logoUrl The URL of the organisation's logo
   * @param members List of members with their posts
   */
  fun createOrganisation(
    name: String,
    description: String,
    logoUrl: String,
    priority: Int,
    members: List<Triple<Member, String, Int>>
  ) {
    launch {
      _createOrganisationState.value =
        _createOrganisationState.value.copy(
          isCreating = true,
          isSuccess = false,
          error = null,
          createdOrganisationId = null
        )

      organisationsRepository.createOrganisation(name, description, logoUrl, priority, members).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set loading state above
          }

          is Result.Success -> {
            _createOrganisationState.value =
              _createOrganisationState.value.copy(
                isCreating = false,
                isSuccess = true,
                createdOrganisationId = result.data
              )
            // Reload organisations after successful creation
            loadOrganisations()
          }

          is Result.Error -> {
            _createOrganisationState.value =
              _createOrganisationState.value.copy(
                isCreating = false,
                error = result.message
              )
          }
        }
      }
    }
  }

  /**
   * Delete an organisation
   * @param orgId The ID of the organisation to delete
   */
  fun deleteOrganisation(orgId: String) {
    launch {
      _deleteOrganisationState.value =
        _deleteOrganisationState.value.copy(
          isDeleting = true,
          isSuccess = false,
          error = null
        )

      organisationsRepository.deleteOrganisation(orgId).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already set loading state above
          }

          is Result.Success -> {
            if (result.data) {
              _deleteOrganisationState.value =
                _deleteOrganisationState.value.copy(
                  isDeleting = false,
                  isSuccess = true
                )
              // Reload organisations after successful deletion
              loadOrganisations()
            } else {
              _deleteOrganisationState.value =
                _deleteOrganisationState.value.copy(
                  isDeleting = false,
                  error = "Failed to delete organisation"
                )
            }
          }

          is Result.Error -> {
            _deleteOrganisationState.value =
              _deleteOrganisationState.value.copy(
                isDeleting = false,
                error = result.message
              )
          }
        }
      }
    }
  }
}
