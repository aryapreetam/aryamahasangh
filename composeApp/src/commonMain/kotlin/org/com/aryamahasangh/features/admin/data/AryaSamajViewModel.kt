package com.aryamahasangh.features.admin.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.components.MembersState
import com.aryamahasangh.domain.error.AppError
import com.aryamahasangh.domain.error.ErrorHandler
import com.aryamahasangh.domain.error.getUserMessage
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.network.bucket
import com.aryamahasangh.viewmodel.ErrorState
import com.aryamahasangh.viewmodel.handleResult
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
  val totalCount: Int = 0
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
  val originalFormData: AryaSamajFormData? = null // Track original data for change detection
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

  // List operations
  fun loadAryaSamajs() {
    viewModelScope.launch {
      repository.getAryaSamajs().collect { result ->
        result.handleResult(
          onLoading = {
            _listUiState.value =
              _listUiState.value.copy(
                isLoading = true,
                error = null,
                appError = null
              )
          },
          onSuccess = { aryaSamajs ->
            _listUiState.value =
              _listUiState.value.copy(
                aryaSamajs = aryaSamajs,
                isLoading = false,
                error = null,
                appError = null
              )
          },
          onError = { appError ->
            ErrorHandler.logError(appError, "AryaSamajViewModel.loadAryaSamajs")
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
            loadAryaSamajs()
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

    // Debug: Log the current form data to understand the validation issue
    println("DEBUG: Submitting form with data:")
    println("  Name: ${formData.name}")
    println("  Description: ${formData.description}")
    println("  Location: ${formData.addressData.location}")
    println("  Address: ${formData.addressData.address}")
    println("  State: ${formData.addressData.state}")
    println("  District: ${formData.addressData.district}")

    // Validate form
    val validationErrors = validateForm(formData)
    println("DEBUG: Validation errors: $validationErrors")

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
            println("DEBUG: Successfully uploaded image: $publicUrl")
          } catch (e: Exception) {
            println("DEBUG: Failed to upload image ${file.name}: ${e.message}")
            throw Exception("चित्र अपलोड करने में त्रुटि: ${e.message}")
          }
        }

        println("DEBUG: Final uploaded image URLs: $uploadedImageUrls")

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
                loadAryaSamajs()
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
                loadAryaSamajs()
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
        println("DEBUG: Exception in submitForm: ${e.message}")
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
}
