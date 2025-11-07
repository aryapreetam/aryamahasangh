package com.aryamahasangh.features.gurukul.viewmodel

import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.features.gurukul.domain.models.CourseRegistrationFormData
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// UiState for the form
sealed class UiEffect {
  data class ShowSnackbar(val message: String) : UiEffect()
  object None : UiEffect()
}

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
  val validationMessages: List<String> = emptyList(),
  val uiEffect: UiEffect = UiEffect.None
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
  private val _state = MutableStateFlow(UiState())
  // Internal effect flow for one-shot UI events
  private val _effectFlow = MutableSharedFlow<UiEffect>(extraBufferCapacity = 1)

  // Public only single UiState for screen
  val uiState: StateFlow<UiState> = combine(
    _state,
    _effectFlow
      .onSubscription { emit(UiEffect.None) }
      .scan(UiEffect.None as UiEffect) { _, effect -> effect })
  { currentState, effect ->
    currentState.copy(uiEffect = effect)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

  fun validate(): String? {
    val s = _state.value
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
    _state.update { current ->
      current.copy(
        name = name ?: current.name,
        satrDate = satrDate ?: current.satrDate,
        satrPlace = satrPlace ?: current.satrPlace,
        recommendation = recommendation ?: current.recommendation,
        imagePickerState = imagePickerState ?: current.imagePickerState,
        isDirty = true,
        submitErrorMessage = null,
        validationMessages = emptyList(),
        uiEffect = UiEffect.None
      )
    }
  }

  suspend fun onSubmit() {
    val validationError = validate()
    if (validationError != null) {
      _state.update { it.copy(submitErrorMessage = validationError, isSubmitting = false, uiEffect = UiEffect.None) }
      return
    }
    _state.update { it.copy(isSubmitting = true, submitErrorMessage = null, uiEffect = UiEffect.None) }
    try {
      val s = _state.value
      val picker = s.imagePickerState
      val imageBytes = picker.newImages.firstOrNull()?.let { it.readBytes() }
      val imageFilename = picker.newImages.firstOrNull()?.name ?: "receipt.jpg"
      val formData = CourseRegistrationFormData(
        activityId = activityId,
        name = s.name,
        satrDate = s.satrDate,
        satrPlace = s.satrPlace,
        recommendation = s.recommendation,
        imageBytes = imageBytes,
        imageFilename = imageFilename
      )
      val result = registerForCourseUseCase.execute(formData)
      if (result.isSuccess) {
        _effectFlow.tryEmit(UiEffect.ShowSnackbar("पंजीकरण सफलतापूर्वक हुआ"))
        _state.update {
          it.copy(
            isSubmitting = false,
            submitSuccess = true,
            submitErrorMessage = null,
            uiEffect = UiEffect.None
          )
        }
      } else {
        _effectFlow.tryEmit(UiEffect.ShowSnackbar("पंजीकरण विफल हुआ"))
        _state.update {
          it.copy(
            isSubmitting = false,
            submitSuccess = false,
            submitErrorMessage = "नेटवर्क त्रुटि या अन्य समस्या हुई",
            uiEffect = UiEffect.None
          )
        }
      }
    }catch (e: Exception) {
      _effectFlow.tryEmit(UiEffect.ShowSnackbar("पंजीकरण विफल"))
      _state.update {
        it.copy(
          isSubmitting = false,
          submitSuccess = false,
          submitErrorMessage = "पंजीकरण विफल",
          uiEffect = UiEffect.None
        )
      }
    }
  }

  fun onNavigateBackAttempt() {
    _state.update {
      if (!it.isDirty || it.submitSuccess) it else it.copy(showUnsavedExitDialog = true)
    }
  }

  fun confirmUnsavedExit() {
    _state.update { it.copy(showUnsavedExitDialog = false) }
  }

  fun dismissUnsavedExitDialog() {
    _state.update { it.copy(showUnsavedExitDialog = false) }
  }

  fun resetSubmitState() {
    _state.update { it.copy(submitSuccess = false, uiEffect = UiEffect.None) }
  }
}
