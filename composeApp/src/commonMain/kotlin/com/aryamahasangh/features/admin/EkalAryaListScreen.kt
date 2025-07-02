package com.aryamahasangh.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.error_profile_image
import coil3.compose.AsyncImage
import com.aryamahasangh.features.activities.toDevanagariNumerals
import com.aryamahasangh.navigation.LocalSnackbarHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

// Global object to persist pagination state across ViewModel recreation
private object EkalAryaPageState {
  var members: List<MemberShort> = emptyList()
  var paginationState: PaginationState<MemberShort> = PaginationState()
  var lastSearchQuery: String = ""

  fun clear() {
    members = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
  }

  fun saveState(newMembers: List<MemberShort>, newPaginationState: PaginationState<MemberShort>, searchQuery: String) {
    members = newMembers
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = members.isNotEmpty()
}

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
  val windowInfo = currentWindowAdaptiveInfo()
  val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

  val listState = rememberSaveable(
    key = "admin_ekal_arya_list_state",
    saver = LazyListState.Saver
  ) {
    LazyListState()
  }

  // Reset scroll to top when user searches
  LaunchedEffect(uiState.searchQuery) {
    if (uiState.searchQuery.isNotEmpty()) {
      // Clear saved state when user searches
      EkalAryaPageState.clear()
      listState.animateScrollToItem(0)
    }
  }

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

  LaunchedEffect(Unit) {
    // Preserve pagination if we have existing data (user navigated back)
    if (EkalAryaPageState.hasData() && EkalAryaPageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preserveEkalAryaPagination(EkalAryaPageState.members, EkalAryaPageState.paginationState)
    }

    // Load data (will be skipped if preserved)
    viewModel.loadEkalAryaMembersPaginated(pageSize = pageSize, resetPagination = true)
  }

  LaunchedEffect(listState) {
    snapshotFlow {
      val layoutInfo = listState.layoutInfo
      val totalItems = layoutInfo.totalItemsCount
      val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

      val threshold = (totalItems * 0.9).toInt()
      lastVisibleItem >= threshold && totalItems > 0
    }
      .distinctUntilChanged()
      .collect { shouldLoadMore ->
        if (shouldLoadMore && uiState.paginationState.hasNextPage && !uiState.paginationState.isLoadingNextPage) {
          viewModel.loadNextEkalAryaPage()
        }
      }
  }

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

  LaunchedEffect(uiState) {
    EkalAryaPageState.saveState(uiState.members, uiState.paginationState, uiState.searchQuery)
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = if (isCompact) Arrangement.SpaceBetween else Arrangement.Start
    ) {
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = viewModel::searchEkalAryaMembersWithDebounce,
        modifier = if (isCompact) Modifier.weight(1f) else Modifier.widthIn(max = 600.dp),
        placeholder = { Text("आर्य का नाम/दूरभाष") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
          if (uiState.paginationState.isSearching) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp
            )
          }
        },
        singleLine = true
      )

      if (!isCompact) {
        Spacer(modifier = Modifier.weight(1f))
      } else {
        Spacer(modifier = Modifier.width(16.dp))
      }

      if (isCompact) {
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

    if (uiState.paginationState.isInitialLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
      return@Column
    }

    uiState.paginationState.error?.let { error ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.errorContainer
        )
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            text = error,
            color = MaterialTheme.colorScheme.onErrorContainer
          )
          if (uiState.paginationState.showRetryButton) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
              onClick = { viewModel.retryEkalAryaLoad() }
            ) {
              Text("पुनः प्रयास करें")
            }
          }
        }
      }
      return@Column
    }

    val membersToShow = uiState.members

    if (membersToShow.isEmpty() && !uiState.paginationState.isInitialLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text("कोई एकल आर्य नहीं मिले")
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(0.dp)
      ) {
        LazyColumn(
          state = listState,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          if (isCompact) {
            items(membersToShow) { member ->
              MemberItem(
                member = member,
                onMemberClick = { onNavigateToMemberDetail(member.id) },
                onEditClick = { onNavigateToEditMember(member.id) },
                onDeleteClick = { showDeleteDialog = member },
                modifier = Modifier.width(490.dp)
              )
            }
          } else {
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
                    modifier = Modifier.width(490.dp)
                  )
                }
              }
            }
          }

          if (uiState.paginationState.isLoadingNextPage) {
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator()
              }
            }
          }

          if (uiState.paginationState.hasReachedEnd && !uiState.paginationState.hasNextPage && membersToShow.isNotEmpty()) {
            item {
              val totalItemsDisplayed = "${uiState.members.size}".toDevanagariNumerals()
              Text(
                text = "सभी आर्य सदस्य दिखाए गए(${totalItemsDisplayed})",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        CustomScrollbar(
          modifier = Modifier.align(Alignment.CenterEnd)
            .fillMaxHeight(),
          scrollState = listState
        )
      }
    }
  }

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

@Composable
private fun CustomScrollbar(
  modifier: Modifier = Modifier,
  scrollState: LazyListState
) {
  val layoutInfo = scrollState.layoutInfo
  val totalItems = layoutInfo.totalItemsCount
  val visibleItems = layoutInfo.visibleItemsInfo

  if (totalItems > 0 && visibleItems.isNotEmpty()) {
    val firstVisibleIndex = visibleItems.first().index

    val scrollProgress = if (totalItems > 1) {
      firstVisibleIndex.toFloat() / (totalItems - 1).coerceAtLeast(1)
    } else 0f

    val thumbSize = if (totalItems > 0) {
      (visibleItems.size.toFloat() / totalItems).coerceIn(0.1f, 1.0f)
    } else 0.1f

    Box(
      modifier = modifier
        .width(8.dp)
        .fillMaxHeight()
        .padding(2.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            shape = RoundedCornerShape(4.dp)
          )
      )

      BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
      ) {
        val trackHeight = maxHeight
        val thumbHeight = trackHeight * thumbSize
        val thumbOffset = (trackHeight - thumbHeight) * scrollProgress

        Box(
          modifier = Modifier
            .width(8.dp)
            .height(thumbHeight)
            .offset(y = thumbOffset)
            .background(
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
              shape = RoundedCornerShape(4.dp)
            )
        )
      }
    }
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
      onMemberClick = {},
      onEditClick = {},
      onDeleteClick = {}
    )
  }
}
