package com.aryamahasangh.features.gurukul.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.features.gurukul.domain.models.CourseRegistrationFormData
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import com.aryamahasangh.ui.components.buttons.SubmissionError
import com.aryamahasangh.util.GlobalMessageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// UiState for the form
data class UiState(
  val name: String = "",
  val satrDate: String = "",
  val satrPlace: String = "",
  val recommendation: String = "",
  val imagePickerState: ImagePickerState = ImagePickerState(),
  val isSubmitting: Boolean = false,
  val submitSuccess: Boolean = false,
  val submitErrorMessage: String? = null,
  val showUnsavedExitDialog: Boolean = false,
  val isDirty: Boolean = false,
  val validationMessages: List<String> = emptyList()
){
  val isValid: Boolean
    get() = name.isNotEmpty() &&
      satrDate.isNotEmpty() &&
      satrPlace.isNotEmpty() &&
      recommendation.isNotEmpty() &&
      imagePickerState.hasImages
}

class CourseRegistrationViewModel(
  private val registerForCourseUseCase: RegisterForCourseUseCase,
  private val activityId: String
) : androidx.lifecycle.ViewModel() {
  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState

  fun validate(): String? {
    val s = _uiState.value
    val errors = mutableListOf<String>()
    if (s.name.isBlank()) errors += "कृपया नाम दर्ज करें"
    if (s.satrDate.isBlank()) errors += "कृपया सत्र दिनांक चुनें"
    else try {
      val date = LocalDate.parse(s.satrDate)
      val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
      if (date > today) errors += "सत्र दिनांक आज या पूर्व का होना चाहिए"
    } catch (_: Exception) {
      errors += "अमान्य दिनांक"
    }
    if (s.satrPlace.isBlank()) errors += "कृपया सत्र स्थान भरें"
    if (s.imagePickerState.newImages.isEmpty()) errors += "कृपया भुगतान रसीद अपलोड करें"
    if (errors.isNotEmpty()) {
      // Only first validation error shown for submit button
      return errors.first()
    }
    return null
  }

  fun onFieldChange(
    name: String? = null,
    satrDate: String? = null,
    satrPlace: String? = null,
    recommendation: String? = null,
    imagePickerState: ImagePickerState? = null
  ) {
    val current = _uiState.value
    val updated = current.copy(
      name = name ?: current.name,
      satrDate = satrDate ?: current.satrDate,
      satrPlace = satrPlace ?: current.satrPlace,
      recommendation = recommendation ?: current.recommendation,
      imagePickerState = imagePickerState ?: current.imagePickerState,
      isDirty = true
    )
    _uiState.value = updated.copy(submitErrorMessage = null, validationMessages = emptyList())
  }

  suspend fun onSubmit() {
    val validationError = validate()
    if (validationError != null) {
      _uiState.value = _uiState.value.copy(submitErrorMessage = validationError, isSubmitting = false)
      return
    }
    _uiState.value = _uiState.value.copy(isSubmitting = true, submitErrorMessage = null)
    try {
      val picker = _uiState.value.imagePickerState
      val imageBytes = picker.newImages.firstOrNull()?.let { it.readBytes() }
      val imageFilename = picker.newImages.firstOrNull()?.name ?: "receipt.jpg"
      val formData = CourseRegistrationFormData(
        activityId = activityId,
        name = _uiState.value.name,
        satrDate = _uiState.value.satrDate,
        satrPlace = _uiState.value.satrPlace,
        recommendation = _uiState.value.recommendation,
        imageBytes = imageBytes,
        imageFilename = imageFilename
      )
      val result = registerForCourseUseCase.execute(formData)
      if (result.isSuccess) {
        GlobalMessageManager.showSuccess("पंजीकरण सफलतापूर्वक हुआ")
        _uiState.value = _uiState.value.copy(
          isSubmitting = false,
          submitSuccess = true,
          submitErrorMessage = null
        )
      } else {
        GlobalMessageManager.showError("पंजीकरण विफल हुआ")
        _uiState.value = _uiState.value.copy(
          isSubmitting = false,
          submitSuccess = false,
          submitErrorMessage = "नेटवर्क त्रुटि या अन्य समस्या हुई"
        )
      }
    }catch (e: Exception) {
      GlobalMessageManager.showError("पंजीकरण विफल")
      _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = false, submitErrorMessage = "पंजीकरण विफल")
    }
  }

  fun onNavigateBackAttempt() {
    if (_uiState.value.isDirty && !_uiState.value.submitSuccess) {
      _uiState.value = _uiState.value.copy(showUnsavedExitDialog = true)
    }
  }

  fun confirmUnsavedExit() {
    _uiState.value = _uiState.value.copy(showUnsavedExitDialog = false)
  }

  fun dismissUnsavedExitDialog() {
    _uiState.value = _uiState.value.copy(showUnsavedExitDialog = false)
  }

  fun resetSubmitState() {
    _uiState.value = _uiState.value.copy(submitSuccess = false)
  }
}
