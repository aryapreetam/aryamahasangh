package com.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryamahasangh.components.ErrorSnackbar
import com.aryamahasangh.components.InlineErrorMessage
import com.aryamahasangh.features.activities.toDevanagariNumerals
import com.aryamahasangh.features.admin.data.AryaSamajViewModel
import com.aryamahasangh.navigation.LocalSnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContainerScreen(
  viewModel: AdminViewModel,
  aryaSamajViewModel: AryaSamajViewModel,
  familyViewModel: FamilyViewModel,
  initialTabIndex: Int = 0,
  onNavigateToMemberDetail: (String) -> Unit = {},
  onNavigateToAddMember: () -> Unit = {},
  onNavigateToEditMember: (String) -> Unit = {},
  onNavigateToAddAryaSamaj: () -> Unit = {},
  onNavigateToAryaSamajDetail: (String) -> Unit = {},
  onEditAryaSamaj: (String) -> Unit = {},
  onNavigateToCreateFamily: () -> Unit = {},
  onNavigateToFamilyDetail: (String) -> Unit = {},
  onEditFamily: (String) -> Unit = {},
  onDeleteFamily: (String) -> Unit = {}
) {
  val adminCounts by viewModel.adminCounts.collectAsState()
  val membersUiState by viewModel.membersUiState.collectAsState()
  val ekalAryaUiState by viewModel.ekalAryaUiState.collectAsState()
  val aryaSamajUiState by aryaSamajViewModel.listUiState.collectAsState()
  val familyUiState by familyViewModel.familiesUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current


  LaunchedEffect(Unit) {
    viewModel.loadAdminCounts()
    //viewModel.loadEkalAryaMembers()
    //familyViewModel.loadFamilies()
    //aryaSamajViewModel.loadAryaSamajs()
  }

  // Listen to real-time admin count changes - Compose-managed lifecycle
  LaunchedEffect(Unit) {
    viewModel.listenToAdminCountChanges().collect {
      viewModel.loadAdminCounts()
    }
  }

  // Show error snackbar for admin counts loading errors (if any)
  ErrorSnackbar(
    error = adminCounts.appError,
    snackbarHostState = snackbarHostState,
    onRetry = {
      viewModel.clearAdminCountsError()
      viewModel.loadAdminCounts()
    },
    onDismiss = { viewModel.clearAdminCountsError() }
  )

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

  // Show error snackbar for EkalArya loading errors (if any)
  ErrorSnackbar(
    error = ekalAryaUiState.appError,
    snackbarHostState = snackbarHostState,
    onRetry = {
      viewModel.clearEkalAryaError()
      viewModel.loadEkalAryaMembers()
    },
    onDismiss = { viewModel.clearEkalAryaError() }
  )

  // Show error snackbar for family loading errors (if any)
  familyUiState.error?.let { errorMessage ->
    LaunchedEffect(errorMessage) {
      val result = snackbarHostState.showSnackbar(
        message = errorMessage,
        actionLabel = "पुनः प्रयास"
      )
      if (result == SnackbarResult.ActionPerformed) {
        familyViewModel.clearError()
        familyViewModel.loadFamilies()
      }
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTabIndex) }
    val pagerState = rememberPagerState { 4 } // Now showing 2 tabs: members and arya samaj

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

    PrimaryScrollableTabRow(
      selectedTabIndex = selectedTabIndex
    ) {
      val organisationalMembersCount =
        if (adminCounts.appError == null) {
          "${adminCounts.counts.organisationalMembersCount}".toDevanagariNumerals()
        } else {
          "?" // Show question mark if count couldn't be loaded
        }

      val aryaSamajCount =
        if (adminCounts.appError == null) {
          "${adminCounts.counts.aryaSamajCount}".toDevanagariNumerals()
        } else {
          "?"
        }

      val ekalAryaCount =
        if (adminCounts.appError == null) {
          "${adminCounts.counts.ekalAryaCount}".toDevanagariNumerals()
        } else {
          "?"
        }

      val familyCount =
        if (adminCounts.appError == null) {
          "${adminCounts.counts.familyCount}".toDevanagariNumerals()
        } else {
          "?"
        }

      Tab(
        selected = selectedTabIndex == 0,
        onClick = { selectedTabIndex = 0 },
        text = { Text("पदाधिकारी ($organisationalMembersCount)") }
      )
      Tab(
        selected = selectedTabIndex == 1,
        onClick = { selectedTabIndex = 1 },
        text = { Text("आर्य समाज ($aryaSamajCount)") }
      )
      Tab(
        selected = selectedTabIndex == 2,
        onClick = { selectedTabIndex = 2 },
        text = { Text("आर्य परिवार ($familyCount)") }
      )
      Tab(
        selected = selectedTabIndex == 3,
        onClick = { selectedTabIndex = 3 },
        text = { Text("एकल आर्य ($ekalAryaCount)") }
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
          OrganisationalMembersScreen(
            viewModel = viewModel,
            onNavigateToMemberDetail = onNavigateToMemberDetail,
            onNavigateToEditMember = { id -> onNavigateToEditMember(id) }
          )
        } else if (it == 1) {
          AryaSamajListScreen(
            viewModel = aryaSamajViewModel,
            onNavigateToAddAryaSamaj = onNavigateToAddAryaSamaj,
            onNavigateToAryaSamajDetail = onNavigateToAryaSamajDetail,
            onEditAryaSamaj = onEditAryaSamaj,
            onDataChanged = {
              // Refresh admin counts when Arya Samaj data changes
              viewModel.loadAdminCounts()
            }
          )
        } else if (it == 2) {
          AryaPariwarListScreen(
            viewModel = familyViewModel,
            onNavigateToFamilyDetail = onNavigateToFamilyDetail,
            onNavigateToCreateFamily = onNavigateToCreateFamily,
            onEditFamily = onEditFamily,
            onDeleteFamily = onDeleteFamily
          )
        } else if (it == 3) {
          EkalAryaListScreen(
            viewModel = viewModel,
            onNavigateToMemberDetail = onNavigateToMemberDetail,
            onNavigateToAddMember = { onNavigateToAddMember() },
            onNavigateToEditMember = { id -> onNavigateToEditMember(id) }
          )
        }
      }
    }
  }
}
