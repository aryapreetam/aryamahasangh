//package com.aryamahasangh.features.gurukul.presenter
//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import com.aryamahasangh.components.ImagePickerState
//import com.aryamahasangh.features.gurukul.domain.usecase.RegisterForCourseUseCase
//import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormEffect
//import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormIntent
//import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormUiState
//import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationFormUiState.Field
//import com.aryamahasangh.util.GlobalMessageManager
//import kotlinx.coroutines.flow.MutableSharedFlow
//
///**
// * Presenter for the Course Registration Form using Molecule pattern
// *
// * This presenter handles UI state transformations based on intents
// * and validation logic in a reactive way.
// */
//@Composable
//fun CourseRegistrationFormPresenter(
//    activityId: String,
//    registerForCourseUseCase: RegisterForCourseUseCase,
//    globalMessageManager: GlobalMessageManager,
//    onIntent: (CourseRegistrationFormIntent) -> Unit,
//    effectFlow: MutableSharedFlow<CourseRegistrationFormEffect?>
//): CourseRegistrationFormPresenterHolder {
//    // In Molecule, we maintain state through recompositions
//    var state = remember { CourseRegistrationFormUiState() }
//
//  // Simulate intent processing with a key for recomposition
//    val observedKey = remember { mutableStateOf(0) }
//
//  // Create a presenter holder that includes both state and intent handler
//  val holder = remember {
//    CourseRegistrationFormPresenterHolder(
//      state = CourseRegistrationFormUiState(),
//      handleIntent = { intent ->
//        // Process the intent and update state accordingly
//        when (intent) {
//          is CourseRegistrationFormIntent.NameChanged -> {
//            state = state.copy(name = intent.value)
//          }
//
//          is CourseRegistrationFormIntent.SatrDateChanged -> {
//            state = state.copy(satrDate = intent.value)
//          }
//
//          is CourseRegistrationFormIntent.SatrPlaceChanged -> {
//            state = state.copy(satrPlace = intent.value)
//          }
//
//          is CourseRegistrationFormIntent.RecommendationChanged -> {
//            state = state.copy(recommendation = intent.value)
//          }
//
//          is CourseRegistrationFormIntent.ImageSelected -> {
//            state = state.copy(
//              imageBytes = intent.imageBytes,
//              imageFilename = intent.filename
//            )
//          }
//
//          is CourseRegistrationFormIntent.ImagePickerStateChanged -> {
//            state = state.copy(imagePickerState = intent.state)
//          }
//          // Other intents would be handled similarly
//          else -> {}
//        }
//
//        // Mark state as dirty
//        state = state.copy(isDirty = true)
//
//        // Update validation state
//        val errors = validateAllFields(state)
//        val isValid = isFormValid(state.copy(fieldErrors = errors))
//        state = state.copy(fieldErrors = errors, isSubmitEnabled = isValid)
//
//        // Trigger recomposition
//        observedKey.value++
//            }
//        )
//    }
//
//  // Update the holder's state with the current state
//  holder.state = state
//
//  // Validation errors based on current state
//    val errors = validateAllFields(state)
//    val isValid = isFormValid(state.copy(fieldErrors = errors))
//
//  // Return the updated holder
//  return holder.copy(
//    state = state.copy(
//        fieldErrors = errors,
//        isSubmitEnabled = isValid
//    )
//    )
//}
//
///**
// * Validates a single field and returns updated error map
// */
//private fun validateField(field: Field, value: String, currentErrors: Map<Field, String>): Map<Field, String> {
//    val errors = currentErrors.toMutableMap()
//
//    when (field) {
//        Field.NAME -> {
//            if (value.isBlank()) {
//                errors[field] = "कृपया अपना नाम दर्ज करें"
//            } else {
//                errors.remove(field)
//            }
//        }
//        Field.DATE -> {
//            if (value.isBlank()) {
//                errors[field] = "कृपया सत्र दिवसांक चुनें"
//            } else {
//                errors.remove(field)
//            }
//        }
//        Field.PLACE -> {
//            if (value.isBlank()) {
//                errors[field] = "कृपया सत्र स्थान दर्ज करें"
//            } else {
//                errors.remove(field)
//            }
//        }
//        Field.RECEIPT -> {
//            if (value.isBlank()) {
//                errors[field] = "कृपया भुगतान रसीद अपलोड करें"
//            } else {
//                errors.remove(field)
//            }
//        }
//        Field.RECOMMENDATION -> errors.remove(field) // Optional field
//        Field.GENERAL -> { /* General errors handled separately */ }
//    }
//
//    return errors
//}
//
///**
// * Validates all fields at once (used for form submission)
// */
//private fun validateAllFields(state: CourseRegistrationFormUiState): Map<Field, String> {
//    val errors = mutableMapOf<Field, String>()
//
//    // Validate each required field
//    if (state.name.isBlank()) {
//        errors[Field.NAME] = "कृपया अपना नाम दर्ज करें"
//    }
//
//    if (state.satrDate.isBlank()) {
//        errors[Field.DATE] = "कृपया सत्र दिवसांक चुनें"
//    }
//
//    if (state.satrPlace.isBlank()) {
//        errors[Field.PLACE] = "कृपया सत्र स्थान दर्ज करें"
//    }
//
//    if (!state.imagePickerState.hasImages && state.imageBytes == null) {
//        errors[Field.RECEIPT] = "कृपया भुगतान रसीद अपलोड करें"
//    }
//
//    return errors
//}
//
///**
// * Checks if the form is valid (for enabling submit button)
// */
//private fun isFormValid(state: CourseRegistrationFormUiState): Boolean {
//    // Check required fields
//    if (state.name.isBlank() || state.satrDate.isBlank() || state.satrPlace.isBlank()) {
//        return false
//    }
//
//    // Check if image is uploaded
//    if (!state.imagePickerState.hasImages && state.imageBytes == null) {
//        return false
//    }
//
//    // Check if there are any validation errors
//    if (state.fieldErrors.isNotEmpty()) {
//        return false
//    }
//
//    return true
//}
