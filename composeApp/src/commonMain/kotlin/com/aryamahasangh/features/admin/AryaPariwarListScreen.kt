package com.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import coil3.compose.AsyncImage
import com.aryamahasangh.features.activities.toDevanagariNumerals
import com.aryamahasangh.navigation.LocalSnackbarHostState
import kotlinx.datetime.Clock

// Global object to persist pagination state across ViewModel recreation
internal object AryaPariwarPageState {
  var families: List<FamilyShort> = emptyList()
  var paginationState: PaginationState<FamilyShort> = PaginationState()
  var lastSearchQuery: String = ""
  var needsRefresh: Boolean = false

  fun clear() {
    families = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    needsRefresh = false
  }

  fun saveState(newFamilies: List<FamilyShort>, newPaginationState: PaginationState<FamilyShort>, searchQuery: String) {
    families = newFamilies
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = families.isNotEmpty()

  fun markForRefresh() {
    needsRefresh = true
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AryaPariwarListScreen(
  viewModel: FamilyViewModel,
  onNavigateToFamilyDetail: (String) -> Unit = {},
  onNavigateToCreateFamily: () -> Unit = {},
  onEditFamily: (String) -> Unit = {},
  onDeleteFamily: (String) -> Unit = {},
  onDataChanged: () -> Unit = {} // Add callback for count updates
) {
  val uiState by viewModel.familiesUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val scope = rememberCoroutineScope()
  var showDeleteDialog by remember { mutableStateOf<FamilyShort?>(null) }
  val keyboardController = LocalSoftwareKeyboardController.current
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
  val refreshKey = remember(AryaPariwarPageState.needsRefresh) {
    if (AryaPariwarPageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
  }

  LaunchedEffect(refreshKey) {
    // Clear preserved state if refresh is requested
    if (AryaPariwarPageState.needsRefresh) {
      AryaPariwarPageState.clear()
    }

    // Preserve pagination if we have existing data (user navigated back from view-only)
    if (!AryaPariwarPageState.needsRefresh && AryaPariwarPageState.hasData() && AryaPariwarPageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preserveFamilyPagination(AryaPariwarPageState.families, AryaPariwarPageState.paginationState)
    }

    // Load data: Reset pagination if refresh needed OR no existing data (initial load)
    val shouldReset = AryaPariwarPageState.needsRefresh || !AryaPariwarPageState.hasData()
    viewModel.loadFamiliesPaginated(pageSize = pageSize, resetPagination = shouldReset)
    AryaPariwarPageState.needsRefresh = false
  }

  // Save state only when families list changes significantly
  LaunchedEffect(uiState.families.size, uiState.searchQuery) {
    // Only save state if we have families and it's not during initial loading
    if (uiState.families.isNotEmpty() && !uiState.paginationState.isInitialLoading) {
      AryaPariwarPageState.saveState(uiState.families, uiState.paginationState, uiState.searchQuery)
    }
  }

  PaginatedListScreen(
    items = uiState.families,
    paginationState = uiState.paginationState,
    searchQuery = uiState.searchQuery,
    onSearchChange = viewModel::searchFamiliesWithDebounce,
    onLoadMore = viewModel::loadNextFamilyPage,
    onRetry = viewModel::retryFamilyLoad,
    searchPlaceholder = "परिवार का नाम",
    emptyStateText = "कोई परिवार नहीं मिले",
    endOfListText = { count -> "सभी परिवार दिखाए गए(${uiState.families.count().toString().toDevanagariNumerals()})" },
    addButtonText = "नया परिवार जोड़ें",
    onAddClick = onNavigateToCreateFamily,
    isCompactLayout = isCompact,
    itemsPerRow = if (isCompact) 1 else 2,
    itemContent = { family ->
      FamilyItem(
        family = family,
        onItemClick = { onNavigateToFamilyDetail(family.id) },
        onEditClick = { onEditFamily(family.id) },
        onDeleteClick = {
          // Mark for refresh and delete
          AryaPariwarPageState.markForRefresh()
          viewModel.deleteFamily(family.id) {
            onDataChanged()
          }
        }
      )
    }
  )
}

@Composable
private fun FamilyItem(
  family: FamilyShort,
  onItemClick: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  var expanded by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  // Create a tinted painter for error state
  val vectorPainter = rememberVectorPainter(Icons.Default.FamilyRestroom)
  val errorPainter = remember(vectorPainter) {
    object : Painter() {
      override val intrinsicSize: Size get() = vectorPainter.intrinsicSize

      override fun DrawScope.onDraw() {
        with(vectorPainter) {
          draw(
            size = size,
            colorFilter = ColorFilter.tint(Color.Gray)
          )
        }
      }
    }
  }

  Card(
    onClick = onItemClick,
    modifier = Modifier.width(490.dp),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Family Photo (square shape)
      val familyPhoto = family.photos.firstOrNull()
      if (familyPhoto != null) {
        AsyncImage(
          model = familyPhoto,
          contentDescription = "परिवार फोटो",
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Crop,
          error = errorPainter,
          fallback = errorPainter
        )
      } else {
        // Placeholder for family photo
        Surface(
          modifier = Modifier.size(80.dp),
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Icon(
              Icons.Default.FamilyRestroom,
              contentDescription = "परिवार फोटो",
              modifier = Modifier.size(40.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      // Family Info
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = family.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )

        if (family.address.isNotEmpty()) {
          Text(
            text = family.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (family.aryaSamajName.isNotEmpty()) {
          Text(
            text = "आर्य समाज: ${family.aryaSamajName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }

      // Overflow menu
      Box {
        IconButton(onClick = { expanded = !expanded }) {
          Icon(Icons.Default.MoreVert, contentDescription = "अधिक क्रियाएँ")
        }

        DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false }
        ) {
          DropdownMenuItem(
            text = { Text("संपादित करें") },
            onClick = {
              expanded = false
              onEditClick()
            },
            leadingIcon = {
              Icon(Icons.Default.Edit, contentDescription = "संपादित करें")
            }
          )
          DropdownMenuItem(
            text = { Text("हटाएँ") },
            onClick = {
              expanded = false
              showDeleteDialog = true
            },
            leadingIcon = {
              Icon(
                Icons.Default.Delete,
                contentDescription = "हटाएँ",
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
      title = { Text("परिवार हटाएँ") },
      text = {
        Text("क्या आप वाकई \"${family.name}\" परिवार को हटाना चाहते हैं? यह कार्रवाई पूर्ववत नहीं की जा सकती।")
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
