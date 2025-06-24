package org.aryamahasangh.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.aryamahasangh.components.AddressData
import org.aryamahasangh.components.AryaSamaj
import org.aryamahasangh.components.ImagePickerState
import org.aryamahasangh.components.getActiveImageUrls
import org.aryamahasangh.fragment.FamilyFields
import org.aryamahasangh.fragment.MemberDetails
import org.aryamahasangh.util.Result
import org.aryamahasangh.utils.FileUploadUtils
import org.aryamahasangh.features.activities.Member
import org.aryamahasangh.components.FamilyRelation
import kotlinx.coroutines.launch

data class FamiliesUiState(
  val isLoading: Boolean = false,
  val families: List<FamilyShort> = emptyList(),
  val searchQuery: String = "",
  val searchResults: List<FamilyShort> = emptyList(),
  val error: String? = null
)

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
  val availableAryaSamajs: List<AryaSamaj> = emptyList(),
  val availableMembers: List<Member> = emptyList(),
  val memberAddresses: List<AddressWithMemberId> = emptyList(),
  val error: String? = null,
  val submitSuccess: Boolean = false
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

  // Load families
  fun loadFamilies() {
    viewModelScope.launch {
      familyRepository.getFamilies().collect { result ->
        when (result) {
          is Result.Loading -> {
            _familiesUiState.value = _familiesUiState.value.copy(isLoading = true, error = null)
          }

          is Result.Success -> {
            val families = result.data.map { familyFields ->
              familyFields.toFamilyShort()
            }
            _familiesUiState.value = _familiesUiState.value.copy(isLoading = false, families = families)
          }
          is Result.Error -> {
            _familiesUiState.value = _familiesUiState.value.copy(
              isLoading = false,
              error = "परिवार लोड करने में त्रुटि: ${result.message}"
            )
          }
        }
      }
    }
  }

  // Search families
  fun searchFamilies(query: String) {
    _familiesUiState.value = _familiesUiState.value.copy(searchQuery = query)

    if (query.isBlank()) {
      _familiesUiState.value = _familiesUiState.value.copy(searchResults = emptyList())
      return
    }

    viewModelScope.launch {
      familyRepository.searchFamilies(query).collect { result ->
        when (result) {
          is Result.Success -> {
            val searchResults = result.data.map { familyFields ->
              familyFields.toFamilyShort()
            }
            _familiesUiState.value = _familiesUiState.value.copy(searchResults = searchResults)
          }
          is Result.Error -> {
            // Handle search errors silently
          }

          is Result.Loading -> {
            // Don't show loading for search
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

  fun updateSelectedAddressIndex(index: Int?) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(selectedAddressIndex = index)

    if (index != null && index >= 0) {
      val selectedAddress = _createFamilyUiState.value.memberAddresses.getOrNull(index)
      if (selectedAddress != null) {
        _createFamilyUiState.value = _createFamilyUiState.value.copy(
          addressData = selectedAddress.addressData
        )
      }
    } else {
      // Clear address data if no address is selected
      _createFamilyUiState.value = _createFamilyUiState.value.copy(
        addressData = AddressData()
      )
    }
  }

  fun updateAddressData(addressData: AddressData) {
    _createFamilyUiState.value = _createFamilyUiState.value.copy(addressData = addressData)
  }

  fun updateMemberHead(memberId: String, isHead: Boolean) {
    val currentMembers = _createFamilyUiState.value.familyMembers
    val updatedMembers = currentMembers.map { familyMember ->
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

  fun updateMemberRelation(memberId: String, relation: FamilyRelation?) {
    val currentMembers = _createFamilyUiState.value.familyMembers
    val updatedMembers = currentMembers.map { familyMember ->
      if (familyMember.member.id == memberId) {
        familyMember.copy(relationToHead = relation)
      } else {
        familyMember
      }
    }
    _createFamilyUiState.value = _createFamilyUiState.value.copy(familyMembers = updatedMembers)
  }

  // Search members without family
  fun searchMembersWithoutFamily(query: String) {
    viewModelScope.launch {
      _createFamilyUiState.value = _createFamilyUiState.value.copy(isLoadingMembers = true)
      familyRepository.searchMembersWithoutFamily(query).collect { result ->
        when (result) {
          is Result.Success -> {
            val members = result.data.map { memberDetails ->
              Member(
                id = memberDetails.id!!,
                name = memberDetails.name!!,
                phoneNumber = memberDetails.phoneNumber!!,
                profileImage = memberDetails.profileImage ?: "",
                addressId = memberDetails.addressId ?: ""
              )
            }
            _createFamilyUiState.value = _createFamilyUiState.value.copy(
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
            _createFamilyUiState.value = _createFamilyUiState.value.copy(
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
            val addressDataMap = result.data.associate { addressNode ->
              addressNode.id to AddressData(
                address = addressNode.basicAddress ?: "",
                district = addressNode.district ?: "",
                state = addressNode.state ?: "",
                pincode = addressNode.pincode ?: "",
                vidhansabha = addressNode.vidhansabha ?: ""
              )
            }

            val memberAddresses = addressGroups.map { (addressId, memberIds) ->
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

  // Submit family creation
  fun createFamily() {
    viewModelScope.launch {
      _createFamilyUiState.value = _createFamilyUiState.value.copy(isSubmitting = true, error = null)

      try {
        val currentState = _createFamilyUiState.value

        // Step 1: Upload images if any
        val imageUrls = mutableListOf<String>()
        if (currentState.imagePickerState.newImages.isNotEmpty()) {
          val uploadResult = fileUploadUtils.uploadFiles(
            currentState.imagePickerState.newImages,
            "family_photos"
          )
          when (uploadResult) {
            is Result.Success -> imageUrls.addAll(uploadResult.data)
            is Result.Error -> {
              _createFamilyUiState.value = _createFamilyUiState.value.copy(
                isSubmitting = false,
                error = uploadResult.message
              )
              return@launch
            }

            is Result.Loading -> {} // This shouldn't happen in a suspend function
          }
        }

        // Add existing image URLs
        imageUrls.addAll(currentState.imagePickerState.getActiveImageUrls())

        // Step 2: Create or update address
        var addressId: String? = null

        if (currentState.selectedAddressIndex == null || currentState.selectedAddressIndex == -1) {
          // Create new address
          adminRepository.createAddress(
            basicAddress = currentState.addressData.address,
            state = currentState.addressData.state,
            district = currentState.addressData.district,
            pincode = currentState.addressData.pincode,
            latitude = currentState.addressData.location?.latitude,
            longitude = currentState.addressData.location?.longitude,
            vidhansabha = currentState.addressData.vidhansabha.takeIf { it.isNotBlank() }
          ).collect { result ->
            when (result) {
              is Result.Success -> addressId = result.data
              is Result.Error -> {
                _createFamilyUiState.value = _createFamilyUiState.value.copy(
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
          _createFamilyUiState.value = _createFamilyUiState.value.copy(
            isSubmitting = false,
            error = "पता आईडी प्राप्त नहीं हुई"
          )
          return@launch
        }
println("submit: $imageUrls")
        // Step 3: Create family
        var familyId: String? = null
        familyRepository.createFamily(
          name = currentState.familyName,
          addressId = addressId,
          aryaSamajId = currentState.selectedAryaSamaj?.id,
          photos = imageUrls
        ).collect { result ->
          when (result) {
            is Result.Success -> familyId = result.data
            is Result.Error -> {
              _createFamilyUiState.value = _createFamilyUiState.value.copy(
                isSubmitting = false,
                error = "परिवार बनाने में त्रुटि: ${result.message}"
              )
              return@collect
            }

            is Result.Loading -> {}
          }
        }

        if (familyId == null) {
          _createFamilyUiState.value = _createFamilyUiState.value.copy(
            isSubmitting = false,
            error = "परिवार आईडी प्राप्त नहीं हुई"
          )
          return@launch
        }

        // Step 4: Add family members
        val familyMemberData = currentState.familyMembers.map { familyMember ->
          FamilyMemberData(
            memberId = familyMember.member.id,
            isHead = familyMember.isHead,
            relationToHead = familyMember.relationToHead?.toGraphQL()
          )
        }

        val finalAddressId = addressId ?: return@launch
        familyRepository.addMembersToFamily(familyId = familyId, members = familyMemberData)
          .collect { result ->
            when (result) {
              is Result.Success -> {
                // Step 5: Update member addresses if needed
                if (currentState.selectedAddressIndex != null && currentState.selectedAddressIndex >= 0) {
                  val memberIds = currentState.familyMembers.map { it.member.id }
                  familyRepository.updateMemberAddresses(memberIds, finalAddressId).collect { updateResult ->
                    when (updateResult) {
                      is Result.Success -> {
                        _createFamilyUiState.value = _createFamilyUiState.value.copy(
                          isSubmitting = false,
                          submitSuccess = true
                        )
                      }

                      is Result.Error -> {
                        // Family was created successfully, but address update failed
                        // Still consider this a success
                        _createFamilyUiState.value = _createFamilyUiState.value.copy(
                          isSubmitting = false,
                          submitSuccess = true,
                          error = "परिवार बनाया गया लेकिन सदस्य पते अपडेट नहीं हुए: ${updateResult.message}"
                        )
                      }

                      is Result.Loading -> {}
                    }
                  }
                } else {
                  _createFamilyUiState.value = _createFamilyUiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = true
                  )
                }
              }

              is Result.Error -> {
                _createFamilyUiState.value = _createFamilyUiState.value.copy(
                  isSubmitting = false,
                  error = "पारिवारिक सदस्य जोड़ने में त्रुटि: ${result.message}"
                )
              }

              is Result.Loading -> {}
            }
          }
      } catch (e: Exception) {
        _createFamilyUiState.value = _createFamilyUiState.value.copy(
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
  }
}

// Extension functions to convert between generated types and UI models
private fun FamilyFields.toFamilyShort(): FamilyShort {
  return FamilyShort(
    id = id,
    name = name ?: "",
    photos = photos?.filterNotNull() ?: emptyList(),
    address = address?.let { addr ->
      "${addr.basicAddress ?: ""}, ${addr.district ?: ""}, ${addr.state ?: ""}"
    }?.trim()?.let { if (it == ", , ") "" else it } ?: "",
    aryaSamajName = aryaSamaj?.name ?: ""
  )
}
