package org.aryamahasangh.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.aryamahasangh.components.AryaSamaj
import org.aryamahasangh.components.Gender
import org.aryamahasangh.domain.error.AppError
import org.aryamahasangh.domain.error.ErrorHandler
import org.aryamahasangh.domain.error.getUserMessage
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.ErrorState
import org.aryamahasangh.viewmodel.handleResult

data class MembersUiState(
  val members: List<MemberShort> = emptyList(),
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null,
  val searchQuery: String = "",
  val searchResults: List<MemberShort> = emptyList(),
  val isSearching: Boolean = false
) : ErrorState

data class MemberDetailUiState(
  val member: MemberDetail? = null,
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null,
  val isEditingProfile: Boolean = false,
  val isEditingDetails: Boolean = false,
  val isUpdating: Boolean = false,
  val updateSuccess: Boolean = false,
  val allMembers: List<Member> = emptyList(),
  val searchMembersResults: List<Member> = emptyList(),
  val allAryaSamajs: List<AryaSamaj> = emptyList(),
  val searchAryaSamajResults: List<AryaSamaj> = emptyList()
) : ErrorState

data class DeleteMemberState(
  val isDeleting: Boolean = false,
  val deleteSuccess: Boolean = false,
  val deleteError: String? = null,
  val deleteAppError: AppError? = null
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
        result.handleResult(
          onLoading = {
            _membersUiState.value = _membersUiState.value.copy(
              isLoading = true,
              error = null,
              appError = null
            )
          },
          onSuccess = { members ->
            _membersUiState.value = _membersUiState.value.copy(
              members = members,
              isLoading = false,
              error = null,
              appError = null
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadMembers")
            _membersUiState.value = _membersUiState.value.copy(
              isLoading = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        )
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
        result.handleResult(
          onLoading = {
            _membersUiState.value = _membersUiState.value.copy(isSearching = true)
          },
          onSuccess = { searchResults ->
            _membersUiState.value = _membersUiState.value.copy(
              searchResults = searchResults,
              isSearching = false
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.searchMembers")
            _membersUiState.value = _membersUiState.value.copy(
              isSearching = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        )
      }
    }
  }

  // New method for searching members for selection component
  fun searchMembersForSelection(query: String): List<Member> {
    // This is a synchronous version that returns the current results
    return _memberDetailUiState.value.searchMembersResults.filter { member ->
      member.name.contains(query, ignoreCase = true) ||
        member.phoneNumber.contains(query, ignoreCase = true) ||
        member.email.contains(query, ignoreCase = true)
    }
  }

  // Load all members for member selection component
  fun loadAllMembersForSelection() {
    viewModelScope.launch {
      repository.searchMembersForSelection("").collect { result ->
        result.handleResult(
          onSuccess = { allMembers ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              allMembers = allMembers
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadAllMembersForSelection")
          }
        )
      }
    }
  }

  // Load all AryaSamajs for selection
  fun loadAllAryaSamajsForSelection() {
    viewModelScope.launch {
      repository.getAllAryaSamajs().collect { result ->
        result.handleResult(
          onSuccess = { allAryaSamajs ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              allAryaSamajs = allAryaSamajs
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadAllAryaSamajsForSelection")
          }
        )
      }
    }
  }

  // Search AryaSamajs
  fun searchAryaSamajs(query: String): List<AryaSamaj> {
    // This is a synchronous version that returns the current results
    return _memberDetailUiState.value.searchAryaSamajResults.filter { aryaSamaj ->
      aryaSamaj.name.contains(query, ignoreCase = true) ||
        aryaSamaj.address.contains(query, ignoreCase = true) ||
        aryaSamaj.district.contains(query, ignoreCase = true)
    }
  }

  // Trigger search for AryaSamaj selection
  fun triggerAryaSamajSearch(query: String) {
    viewModelScope.launch {
      if (query.isBlank()) {
        return@launch
      }

      repository.searchAryaSamajs(query).collect { result ->
        result.handleResult(
          onSuccess = { searchResults ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              searchAryaSamajResults = searchResults
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.triggerAryaSamajSearch")
          }
        )
      }
    }
  }

  // Trigger search for member selection
  fun triggerMemberSearch(query: String) {
    viewModelScope.launch {
      if (query.isBlank()) {
        return@launch
      }

      repository.searchMembersForSelection(query).collect { result ->
        result.handleResult(
          onSuccess = { searchResults ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              searchMembersResults = searchResults
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.triggerMemberSearch")
          }
        )
      }
    }
  }

  fun loadMemberDetail(memberId: String) {
    viewModelScope.launch {
      repository.getMember(memberId).collect { result ->
        result.handleResult(
          onLoading = {
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isLoading = true,
              error = null,
              appError = null
            )
          },
          onSuccess = { member ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              member = member,
              isLoading = false,
              error = null,
              appError = null
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadMemberDetail")
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isLoading = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        )
      }
    }
  }

  fun deleteMember(memberId: String, memberName: String) {
    viewModelScope.launch {
      _deleteMemberState.value = _deleteMemberState.value.copy(
        isDeleting = true,
        deleteError = null,
        deleteAppError = null
      )

      repository.deleteMember(memberId).collect { result ->
        result.handleResult(
          onLoading = {
            // Already handled above
          },
          onSuccess = { _ ->
            _deleteMemberState.value = _deleteMemberState.value.copy(
              isDeleting = false,
              deleteSuccess = true,
              deleteError = null,
              deleteAppError = null
            )
            // Refresh members list
            loadMembers()
            getMembersCount()
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.deleteMember")
            _deleteMemberState.value = _deleteMemberState.value.copy(
              isDeleting = false,
              deleteError = appError.getUserMessage(),
              deleteAppError = appError
            )
          }
        )
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
    email: String?
  ) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.updateMemberDetails(
        memberId, name, phoneNumber, educationalQualification,
        email
      ).collect { result ->
        result.handleResult(
          onLoading = {
            // Already handled above
          },
          onSuccess = { _ ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              updateSuccess = true,
              isEditingDetails = false
            )
            // Reload member details
            loadMemberDetail(memberId)
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.updateMemberDetails")
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        )
      }
    }
  }

  fun updateMemberPhoto(memberId: String, photoUrl: String) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.updateMemberPhoto(memberId, photoUrl).collect { result ->
        result.handleResult(
          onLoading = {
            // Already handled above
          },
          onSuccess = { _ ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              updateSuccess = true,
              isEditingProfile = false
            )
            // Reload member details
            loadMemberDetail(memberId)
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.updateMemberPhoto")
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        )
      }
    }
  }

  fun createMember(
    name: String,
    phoneNumber: String,
    email: String?,
    dob: LocalDate?,
    gender: Gender?,
    educationalQualification: String?,
    occupation: String?,
    joiningDate: LocalDate?,
    introduction: String?,
    profileImageUrl: String?,
    addressId: String,
    tempAddressId: String?,
    referrerId: String?,
    aryaSamajId: String?
  ) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.createMember(
        name = name,
        phoneNumber = phoneNumber,
        email = email,
        dob = dob,
        gender = gender,
        educationalQualification = educationalQualification,
        occupation = occupation,
        joiningDate = joiningDate,
        introduction = introduction,
        profileImageUrl = profileImageUrl,
        addressId = addressId,
        tempAddressId = tempAddressId,
        referrerId = referrerId,
        aryaSamajId = aryaSamajId
      ).collect { result ->
        result.handleResult(
          onLoading = {
            // Already handled above
          },
          onSuccess = { _ ->
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              updateSuccess = true
            )
            // Refresh members list
            loadMembers()
            getMembersCount()
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.createMember")
            _memberDetailUiState.value = _memberDetailUiState.value.copy(
              isUpdating = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        )
      }
    }
  }

  // New method to create address
  suspend fun createAddress(
    basicAddress: String,
    state: String,
    district: String,
    pincode: String,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?
  ): String? {
    var addressId: String? = null
    repository.createAddress(
      basicAddress, state, district, pincode, latitude, longitude, vidhansabha
    ).collect { result ->
      result.handleResult(
        onSuccess = { id ->
          addressId = id
        },
        onError = { appError ->
          ErrorHandler.logError(appError, "AdminViewModel.createAddress")
          _memberDetailUiState.value = _memberDetailUiState.value.copy(
            error = appError.getUserMessage(),
            appError = appError
          )
        }
      )
    }
    return addressId
  }

  fun resetUpdateState() {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(
      updateSuccess = false,
      error = null,
      appError = null
    )
  }

  fun getMembersCount() {
    viewModelScope.launch {
      repository.getMembersCount().collect { result ->
        result.handleResult(
          onSuccess = { count ->
            _membersCount.value = count
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.getMembersCount")
            // For count loading, we don't need to show error to user, just log it
          }
        )
      }
    }
  }

  /**
   * Clear error states
   */
  fun clearMembersError() {
    _membersUiState.value = _membersUiState.value.copy(error = null, appError = null)
  }

  fun clearMemberDetailError() {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(error = null, appError = null)
  }
}
