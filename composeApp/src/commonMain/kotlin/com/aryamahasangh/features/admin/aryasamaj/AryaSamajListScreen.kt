package com.aryamahasangh.features.admin.aryasamaj

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TempleHindu
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.AsyncImage
import com.aryamahasangh.features.activities.toDevanagariNumerals
import com.aryamahasangh.navigation.LocalSnackbarHostState
import kotlinx.datetime.Clock

// Global object to persist pagination state across ViewModel recreation
internal object AryaSamajPageState {
  var aryaSamajs: List<com.aryamahasangh.fragment.AryaSamajWithAddress> = emptyList()
  var paginationState: com.aryamahasangh.features.admin.PaginationState<com.aryamahasangh.fragment.AryaSamajWithAddress> =
    com.aryamahasangh.features.admin.PaginationState()
  var lastSearchQuery: String = ""
  var needsRefresh: Boolean = false

  fun clear() {
    aryaSamajs = emptyList()
    paginationState = com.aryamahasangh.features.admin.PaginationState()
    lastSearchQuery = ""
    needsRefresh = false
  }

  fun saveState(
    newAryaSamajs: List<com.aryamahasangh.fragment.AryaSamajWithAddress>,
    newPaginationState: com.aryamahasangh.features.admin.PaginationState<com.aryamahasangh.fragment.AryaSamajWithAddress>,
    searchQuery: String
  ) {
    aryaSamajs = newPaginationState.items
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = aryaSamajs.isNotEmpty()

  fun markForRefresh() {
    needsRefresh = true
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AryaSamajListScreen(
  viewModel: AryaSamajViewModel,
  onNavigateToAddAryaSamaj: () -> Unit = {},
  onNavigateToAryaSamajDetail: (String) -> Unit = {},
  onEditAryaSamaj: (String) -> Unit = {},
  onDeleteAryaSamaj: (String) -> Unit = {},
  onDataChanged: () -> Unit = {}
) {
  val snackbarHostState = LocalSnackbarHostState.current
  val uiState by viewModel.listUiState.collectAsState()
  val scope = rememberCoroutineScope()
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

  // Generate a unique key when refresh is needed
  val refreshKey = remember(AryaSamajPageState.needsRefresh) {
    if (AryaSamajPageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
  }

  LaunchedEffect(refreshKey) {
    // Clear preserved state if refresh is requested
    if (AryaSamajPageState.needsRefresh) {
      AryaSamajPageState.clear()
    }

    when {
      // Scenario 1: Have saved search query → restore and search with fresh results
      !AryaSamajPageState.needsRefresh && AryaSamajPageState.lastSearchQuery.isNotEmpty() -> {
        viewModel.restoreAndSearchAryaSamaj(AryaSamajPageState.lastSearchQuery)
      }

      // Scenario 2: Have saved non-search data → preserve pagination  
      !AryaSamajPageState.needsRefresh && AryaSamajPageState.hasData() -> {
        viewModel.preserveAryaSamajPagination(AryaSamajPageState.aryaSamajs, AryaSamajPageState.paginationState)
      }

      // Scenario 3: No saved data → load fresh initial data
      else -> {
        viewModel.loadAryaSamajsPaginated(pageSize = pageSize, resetPagination = true)
      }
    }

    AryaSamajPageState.needsRefresh = false
  }

  LaunchedEffect(uiState) {
    AryaSamajPageState.saveState(uiState.paginationState.items, uiState.paginationState, uiState.searchQuery)
  }

  // Show deletion errors from ViewModel state
  LaunchedEffect(uiState.deleteError) {
    uiState.deleteError?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.clearListError() // Clear the error after showing
    }
  }

  // Show deletion success from ViewModel state
  LaunchedEffect(uiState.deleteSuccess) {
    uiState.deleteSuccess?.let { success ->
      snackbarHostState.showSnackbar(success)
      viewModel.clearListError() // Clear the success message after showing
    }
  }

  com.aryamahasangh.features.admin.PaginatedListScreen(
    items = uiState.aryaSamajs,
    paginationState = uiState.paginationState,
    searchQuery = uiState.searchQuery,
    onSearchChange = viewModel::searchAryaSamajsWithDebounce,
    onLoadMore = viewModel::loadNextAryaSamajPage,
    onRetry = viewModel::retryAryaSamajLoad,
    searchPlaceholder = "आर्य समाज का नाम",
    emptyStateText = "कोई आर्य समाज नहीं मिले",
    endOfListText = { count -> "सभी आर्य समाज दिखाए गए(${count.toString().toDevanagariNumerals()})" },
    addButtonText = "नया आर्य समाज जोड़ें",
    onAddClick = onNavigateToAddAryaSamaj,
    isCompactLayout = isCompact,
    itemsPerRow = if (isCompact) 1 else 2,
    itemContent = { aryaSamaj ->
      AryaSamajItem(
        aryaSamaj = aryaSamaj,
        onItemClick = { onNavigateToAryaSamajDetail(aryaSamaj.id) },
        onEditClick = { onEditAryaSamaj(aryaSamaj.id) },
        onDeleteClick = {
          // Mark for refresh and delete
          AryaSamajPageState.markForRefresh()
          viewModel.deleteAryaSamaj(aryaSamaj.id) {
            onDataChanged()
          }
        }
      )
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AryaSamajItem(
  aryaSamaj: AryaSamajListItem,
  onItemClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onEditClick: () -> Unit
) {
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showDropdownMenu by remember { mutableStateOf(false) }

  Card(
    onClick = onItemClick,
    modifier = Modifier.width(490.dp),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      // Arya Samaj Photo
      val aryaSamajPhoto = aryaSamaj.mediaUrls.firstOrNull()
      if (aryaSamajPhoto != null) {
        AsyncImage(
          model = aryaSamajPhoto,
          contentDescription = "आर्य समाज फोटो",
          modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Crop
        )
      } else {
        // Placeholder for arya samaj photo
        Surface(
          modifier = Modifier.size(60.dp),
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Icon(
              Icons.Default.TempleHindu,
              contentDescription = "आर्य समाज फोटो",
              modifier = Modifier.size(36.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = aryaSamaj.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = aryaSamaj.formattedAddress,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Box {
        IconButton(onClick = { showDropdownMenu = true }) {
          Icon(
            Icons.Default.MoreVert,
            contentDescription = "विकल्प"
          )
        }

        DropdownMenu(
          expanded = showDropdownMenu,
          onDismissRequest = { showDropdownMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("संपादित करें") },
            onClick = {
              showDropdownMenu = false
              onEditClick()
            },
            leadingIcon = {
              Icon(
                Icons.Default.Edit,
                contentDescription = null
              )
            }
          )
          DropdownMenuItem(
            text = { Text("हटाएँ") },
            onClick = {
              showDropdownMenu = false
              showDeleteDialog = true
            },
            leadingIcon = {
              Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
              )
            }
          )
        }
      }
    }
  }

  // Delete confirmation dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("आर्य समाज मिटाएं") },
      text = {
        Text("क्या आप वाकई \"${aryaSamaj.name}\" को मिटाना चाहते हैं? यह कार्रवाई पूर्ववत नहीं की जा सकती।")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDeleteClick()
          },
          colors =
            ButtonDefaults.textButtonColors(
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
