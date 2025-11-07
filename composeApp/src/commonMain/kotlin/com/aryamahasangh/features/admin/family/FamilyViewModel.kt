package com.aryamahasangh.features.admin.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.GetFamilyDetailQuery
import com.aryamahasangh.components.*
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.admin.AdminRepository
import com.aryamahasangh.features.admin.PaginationResult
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.util.GlobalMessageManager
import com.aryamahasangh.util.Result
import com.aryamahasangh.utils.FileUploadUtils
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

data class FamiliesUiState(
  val isLoading: Boolean = false,
  val families: List<FamilyShort> = emptyList(),
  val searchQuery: String = "",
  val searchResults: List<FamilyShort> = emptyList(),
  val error: String? = null,
  val familyCount: Int = 0,
  val familyMemberCount: Int = 0,
  val paginationState: PaginationState<FamilyShort> = PaginationState(),
  val hasLoadedOnce: Boolean = false
)

data class FamilyDetailUiState(
  val isLoading: Boolean = false,
  val familyDetail: GetFamilyDetailQuery.Node? = null,
  val error: String? = null
)

@Serializable
data class FamilyMemberForCreation(
  val member: Member,
  val isHead: Boolean = false,
  val relationToHead: FamilyRelation? = null
)

data class CreateFamilyUiState(
  val isLoading: Boolean = false,
  val isSubmitting: Boolean = false,
  val isLoadingMembers: Boolean = false, // New loading state for member search
  val familyName: String = "",
  val selectedAryaSamaj: AryaSamaj? = null,
  val imagePickerState: ImagePickerState = ImagePickerState(),
  val familyMembers: List<FamilyMemberForCreation> = emptyList(),
  val selectedAddressIndex: Int? = null,
  val addressData: AddressData = AddressData(),
  val memberAddresses: List<AddressWithMemberId> = emptyList(),
  val error: String? = null,
  val submitSuccess: Boolean = false,
  val familyId: String = "",
  val availableAryaSamajs: List<AryaSamaj> = emptyList(),
  val availableMembers: List<Member> = emptyList()
)

data class DeleteFamilyState(
  val isDeleting: Boolean = false,
  val deleteSuccess: Boolean = false,
  val deleteError: String? = null,
  val deletingFamilyId: String? = null // Track which family is being deleted
)

