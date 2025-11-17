package com.aryamahasangh.features.gurukul.viewmodel

import androidx.lifecycle.viewModelScope
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.features.gurukul.domain.models.CourseRegistrationFormData
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// UiState for the form
sealed class UiEffect {
  data class ShowSnackbar(val message: String, val isError:  Boolean = false) : UiEffect()
  object None : UiEffect()
}

sealed interface ButtonState {
  object Idle : ButtonState
  object Loading : ButtonState
  object Success : ButtonState
  data class Error(val message: String) : ButtonState
}

data class UiState(
  val name: String = "",
  val satrDate: String = "",
  val satrPlace: String = "",
  val recommendation: String = "",
  val imagePickerState: ImagePickerState = ImagePickerState(), // Receipt image
  val photoPickerState: ImagePickerState = ImagePickerState(), // User photo
//  val isSubmitting: Boolean = false,
//  val submitSuccess: Boolean = false,
//  val submitErrorMessage: String? = null,
  val showUnsavedExitDialog: Boolean = false,
  val isDirty: Boolean = false,
  val validationMessages: List<String> = emptyList(),

  val buttonState: ButtonState = ButtonState.Idle
){
  val isValid: Boolean
    get() = name.isNotEmpty() &&
      satrDate.isNotEmpty() &&
      satrPlace.isNotEmpty() &&
      recommendation.isNotEmpty() &&
      imagePickerState.hasImages &&
      photoPickerState.hasImages
  val isSubmitting: Boolean
    get() = buttonState == ButtonState.Loading
  val submitErrorMessage: String?
    get() = if(buttonState is ButtonState.Error) buttonState.message else null
}

class CourseRegistrationViewModel(
  private val registerForCourseUseCase: RegisterForCourseUseCase,
  private val activityId: String
) : androidx.lifecycle.ViewModel() {

  private val _state = MutableStateFlow(UiState())

  /**
   * One-shot effects. No replay. Buffered so duplicates are allowed.
   */
  private val _effect = MutableSharedFlow<UiEffect>(
    replay = 0,
    extraBufferCapacity = 64,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val effect: SharedFlow<UiEffect> = _effect.asSharedFlow()

  private val _buttonState = MutableStateFlow<ButtonState>(ButtonState.Idle)

  val uiState: StateFlow<UiState> = combine(
    _state,
    _buttonState
  ) { form, button ->
    form.copy(buttonState = button)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())


  private fun emitEffect(e: UiEffect) {
    if (!_effect.tryEmit(e)) {
      viewModelScope.launch { _effect.emit(e) }
    }
  }

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
    if (s.photoPickerState.newImages.isEmpty()) errors += "कृपया फोटो अपलोड करें"
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
    imagePickerState: ImagePickerState? = null,
    photoPickerState: ImagePickerState? = null
  ) {
    _state.update { current ->
      current.copy(
        name = name ?: current.name,
        satrDate = satrDate ?: current.satrDate,
        satrPlace = satrPlace ?: current.satrPlace,
        recommendation = recommendation ?: current.recommendation,
        imagePickerState = imagePickerState ?: current.imagePickerState,
        photoPickerState = photoPickerState ?: current.photoPickerState,
        isDirty = true,
        validationMessages = emptyList(),
      )
    }
  }

  fun onSubmit() {
    val validationError = validate()
    if (validationError != null) {
      _buttonState.value = ButtonState.Error(validationError)
      return
    }
    viewModelScope.launch(Dispatchers.Default) {
      _buttonState.value = ButtonState.Loading
      try {
        val s = _state.value

        // Receipt image
        val picker = s.imagePickerState
        val imageBytes = picker.newImages.firstOrNull()?.let { it.readBytes() }
        val imageFilename = picker.newImages.firstOrNull()?.name ?: "receipt.jpg"

        // User photo
        val photoPicker = s.photoPickerState
        val photoBytes = photoPicker.newImages.firstOrNull()?.let { it.readBytes() }
        val photoFilename = photoPicker.newImages.firstOrNull()?.name ?: "photo.jpg"

        val formData = CourseRegistrationFormData(
          activityId = activityId,
          name = s.name,
          satrDate = s.satrDate,
          satrPlace = s.satrPlace,
          recommendation = s.recommendation,
          imageBytes = imageBytes,
          imageFilename = imageFilename,
          photoBytes = photoBytes,
          photoFilename = photoFilename
        )
        val result = registerForCourseUseCase.execute(formData)
        if (result.isSuccess) {
          _buttonState.value = ButtonState.Success
          emitEffect(UiEffect.ShowSnackbar("पंजीकरण सफलतापूर्वक हुआ"))
          delay(1000)
          _buttonState.value = ButtonState.Idle
        } else {
          val error = result.exceptionOrNull()?.message
          val msg = result.exceptionOrNull()?.message ?: "त्रुटि"
          _buttonState.value = ButtonState.Error(msg)
          emitEffect(UiEffect.ShowSnackbar("पंजीकरण विफल हुआ: $error", true))
          delay(1000)
          _buttonState.value = ButtonState.Idle
        }
      } catch (e: Exception) {
        val msg = e.message ?: "अज्ञात त्रुटि"
        _buttonState.value = ButtonState.Error(msg)
        _effect.emit(UiEffect.ShowSnackbar("पंजीकरण विफल: $msg", true))
        delay(1000)
        _buttonState.value = ButtonState.Idle
      }
    }
  }

  fun onNavigateBackAttempt() {
    val s = _state.value
    if (s.isDirty && s.buttonState != ButtonState.Success) {
      _state.update { it.copy(showUnsavedExitDialog = true) }
    }
  }

  fun confirmUnsavedExit() {
    _state.update { it.copy(showUnsavedExitDialog = false) }
  }

  fun dismissUnsavedExitDialog() {
    _state.update { it.copy(showUnsavedExitDialog = false) }
  }

  fun resetSubmitState() {
    //_state.update { it.copy(submitSuccess = false, uiEffect = UiEffect.None) }
  }
}
