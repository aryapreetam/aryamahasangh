package org.aryamahasangh.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.aryamahasangh.util.Result

data class MembersUiState(
  val members: List<MemberShort> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null,
  val searchQuery: String = "",
  val searchResults: List<MemberShort> = emptyList(),
  val isSearching: Boolean = false
)

data class MemberDetailUiState(
  val member: MemberDetail? = null,
  val isLoading: Boolean = false,
  val error: String? = null,
  val isEditingProfile: Boolean = false,
  val isEditingDetails: Boolean = false,
  val isUpdating: Boolean = false,
  val updateSuccess: Boolean = false
)

data class DeleteMemberState(
  val isDeleting: Boolean = false,
  val deleteSuccess: Boolean = false,
  val deleteError: String? = null
)

class AdminViewModel(private val repository: AdminRepository) : ViewModel() {

  private val _membersCount = MutableStateFlow(0L)
  val membersCount: StateFlow<Long> = _membersCount.asStateFlow()
  private val _membersUiState = MutableStateFlow(MembersUiState())
  val membersUiState: StateFlow<MembersUiState> = _membersUiState.asStateFlow()

  private val _memberDetailUiState = MutableStateFlow(MemberDetailUiState())
  val memberDetailUiState: StateFlow<MemberDetailUiState> = _memberDetailUiState.asStateFlow()

  private val _deleteMemberState = MutableStateFlow(DeleteMemberState())
  val deleteMemberState: StateFlow<DeleteMemberState> = _deleteMemberState.asStateFlow()

  private var searchJob: Job? = null

  fun loadMembers() {
    viewModelScope.launch {
      repository.getMembers().collect { result ->
        when (result) {
          is Result.Loading -> {
            _membersUiState.value = _membersUiState.value.copy(isLoading = true, error = null)
          }

          is Result.Success -> {
            _membersUiState.value = _membersUiState.value.copy(
              members = result.data,
              isLoading = false,
              error = null
            )
          }

          is Result.Error -> {
            _membersUiState.value = _membersUiState.value.copy(
              isLoading = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun searchMembers(query: String) {
    _membersUiState.value = _membersUiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      if (query.isBlank()) {
        _membersUiState.value = _membersUiState.value.copy(
          searchResults = emptyList(),
          isSearching = false
        )
        return@launch
      }

      // Debounce search
      delay(500)

      repository.searchMembers(query).collect { result ->
        when (result) {
          is Result.Loading -> {
            _membersUiState.value = _membersUiState.value.copy(isSearching = true)
          }

          is Result.Success -> {
            _membersUiState.value = _membersUiState.value.copy(
              searchResults = result.data,
              isSearching = false
            )
          }

          is Result.Error -> {
            _membersUiState.value = _membersUiState.value.copy(
              isSearching = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun loadMemberDetail(memberId: String) {
    viewModelScope.launch {
      repository.getMember(memberId).collect { result ->
        when (result) {
          is Result.Loading -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(isLoading = true, error = null)
          }

          is Result.Success -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              member = result.data,
              isLoading = false,
              error = null
            )
          }

          is Result.Error -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isLoading = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun deleteMember(memberId: String, memberName: String) {
    viewModelScope.launch {
      _deleteMemberState.value = _deleteMemberState.value.copy(isDeleting = true, deleteError = null)

      repository.deleteMember(memberId).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already handled above
          }

          is Result.Success -> {
            _deleteMemberState.value = _deleteMemberState.value.copy(
              isDeleting = false,
              deleteSuccess = true,
              deleteError = null
            )
            // Refresh members list
            loadMembers()
            getMembersCount()
          }

          is Result.Error -> {
            _deleteMemberState.value = _deleteMemberState.value.copy(
              isDeleting = false,
              deleteError = result.message
            )
          }
        }
      }
    }
  }

  fun resetDeleteState() {
    _deleteMemberState.value = DeleteMemberState()
  }

  fun setEditingProfile(editing: Boolean) {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(isEditingProfile = editing)
  }

  fun setEditingDetails(editing: Boolean) {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(isEditingDetails = editing)
  }

  fun updateMemberDetails(
    memberId: String,
    name: String?,
    phoneNumber: String?,
    educationalQualification: String?,
    email: String?,
    address: String?,
    state: String?,
    district: String?,
    pincode: String?
  ) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.updateMemberDetails(
        memberId, name, phoneNumber, educationalQualification,
        email, address, state, district, pincode
      ).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already handled above
          }

          is Result.Success -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              updateSuccess = true,
              isEditingDetails = false
            )
            // Reload member details
            loadMemberDetail(memberId)
          }

          is Result.Error -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun updateMemberPhoto(memberId: String, photoUrl: String) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.updateMemberPhoto(memberId, photoUrl).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already handled above
          }

          is Result.Success -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              updateSuccess = true,
              isEditingProfile = false
            )
            // Reload member details
            loadMemberDetail(memberId)
          }

          is Result.Error -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun addMember(
    name: String,
    phoneNumber: String,
    educationalQualification: String?,
    profileImageUrl: String?,
    email: String?,
    address: String?,
    state: String?,
    district: String?,
    pincode: String?
  ) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.addMember(
        name, phoneNumber, educationalQualification,
        profileImageUrl, email, address, state, district, pincode
      ).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already handled above
          }

          is Result.Success -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              updateSuccess = true
            )
            // Refresh members list
            loadMembers()
          }

          is Result.Error -> {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              error = result.message
            )
          }
        }
      }
    }
  }

  fun resetUpdateState() {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(
      updateSuccess = false,
      error = null
    )
  }

  fun getMembersCount() {
    viewModelScope.launch {
      repository.getMembersCount().collect {
        when(it) {
          is Result.Success -> {
            _membersCount.value = it.data
          }
          is Result.Error -> println("error getting members count ${it.message}")
          Result.Loading -> println("loading members count")
        }
      }
    }
  }
}
