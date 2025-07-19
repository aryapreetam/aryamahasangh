package com.aryamahasangh.features.admin.member

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.error_profile_image
import coil3.compose.AsyncImage
import com.aryamahasangh.features.activities.toDevanagariNumerals
import com.aryamahasangh.features.admin.AdminViewModel
import com.aryamahasangh.features.admin.MemberShort
import com.aryamahasangh.features.admin.PaginatedListScreen
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.navigation.LocalSnackbarHostState
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

// Global object to persist pagination state across ViewModel recreation
internal object SingleMemberPageState {
  var members: List<MemberShort> = emptyList()
  var paginationState: PaginationState<MemberShort> = PaginationState()
  var lastSearchQuery: String = ""
  var needsRefresh: Boolean = false

  fun clear() {
    members = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    needsRefresh = false
  }

  fun saveState(newMembers: List<MemberShort>, newPaginationState: PaginationState<MemberShort>, searchQuery: String) {
    members = newMembers
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = members.isNotEmpty()

  fun markForRefresh() {
    needsRefresh = true
  }

  fun clearSearchState() {
    lastSearchQuery = ""
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SingleMemberListScreen(
  viewModel: AdminViewModel,
  onNavigateToMemberDetail: (String) -> Unit = {},
  onNavigateToAddMember: () -> Unit = {},
  onEditMember: (String) -> Unit = {},
  onDeleteMember: (String) -> Unit = {},
  onDataChanged: () -> Unit = {}
) {
  val snackbarHostState = LocalSnackbarHostState.current
  val uiState by viewModel.ekalAryaUiState.collectAsState()
  val deleteState by viewModel.deleteMemberState.collectAsState()
  val windowInfo = currentWindowAdaptiveInfo()
  val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

  val density = LocalDensity.current
  val screenWidthDp = with(density) {
    windowInfo.windowSizeClass.windowWidthSizeClass.let {
      when (it) {
        WindowWidthSizeClass.COMPACT -> 600f
        WindowWidthSizeClass.MEDIUM -> 800f
        WindowWidthSizeClass.EXPANDED -> 1200f
        else -> 600f
      }
    }
  }
  val pageSize = viewModel.calculatePageSize(screenWidthDp)

  val refreshKey = remember(SingleMemberPageState.needsRefresh) {
    if (SingleMemberPageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
  }

  LaunchedEffect(refreshKey) {
    if (SingleMemberPageState.needsRefresh) {
      SingleMemberPageState.clear()
    }

    when {
      // Scenario 1: Have saved search query → restore and search with fresh results
      !SingleMemberPageState.needsRefresh && SingleMemberPageState.lastSearchQuery.isNotEmpty() -> {
        viewModel.restoreAndSearchEkalArya(SingleMemberPageState.lastSearchQuery)
      }

      // Scenario 2: Have saved non-search data → preserve pagination  
      !SingleMemberPageState.needsRefresh && SingleMemberPageState.hasData() -> {
        viewModel.preserveEkalAryaPagination(SingleMemberPageState.members, SingleMemberPageState.paginationState)
      }

      // Scenario 3: No saved data → load fresh initial data
      else -> {
        viewModel.loadEkalAryaMembersPaginated(pageSize = pageSize, resetPagination = true)
      }
    }

    SingleMemberPageState.needsRefresh = false
  }

  LaunchedEffect(uiState) {
    SingleMemberPageState.saveState(uiState.members, uiState.paginationState, uiState.searchQuery)
  }

  // Snackbar logic for deletion feedback
  LaunchedEffect(deleteState.isDeleting) {
    if (deleteState.isDeleting) {
      snackbarHostState.showSnackbar("सदस्य हटाया जा रहा है...")
    }
  }

  LaunchedEffect(deleteState.deleteSuccess) {
    if (deleteState.deleteSuccess) {
      snackbarHostState.showSnackbar("सदस्य सफलतापूर्वक हटाया गया")
      viewModel.resetDeleteState()
    }
  }

  LaunchedEffect(deleteState.deleteError) {
    deleteState.deleteError?.let { error ->
      val result = snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "पुनः प्रयास"
      )
      if (result == SnackbarResult.ActionPerformed) {
        // Handle retry if needed
      }
      viewModel.resetDeleteState()
    }
  }

  PaginatedListScreen(
    items = uiState.members,
    paginationState = uiState.paginationState,
    searchQuery = uiState.searchQuery,
    onSearchChange = viewModel::searchEkalAryaMembersWithDebounce,
    onLoadMore = viewModel::loadNextEkalAryaPage,
    onRetry = viewModel::retryEkalAryaLoad,
    searchPlaceholder = "आर्य का नाम/दूरभाष",
    emptyStateText = "कोई एकल आर्य नहीं मिले",
    endOfListText = { count ->
      "सभी आर्य सदस्य दिखाए गए(${
        uiState.members.count().toString().toDevanagariNumerals()
      })"
    },
    addButtonText = "नए आर्य जोड़ें",
    onAddClick = onNavigateToAddMember,
    isCompactLayout = isCompact,
    itemsPerRow = if (isCompact) 1 else 2,
    itemContent = { member ->
      MemberItem(
        member = member,
        onItemClick = { onNavigateToMemberDetail(member.id) },
        onEditClick = { onEditMember(member.id) },
        onDeleteClick = {
          SingleMemberPageState.markForRefresh()
          viewModel.deleteMember(member.id) {
            onDataChanged()
          }
        }
      )
    }
  )
}

@Composable
private fun MemberItem(
  member: MemberShort,
  onItemClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showOptionsMenu by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  ElevatedCard(
    modifier =
      modifier
        .clickable { onItemClick() }
        .width(490.dp),
    shape = RoundedCornerShape(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = member.profileImage,
        contentDescription = "Profile Image",
        modifier =
          Modifier
            .size(60.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        error = painterResource(Res.drawable.error_profile_image)
      )

      Spacer(modifier = Modifier.width(12.dp))

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
              showDeleteDialog = true
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
          )
        }
      }
    }
  }
  // Delete confirmation dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("परिवार हटाएँ") },
      text = {
        Text("क्या आप निश्चित हैं कि आप \"${member.name}\" को हटाना चाहते हैं? यह कार्रवाई पूर्ववत नहीं की जा सकती।")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDeleteClick()
          },
          colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error
          )
        ) {
          Text("हटाएँ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("रद्द करें")
        }
      }
    )
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
      onItemClick = {},
      onEditClick = {},
      onDeleteClick = {}
    )
  }
}
