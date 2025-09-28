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
import com.aryamahasangh.type.GenderFilter
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun GurukulCollegeHomeScreen(
  onNavigateToRegistration: (String) -> Unit
) {
  Column(Modifier.padding(8.dp)) {
    Text(
      text = "आगामी कक्षाएं",
      style = MaterialTheme.typography.headlineSmall,
      modifier = Modifier.padding(vertical = 8.dp)
    )
    val viewModel: UpcomingCoursesViewModel = koinInject(parameters = { parametersOf(GenderFilter.MALE) })
    val uiState = viewModel.collectUiStateAsState {
      // Handle unexpected errors
    }

    LaunchedEffect(viewModel) {
      viewModel.effect.collect { effect ->
        when (effect) {
          is UpcomingCoursesEffect.NavigateToRegistration -> {
            onNavigateToRegistration(effect.activityId)
            viewModel.clearEffect()
          }
          null -> {}
        }
      }
    }

    UpcomingCoursesScreen(
      uiState = uiState,
      onRegisterClick = { courseId -> viewModel.sendIntent(CourseIntent.OnCourseRegisterClicked(courseId)) },
    )
  }
}
