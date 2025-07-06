package com.aryamahasangh.features.admin.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.components.MembersState
import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.ErrorHandler
import com.aryamahasangh.domain.error.getUserMessage
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.fragment.AryaSamajWithAddress
import com.aryamahasangh.network.bucket
import com.aryamahasangh.viewmodel.ErrorState
import com.aryamahasangh.viewmodel.handleResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AryaSamajListUiState(
  val aryaSamajs: List<AryaSamajListItem> = emptyList(),
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null,
  val searchQuery: String = "",
  val totalCount: Int = 0,
  val paginationState: PaginationState<AryaSamajWithAddress> = PaginationState()
) : ErrorState

data class AryaSamajDetailUiState(
  val aryaSamaj: AryaSamajDetail? = null,
  override val isLoading: Boolean = false,
  override val error: String? = null,
  override val appError: AppError? = null
) : ErrorState

data class AryaSamajFormUiState(
  val formData: AryaSamajFormData = AryaSamajFormData(),
  val isSubmitting: Boolean = false,
  val submitSuccess: Boolean = false,
  val submitError: String? = null,
  val validationErrors: Map<String, String> = emptyMap(),
  val hasUnsavedChanges: Boolean = false,
  val editingAryaSamajId: String? = null, // Track which AryaSamaj we're editing
  val originalFormData: AryaSamajFormData? = null, // Track original data for change detection
  val createdAryaSamajId: String? = null
) {
  private val initialFormData = AryaSamajFormData()

  fun updateHasUnsavedChanges(): AryaSamajFormUiState {
    val baselineData = originalFormData ?: initialFormData
    return copy(hasUnsavedChanges = formData.hasUnsavedChanges(baselineData))
  }

  val isEditMode: Boolean get() = editingAryaSamajId != null
}

class AryaSamajViewModel(private val repository: AryaSamajRepository) : ViewModel() {
  private val _listUiState = MutableStateFlow(AryaSamajListUiState())
  val listUiState: StateFlow<AryaSamajListUiState> = _listUiState.asStateFlow()

  private val _detailUiState = MutableStateFlow(AryaSamajDetailUiState())
  val detailUiState: StateFlow<AryaSamajDetailUiState> = _detailUiState.asStateFlow()

  private val _formUiState = MutableStateFlow(AryaSamajFormUiState())
  val formUiState: StateFlow<AryaSamajFormUiState> = _formUiState.asStateFlow()

  private var searchJob: Job? = null

  // Flag to track if pagination should be preserved (e.g., when navigating back)
  private var shouldPreservePagination = false

  // Method to check if we have existing data and should preserve it
  fun hasExistingAryaSamajData(): Boolean {
    return _listUiState.value.aryaSamajs.isNotEmpty()
  }

  // Method to preserve pagination state when navigating back
  fun preserveAryaSamajPagination(
    savedAryaSamajs: List<AryaSamajWithAddress>,
    savedPaginationState: PaginationState<AryaSamajWithAddress>
  ) {
    // Convert AryaSamajWithAddress to AryaSamajListItem for display
    val listItems = savedAryaSamajs.map { aryaSamaj ->
      AryaSamajListItem(
        id = aryaSamaj.aryaSamajFields.id,
        name = aryaSamaj.aryaSamajFields.name ?: "",
        description = aryaSamaj.aryaSamajFields.description ?: "",
        formattedAddress = aryaSamaj.getFormattedAddress(),
        memberCount = 0, // We don't have member count in AryaSamajWithAddress
        mediaUrls = aryaSamaj.aryaSamajFields.mediaUrls?.filterNotNull() ?: emptyList()
      )
    }

    _listUiState.value = _listUiState.value.copy(
      aryaSamajs = listItems,
      paginationState = savedPaginationState
    )
    shouldPreservePagination = true
  }

