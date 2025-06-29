package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aryamahasangh.components.ErrorSnackbar
import org.aryamahasangh.components.InlineErrorMessage
import org.aryamahasangh.features.activities.toDevanagariNumerals
import org.aryamahasangh.features.admin.data.AryaSamajViewModel
import org.aryamahasangh.navigation.LocalSnackbarHostState

@Composable
fun AdminContainerScreen(
  viewModel: AdminViewModel,
  aryaSamajViewModel: AryaSamajViewModel,
  familyViewModel: FamilyViewModel, // Add family view model
  initialTabIndex: Int = 0, // Add initial tab index parameter
  onNavigateToMemberDetail: (String) -> Unit = {},
  onNavigateToAddMember: () -> Unit = {},
  onNavigateToEditMember: (String) -> Unit = {}, // Add edit member navigation
  onNavigateToAddAryaSamaj: () -> Unit = {},
  onNavigateToAryaSamajDetail: (String) -> Unit = {},
  onEditAryaSamaj: (String) -> Unit = {}, // New parameter for editing
  onNavigateToCreateFamily: () -> Unit = {}, // Fixed parameter name
  onNavigateToFamilyDetail: (String) -> Unit = {}, // Add navigation for family detail
  onEditFamily: (String) -> Unit = {},
  onDeleteFamily: (String) -> Unit = {}
) {
  val membersCount by viewModel.membersCount.collectAsState()
  val membersUiState by viewModel.membersUiState.collectAsState()
  val ekalAryaUiState by viewModel.ekalAryaUiState.collectAsState()
  val aryaSamajUiState by aryaSamajViewModel.listUiState.collectAsState()
  val familyUiState by familyViewModel.familiesUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current


  LaunchedEffect(Unit) {
    viewModel.getMembersCount()
    viewModel.loadEkalAryaMembers()
    familyViewModel.loadFamilies()
    aryaSamajViewModel.loadAryaSamajs()
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
      println(membersCount)
      val count =
        if (membersUiState.appError == null) {
          "$membersCount".toDevanagariNumerals()
        } else {
          "?" // Show question mark if count couldn't be loaded
        }

      val aryaSamajCount =
        if (aryaSamajUiState.appError == null) {
          "${aryaSamajUiState.aryaSamajs.size}".toDevanagariNumerals()
        } else {
          "?"
        }

      val ekalAryaCount =
        if (ekalAryaUiState.appError == null) {
          "${ekalAryaUiState.ekalAryaMembers.size}".toDevanagariNumerals()
        } else {
          "?"
        }

      val familyCount =
        if (familyUiState.error == null) {
          "${familyUiState.families.size}".toDevanagariNumerals()
        } else {
          "?"
        }

      Tab(
        selected = selectedTabIndex == 0,
        onClick = { selectedTabIndex = 0 },
        text = { Text("पदाधिकारी ($count)") }
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
            onNavigateToEditMember = onNavigateToEditMember // Pass the new handler
          )
        } else if (it == 1) {
          AryaSamajListScreen(
            viewModel = aryaSamajViewModel,
            onNavigateToAddAryaSamaj = onNavigateToAddAryaSamaj,
            onNavigateToAryaSamajDetail = onNavigateToAryaSamajDetail,
            onEditAryaSamaj = onEditAryaSamaj // Pass the new handler
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
            onNavigateToAddMember = onNavigateToAddMember,
            onNavigateToEditMember = onNavigateToEditMember
          )
        }
      }
    }
  }
}
