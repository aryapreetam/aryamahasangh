package com.aryamahasangh.features.gurukul.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.aryamahasangh.features.gurukul.domain.usecase.GetCoursesUseCase
import com.aryamahasangh.features.gurukul.presenter.UpcomingCoursesPresenter
import com.aryamahasangh.features.gurukul.presenter.UpcomingCoursesUiState
import com.aryamahasangh.type.GenderFilter

/**
 * Multiplatform ViewModel for upcoming courses listing.
 * Uses intent-based event handling for testability and scalability.
 */
class UpcomingCoursesViewModel(
  private val getCoursesUseCase: GetCoursesUseCase,
  private val genderFilter: GenderFilter
) : ViewModel() {
  // All deps injected via constructor; no KoinComponent required.

  /**
   * Handle UI/user intents (unit-testable with direct method call).
   * Future: Support more CourseIntent types for full event/MVI/side-effect modeling.
   */
  fun sendIntent(intent: CourseIntent) {
    when (intent) {
      is CourseIntent.OnCourseRegisterClicked -> {
        // Handle navigation logic or emit effect (e.g., update effect Flow/State for parent to observe)
        // In production, may want Effect state (MutableSharedFlow, etc.) here.
        println("Register clicked for courseId: ${intent.courseId}")
      }
    }
  }

  /**
   * Collects the UIState using Molecule in a Composable which guarantees
   * MonotonicFrameClock will always be present. CALL ONLY FROM @Composable context.
   * Wires navigation via intent for testability and modularity.
   */
  @Composable
  fun collectUiStateAsState(
    intentHandler: (CourseIntent) -> Unit
  ): UpcomingCoursesUiState {
    val scope = rememberCoroutineScope()
    val uiStateFlow = androidx.compose.runtime.remember {
      scope.launchMolecule(mode = RecompositionMode.ContextClock) {
        UpcomingCoursesPresenter(
          gender = genderFilter,
          getCoursesUseCase = getCoursesUseCase,
          onCourseRegisterClicked = { id -> intentHandler(CourseIntent.OnCourseRegisterClicked(id)) }
        )
      }
    }
    val uiState by uiStateFlow.collectAsState(UpcomingCoursesUiState.Loading)
    return uiState
  }
}

// Intents: All user actions forwarded to ViewModel for separation, MVI pattern, testability
sealed class CourseIntent {
  data class OnCourseRegisterClicked(val courseId: String) : CourseIntent()
}
