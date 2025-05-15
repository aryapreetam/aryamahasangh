package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.components.OrgItem
import org.aryamahasangh.navigation.Screen
import org.aryamahasangh.viewmodel.OrganisationsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrgsScreen(
  navController: NavHostController, 
  onNavigateToOrgDetails: (String) -> Unit,
  viewModel: OrganisationsViewModel
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  
  // Collect UI state from ViewModel
  val uiState by viewModel.uiState.collectAsState()
  
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

  Column(
    modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    FlowRow(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      uiState.organisations.forEach { org ->
        OrgItem(org.name, org.description) {
          onNavigateToOrgDetails(org.name)
          navController.navigate(Screen.OrgDetails(org.name))
        }
      }
    }
  }
}