  // List operations
//  fun loadAryaSamajs() {
//    println("loadAryaSamajs")
//    viewModelScope.launch {
//      repository.getAryaSamajs().collect { result ->
//        result.handleResult(
//          onLoading = {
//            _listUiState.value =
//              _listUiState.value.copy(
//                isLoading = true,
//                error = null,
//                appError = null
//              )
//          },
//          onSuccess = { aryaSamajs ->
//            _listUiState.value =
//              _listUiState.value.copy(
//                aryaSamajs = aryaSamajs,
//                isLoading = false,
//                error = null,
//                appError = null
//              )
//          },
//          onError = { appError ->
//            ErrorHandler.logError(appError, "AryaSamajViewModel.loadAryaSamajs")
//            _listUiState.value =
//              _listUiState.value.copy(
//                isLoading = false,
//                error = appError.getUserMessage(),
//                appError = appError
//              )
//          }
//        )
//      }
//    }
//  }

  fun searchAryaSamajs(query: String) {
    _listUiState.value = _listUiState.value.copy(searchQuery = query)
  }

  fun deleteAryaSamaj(id: String) {
    viewModelScope.launch {
      repository.deleteAryaSamaj(id).collect { result ->
        result.handleResult(
          onLoading = {
            // Could add a loading state for delete if needed
          },
          onSuccess = { _ ->
            // Refresh the list
            loadAryaSamajsPaginated(resetPagination = true)
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AryaSamajViewModel.deleteAryaSamaj")
            _listUiState.value =
              _listUiState.value.copy(
                error = appError.getUserMessage(),
                appError = appError
              )
          }
        )
      }
    }
  }

  // Detail operations
  fun loadAryaSamajDetail(id: String) {
    viewModelScope.launch {
      repository.getAryaSamajDetail(id).collect { result ->
        result.handleResult(
          onLoading = {
            _detailUiState.value =
              _detailUiState.value.copy(
                isLoading = true,
                error = null,
                appError = null
              )
          },
          onSuccess = { aryaSamaj ->
            _detailUiState.value =
              _detailUiState.value.copy(
                aryaSamaj = aryaSamaj,
                isLoading = false,
                error = null,
                appError = null
              )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AryaSamajViewModel.loadAryaSamajDetail")
            _detailUiState.value =
              _detailUiState.value.copy(
                isLoading = false,
                error = appError.getUserMessage(),
                appError = appError
              )
          }
        )
      }
    }
  }

  // Form operations
  fun updateFormData(formData: AryaSamajFormData) {
    _formUiState.value =
      _formUiState.value.copy(
        formData = formData
      ).updateHasUnsavedChanges()
  }

