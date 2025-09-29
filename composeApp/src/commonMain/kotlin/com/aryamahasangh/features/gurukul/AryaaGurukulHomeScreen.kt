package com.aryamahasangh.features.gurukul

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.features.gurukul.ui.CourseRegistrationsReceivedScreen
import com.aryamahasangh.features.gurukul.viewmodel.CourseRegistrationsReceivedViewModel
import com.aryamahasangh.features.gurukul.viewmodel.UpcomingCoursesViewModel
import com.aryamahasangh.type.GenderFilter
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AryaaGurukulHomeScreen(
  viewModel: UpcomingCoursesViewModel,
  onNavigateToRegistration: (String) -> Unit
) {
  var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
  val pagerState = rememberPagerState { 2 }
  val isLoggedIn = LocalIsAuthenticated.current
  LaunchedEffect(selectedTabIndex) {
    pagerState.animateScrollToPage(selectedTabIndex)
  }
  if(isLoggedIn) {
    Column(modifier = Modifier.fillMaxSize()) {
      PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex
      ) {
        Tab(
          selected = selectedTabIndex == 0,
          onClick = { selectedTabIndex = 0 },
          text = { Text("आगामी कक्षाएं") }
        )
        Tab(
          selected = selectedTabIndex == 1,
          onClick = { selectedTabIndex = 1 },
          text = { Text("प्राप्त आवेदन") }
        )
      }
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().weight(1f),
        userScrollEnabled = false
      ) {
        Box(
          modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
          if (it == 0) {
            UpcomingCoursesList(GenderFilter.FEMALE) { courseId -> onNavigateToRegistration(courseId) }
          } else if (it == 1) {
            val registrationsReceivedViewModel: CourseRegistrationsReceivedViewModel =
              koinInject(parameters = { parametersOf(GenderFilter.FEMALE) })
            CourseRegistrationsReceivedScreen(viewModel = registrationsReceivedViewModel)
          }
        }
      }
    }
  }else{
    Column(Modifier.padding(8.dp)) {
      Text(
        text = "आगामी कक्षाएं",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(vertical = 8.dp)
      )
      UpcomingCoursesList(GenderFilter.FEMALE) { courseId -> onNavigateToRegistration(courseId) }
    }
  }
}
