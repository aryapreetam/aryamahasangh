package com.aryamahasangh.features.organisations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.OrgItem
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.utils.WithTooltip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrgsScreen(
  onNavigateToOrgDetails: (String) -> Unit,
  onNavigateToCreateOrganisation: (Int) -> Unit = {},
  viewModel: OrganisationsViewModel
) {
  val snackbarHostState = LocalSnackbarHostState.current
  val isLoggedIn = LocalIsAuthenticated.current
  val scope = rememberCoroutineScope()

  // Collect UI state from ViewModel
  val uiState by viewModel.uiState.collectAsState()
  val deleteState by viewModel.deleteOrganisationState.collectAsState()

  // Handle loading state
  if (uiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      LinearProgressIndicator()
    }
    return
  }

  // Handle error state
  uiState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Retry"
      )
    }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Failed to load organisations")
        Button(onClick = { viewModel.loadOrganisations() }) {
          Text("Retry")
        }
      }
    }
    return
  }

  // Handle delete state
  deleteState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Retry"
      )
    }
  }

  // Handle delete success
  LaunchedEffect(deleteState.isSuccess) {
    if (deleteState.isSuccess) {
      snackbarHostState.showSnackbar("संस्था सफलतापूर्वक हटा दी गई")
    }
  }

  // Handle empty state
  if (uiState.organisations.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("No organisations available")
    }
    return
  }

  Box {
    // Main content
    Column(
      modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      FlowRow(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = if (isLoggedIn) 80.dp else 0.dp) // Add padding when FAB is visible
      ) {
        uiState.organisations.forEach { org ->
          if (org.name == "आर्य महासंघ") return@forEach
          OrgItem(
            name = org.name,
            description = org.description,
            navigateToOrgDetails = {
              onNavigateToOrgDetails(org.id)
            },
            onDeleteOrganisation = {
              scope.launch {
                viewModel.deleteOrganisation(org.id)
              }
            }
          )
        }
      }
    }

    // Add Organisation FAB - only visible to logged in users
    if (isLoggedIn) {
      FloatingActionButton(
        onClick = { onNavigateToCreateOrganisation(uiState.organisations.size + 1) },
        modifier =
          Modifier
            .align(Alignment.BottomEnd).padding(16.dp)
      ) {
        WithTooltip("नयी संस्था जोड़ें") {
          Icon(
            Icons.Default.Add,
            contentDescription = "नयी संस्था जोड़ें",
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    }
  }
}
