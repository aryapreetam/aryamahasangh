package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.components.OrganisationDetail
import org.aryamahasangh.viewmodel.OrganisationsViewModel

@Composable
fun OrgDetailScreen(
  name: String, 
  navController: NavHostController,
  viewModel: OrganisationsViewModel
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  
  // Load organisation details
  LaunchedEffect(name) {
    viewModel.loadOrganisationDetail(name)
  }
  
  // Collect UI state from ViewModel
  val uiState by viewModel.organisationDetailUiState.collectAsState()
  
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
        Text("Failed to load organisation details")
        Button(onClick = { viewModel.loadOrganisationDetail(name) }) {
          Text("Retry")
        }
      }
    }
    return
  }
  
  // Handle null organisation
  if (uiState.organisation == null) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("Organisation not found")
    }
    return
  }
  
  OrganisationDetail(uiState.organisation!!)
}