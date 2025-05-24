package org.aryamahasangh.screens

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
import org.aryamahasangh.viewmodel.BookOrderViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun BookOrdersContainer(
  viewModel: BookOrderViewModel,
  onNavigateToDetails: (String) -> Unit = {}
) {
  Column(modifier = Modifier.fillMaxSize()) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { 2 }

    LaunchedEffect(selectedTabIndex) {
      pagerState.animateScrollToPage(selectedTabIndex)
    }

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
      if(!pagerState.isScrollInProgress) {
        selectedTabIndex = pagerState.currentPage
      }
    }

    ScrollableTabRow(
      selectedTabIndex = selectedTabIndex
    ){
      Tab(
        selected = selectedTabIndex == 0,
        onClick = { selectedTabIndex = 0 },
        text = { Text("Book order form") }
      )
      Tab(
        selected = selectedTabIndex == 1,
        onClick = { selectedTabIndex = 1 },
        text = { Text("Received orders") }
      )
    }

    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      userScrollEnabled = false
    ){
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        if(it == 0){
          BookOrderFormScreen(viewModel)
        }else{
          ReceivedOrdersScreen(viewModel,onNavigateToDetails)
        }
      }
    }
  }
}