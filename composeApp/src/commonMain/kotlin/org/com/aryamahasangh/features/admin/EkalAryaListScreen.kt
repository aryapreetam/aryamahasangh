package com.aryamahasangh.features.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.AsyncImage
import com.aryamahasangh.navigation.LocalSnackbarHostState
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EkalAryaListScreen(
  viewModel: AdminViewModel,
  onNavigateToMemberDetail: (String) -> Unit = {},
  onNavigateToEditMember: (id: String) -> Unit = {},
  onNavigateToAddMember: () -> Unit = {}
) {
  val uiState by viewModel.ekalAryaUiState.collectAsState()
  val deleteState by viewModel.deleteMemberState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val scope = rememberCoroutineScope()
  var showDeleteDialog by remember { mutableStateOf<MemberShort?>(null) }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    viewModel.loadEkalAryaMembers()
  }

  // Handle delete state changes
  LaunchedEffect(deleteState.isDeleting) {
    if (deleteState.isDeleting) {
      snackbarHostState.showSnackbar("सदस्य हटाया जा रहा है...")
    }
  }

  LaunchedEffect(deleteState.deleteSuccess) {
    if (deleteState.deleteSuccess) {
      snackbarHostState.showSnackbar("सदस्य सफलतापूर्वक हटा दिया गया")
      viewModel.resetDeleteState()
    }
  }

  LaunchedEffect(deleteState.deleteError) {
    deleteState.deleteError?.let { error ->
      val result =
        snackbarHostState.showSnackbar(
          message = error,
          actionLabel = "पुनः प्रयास"
        )
      if (result == SnackbarResult.ActionPerformed) {
        // Handle retry if needed
      }
      viewModel.resetDeleteState()
    }
  }

  val windowInfo = currentWindowAdaptiveInfo()
  val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = if (isCompact) Arrangement.SpaceBetween else Arrangement.Start
    ) {
      // Search Bar
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = viewModel::searchEkalAryaMembers,
        modifier = if (isCompact) Modifier.weight(1f) else Modifier.widthIn(max = 600.dp),
        placeholder = { Text("आर्य का नाम/दूरभाष") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        singleLine = true
      )

      if (!isCompact) {
        Spacer(modifier = Modifier.weight(1f))
      } else {
        Spacer(modifier = Modifier.width(16.dp))
      }

      if (isCompact) {
        // Tooltip for IconButton on compact screens
        TooltipBox(
          positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
          tooltip = {
            PlainTooltip {
              Text("नए आर्य जोड़ें")
            }
          },
          state = rememberTooltipState()
        ) {
          IconButton(
            onClick = onNavigateToAddMember
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = "Add new member")
          }
        }
      } else {
        // Button with text for larger screens
        Button(
          onClick = onNavigateToAddMember
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("नए आर्य जोड़ें")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Loading state
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
      return@Column
    }

    // Error state
    uiState.error?.let { error ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
          )
      ) {
        Text(
          text = error,
          modifier = Modifier.padding(16.dp),
          color = MaterialTheme.colorScheme.onErrorContainer
        )
      }
      return@Column
    }

    // Members List
    val membersToShow =
      if (uiState.searchQuery.isNotBlank()) {
        uiState.searchResults
      } else {
        uiState.ekalAryaMembers
      }

    if (membersToShow.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text("कोई एकल आर्य नहीं मिले")
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(membersToShow.chunked(2)) { chunk ->
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            chunk.forEach { member ->
              MemberItem(
                member = member,
                onMemberClick = { onNavigateToMemberDetail(member.id) },
                onEditClick = { onNavigateToEditMember(member.id) },
                onDeleteClick = { showDeleteDialog = member },
                modifier = Modifier.width(450.dp)
              )
            }
          }
        }
      }
    }
  }

  // Delete Confirmation Dialog
  showDeleteDialog?.let { member ->
    AlertDialog(
      onDismissRequest = { showDeleteDialog = null },
      title = { Text("सदस्य हटाएँ") },
      text = { Text("क्या निश्चितरूप से इन सदस्य को हटाना चाहते है?") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteMember(member.id, member.name)
            showDeleteDialog = null
          }
        ) {
          Text("हाँ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = null }) {
          Text("नहीं")
        }
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberItem(
  member: MemberShort,
  onMemberClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showOptionsMenu by remember { mutableStateOf(false) }

  ElevatedCard(
    modifier =
      modifier
        .clickable { onMemberClick() }
        .width(500.dp),
    shape = RoundedCornerShape(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Profile Image
      AsyncImage(
        model = member.profileImage.ifEmpty { "https://via.placeholder.com/60" },
        contentDescription = "Profile Image",
        modifier =
          Modifier
            .size(60.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.width(12.dp))

      // Member Info
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = member.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )
        if (member.place.isNotEmpty()) {
          Text(
            text = member.place,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }

      // Options Menu
      Box {
        IconButton(onClick = { showOptionsMenu = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }

        DropdownMenu(
          expanded = showOptionsMenu,
          onDismissRequest = { showOptionsMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("विवरण बदलें") },
            onClick = {
              showOptionsMenu = false
              onEditClick()
            },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
          )
          DropdownMenuItem(
            text = { Text("सदस्य हटाएँ") },
            onClick = {
              showOptionsMenu = false
              onDeleteClick()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
          )
        }
      }
    }
  }
}

@Preview
@Composable
private fun EkalAryaMemberItemPreview() {
  MaterialTheme {
    MemberItem(
      member =
        MemberShort(
          id = "1",
          name = "राम शर्मा",
          profileImage = "",
          place = "दिल्ली, भारत"
        ),
      onMemberClick = {},
      onEditClick = {},
      onDeleteClick = {}
    )
  }
}
