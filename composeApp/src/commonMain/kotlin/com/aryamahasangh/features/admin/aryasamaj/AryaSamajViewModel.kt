package com.aryamahasangh.features.admin.aryasamaj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.components.MembersState
import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.ErrorHandler
import com.aryamahasangh.domain.error.getUserMessage
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.admin.PaginationResult
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.fragment.AryaSamajWithAddress
import com.aryamahasangh.util.GlobalMessageManager
import com.aryamahasangh.util.ImageCompressionService
import com.aryamahasangh.utils.FileUploadUtils
import com.aryamahasangh.util.Result
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
  val paginationState: PaginationState<AryaSamajWithAddress> = PaginationState(),
  val isDeletingId: String? = null,
  val deleteError: String? = null,
  val deleteSuccess: String? = null
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

class AryaSamajViewModel(
  private val repository: AryaSamajRepository
) : ViewModel() {
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
      paginationState = savedPaginationState.copy(items = savedAryaSamajs) // Ensure consistency
    )
    shouldPreservePagination = true
  }

  // List operations
//  fun loadAryaSamajs() {
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

  fun deleteAryaSamaj(id: String, onSuccess: (() -> Unit)? = null) {
    viewModelScope.launch {
      repository.deleteAryaSamaj(id).collect { result ->
        result.handleResult(
          onLoading = {
            _listUiState.value = _listUiState.value.copy(
              isDeletingId = id,
              deleteError = null,
              deleteSuccess = null
            )
          },
          onSuccess = { success ->
            // Clear any pagination preservation flags that might interfere
            shouldPreservePagination = false

            // Force refresh the list by clearing current state first
            _listUiState.value = _listUiState.value.copy(
              paginationState = PaginationState() // Reset pagination state completely
            )

            // Then reload with forced reset
            loadAryaSamajsPaginated(resetPagination = true)

            // Call the success callback
            onSuccess?.invoke()
            _listUiState.value = _listUiState.value.copy(
              isDeletingId = null,
              deleteError = null,
              deleteSuccess = "आर्य समाज सफलतापूर्वक हटा दिया गया"
            )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AryaSamajViewModel.deleteAryaSamaj")
            _listUiState.value = _listUiState.value.copy(
              isDeletingId = null,
              deleteError = "आर्य समाज हटाने में त्रुटि: ${appError.getUserMessage()}",
              deleteSuccess = null
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

            val imageBytes = if (formData.imagePickerState.hasCompressedData(file)) {
              formData.imagePickerState.getCompressedBytes(file)!!
            } else {
              com.aryamahasangh.util.ImageCompressionService.compressSync(
                file = file,
                targetKb = 10,
                maxLongEdge = 1024
              )
            }
            val uploadResponse = FileUploadUtils.uploadBytes(
              path = "arya_samaj_$randomNum.webp",
              data = imageBytes
            )
            val publicUrl = when (uploadResponse) {
              is Result.Success -> uploadResponse.data
              is Result.Error -> throw Exception(uploadResponse.message)
              else -> throw Exception("अज्ञात त्रुटि")
            }
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
                GlobalMessageManager.showSuccess("आर्य समाज सफलतापूर्वक संपादित किया गया")
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
                GlobalMessageManager.showSuccess("आर्य समाज सफलतापूर्वक जोड़ा गया")
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
    _listUiState.value =
      _listUiState.value.copy(error = null, appError = null, deleteError = null, deleteSuccess = null)
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
    viewModelScope.launch {
      val currentState = _listUiState.value.paginationState

      // Check if we should preserve existing data (e.g., navigating back from detail screen)
      val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExistingAryaSamajData()

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
      _listUiState.value = _listUiState.value.copy(
        paginationState = currentState.copy(
          isInitialLoading = shouldReset || currentState.items.isEmpty(),
          isLoadingNextPage = !shouldReset && currentState.items.isNotEmpty(),
          error = null
        )
      )

      repository.getItemsPaginated(pageSize = pageSize, cursor = cursor).collect { result ->
        when (result) {
          is PaginationResult.Loading -> {
            // Loading state already set above
          }

          is PaginationResult.Success -> {
            // Prevent duplication by ensuring clean state management
            val existingItems = if (shouldReset) emptyList() else currentState.items
            val newItems = existingItems + result.data

            // Remove duplicates based on ID
            val uniqueItems = newItems.distinctBy { it.aryaSamajFields.id }

            // Convert to AryaSamajListItem for display
            val listItems = uniqueItems.map { aryaSamaj ->
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
                items = uniqueItems,
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
          is PaginationResult.Loading -> {
            // Loading state already set above
          }

          is PaginationResult.Success -> {
            val existingItems = if (resetPagination) emptyList() else currentState.items
            val newItems = existingItems + result.data

            // Remove duplicates based on ID
            val uniqueItems = newItems.distinctBy { it.aryaSamajFields.id }

            // Convert to AryaSamajListItem for display
            val listItems = uniqueItems.map { aryaSamaj ->
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
                items = uniqueItems,
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
        // Clear search state and reset pagination completely for initial load
        _listUiState.value = _listUiState.value.copy(
          paginationState = PaginationState() // Reset pagination state completely
        )
        // Load regular AryaSamajs when search is cleared
        loadAryaSamajsPaginated(resetPagination = true)
        return@launch
      }

      // Debounce search by 1 second
      delay(1000)

      searchAryaSamajsPaginated(searchTerm = query.trim(), resetPagination = true)
    }
  }

  // Method to restore search query and trigger fresh search results
  fun restoreAndSearchAryaSamaj(query: String) {
    searchAryaSamajsWithDebounce(query)
  }

  // Calculate page size based on screen width  
  fun calculatePageSize(screenWidthDp: Float): Int {
    return when {
      screenWidthDp < 600f -> 15      // Mobile portrait
      screenWidthDp < 840f -> 25      // Tablet, mobile landscape
      else -> 35                      // Desktop, large tablets
    }
  }

  // NEW: Method to set delete error state
  fun setDeleteError(error: String) {
    _listUiState.value = _listUiState.value.copy(deleteError = error)
  }

  // NEW: Method to set delete success state
  fun setDeleteSuccess(message: String) {
    _listUiState.value = _listUiState.value.copy(deleteSuccess = message)
  }

  fun isFormValid(): Boolean {
    return validateForm(_formUiState.value.formData).isEmpty()
  }
}
