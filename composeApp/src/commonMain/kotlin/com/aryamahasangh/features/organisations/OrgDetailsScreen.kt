package com.aryamahasangh.features.organisations

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
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.OrganisationDetail
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.navigation.LocalSnackbarHostState

@Composable
fun OrgDetailScreen(
  id: String,
  viewModel: OrganisationsViewModel,
  searchMembers: (String) -> List<Member> = { emptyList() },
  allMembers: List<Member> = emptyList(),
  onTriggerSearch: (String) -> Unit = {}
) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current ?: return
  val isAuthenticated = LocalIsAuthenticated.current

  // Load organisation details
  LaunchedEffect(id) {
    viewModel.loadOrganisationDetail(id)
  }

  // Collect UI state from ViewModel
  val uiState by viewModel.organisationDetailUiState.collectAsState()
  val descriptionState by viewModel.organisationDescriptionState.collectAsState()
  val logoState by viewModel.organisationLogoState.collectAsState()
  val memberManagementState by viewModel.memberManagementState.collectAsState()

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

  // Handle description update error
  descriptionState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Close"
      )
    }
  }

  // Handle logo update error
  logoState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Close"
      )
    }
  }

  // Handle logo update success
  logoState.successMessage?.let { message ->
    LaunchedEffect(message) {
      snackbarHostState.showSnackbar(
        message = message,
        actionLabel = "Close"
      )
      viewModel.clearLogoSuccessMessage()
    }
  }

  // Handle member management progress messages
  LaunchedEffect(memberManagementState.isRemovingMember) {
    if (memberManagementState.isRemovingMember) {
      snackbarHostState.showSnackbar("Removing user from organisation...")
    }
  }

  LaunchedEffect(memberManagementState.isUpdatingPost) {
    if (memberManagementState.isUpdatingPost) {
      snackbarHostState.showSnackbar("Updating post...")
    }
  }

  LaunchedEffect(memberManagementState.isAddingMember) {
    if (memberManagementState.isAddingMember) {
      snackbarHostState.showSnackbar("Adding member to organisation...")
    }
  }

  // Handle member management success messages
  memberManagementState.successMessage?.let { message ->
    LaunchedEffect(message) {
      snackbarHostState.showSnackbar(
        message = message,
        actionLabel = "Close"
      )
      viewModel.clearMemberManagementMessage()
    }
  }

  // Handle member management error messages
  memberManagementState.removeError?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Close"
      )
      viewModel.clearMemberManagementMessage()
    }
  }

  memberManagementState.updatePostError?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Close"
      )
      viewModel.clearMemberManagementMessage()
    }
  }

  memberManagementState.addMemberError?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Close"
      )
      viewModel.clearMemberManagementMessage()
    }
  }

  OrganisationDetail(
    id = uiState.organisation!!.id,
    name = uiState.organisation!!.name,
    logo = uiState.organisation!!.logo,
    description = uiState.organisation!!.description,
    keyPeople = uiState.organisation!!.members,
    isLoggedIn = isAuthenticated,
    organisationDescriptionState = descriptionState,
    onDescriptionEditModeChange = viewModel::setDescriptionEditMode,
    updateOrganisationDescription = viewModel::updateOrganisationDescription,
    updateOrganisationLogo = viewModel::updateOrganisationLogo,
    organisationLogoState = logoState,
    onRemoveMember = viewModel::removeMemberFromOrganisation,
    onUpdateMemberPost = viewModel::updateMemberPost,
    onUpdateMemberPriority = viewModel::updateMemberPriority,
    onUpdateMemberPriorities = viewModel::updateMemberPriorities,
    onAddMemberToOrganisation = viewModel::addMemberToOrganisation,
    onTriggerSearch = onTriggerSearch,
    memberManagementState = memberManagementState,
    searchMembers = searchMembers,
    allMembers = allMembers
  )
}