class FamilyViewModel(
  private val familyRepository: FamilyRepository,
  private val adminRepository: AdminRepository,
  private val fileUploadUtils: FileUploadUtils
) : ViewModel() {
  private val _familiesUiState = MutableStateFlow(FamiliesUiState())
  val familiesUiState: StateFlow<FamiliesUiState> = _familiesUiState.asStateFlow()

  private val _createFamilyUiState = MutableStateFlow(CreateFamilyUiState())
  val createFamilyUiState: StateFlow<CreateFamilyUiState> = _createFamilyUiState.asStateFlow()

  private val _familyDetailUiState = MutableStateFlow(FamilyDetailUiState())
  val familyDetailUiState: StateFlow<FamilyDetailUiState> = _familyDetailUiState.asStateFlow()

  private val _deleteFamilyState = MutableStateFlow(DeleteFamilyState())
  val deleteFamilyState: StateFlow<DeleteFamilyState> = _deleteFamilyState.asStateFlow()

  private var searchJob: Job? = null

  // Flag to track if pagination should be preserved (e.g., when navigating back)
  private var shouldPreservePagination = false

  // Method to check if we have existing data and should preserve it
  fun hasExistingFamilyData(): Boolean {
    return _familiesUiState.value.families.isNotEmpty()
  }

  // Method to preserve pagination state when navigating back
  fun preserveFamilyPagination(families: List<FamilyShort>, paginationState: PaginationState<FamilyShort>) {
    _familiesUiState.value = _familiesUiState.value.copy(
      families = families,
      paginationState = paginationState.copy(items = families) // Ensure consistency
    )
    shouldPreservePagination = true
  }

  // Load families
  fun loadFamilies() {
    loadFamiliesPaginated(resetPagination = true)
  }

  fun loadFamiliesPaginated(pageSize: Int = 30, resetPagination: Boolean = false) {
    viewModelScope.launch {
      val currentState = _familiesUiState.value.paginationState

      // Check if we should preserve existing data (e.g., navigating back from detail screen)
      val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExistingFamilyData()

      // Reset the preservation flag after checking
      if (shouldPreservePagination) {
        shouldPreservePagination = false

        // If preserving data, don't make API call
        if (shouldPreserveExistingData) {
          return@launch
        }
      }

      // For pagination (resetPagination=false), check if we already have the data
      if (!resetPagination && currentState.hasNextPage == false) {
        // No more pages to load
        return@launch
      }

      val shouldReset = resetPagination
      val cursor = if (shouldReset) null else currentState.endCursor

      // Set loading state
      _familiesUiState.value = _familiesUiState.value.copy(
        paginationState = currentState.copy(
          isInitialLoading = shouldReset || currentState.items.isEmpty(),
          isLoadingNextPage = !shouldReset && currentState.items.isNotEmpty(),
          error = null
        )
      )

      familyRepository.getItemsPaginated(pageSize = pageSize, cursor = cursor).collect { result ->
        when (result) {
          is PaginationResult.Loading -> {
            // Loading state already set above
          }

          is PaginationResult.Success -> {
            val familyShorts = result.data.map { it.toFamilyShort() }

            // Query Watchers prevent duplication automatically
            val existingFamilies = if (shouldReset) emptyList() else _familiesUiState.value.families
            val newFamilies = existingFamilies + familyShorts

            _familiesUiState.value = _familiesUiState.value.copy(
              families = newFamilies,
              hasLoadedOnce = true,
              paginationState = currentState.copy(
                items = newFamilies,
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
            _familiesUiState.value = _familiesUiState.value.copy(
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

  // Search families
  fun searchFamiliesPaginated(searchTerm: String, pageSize: Int = 30, resetPagination: Boolean = true) {
    viewModelScope.launch {
      val currentState = _familiesUiState.value.paginationState

      // Clear cache on search change
      if (resetPagination && searchTerm != currentState.currentSearchTerm) {
        // TODO: Clear Apollo cache for this query
      }

      val cursor = if (resetPagination) null else currentState.endCursor

      // Set loading state
      _familiesUiState.value = _familiesUiState.value.copy(
        searchQuery = searchTerm,
        paginationState = currentState.copy(
          isSearching = resetPagination,
          isLoadingNextPage = !resetPagination,
          error = null,
          currentSearchTerm = searchTerm
        )
      )

      familyRepository.searchItemsPaginated(
        searchTerm = searchTerm,
        pageSize = pageSize,
        cursor = cursor
      ).collect { result ->
        when (result) {
          is PaginationResult.Loading -> {
            // Loading state already set above
          }

          is PaginationResult.Success -> {
            val familyShorts = result.data.map { it.toFamilyShort() }

            // Query Watchers prevent duplication automatically
            val existingFamilies = if (resetPagination) emptyList() else currentState.items
            val newFamilies = existingFamilies + familyShorts

            _familiesUiState.value = _familiesUiState.value.copy(
              families = newFamilies,
              hasLoadedOnce = true,
              paginationState = currentState.copy(
                items = newFamilies,
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
            _familiesUiState.value = _familiesUiState.value.copy(
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

  fun loadNextFamilyPage() {
    val currentState = _familiesUiState.value.paginationState

    if (currentState.hasNextPage && !currentState.isLoadingNextPage) {
      if (currentState.currentSearchTerm.isNotBlank()) {
        searchFamiliesPaginated(
          searchTerm = currentState.currentSearchTerm,
          resetPagination = false
        )
      } else {
        loadFamiliesPaginated(resetPagination = false)
      }
    }
  }

  fun retryFamilyLoad() {
    val currentState = _familiesUiState.value.paginationState
    _familiesUiState.value = _familiesUiState.value.copy(
      paginationState = currentState.copy(showRetryButton = false)
    )

    if (currentState.currentSearchTerm.isNotBlank()) {
      searchFamiliesPaginated(
        searchTerm = currentState.currentSearchTerm,
        resetPagination = currentState.items.isEmpty()
      )
    } else {
      loadFamiliesPaginated(resetPagination = currentState.items.isEmpty())
    }
  }

  // Debounced search method
  fun searchFamiliesWithDebounce(query: String) {
    _familiesUiState.value = _familiesUiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      if (query.isBlank()) {
        // Clear search state and reset pagination completely for initial load
        _familiesUiState.value = _familiesUiState.value.copy(
          paginationState = PaginationState() // Reset pagination state completely
        )
        // Load regular families when search is cleared
        loadFamiliesPaginated(resetPagination = true)
        return@launch
      }

      // Debounce search by 1 second
      delay(1000)

      searchFamiliesPaginated(searchTerm = query.trim(), resetPagination = true)
    }
  }

  // Method to restore search query and trigger fresh search results
  fun restoreAndSearchAryaPariwar(query: String) {
    searchFamiliesWithDebounce(query)
  }

  // Calculate page size based on screen width  
  fun calculatePageSize(screenWidthDp: Float): Int {
    return when {
      screenWidthDp < 600f -> 15      // Mobile portrait
      screenWidthDp < 840f -> 25      // Tablet, mobile landscape
      else -> 35                      // Desktop, large tablets
    }
  }

  // Get family detail
  fun getFamilyDetail(familyId: String) {
    viewModelScope.launch {
      familyRepository.getFamilyDetail(familyId).collect { result ->
        when (result) {
          is Result.Loading -> {
            _familyDetailUiState.value = _familyDetailUiState.value.copy(isLoading = true, error = null)
          }

          is Result.Success -> {
            _familyDetailUiState.value =
              _familyDetailUiState.value.copy(
                isLoading = false,
                familyDetail = result.data
              )
          }

          is Result.Error -> {
            _familyDetailUiState.value =
              _familyDetailUiState.value.copy(
                isLoading = false,
                error = "परिवार विवरण प्राप्त करने में त्रुटि: ${result.message}"
              )
          }
        }
      }
    }
  }

  // Create family form methods
  fun updateFamilyName(name: String) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(familyName = name)
  }

  fun updateSelectedAryaSamaj(aryaSamaj: AryaSamaj?) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(selectedAryaSamaj = aryaSamaj)
  }

  fun updateImagePickerState(state: ImagePickerState) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(imagePickerState = state)
  }

  fun updateFamilyMembers(members: List<FamilyMemberForCreation>) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(familyMembers = members)
    updateMemberAddresses(members.map { it.member })
  }

  private fun updateMemberAddresses(members: List<Member>) {
    // First, group members by their addressId
    val addressGroups = mutableMapOf<String, MutableList<String>>()

    members.forEach { member ->
      val addressId = member.addressId
      if (addressId.isNotBlank()) {
        if (!addressGroups.containsKey(addressId)) {
          addressGroups[addressId] = mutableListOf()
        }
        addressGroups[addressId]?.add(member.id)
      }
    }

    if (addressGroups.isEmpty()) {
      // No addresses to fetch, clear member addresses
      _createFamilyUiState.value = _createFamilyUiState.value.copy(memberAddresses = emptyList())
      return
    }

    // Fetch addresses for all unique addressIds
    val addressIds = addressGroups.keys.toList()
    viewModelScope.launch {
      familyRepository.getAddressesByIds(addressIds).collect { result ->
        when (result) {
          is Result.Success -> {
            val addressDataMap =
              result.data.associate { addressNode ->
                addressNode.id to
                  AddressData(
                    address = addressNode.basicAddress ?: "",
                    district = addressNode.district ?: "",
                    state = addressNode.state ?: "",
                    pincode = addressNode.pincode ?: "",
                    vidhansabha = addressNode.vidhansabha ?: ""
                  )
              }

            val memberAddresses =
              addressGroups.map { (addressId, memberIds) ->
                AddressWithMemberId(
                  addressId = addressId,
                  memberIds = memberIds,
                  addressData = addressDataMap[addressId] ?: AddressData()
                )
              }

            _createFamilyUiState.value = _createFamilyUiState.value.copy(memberAddresses = memberAddresses)
          }

          is Result.Error -> {
            // Handle error silently for now, but keep empty member addresses
            println("Failed to fetch member addresses: ${result.message}")
            _createFamilyUiState.value = _createFamilyUiState.value.copy(memberAddresses = emptyList())
          }

          is Result.Loading -> {
            // Don't change state during loading
          }
        }
      }
    }
  }

  fun updateSelectedAddressIndex(index: Int?) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(selectedAddressIndex = index)

    if (index != null && index >= 0) {
      val selectedAddress = _createFamilyUiState.value.memberAddresses.getOrNull(index)
      if (selectedAddress != null) {
        _createFamilyUiState.value =
          _createFamilyUiState.value.copy(
            addressData = selectedAddress.addressData
          )
      }
    } else {
      // Clear address data if no address is selected
      _createFamilyUiState.value =
        _createFamilyUiState.value.copy(
          addressData = AddressData()
        )
    }
  }

  fun updateAddressData(addressData: AddressData) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(addressData = addressData)
  }

  fun updateMemberHead(
    memberId: String,
    isHead: Boolean
  ) {
    val currentMembers = _createFamilyUiState.value.familyMembers
    val updatedMembers =
      currentMembers.map { familyMember ->
        if (familyMember.member.id == memberId) {
          familyMember.copy(
            isHead = isHead,
            relationToHead = if (isHead) FamilyRelation.SELF else null
          )
        } else if (isHead) {
          familyMember.copy(isHead = false, relationToHead = null)
        } else {
          familyMember
        }
      }
    _createFamilyUiState.value = _createFamilyUiState.value.copy(familyMembers = updatedMembers)
  }

  fun updateMemberRelation(
    memberId: String,
    relation: FamilyRelation?
  ) {
    val currentMembers = _createFamilyUiState.value.familyMembers
    val updatedMembers =
      currentMembers.map { familyMember ->
        if (familyMember.member.id == memberId) {
          familyMember.copy(relationToHead = relation)
        } else {
          familyMember
        }
      }
    _createFamilyUiState.value = _createFamilyUiState.value.copy(familyMembers = updatedMembers)
  }

  fun updateFamilyId(familyId: String) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(familyId = familyId)
  }

  // Load family data for editing
  fun loadFamilyForEditing(familyId: String) {
    viewModelScope.launch {
      _createFamilyUiState.value = _createFamilyUiState.value.copy(isLoading = true, error = null)

      familyRepository.getFamilyDetail(familyId).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Already handling in UI state above
          }

          is Result.Success -> {
            val familyData = result.data.familyFields

            // Populate basic family info
            _createFamilyUiState.value = _createFamilyUiState.value.copy(
              familyName = familyData.name ?: "",
              isLoading = false
            )

            // Set family photos
            val imageUrls = familyData.photos?.filterNotNull() ?: emptyList()
            if (imageUrls.isNotEmpty()) {
              val imagePickerState = ImagePickerState(existingImageUrls = imageUrls)
              _createFamilyUiState.value = _createFamilyUiState.value.copy(
                imagePickerState = imagePickerState
              )
            }

            // Set selected Arya Samaj
            familyData.aryaSamaj?.let { aryaSamaj ->
              val selectedAryaSamaj = AryaSamaj(
                id = aryaSamaj.id,
                name = aryaSamaj.name ?: "",
                address = aryaSamaj.address?.basicAddress ?: "",
                district = aryaSamaj.address?.district ?: ""
              )
              _createFamilyUiState.value = _createFamilyUiState.value.copy(
                selectedAryaSamaj = selectedAryaSamaj
              )
            }

            // Set address data
            familyData.address?.let { address ->
              val addressData = AddressData(
                address = address.basicAddress ?: "",
                state = address.state ?: "",
                district = address.district ?: "",
                pincode = address.pincode ?: "",
                vidhansabha = address.vidhansabha ?: ""
              )
              _createFamilyUiState.value = _createFamilyUiState.value.copy(
                addressData = addressData,
                selectedAddressIndex = -1 // Use custom address
              )
            }

            // Convert family members to FamilyMemberForCreation
            val familyMembers = familyData.familyMemberCollection?.edges?.mapNotNull { edge ->
              edge.node.member?.let { memberWrapper ->
                val memberDetails = memberWrapper.memberDetails
                val member = Member(
                  id = memberDetails.id,
                  name = memberDetails.name,
                  phoneNumber = memberDetails.phoneNumber ?: "",
                  profileImage = memberDetails.profileImage ?: "",
                  addressId = memberDetails.address?.id ?: ""
                )

                FamilyMemberForCreation(
                  member = member,
                  isHead = edge.node.isHead,
                  relationToHead = edge.node.relationToHead.toComponents()
                )
              }
            } ?: emptyList()

            _createFamilyUiState.value = _createFamilyUiState.value.copy(
              familyMembers = familyMembers
            )
          }

          is Result.Error -> {
            _createFamilyUiState.value = _createFamilyUiState.value.copy(
              isLoading = false,
              error = "परिवार डेटा लोड करने में त्रुटि: ${result.message}"
            )
          }
        }
      }
    }
  }

  // Search members without family
  fun searchMembersWithoutFamily(query: String) {
    viewModelScope.launch {
      _createFamilyUiState.value = _createFamilyUiState.value.copy(isLoadingMembers = true)
      familyRepository.searchMembersWithoutFamily(query).collect { result ->
        when (result) {
          is Result.Success -> {
            val members =
              result.data.map { memberDetails ->
                Member(
                  id = memberDetails.id!!,
                  name = memberDetails.name!!,
                  phoneNumber = memberDetails.phoneNumber!!,
                  profileImage = memberDetails.profileImage ?: "",
                  addressId = memberDetails.addressId ?: ""
                )
              }
            _createFamilyUiState.value =
              _createFamilyUiState.value.copy(
                availableMembers = members,
                isLoadingMembers = false
              )
          }

          is Result.Error -> {
            // Handle search errors silently for now, but log them
            println("Failed to search members: ${result.message}")
            _createFamilyUiState.value = _createFamilyUiState.value.copy(isLoadingMembers = false)
          }

          is Result.Loading -> {
            // isLoadingMembers is already set to true
          }
        }
      }
    }
  }

  // Load AryaSamajs
  fun loadAryaSamajs() {
    viewModelScope.launch {
      adminRepository.getAllAryaSamajs().collect { result ->
        when (result) {
          is Result.Success -> {
            _createFamilyUiState.value =
              _createFamilyUiState.value.copy(
                availableAryaSamajs = result.data
              )
          }

          is Result.Error -> {
            // Handle error silently for now
          }

          is Result.Loading -> {
            // Don't show loading for AryaSamajs
          }
        }
      }
    }
  }

  fun updateFamily(familyId: String) {
    viewModelScope.launch {
      _createFamilyUiState.value = _createFamilyUiState.value.copy(isSubmitting = true, error = null)

      try {
        val currentState = _createFamilyUiState.value

        // Step 1: Upload images if any
        val imageUrls = mutableListOf<String>()
        if (currentState.imagePickerState.newImages.isNotEmpty()) {
          for (file in currentState.imagePickerState.newImages) {
            val bytesToUpload = if (currentState.imagePickerState.hasCompressedData(file)) {
              currentState.imagePickerState.getCompressedBytes(file)!!
            } else {
              file.readBytes()
            }

            val uploadResult = fileUploadUtils.uploadCompressedImage(
              imageBytes = bytesToUpload,
              folder = "family_photos",
              extension = "webp"
            )

            when (uploadResult) {
              is Result.Success -> imageUrls.add(uploadResult.data)
              is Result.Error -> {
                _createFamilyUiState.value = _createFamilyUiState.value.copy(
                  isSubmitting = false,
                  error = "चित्र अपलोड विफल: ${uploadResult.message}। कृपया पुनः प्रयास करें।"
                )
                return@launch
              }

              is Result.Loading -> {} // This shouldn't happen in a suspend function
            }
          }
        }

        // Add existing image URLs
        imageUrls.addAll(currentState.imagePickerState.getActiveImageUrls())

        // Step 2: Prepare family members data
        val familyMemberData =
          currentState.familyMembers.map { familyMember ->
            FamilyMemberData(
              memberId = familyMember.member.id,
              isHead = familyMember.isHead,
              relationToHead = familyMember.relationToHead?.toGraphQL()
            )
          }

        // Step 3: Handle address (create new if needed)
        var addressId: String? = null

        if (currentState.selectedAddressIndex == null || currentState.selectedAddressIndex == -1) {
          // Create new address
          adminRepository.createAddress(
            basicAddress = currentState.addressData.address,
            state = currentState.addressData.state,
            district = currentState.addressData.district,
            pincode = currentState.addressData.pincode,
            latitude = null, // AddressData doesn't have location field
            longitude = null,
            vidhansabha = currentState.addressData.vidhansabha.takeIf { it.isNotBlank() }
          ).collect { result ->
            when (result) {
              is Result.Success -> addressId = result.data
              is Result.Error -> {
                _createFamilyUiState.value =
                  _createFamilyUiState.value.copy(
                    isSubmitting = false,
                    error = "पता बनाने में त्रुटि: ${result.message}"
                  )
                return@collect
              }

              is Result.Loading -> {}
            }
          }
        } else {
          // Use existing address
          val selectedAddress = currentState.memberAddresses.getOrNull(currentState.selectedAddressIndex)
          addressId = selectedAddress?.addressId
        }

        if (addressId == null) {
          _createFamilyUiState.value =
            _createFamilyUiState.value.copy(
              isSubmitting = false,
              error = "पता आईडी प्राप्त नहीं हुई"
            )
          return@launch
        }

        // Step 4: Update family
        familyRepository.updateFamily(
          familyId = familyId,
          name = currentState.familyName,
          addressId = addressId,
          aryaSamajId = currentState.selectedAryaSamaj?.id,
          photos = imageUrls
        ).collect { result ->
          when (result) {
            is Result.Success -> {
              // Step 5: Update family members if needed
              if (familyMemberData.isNotEmpty()) {
                familyRepository.updateFamilyMembers(
                  familyId = familyId,
                  members = familyMemberData
                ).collect { memberResult ->
                  when (memberResult) {
                    is Result.Success -> {
                      _createFamilyUiState.value =
                        _createFamilyUiState.value.copy(
                          isSubmitting = false,
                          submitSuccess = true
                        )
                      GlobalMessageManager.showSuccess("परिवार और सदस्य सफलतापूर्वक संपादित किए गए")
                    }

                    is Result.Error -> {
                      _createFamilyUiState.value =
                        _createFamilyUiState.value.copy(
                          isSubmitting = false,
                          error = "परिवार सदस्य अपडेट करने में त्रुटि: ${memberResult.message}"
                        )
                    }

                    is Result.Loading -> {
                      // Already handling loading state
                    }
                  }
                }
              } else {
                _createFamilyUiState.value =
                  _createFamilyUiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = true
                  )
                GlobalMessageManager.showSuccess("परिवार सफलतापूर्वक संपादित किया गया")
              }
            }

            is Result.Error -> {
              _createFamilyUiState.value =
                _createFamilyUiState.value.copy(
                  isSubmitting = false,
                  error = "परिवार अपडेट करने में त्रुटि: ${result.message}"
                )
            }

            is Result.Loading -> {}
          }
        }
      } catch (e: Exception) {
        _createFamilyUiState.value =
          _createFamilyUiState.value.copy(
            isSubmitting = false,
            error = "परिवार अपडेट करने में त्रुटि: ${e.message}"
          )
      }
    }
  }

  // Submit family creation
  fun createFamily() {
    viewModelScope.launch {
      _createFamilyUiState.value = _createFamilyUiState.value.copy(isSubmitting = true, error = null)

      try {
        val currentState = _createFamilyUiState.value

        // Step 1: Upload images if any
        val imageUrls = mutableListOf<String>()
        if (currentState.imagePickerState.newImages.isNotEmpty()) {
          for (file in currentState.imagePickerState.newImages) {
            val bytesToUpload = if (currentState.imagePickerState.hasCompressedData(file)) {
              currentState.imagePickerState.getCompressedBytes(file)!!
            } else {
              file.readBytes()
            }

            val uploadResult = fileUploadUtils.uploadCompressedImage(
              imageBytes = bytesToUpload,
              folder = "family_photos",
              extension = "webp"
            )

            when (uploadResult) {
              is Result.Success -> imageUrls.add(uploadResult.data)
              is Result.Error -> {
                _createFamilyUiState.value =
                  _createFamilyUiState.value.copy(
                    isSubmitting = false,
                    error = "चित्र अपलोड विफल: ${uploadResult.message}। कृपया पुनः प्रयास करें।"
                  )
                return@launch
              }

              is Result.Loading -> {} // This shouldn't happen in a suspend function
            }
          }
        }

        // Add existing image URLs
        imageUrls.addAll(currentState.imagePickerState.getActiveImageUrls())

        // Step 2: Prepare family members data
        val familyMemberData =
          currentState.familyMembers.map { familyMember ->
            FamilyMemberData(
              memberId = familyMember.member.id,
              isHead = familyMember.isHead,
              relationToHead = familyMember.relationToHead?.toGraphQL()
            )
          }

        // Step 3: Determine address parameters
        var addressId: String? = null
        var basicAddress: String? = null
        var state: String? = null
        var district: String? = null
        var pincode: String? = null
        var vidhansabha: String? = null
        var latitude: Double? = null
        var longitude: Double? = null

        if (currentState.selectedAddressIndex != null && currentState.selectedAddressIndex >= 0) {
          // Use existing address
          val selectedAddress = currentState.memberAddresses.getOrNull(currentState.selectedAddressIndex)
          addressId = selectedAddress?.addressId
        } else {
          // Use new address fields
          basicAddress = currentState.addressData.address.takeIf { it.isNotBlank() }
          state = currentState.addressData.state.takeIf { it.isNotBlank() }
          district = currentState.addressData.district.takeIf { it.isNotBlank() }
          pincode = currentState.addressData.pincode.takeIf { it.isNotBlank() }
          vidhansabha = currentState.addressData.vidhansabha.takeIf { it.isNotBlank() }
          latitude = currentState.addressData.location?.latitude
          longitude = currentState.addressData.location?.longitude
        }

        // Step 4: Create family using the new repository method
        familyRepository.createFamily(
          name = currentState.familyName,
          aryaSamajId = currentState.selectedAryaSamaj?.id ?: throw Exception("आर्य समाज चुनना आवश्यक है"),
          photos = imageUrls,
          familyMembers = familyMemberData,
          addressId = addressId,
          basicAddress = basicAddress,
          state = state,
          district = district,
          pincode = pincode,
          vidhansabha = vidhansabha,
          latitude = latitude,
          longitude = longitude
        ).collect { result ->
          when (result) {
            is Result.Success -> {
              _createFamilyUiState.value =
                _createFamilyUiState.value.copy(
                  isSubmitting = false,
                  submitSuccess = true,
                  familyId = result.data
                )
              GlobalMessageManager.showSuccess("परिवार सफलतापूर्वक बनाया गया")
            }

            is Result.Error -> {
              _createFamilyUiState.value =
                _createFamilyUiState.value.copy(
                  isSubmitting = false,
                  error = "परिवार बनाने में त्रुटि: ${result.message}"
                )
            }

            is Result.Loading -> {}
          }
        }
      } catch (e: Exception) {
        _createFamilyUiState.value =
          _createFamilyUiState.value.copy(
            isSubmitting = false,
            error = "परिवार बनाने में त्रुटि: ${e.message}"
          )
      }
    }
  }

  fun clearCreateFamilyState() {
    _createFamilyUiState.value = CreateFamilyUiState()
  }

  fun clearError() {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(error = null)
    _familiesUiState.value = _familiesUiState.value.copy(error = null)
    _familyDetailUiState.value = _familyDetailUiState.value.copy(error = null)
  }

  fun deleteFamily(familyId: String, onSuccess: (() -> Unit)? = null) {
    viewModelScope.launch {
      _deleteFamilyState.value = DeleteFamilyState(
        isDeleting = true,
        deletingFamilyId = familyId
      )

      familyRepository.deleteFamily(familyId).collect { result ->
        when (result) {
          is Result.Loading -> {
            // Loading state already set above
          }

          is Result.Success -> {
            // Refresh the families list
            loadFamiliesPaginated(resetPagination = true)
            // Clear shouldPreservePagination flag to prevent interference
            shouldPreservePagination = false
            // Call the success callback for parent updates
            onSuccess?.invoke()
            _deleteFamilyState.value = DeleteFamilyState(
              deleteSuccess = true,
              deletingFamilyId = familyId
            )
          }

          is Result.Error -> {
            _deleteFamilyState.value = DeleteFamilyState(
              deleteError = result.message,
              deletingFamilyId = familyId
            )
          }
        }
      }
    }
  }

  fun resetDeleteState() {
    _deleteFamilyState.value = DeleteFamilyState()
    shouldPreservePagination = false
  }

  fun loadFamilyAndFamilyMemberCount() {
    viewModelScope.launch {
      _familiesUiState.value = _familiesUiState.value.copy(isLoading = true, error = null)
      familyRepository.getFamilyAndMembersCount().collect { result ->
        when (result) {
          is Result.Loading -> {
            _familiesUiState.value = _familiesUiState.value.copy(isLoading = true, error = null)
          }

          is Result.Success -> {
            val (familyCount, memberCount) = result.data
            _familiesUiState.value = _familiesUiState.value.copy(
              isLoading = false,
              familyCount = familyCount,
              familyMemberCount = memberCount
            )
          }

          is Result.Error -> {
            _familiesUiState.value =
              _familiesUiState.value.copy(
                isLoading = false,
                error = "Counts लोड करने में त्रुटि: ${result.message}"
              )
          }
        }
      }
    }
  }
}

