package org.aryamahasangh.features.activities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesContainer(
  onNavigateToActivityDetails: (String) -> Unit,
  viewModel: ActivitiesViewModel,
  onNavigateToEditActivity: (String) -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxSize()) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { 2 }

    LaunchedEffect(selectedTabIndex) {
      pagerState.animateScrollToPage(selectedTabIndex)
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
      if (!pagerState.isScrollInProgress) {
        selectedTabIndex = pagerState.currentPage
      }
    }

    ScrollableTabRow(
      selectedTabIndex = selectedTabIndex
    ) {
      Tab(
        selected = selectedTabIndex == 0,
        onClick = { selectedTabIndex = 0 },
        text = { Text("गतिविधियां") }
      )
      Tab(
        selected = selectedTabIndex == 1,
        onClick = { selectedTabIndex = 1 },
        text = { Text("नयी गतिविधी बनायें") }
      )
    }
    HorizontalPager(
      state = pagerState,
      modifier =
        Modifier
          .fillMaxWidth()
          .weight(1f),
      userScrollEnabled = false
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        if (it == 0) {
          ActivitiesScreen(
            onNavigateToActivityDetails = onNavigateToActivityDetails,
            onNavigateToEditActivity = onNavigateToEditActivity,
            viewModel = viewModel
          )
        } else {
          CreateActivityScreen(
            viewModel = viewModel,
            editingActivityId = null,
            onActivitySaved = { activityId ->
              onNavigateToActivityDetails(activityId)
            },
            onCancel = {
              selectedTabIndex = 0
            }
          )
        }
      }
    }
  }
}
