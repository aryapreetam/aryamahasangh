package com.aryamahasangh.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.AryaSamaj
import com.aryamahasangh.components.Gender
import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.ErrorHandler
import com.aryamahasangh.domain.error.getUserMessage
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.fragment.MemberInOrganisationShort
import com.aryamahasangh.viewmodel.ErrorState
import com.aryamahasangh.viewmodel.handleResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class AdminCountsUiState(
  val counts: AdminCounts = AdminCounts(),
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null
) : ErrorState

data class MembersUiState(
  val members: List<MemberShort> = emptyList(),
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null,
  val searchQuery: String = "",
  val searchResults: List<MemberShort> = emptyList(),
  val organisationalSearchResults: List<MemberInOrganisationShort> = emptyList(),
  val isSearching: Boolean = false
) : ErrorState

data class EkalAryaUiState(
  val members: List<MemberShort> = emptyList(),
  val paginationState: PaginationState<MemberShort> = PaginationState(),
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null,
  val searchQuery: String = "",
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
  val searchAryaSamajResults: List<AryaSamaj> = emptyList(),
  val memberId: String? = null
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

  private val _adminCounts = MutableStateFlow(AdminCountsUiState())
  val adminCounts: StateFlow<AdminCountsUiState> = _adminCounts.asStateFlow()

  private val _membersUiState = MutableStateFlow(MembersUiState())
  val membersUiState: StateFlow<MembersUiState> = _membersUiState.asStateFlow()

  private val _ekalAryaUiState = MutableStateFlow(EkalAryaUiState())
  val ekalAryaUiState: StateFlow<EkalAryaUiState> = _ekalAryaUiState.asStateFlow()

  private val _memberDetailUiState = MutableStateFlow(MemberDetailUiState())
  val memberDetailUiState: StateFlow<MemberDetailUiState> = _memberDetailUiState.asStateFlow()

  private val _deleteMemberState = MutableStateFlow(DeleteMemberState())
  val deleteMemberState: StateFlow<DeleteMemberState> = _deleteMemberState.asStateFlow()

  private var searchJob: Job? = null

  // Flag to track if pagination should be preserved (e.g., when navigating back)
  private var shouldPreservePagination = false

  // Expose repository flow for Compose-managed lifecycle
  fun listenToAdminCountChanges(): Flow<Unit> = repository.listenToAdminCountChanges()

  // Method to check if we have existing data and should preserve it
  fun hasExistingEkalAryaData(): Boolean {
    return _ekalAryaUiState.value.members.isNotEmpty()
  }

  // Method to preserve pagination state when navigating back
  fun preserveEkalAryaPagination(savedMembers: List<MemberShort>, savedPaginationState: PaginationState<MemberShort>) {
    _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
      members = savedMembers,
      paginationState = savedPaginationState
    )
    shouldPreservePagination = true
  }

  fun loadMembers() {
    viewModelScope.launch {
      repository.getOrganisationalMembers().collect { result ->
        result.handleResult(
          onLoading = {
            _membersUiState.value =
              _membersUiState.value.copy(
                isLoading = true,
                error = null,
                appError = null
              )
          },
          onSuccess = { members ->
            _membersUiState.value =
              _membersUiState.value.copy(
                members = members,
                isLoading = false,
                error = null,
                appError = null
              )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadMembers")
            _membersUiState.value =
              _membersUiState.value.copy(
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
    searchJob =
      viewModelScope.launch {
        if (query.isBlank()) {
          _membersUiState.value =
            _membersUiState.value.copy(
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
              _membersUiState.value =
                _membersUiState.value.copy(
                  searchResults = searchResults,
                  isSearching = false
                )
            },
            onError = { appError ->
              ErrorHandler.logError(appError, "AdminViewModel.searchMembers")
              _membersUiState.value =
                _membersUiState.value.copy(
                  isSearching = false,
                  error = appError.getUserMessage(),
                  appError = appError
                )
            }
          )
        }
      }

  }

  fun searchOrganisationalMembers(query: String) {
    _membersUiState.value = _membersUiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob =
      viewModelScope.launch {
        if (query.isBlank()) {
          _membersUiState.value =
            _membersUiState.value.copy(
              organisationalSearchResults = emptyList(),
              isSearching = false
            )
          return@launch
        }

        // Debounce search
        delay(500)

        repository.searchOrganisationalMembers(query).collect { result ->
          result.handleResult(
            onLoading = {
              _membersUiState.value = _membersUiState.value.copy(isSearching = true)
            },
            onSuccess = { searchResults ->
              _membersUiState.value =
                _membersUiState.value.copy(
                  organisationalSearchResults = searchResults,
                  isSearching = false
                )
            },
            onError = { appError ->
              ErrorHandler.logError(appError, "AdminViewModel.searchOrganisationalMembers")
              _membersUiState.value =
                _membersUiState.value.copy(
                  isSearching = false,
                  error = appError.getUserMessage(),
                  appError = appError
                )
            }
          )
        }
      }

  }

  // New method for searching EkalArya members
  fun searchEkalAryaMembers(query: String) {
    _ekalAryaUiState.value = _ekalAryaUiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob =
      viewModelScope.launch {
        if (query.isBlank()) {
          _ekalAryaUiState.value =
            _ekalAryaUiState.value.copy(
              isSearching = false
            )
          return@launch
        }

        // Debounce search
        delay(500)

        repository.searchEkalAryaMembers(query).collect { result ->
          result.handleResult(
            onLoading = {
              _ekalAryaUiState.value = _ekalAryaUiState.value.copy(isSearching = true)
            },
            onSuccess = { searchResults ->
              _ekalAryaUiState.value =
                _ekalAryaUiState.value.copy(
                  members = searchResults,
                  isSearching = false
                )
            },
            onError = { appError ->
              ErrorHandler.logError(appError, "AdminViewModel.searchEkalAryaMembers")
              _ekalAryaUiState.value =
                _ekalAryaUiState.value.copy(
                  isSearching = false,
                  error = appError.getUserMessage(),
                  appError = appError
                )
            }
          )
        }
      }

  }

  // New method to load EkalArya members
  fun loadEkalAryaMembers() {
    viewModelScope.launch {
      repository.getEkalAryaMembers().collect { result ->
        result.handleResult(
          onLoading = {
            _ekalAryaUiState.value =
              _ekalAryaUiState.value.copy(
                isLoading = true,
                error = null,
                appError = null
              )
          },
          onSuccess = { members ->
            _ekalAryaUiState.value =
              _ekalAryaUiState.value.copy(
                members = members,
                isLoading = false,
                error = null,
                appError = null
              )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadEkalAryaMembers")
            _ekalAryaUiState.value =
              _ekalAryaUiState.value.copy(
                isLoading = false,
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
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
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
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
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
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
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
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
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
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                isLoading = true,
                error = null,
                appError = null
              )
          },
          onSuccess = { member ->
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                member = member,
                isLoading = false,
                error = null,
                appError = null
              )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadMemberDetail")
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                isLoading = false,
                error = appError.getUserMessage(),
                appError = appError
              )
          }
        )
      }
    }
  }

  // Alias for loadMemberDetail for AddMemberFormScreen
  fun loadMember(memberId: String) = loadMemberDetail(memberId)

  fun updateMember(
    memberId: String,
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
    addressId: String?,
    tempAddressId: String?,
    referrerId: String?,
    aryaSamajId: String?,
    // Main address fields
    basicAddress: String? = null,
    state: String? = null,
    district: String? = null,
    pincode: String? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    vidhansabha: String? = null,
    // Temp address fields 
    tempBasicAddress: String? = null,
    tempState: String? = null,
    tempDistrict: String? = null,
    tempPincode: String? = null,
    tempLatitude: Double? = null,
    tempLongitude: Double? = null,
    tempVidhansabha: String? = null
  ) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      // Use the comprehensive update mutation
      repository.updateMemberDetails(
        memberId = memberId,
        name = name,
        phoneNumber = phoneNumber,
        educationalQualification = educationalQualification,
        email = email,
        dob = dob,
        gender = gender,
        occupation = occupation,
        joiningDate = joiningDate,
        introduction = introduction,
        profileImage = profileImageUrl,
        addressId = addressId,
        tempAddressId = tempAddressId,
        referrerId = referrerId,
        aryaSamajId = aryaSamajId,
        // Main address fields
        basicAddress = basicAddress,
        state = state,
        district = district,
        pincode = pincode,
        latitude = latitude,
        longitude = longitude,
        vidhansabha = vidhansabha,
        // Temp address fields
        tempBasicAddress = tempBasicAddress,
        tempState = tempState,
        tempDistrict = tempDistrict,
        tempPincode = tempPincode,
        tempLatitude = tempLatitude,
        tempLongitude = tempLongitude,
        tempVidhansabha = tempVidhansabha
      ).collect { result ->
        result.handleResult(
          onLoading = {
            // Already handled above
          },
          onSuccess = { _ ->
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                isUpdating = false,
                updateSuccess = true
              )
            // Reload member details
            loadMemberDetail(memberId)
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.updateMember")
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                isUpdating = false,
                error = appError.getUserMessage(),
                appError = appError
              )
          }
        )
      }
    }
  }

  fun setEditingProfile(editing: Boolean) {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(isEditingProfile = editing)
  }

  fun setEditingDetails(editing: Boolean) {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(isEditingDetails = editing)
  }

