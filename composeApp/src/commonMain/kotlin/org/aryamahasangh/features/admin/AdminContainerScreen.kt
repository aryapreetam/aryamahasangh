package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AdminContainerScreen(viewModel: AdminViewModel) {
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
        text = { Text("आर्यों की सूचि") }
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
          MembersScreen(viewModel)
        }
      }
    }
  }
}
