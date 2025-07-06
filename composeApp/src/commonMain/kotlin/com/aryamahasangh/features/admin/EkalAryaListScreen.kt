package com.aryamahasangh.features.admin

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
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

// Global object to persist pagination state across ViewModel recreation
internal object EkalAryaPageState {
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
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EkalAryaListScreen(
  viewModel: AdminViewModel,
  onNavigateToMemberDetail: (String) -> Unit = {},
  onNavigateToAddMember: () -> Unit = {},
  onEditMember: (String) -> Unit = {},
  onDeleteMember: (String) -> Unit = {},
  onDataChanged: () -> Unit = {}
) {
  val uiState by viewModel.ekalAryaUiState.collectAsState()
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

  val refreshKey = remember(EkalAryaPageState.needsRefresh) {
    if (EkalAryaPageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
  }

  LaunchedEffect(refreshKey) {
    if (EkalAryaPageState.needsRefresh) {
      EkalAryaPageState.clear()
    }

    if (!EkalAryaPageState.needsRefresh && EkalAryaPageState.hasData() && EkalAryaPageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preserveEkalAryaPagination(EkalAryaPageState.members, EkalAryaPageState.paginationState)
    }

    val shouldReset = EkalAryaPageState.needsRefresh
    viewModel.loadEkalAryaMembersPaginated(pageSize = pageSize, resetPagination = shouldReset)
    EkalAryaPageState.needsRefresh = false
  }

  LaunchedEffect(uiState) {
    EkalAryaPageState.saveState(uiState.members, uiState.paginationState, uiState.searchQuery)
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
    endOfListText = { count -> "सभी आर्य सदस्य दिखाए गए(${uiState.members.count().toString().toDevanagariNumerals()})" },
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
          EkalAryaPageState.markForRefresh()
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
      onItemClick = {},
      onEditClick = {},
      onDeleteClick = {}
    )
  }
}
