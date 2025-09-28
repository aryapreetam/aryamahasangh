package com.aryamahasangh.features.gurukul.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.features.gurukul.data.CourseRegistrationRepository
import com.aryamahasangh.features.gurukul.data.CourseRegistrationRepositoryImpl
import com.aryamahasangh.features.gurukul.data.ImageUploadRepository
import com.aryamahasangh.features.gurukul.data.ImageUploadRepositoryImpl
import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
import com.aryamahasangh.features.gurukul.presenter.CourseRegistrationFormPresenter
import com.aryamahasangh.util.GlobalMessageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.dsl.module

class CourseRegistrationFormViewModel(
  private val activityId: String,
  private val registerForCourseUseCase: RegisterForCourseUseCase
) {
  // GlobalMessageManager is a singleton object, access it directly
  private val globalMessageManager = GlobalMessageManager

  // UI State
  private val _uiState = MutableStateFlow(CourseRegistrationFormUiState())
  val uiState: StateFlow<CourseRegistrationFormUiState> = _uiState.asStateFlow()

  // Effect State (navigation, errors, etc.)
  private val _uiEffect = MutableSharedFlow<CourseRegistrationFormEffect?>(replay = 1)
  val uiEffect: SharedFlow<CourseRegistrationFormEffect?> = _uiEffect.asSharedFlow()

  private val viewModelScope: CoroutineScope = MainScope()
  private val intents = MutableSharedFlow<CourseRegistrationFormIntent>(extraBufferCapacity = 64)

  init {
    viewModelScope.launch {
      intents.collect { intent ->
        processIntent(intent)
      }
    }
  }

  // Intent handler - forwards intents to the presenter
  fun sendIntent(intent: CourseRegistrationFormIntent) {
    viewModelScope.launch {
      intents.emit(intent)
    }
  }

  private fun processIntent(intent: CourseRegistrationFormIntent) {
    // The actual processing happens in the presenter
    // This method exists to allow for additional processing if needed
  }

  /**
   * Collects the UIState using Molecule in a Composable which guarantees
   * MonotonicFrameClock will always be present. This MUST be called from a Composable context.
   */
  @Composable
  fun collectUiState(): CourseRegistrationFormUiState {
    // Create molecule flow in the Composable context where MonotonicFrameClock is available
    val moleculeScope = rememberCoroutineScope()
    val presenter = remember {
      moleculeScope.launchMolecule(RecompositionMode.ContextClock) {
        CourseRegistrationFormPresenter(
          activityId = activityId,
          registerForCourseUseCase = registerForCourseUseCase,
          globalMessageManager = globalMessageManager,
          onIntent = { intents.tryEmit(it) },
          effectFlow = _uiEffect
        )
      }
    }

    // Collect state from molecule and update the StateFlow
    val presenterState = presenter.collectAsState(CourseRegistrationFormUiState()).value

    // Update the StateFlow for non-Composable consumers
    LaunchedEffect(presenterState) {
      _uiState.value = presenterState
    }

    // Return the current state
    return presenterState
  }
}

data class CourseRegistrationFormUiState(
  val name: String = "",
  val satrDate: String = "",
  val satrPlace: String = "",
  val recommendation: String = "",
  val imageBytes: ByteArray? = null,
  val imageFilename: String? = null,
  val imagePickerState: ImagePickerState = ImagePickerState(),
  val fieldErrors: Map<Field, String> = emptyMap(),
  val isLoading: Boolean = false,
  val isSubmitEnabled: Boolean = false,
  val isDirty: Boolean = false,
  val showUnsavedDialog: Boolean = false
) {
  enum class Field { NAME, DATE, PLACE, RECOMMENDATION, RECEIPT, GENERAL }
}

sealed class CourseRegistrationFormEffect {
  object Success : CourseRegistrationFormEffect()
  data class Error(val message: String) : CourseRegistrationFormEffect() // Hindi only
  object ShowUnsavedDialog : CourseRegistrationFormEffect()
  object NavigateBack : CourseRegistrationFormEffect()
}

sealed class CourseRegistrationFormIntent {
  data class NameChanged(val value: String) : CourseRegistrationFormIntent()
  data class SatrDateChanged(val value: String) : CourseRegistrationFormIntent()
  data class SatrPlaceChanged(val value: String) : CourseRegistrationFormIntent()
  data class RecommendationChanged(val value: String) : CourseRegistrationFormIntent()
  data class ImageSelected(val imageBytes: ByteArray, val filename: String) : CourseRegistrationFormIntent()
  data class ImagePickerStateChanged(val state: ImagePickerState) : CourseRegistrationFormIntent()
  object Submit : CourseRegistrationFormIntent()
  object BackPressed : CourseRegistrationFormIntent()
  object DiscardUnsavedConfirmed : CourseRegistrationFormIntent()
  object HideUnsavedDialog : CourseRegistrationFormIntent()
}

val GurukulCourseRegistrationModule = module {
  // Use the existing ApolloClient instead of creating a new one
  single<ImageUploadRepository> { ImageUploadRepositoryImpl() }
  single<CourseRegistrationRepository> { CourseRegistrationRepositoryImpl(get()) }
  single { RegisterForCourseUseCase(get(), get()) }
  // We use GlobalMessageManager directly as it's a Kotlin object singleton
  factory { (activityId: String) -> 
    CourseRegistrationFormViewModel(
      activityId = activityId,
      registerForCourseUseCase = get()
    ) 
  }
}
