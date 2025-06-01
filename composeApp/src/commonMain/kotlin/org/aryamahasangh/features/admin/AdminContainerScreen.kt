package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.components.ErrorSnackbar
import org.aryamahasangh.components.InlineErrorMessage
import org.aryamahasangh.features.activities.toDevanagariNumerals

@Composable
fun AdminContainerScreen(
  viewModel: AdminViewModel,
  onNavigateToMemberDetail: (String) -> Unit = {},
  onNavigateToAddMember: () -> Unit = {}
) {
  val membersCount by viewModel.membersCount.collectAsState()
  val membersUiState by viewModel.membersUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current

  LaunchedEffect(Unit) {
    viewModel.getMembersCount()
  }

  // Show error snackbar for member count loading errors (if any)
  ErrorSnackbar(
    error = membersUiState.appError,
    snackbarHostState = snackbarHostState,
    onRetry = {
      viewModel.clearMembersError()
      viewModel.getMembersCount()
    },
    onDismiss = { viewModel.clearMembersError() }
  )

  Column(modifier = Modifier.fillMaxSize()) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { 1 } // Only showing members tab for now

    LaunchedEffect(selectedTabIndex) {
      pagerState.animateScrollToPage(selectedTabIndex)
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
      if (!pagerState.isScrollInProgress) {
        selectedTabIndex = pagerState.currentPage
      }
    }

    // Show inline error if there's an issue loading the member count
    if (membersUiState.appError != null) {
      InlineErrorMessage(
        error = membersUiState.appError,
        modifier = Modifier.padding(16.dp),
        onRetry = {
          viewModel.clearMembersError()
          viewModel.getMembersCount()
        }
      )
    }

    ScrollableTabRow(
      selectedTabIndex = selectedTabIndex
    ) {
      val count = if (membersUiState.appError == null) {
        "$membersCount".toDevanagariNumerals()
      } else {
        "?" // Show question mark if count couldn't be loaded
      }

      Tab(
        selected = selectedTabIndex == 0,
        onClick = { selectedTabIndex = 0 },
        text = { Text("पदाधिकारी ($count)") }
      )
    }

    HorizontalPager(
      state = pagerState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      userScrollEnabled = false
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        if (it == 0) {
          MembersScreen(
            viewModel = viewModel,
            onNavigateToMemberDetail = onNavigateToMemberDetail,
            onNavigateToAddMember = onNavigateToAddMember
          )
        }
      }
    }
  }
}
