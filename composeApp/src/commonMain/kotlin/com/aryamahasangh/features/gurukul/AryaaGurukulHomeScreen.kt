package com.aryamahasangh.features.gurukul

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryamahasangh.features.gurukul.ui.UpcomingCoursesScreen
import com.aryamahasangh.features.gurukul.viewmodel.CourseIntent
import com.aryamahasangh.features.gurukul.viewmodel.UpcomingCoursesEffect
import com.aryamahasangh.features.gurukul.viewmodel.UpcomingCoursesViewModel

@Composable
fun AryaaGurukulHomeScreen(
  viewModel: UpcomingCoursesViewModel,
  onNavigateToRegistration: (String) -> Unit
) {
  Column(Modifier.padding(8.dp)) {
    Text(
      text = "\u0906\u0917\u093e\u092e\u0940 \u0915\u0915\u094d\u0937\u093e\u090f\u0902",
      style = MaterialTheme.typography.headlineSmall,
      modifier = Modifier.padding(vertical = 8.dp)
    )

    val uiState = viewModel.collectUiStateAsState {
      // Handle unexpected errors
    }

    LaunchedEffect(viewModel) {
      viewModel.effect.collect { effect ->
        effect?.let { nonNullEffect ->
          when (nonNullEffect) {
            is UpcomingCoursesEffect.NavigateToRegistration -> {
              onNavigateToRegistration(nonNullEffect.activityId)
              viewModel.clearEffect()
            }
          }
        }
      }
    }

    UpcomingCoursesScreen(
      uiState = uiState,
      onRegisterClick = { courseId ->
        viewModel.sendIntent(CourseIntent.OnCourseRegisterClicked(courseId))
      }
    )
  }
}
