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
fun AryaaGurukulHomeScreen(navigateToAdmissionForm: () -> Unit) {
  Column(Modifier.padding(8.dp)) {
//    listOf("आर्या गुरुकुल महाविद्यालय").forEach { name ->
//      Text(
//        text = name,
//        style = MaterialTheme.typography.headlineSmall,
//        modifier = Modifier.padding(vertical = 8.dp)
//      )
//      FlowRow(
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = Modifier.padding(bottom = 16.dp)
//      ) {
//        listOf("वर्तमान कार्य", "गुरुकुल प्रवेश", "आगामी बैठक", "कक्षाएं").forEach {
//          OutlinedCard(
//            onClick = {
//              if (it == "गुरुकुल प्रवेश") {
//                // navigateToAdmissionForm()
//              }
//            }
//          ) {
//            Text(
//              text = it,
//              style = MaterialTheme.typography.titleMedium,
//              modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp)
//            )
//          }
//        }
//      }
//    }
    Text(
      text = "आगामी कक्षाएं",
      style = MaterialTheme.typography.headlineSmall,
      modifier = Modifier.padding(vertical = 8.dp)
    )
    // Gold-standard: Inject VM with correct parameter using Koin DI, and ensure context7 usage
    val viewModel: UpcomingCoursesViewModel = koinInject(parameters = { parametersOf(GenderFilter.FEMALE) })
    // Collect uiState from ViewModel (ensures MonotonicFrameClock usage)
    val uiState = viewModel.collectUiStateAsState({  })
    UpcomingCoursesScreen(
      uiState = uiState,
      onNavigateToRegister = { courseId -> viewModel.sendIntent(CourseIntent.OnCourseRegisterClicked(courseId)) },
    )
  }
}
