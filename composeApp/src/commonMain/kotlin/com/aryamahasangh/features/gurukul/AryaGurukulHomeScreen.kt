package com.aryamahasangh.features.gurukul

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryamahasangh.features.gurukul.ui.UpcomingCoursesScreen
import com.aryamahasangh.features.gurukul.viewmodel.CourseIntent
import com.aryamahasangh.features.gurukul.viewmodel.UpcomingCoursesViewModel
import com.aryamahasangh.type.GenderFilter
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun GurukulCollegeHomeScreen(
  onNavigateToRegister: (courseId: String) -> Unit,
) {
  Column(Modifier.padding(8.dp)) {
    Text(
      text = "आगामी कक्षाएं",
      style = MaterialTheme.typography.headlineSmall,
      modifier = Modifier.padding(vertical = 8.dp)
    )
    val viewModel: UpcomingCoursesViewModel = koinInject(parameters = { parametersOf(GenderFilter.MALE) })
    // Provide intent-based lambda (CourseIntent) -> Unit
    val uiState = viewModel.collectUiStateAsState{}
//    { intent ->
//      when (intent) {
//        is CourseIntent.OnCourseRegisterClicked -> onNavigateToRegister(intent.courseId)
//      }
//    }
    UpcomingCoursesScreen(
      uiState = uiState,
      onNavigateToRegister = { courseId -> viewModel.sendIntent(CourseIntent.OnCourseRegisterClicked(courseId)) },
    )
  }
}
