package com.aryamahasangh.features.gurukul.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.aryamahasangh.features.gurukul.domain.usecase.GetCoursesUseCase
import com.aryamahasangh.type.GenderFilter

// UIState sealed class for upcoming courses listing
sealed class UpcomingCoursesUiState {
  object Loading : UpcomingCoursesUiState()
  data class Success(val courses: List<CourseItemUiModel>) : UpcomingCoursesUiState()
  data class Error(val message: String) : UpcomingCoursesUiState()
  object NavigateToRegistration : UpcomingCoursesUiState() // effect for navigation
}

// Expanded UI model for course item with all event fields for rendering
// Document fields for clarity/testing
@Suppress("DataClassContainsFunctions")
data class CourseItemUiModel(
  val id: String,
  val name: String,
  val startDatetime: String, // ISO or human display, retained for legacy
  val endDatetime: String,
  val panjikaranOpen: Boolean, // registration open
  val allowedGender: String,
  val district: String, // never null, may be empty
  val state: String,    // never null, may be empty
  val latitude: Double, // NaN if unavailable
  val longitude: Double, // NaN if unavailable
  val capacity: Int = 0, // default if unavailable
)

/**
 * Molecule presenter: streams UpcomingCoursesUiState from business logic.
 * Can be used with launchMolecule or moleculeFlow.
 * Uses mutableStateOf and .value update pattern for multiplatform.
 */
@Composable
fun UpcomingCoursesPresenter(
  gender: GenderFilter,
  getCoursesUseCase: GetCoursesUseCase,
  onCourseRegisterClicked: (courseId: String) -> Unit
): UpcomingCoursesUiState {
  val uiState = remember { mutableStateOf<UpcomingCoursesUiState>(UpcomingCoursesUiState.Loading) }

  LaunchedEffect(gender) {
    try {
      uiState.value = UpcomingCoursesUiState.Loading
      val courses = getCoursesUseCase(gender)
      uiState.value = UpcomingCoursesUiState.Success(courses.map {
        CourseItemUiModel(
          id = it.id,
          name = it.name,
          startDatetime = it.startDatetime.toString(),
          endDatetime = it.endDatetime.toString(),
          panjikaranOpen = true, // TODO: Model open/closed from course capacity/etc.
          allowedGender = it.allowedGender,
          district = it.address?.district ?: "",
          state = it.address?.state ?: "",
          latitude = it.address?.latitude ?: Double.NaN,
          longitude = it.address?.longitude ?: Double.NaN,
          capacity = it.capacity ?: 0
        )
      })
    } catch (e: Exception) {
      uiState.value = UpcomingCoursesUiState.Error("कोर्स लोड करने में त्रुटि") // Hindi error message
    }
  }

  // You would handle effects (like navigation) via a separate state property if needed
  return uiState.value
}
