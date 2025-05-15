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
import org.aryamahasangh.viewmodel.AdmissionsViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AdmissionScreen(viewModel: AdmissionsViewModel) {
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
        text = { Text("प्रवेश प्रपत्र") }
      )
      Tab(
        selected = selectedTabIndex == 1,
        onClick = { selectedTabIndex = 1 },
        text = { Text("प्राप्त आवेदन") }
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
          RegistrationForm(viewModel)
        }else{
          ReceivedApplicationsScreen(viewModel)
        }
      }
    }
  }
}

@Preview
@Composable
fun AdmissionScreenPreview() {
  // This is just a preview, so we don't need a real ViewModel
  // In a real app, we would inject the ViewModel
  // AdmissionScreen(viewModel = AdmissionsViewModel())
}