//  fun updateMemberDetails(
//    memberId: String,
//    name: String?,
//    phoneNumber: String?,
//    educationalQualification: String?,
//    email: String?,
//    dob: LocalDate?,
//    gender: Gender?,
//    occupation: String?,
//    joiningDate: LocalDate?,
//    introduction: String?,
//    profileImage: String?,
//    addressId: String?,
//    tempAddressId: String?,
//    referrerId: String?,
//    aryaSamajId: String?
//  ) {
//    viewModelScope.launch {
//      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)
//
//      repository.updateMemberDetails(
//        memberId = memberId,
//        name = name,
//        phoneNumber = phoneNumber,
//        educationalQualification = educationalQualification,
//        email = email,
//        dob = dob,
//        gender = gender,
//        occupation = occupation,
//        joiningDate = joiningDate,
//        introduction = introduction,
//        profileImage = profileImage,
//        addressId = addressId,
//        tempAddressId = tempAddressId,
//        referrerId = referrerId,
//        aryaSamajId = aryaSamajId
//      ).collect { result ->
//        result.handleResult(
//          onLoading = {
//            // Already handled above
//          },
//          onSuccess = { _ ->
//            _memberDetailUiState.value =
//              _memberDetailUiState.value.copy(
//                isUpdating = false,
//                updateSuccess = true,
//                isEditingDetails = false
//              )
//            // Reload member details
//            loadMemberDetail(memberId)
//          },
//          onError = { appError ->
//            ErrorHandler.logError(appError, "AdminViewModel.updateMemberDetails")
//            _memberDetailUiState.value =
//              _memberDetailUiState.value.copy(
//                isUpdating = false,
//                error = appError.getUserMessage(),
//                appError = appError
//              )
//          }
//        )
//      }
//    }
//  }

  fun deleteMember(
    memberId: String,
    memberName: String? = null,
    onSuccess: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      _deleteMemberState.value =
        _deleteMemberState.value.copy(
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
            _deleteMemberState.value =
              _deleteMemberState.value.copy(
                isDeleting = false,
                deleteSuccess = true,
                deleteError = null,
                deleteAppError = null
              )
            // Refresh members list
            loadMembers()
            getMembersCount()
            loadEkalAryaMembersPaginated(resetPagination = true)
            // Call success callback
            onSuccess?.invoke()
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.deleteMember")
            _deleteMemberState.value =
              _deleteMemberState.value.copy(
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

  fun updateMemberPhoto(
    memberId: String,
    photoUrl: String
  ) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.updateMemberPhoto(memberId, photoUrl).collect { result ->
        result.handleResult(
          onLoading = {
            // Already handled above
          },
          onSuccess = { _ ->
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                isUpdating = false,
                updateSuccess = true,
                isEditingProfile = false
              )
            // Reload member details
            loadMemberDetail(memberId)
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.updateMemberPhoto")
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
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
    referrerId: String?,
    aryaSamajId: String?,
    basicAddress: String,
    state: String,
    district: String,
    pincode: String,
    latitude: Double?,
    longitude: Double?,
    vidhansabha: String?,
    // Temp address fields 
    tempBasicAddress: String?,
    tempState: String?,
    tempDistrict: String?,
    tempPincode: String?,
    tempLatitude: Double?,
    tempLongitude: Double?,
    tempVidhansabha: String?
  ) {
    viewModelScope.launch {
      _memberDetailUiState.value = _memberDetailUiState.value.copy(isUpdating = true)

      repository.createMemberWithAddress(
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
        referrerId = referrerId,
        aryaSamajId = aryaSamajId,
        basicAddress = basicAddress,
        state = state,
        district = district,
        pincode = pincode,
        latitude = latitude,
        longitude = longitude,
        vidhansabha = vidhansabha,
        tempBasicAddress = tempBasicAddress,
        tempState = tempState,
        tempDistrict = tempDistrict,
        tempPincode = tempPincode,
        tempLatitude = tempLatitude,
        tempLongitude = tempLongitude,
        tempVidhansabha = tempVidhansabha,
      ).collect { result ->
        result.handleResult(
          onLoading = {
            // Already handled above
          },
          onSuccess = { id ->
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                isUpdating = false,
                updateSuccess = true,
                memberId = id
              )
            // Refresh members list
            loadMembers()
            loadEkalAryaMembersPaginated(resetPagination = true)
            getMembersCount()
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.createMember")
            _memberDetailUiState.value =
              _memberDetailUiState.value.copy(
                isUpdating = false,
                error = appError.getUserMessage(),
                appError = appError
              )
          }
        )
      }
    }
  }

  fun resetUpdateState() {
    _memberDetailUiState.value =
      _memberDetailUiState.value.copy(
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

  fun loadAdminCounts() {
    viewModelScope.launch {
      repository.getAdminCounts().collect { result ->
        result.handleResult(
          onLoading = {
            _adminCounts.value =
              _adminCounts.value.copy(
                isLoading = true,
                error = null,
                appError = null
              )
          },
          onSuccess = { counts ->
            _adminCounts.value =
              _adminCounts.value.copy(
                counts = counts,
                isLoading = false,
                error = null,
                appError = null
              )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AdminViewModel.loadAdminCounts")
            _adminCounts.value =
              _adminCounts.value.copy(
                isLoading = false,
                error = appError.getUserMessage(),
                appError = appError
              )
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

  fun clearEkalAryaError() {
    _ekalAryaUiState.value = _ekalAryaUiState.value.copy(error = null, appError = null)
  }

  fun clearMemberDetailError() {
    _memberDetailUiState.value = _memberDetailUiState.value.copy(error = null, appError = null)
  }

  fun clearAdminCountsError() {
    _adminCounts.value = _adminCounts.value.copy(error = null, appError = null)
  }

  // NEW: Pagination methods using the new repository interface
  fun loadEkalAryaMembersPaginated(pageSize: Int = 30, resetPagination: Boolean = false) {
    viewModelScope.launch {
      val currentState = _ekalAryaUiState.value.paginationState

      // Only preserve pagination when explicitly requested AND it's a reset operation
      val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExistingEkalAryaData()

      // Reset the preservation flag after checking
      if (shouldPreservePagination) {
        shouldPreservePagination = false
      }

      // Only skip loading if we're preserving existing data from navigation
      if (shouldPreserveExistingData) {
        return@launch
      }

      // For normal pagination (resetPagination=false), always proceed with loading
      val shouldReset = resetPagination
      val cursor = if (shouldReset) null else currentState.endCursor

      // Set loading state
      _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
        paginationState = currentState.copy(
          isInitialLoading = shouldReset || currentState.items.isEmpty(),
          isLoadingNextPage = !shouldReset && currentState.items.isNotEmpty(),
          error = null
        )
      )

      repository.getItemsPaginated(pageSize = pageSize, cursor = cursor, filter = null).collect { result ->
        when (result) {
          is PaginationResult.Loading -> {
            // Loading state already set above
          }

          is PaginationResult.Success -> {
            val newItems = if (shouldReset) {
              result.data
            } else {
              currentState.items + result.data
            }

            _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
              members = newItems,
              paginationState = currentState.copy(
                items = newItems,
                isInitialLoading = false,
                isLoadingNextPage = false,
                hasNextPage = result.hasNextPage,
                endCursor = result.endCursor,
                hasReachedEnd = !result.hasNextPage,
                error = null
              )
            )
          }

          is PaginationResult.Error -> {
            _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
              paginationState = currentState.copy(
                isInitialLoading = false,
                isLoadingNextPage = false,
                error = result.message,
                showRetryButton = true
              )
            )
          }
        }
      }
    }
  }

  fun searchEkalAryaMembersPaginated(searchTerm: String, pageSize: Int = 30, resetPagination: Boolean = true) {
    viewModelScope.launch {
      val currentState = _ekalAryaUiState.value.paginationState

      // Clear cache on search change
      if (resetPagination && searchTerm != currentState.currentSearchTerm) {
        // TODO: Clear Apollo cache for this query
      }

      val cursor = if (resetPagination) null else currentState.endCursor

      // Set loading state
      _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
        searchQuery = searchTerm,
        paginationState = currentState.copy(
          isSearching = resetPagination,
          isLoadingNextPage = !resetPagination,
          error = null,
          currentSearchTerm = searchTerm
        )
      )

      repository.searchItemsPaginated(
        searchTerm = searchTerm,
        pageSize = pageSize,
        cursor = cursor
      ).collect { result ->
        when (result) {
          is PaginationResult.Loading -> {
            // Loading state already set above
          }

          is PaginationResult.Success -> {
            val newItems = if (resetPagination) {
              result.data
            } else {
              currentState.items + result.data
            }

            _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
              members = newItems,
              paginationState = currentState.copy(
                items = newItems,
                isSearching = false,
                isLoadingNextPage = false,
                hasNextPage = result.hasNextPage,
                endCursor = result.endCursor,
                hasReachedEnd = !result.hasNextPage,
                error = null,
                currentSearchTerm = searchTerm
              )
            )
          }

          is PaginationResult.Error -> {
            _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
              paginationState = currentState.copy(
                isSearching = false,
                isLoadingNextPage = false,
                error = result.message,
                showRetryButton = true
              )
            )
          }
        }
      }
    }
  }

  fun loadNextEkalAryaPage() {
    val currentState = _ekalAryaUiState.value.paginationState

    if (currentState.hasNextPage && !currentState.isLoadingNextPage) {
      if (currentState.currentSearchTerm.isNotBlank()) {
        searchEkalAryaMembersPaginated(
          searchTerm = currentState.currentSearchTerm,
          resetPagination = false
        )
      } else {
        loadEkalAryaMembersPaginated(resetPagination = false)
      }
    } else {
    }
  }

  fun retryEkalAryaLoad() {
    val currentState = _ekalAryaUiState.value.paginationState
    _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
      paginationState = currentState.copy(showRetryButton = false)
    )

    if (currentState.currentSearchTerm.isNotBlank()) {
      searchEkalAryaMembersPaginated(
        searchTerm = currentState.currentSearchTerm,
        resetPagination = currentState.items.isEmpty()
      )
    } else {
      loadEkalAryaMembersPaginated(resetPagination = currentState.items.isEmpty())
    }
  }

  // NEW: Debounced search method
  fun searchEkalAryaMembersWithDebounce(query: String) {
    _ekalAryaUiState.value = _ekalAryaUiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      if (query.isBlank()) {
        // Load regular members when search is cleared
        loadEkalAryaMembersPaginated(resetPagination = true)
        return@launch
      }

      // Debounce search by 1 second
      delay(1000)

      searchEkalAryaMembersPaginated(searchTerm = query.trim(), resetPagination = true)
    }
  }

  // NEW: Calculate page size based on screen width  
  fun calculatePageSize(screenWidthDp: Float): Int {
    return when {
      screenWidthDp < 600f -> 15      // Mobile portrait
      screenWidthDp < 840f -> 25      // Tablet, mobile landscape
      else -> 35                      // Desktop, large tablets
    }
  }

  // NEW method for searching members for selection component
}
