package org.aryamahasangh.features.organisations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.components.OrganisationDetail

@Composable
fun OrgDetailScreen(
  id: String,
  viewModel: OrganisationsViewModel
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  
  // Load organisation details
  LaunchedEffect(id) {
    viewModel.loadOrganisationDetail(id)
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
        Button(onClick = { viewModel.loadOrganisationDetail(id) }) {
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
  
  OrganisationDetail(
    organisation = uiState.organisation!!,
    viewModel::updateOrganisationLogo,
    updateOrganisationDescription = viewModel::updateOrganisationDescription
  )
}