  fun submitForm() {
    val currentState = _formUiState.value
    val formData = currentState.formData

    // Validate form
    val validationErrors = validateForm(formData)

    if (validationErrors.isNotEmpty()) {
      _formUiState.value =
        currentState.copy(
          validationErrors = validationErrors
        )
      return
    }

    viewModelScope.launch {
      _formUiState.value =
        currentState.copy(
          isSubmitting = true,
          submitError = null,
          validationErrors = emptyMap()
        )

      try {
        // Step 1: Upload new images first if there are any
        val uploadedImageUrls = mutableListOf<String>()

        // Add existing images that weren't deleted
        uploadedImageUrls.addAll(
          formData.imagePickerState.existingImageUrls.filterNot {
            it in formData.imagePickerState.deletedImageUrls
          }
        )

        // Upload new images
        formData.imagePickerState.newImages.forEach { file ->
          try {
            // Simple random number for file naming
            val randomNum = Clock.System.now().epochSeconds
            val fileType = file.name.substringAfterLast('.')
            val uploadResponse =
              bucket.upload(
                path = "arya_samaj_$randomNum.$fileType",
                data = file.readBytes()
              )
            val publicUrl = bucket.publicUrl(uploadResponse.path)
            uploadedImageUrls.add(publicUrl)
          } catch (e: Exception) {
            throw Exception("चित्र अपलोड करने में त्रुटि: ${e.message}")
          }
        }

        // Step 2: Create/Update AryaSamaj with uploaded image URLs
        val updatedFormData =
          formData.copy(
            imagePickerState =
              formData.imagePickerState.copy(
                existingImageUrls = uploadedImageUrls,
                newImages = emptyList() // Clear new images since they're now uploaded
              )
          )

        if (currentState.editingAryaSamajId != null) {
          // Update existing AryaSamaj
          repository.updateAryaSamaj(currentState.editingAryaSamajId, updatedFormData).collect { result ->
            result.handleResult(
              onLoading = {
                // Already handled above
              },
              onSuccess = { _ ->
                _formUiState.value =
                  _formUiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = true,
                    hasUnsavedChanges = false,
                    editingAryaSamajId = null,
                    originalFormData = null
                  )
                // Refresh the list
                loadAryaSamajsPaginated(resetPagination = true)
              },
              onError = { appError ->
                ErrorHandler.logError(appError, "AryaSamajViewModel.submitForm")
                _formUiState.value =
                  _formUiState.value.copy(
                    isSubmitting = false,
                    submitError = appError.getUserMessage()
                  )
              }
            )
          }
        } else {
          // Create new AryaSamaj
          repository.createAryaSamaj(updatedFormData).collect { result ->
            result.handleResult(
              onLoading = {
                // Already handled above
              },
              onSuccess = { aryaSamajId ->
                _formUiState.value =
                  _formUiState.value.copy(
                    isSubmitting = false,
                    submitSuccess = true,
                    hasUnsavedChanges = false,
                    editingAryaSamajId = null,
                    originalFormData = null,
                    createdAryaSamajId = aryaSamajId
                  )

                // Refresh the list
                loadAryaSamajsPaginated(resetPagination = true)
              },
              onError = { appError ->
                ErrorHandler.logError(appError, "AryaSamajViewModel.submitForm")
                _formUiState.value =
                  _formUiState.value.copy(
                    isSubmitting = false,
                    submitError = appError.getUserMessage()
                  )
              }
            )
          }
        }
      } catch (e: Exception) {
        _formUiState.value =
          _formUiState.value.copy(
            isSubmitting = false,
            submitError = e.message ?: "अज्ञात त्रुटि हुई"
          )
      }
    }
  }

  private fun validateForm(formData: AryaSamajFormData): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    if (formData.name.isBlank()) {
      errors["name"] = "आर्य समाज का नाम अपेक्षित है"
    }

    if (formData.description.isBlank()) {
      errors["description"] = "विवरण अपेक्षित है"
    }

    // Location (latitude/longitude) is mandatory for every address entry
    if (formData.addressData.location == null) {
      errors["location"] = "स्थान चुनना अपेक्षित है"
    }

    if (formData.addressData.address.isBlank()) {
      errors["address"] = "पता अपेक्षित है"
    }

    if (formData.addressData.state.isBlank()) {
      errors["state"] = "राज्य चुनना अपेक्षित है"
    }

    if (formData.addressData.district.isBlank()) {
      errors["district"] = "जिला चुनना अपेक्षित है"
    }

    return errors
  }

  fun resetFormState() {
    _formUiState.value = AryaSamajFormUiState()
  }

  // Error handling
  fun clearListError() {
    _listUiState.value = _listUiState.value.copy(error = null, appError = null)
  }

  fun clearDetailError() {
    _detailUiState.value = _detailUiState.value.copy(error = null, appError = null)
  }

  fun clearFormError() {
    _formUiState.value = _formUiState.value.copy(submitError = null)
  }

  // New methods
  fun getAryaSamajCount() {
    viewModelScope.launch {
      repository.getAryaSamajCount().collect { result ->
        result.handleResult(
          onLoading = {
            _listUiState.value = _listUiState.value.copy(isLoading = true)
          },
          onSuccess = { count ->
            _listUiState.value = _listUiState.value.copy(totalCount = count, isLoading = false)
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AryaSamajViewModel.getAryaSamajCount")
            _listUiState.value =
              _listUiState.value.copy(
                isLoading = false,
                error = appError.getUserMessage(),
                appError = appError
              )
          }
        )
      }
    }
  }

  fun filterAryaSamajsByAddress(address: String) {
    _listUiState.value = _listUiState.value.copy(searchQuery = address)
  }

  fun searchAryaSamajByAddress(state: String? = null, district: String? = null, vidhansabha: String? = null) {
    viewModelScope.launch {
      repository.getAryaSamajByAddress(state, district, vidhansabha).collect { result ->
        result.handleResult(
          onLoading = {
            _listUiState.value = _listUiState.value.copy(isLoading = true, error = null, appError = null)
          },
          onSuccess = { aryaSamajs ->
            _listUiState.value = _listUiState.value.copy(
              aryaSamajs = aryaSamajs,
              isLoading = false,
              error = null,
              appError = null
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AryaSamajViewModel.searchAryaSamajByAddress")
            _listUiState.value = _listUiState.value.copy(
              isLoading = false,
              error = appError.getUserMessage(),
              appError = appError
            )
          }
        )
      }
    }
  }

  fun loadAryaSamajForEdit(aryaSamajId: String) {
    viewModelScope.launch {
      repository.getAryaSamajDetail(aryaSamajId).collect { result ->
        result.handleResult(
          onLoading = {
            _formUiState.value =
              _formUiState.value.copy(
                isSubmitting = true
              )
          },
          onSuccess = { aryaSamajDetail ->
            // Convert AryaSamajDetail to AryaSamajFormData
            val formData =
              AryaSamajFormData(
                name = aryaSamajDetail.name,
                description = aryaSamajDetail.description,
                imagePickerState =
                  ImagePickerState(
                    existingImageUrls = aryaSamajDetail.mediaUrls
                  ),
                addressData = aryaSamajDetail.address,
                membersState =
                  MembersState(
                    members =
                      aryaSamajDetail.members.associate { member ->
                        // Convert AryaSamajMember to Member and post/priority pair
                        val memberObj =
                          Member(
                            id = member.memberId,
                            name = member.memberName,
                            profileImage = member.memberProfileImage ?: "",
                            phoneNumber = member.memberPhoneNumber ?: "",
                            educationalQualification = "", // Not available in AryaSamajMember
                            email = "" // Not available in AryaSamajMember
                          )
                        memberObj to (member.post to member.priority)
                      }.toMutableMap()
                  )
              )

            _formUiState.value =
              _formUiState.value.copy(
                formData = formData,
                isSubmitting = false,
                editingAryaSamajId = aryaSamajId,
                originalFormData = formData
              )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AryaSamajViewModel.loadAryaSamajForEdit")
            _formUiState.value =
              _formUiState.value.copy(
                isSubmitting = false,
                submitError = appError.getUserMessage()
              )
          }
        )
      }
    }
  }

  // NEW: Pagination methods for infinite scroll
  fun loadAryaSamajsPaginated(pageSize: Int = 30, resetPagination: Boolean = false) {
    println("Loading AryaSamajs paginated...")
    viewModelScope.launch {
      val currentState = _listUiState.value.paginationState

      // Only preserve pagination when explicitly requested AND it's a reset operation
      val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExistingAryaSamajData()

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
      _listUiState.value = _listUiState.value.copy(
        paginationState = currentState.copy(
          isInitialLoading = shouldReset || currentState.items.isEmpty(),
          isLoadingNextPage = !shouldReset && currentState.items.isNotEmpty(),
          error = null
        )
      )

      repository.getItemsPaginated(pageSize = pageSize, cursor = cursor).collect { result ->
        when (result) {
          is com.aryamahasangh.features.admin.PaginationResult.Loading -> {
            // Loading state already set above
          }

          is com.aryamahasangh.features.admin.PaginationResult.Success -> {
            val newItems = if (shouldReset) {
              result.data
            } else {
              currentState.items + result.data
            }

            // Convert to AryaSamajListItem for display
            val listItems = newItems.map { aryaSamaj ->
              AryaSamajListItem(
                id = aryaSamaj.aryaSamajFields.id,
                name = aryaSamaj.aryaSamajFields.name ?: "",
                description = aryaSamaj.aryaSamajFields.description ?: "",
                formattedAddress = aryaSamaj.getFormattedAddress(),
                memberCount = 0,
                mediaUrls = aryaSamaj.aryaSamajFields.mediaUrls?.filterNotNull() ?: emptyList()
              )
            }

            _listUiState.value = _listUiState.value.copy(
              aryaSamajs = listItems,
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

          is com.aryamahasangh.features.admin.PaginationResult.Error -> {
            _listUiState.value = _listUiState.value.copy(
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

  fun searchAryaSamajsPaginated(searchTerm: String, pageSize: Int = 30, resetPagination: Boolean = true) {
    viewModelScope.launch {
      val currentState = _listUiState.value.paginationState

      // Clear cache on search change
      if (resetPagination && searchTerm != currentState.currentSearchTerm) {
        // TODO: Clear Apollo cache for this query
      }

      val cursor = if (resetPagination) null else currentState.endCursor

      // Set loading state
      _listUiState.value = _listUiState.value.copy(
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
          is com.aryamahasangh.features.admin.PaginationResult.Loading -> {
            // Loading state already set above
          }

          is com.aryamahasangh.features.admin.PaginationResult.Success -> {
            val newItems = if (resetPagination) {
              result.data
            } else {
              currentState.items + result.data
            }

            // Convert to AryaSamajListItem for display
            val listItems = newItems.map { aryaSamaj ->
              AryaSamajListItem(
                id = aryaSamaj.aryaSamajFields.id,
                name = aryaSamaj.aryaSamajFields.name ?: "",
                description = aryaSamaj.aryaSamajFields.description ?: "",
                formattedAddress = aryaSamaj.getFormattedAddress(),
                memberCount = 0,
                mediaUrls = aryaSamaj.aryaSamajFields.mediaUrls?.filterNotNull() ?: emptyList()
              )
            }

            _listUiState.value = _listUiState.value.copy(
              aryaSamajs = listItems,
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

          is com.aryamahasangh.features.admin.PaginationResult.Error -> {
            _listUiState.value = _listUiState.value.copy(
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

  fun loadNextAryaSamajPage() {
    val currentState = _listUiState.value.paginationState
    if (currentState.hasNextPage && !currentState.isLoadingNextPage) {
      if (currentState.currentSearchTerm.isNotBlank()) {
        searchAryaSamajsPaginated(
          searchTerm = currentState.currentSearchTerm,
          resetPagination = false
        )
      } else {
        loadAryaSamajsPaginated(resetPagination = false)
      }
    }
  }

  fun retryAryaSamajLoad() {
    val currentState = _listUiState.value.paginationState
    _listUiState.value = _listUiState.value.copy(
      paginationState = currentState.copy(showRetryButton = false)
    )

    if (currentState.currentSearchTerm.isNotBlank()) {
      searchAryaSamajsPaginated(
        searchTerm = currentState.currentSearchTerm,
        resetPagination = currentState.items.isEmpty()
      )
    } else {
      loadAryaSamajsPaginated(resetPagination = currentState.items.isEmpty())
    }
  }

  // Debounced search method
  fun searchAryaSamajsWithDebounce(query: String) {
    _listUiState.value = _listUiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      if (query.isBlank()) {
        // Load regular AryaSamajs when search is cleared
        loadAryaSamajsPaginated(resetPagination = true)
        return@launch
      }

      // Debounce search by 1 second
      delay(1000)

      searchAryaSamajsPaginated(searchTerm = query.trim(), resetPagination = true)
    }
  }

  // Calculate page size based on screen width  
  fun calculatePageSize(screenWidthDp: Float): Int {
    return when {
      screenWidthDp < 600f -> 15      // Mobile portrait
      screenWidthDp < 840f -> 25      // Tablet, mobile landscape
      else -> 35                      // Desktop, large tablets
    }
  }
}
