package org.aryamahasangh.features.organisations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aryamahasangh.components.OrgItem
import org.aryamahasangh.navigation.LocalSnackbarHostState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrgsScreen(
  onNavigateToOrgDetails: (String) -> Unit,
  viewModel: OrganisationsViewModel
) {
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
        if(org.name == "आर्य महासंघ") return@forEach
        OrgItem(org.name, org.description) {
          onNavigateToOrgDetails(org.id)
        }
      }
    }
  }
}
