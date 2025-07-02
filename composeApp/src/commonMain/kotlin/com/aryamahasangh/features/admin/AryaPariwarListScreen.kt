package com.aryamahasangh.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.family_add
import coil3.compose.AsyncImage
import com.aryamahasangh.features.activities.toDevanagariNumerals
import com.aryamahasangh.navigation.LocalSnackbarHostState
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.vectorResource

// Global object to persist pagination state across ViewModel recreation
private object AryaPariwarPageState {
  var families: List<FamilyShort> = emptyList()
  var paginationState: PaginationState<FamilyShort> = PaginationState()
  var lastSearchQuery: String = ""
  var hasLoadedOnce: Boolean = false

  fun clear() {
    families = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    hasLoadedOnce = false
  }

  fun saveState(newFamilies: List<FamilyShort>, newPaginationState: PaginationState<FamilyShort>, searchQuery: String) {
    families = newFamilies
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = families.isNotEmpty()
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AryaPariwarListScreen(
  viewModel: FamilyViewModel,
  onNavigateToFamilyDetail: (String) -> Unit = {},
  onNavigateToCreateFamily: () -> Unit = {},
  onEditFamily: (String) -> Unit = {},
  onDeleteFamily: (String) -> Unit = {}
) {
  val uiState by viewModel.familiesUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val scope = rememberCoroutineScope()
  var showDeleteDialog by remember { mutableStateOf<FamilyShort?>(null) }
  val keyboardController = LocalSoftwareKeyboardController.current
  val windowInfo = currentWindowAdaptiveInfo()
  val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

  val listState = rememberSaveable(
    key = "admin_family_list_state",
    saver = LazyListState.Saver
  ) {
    LazyListState()
  }

  // Reset scroll to top when user searches
  LaunchedEffect(uiState.searchQuery) {
    if (uiState.searchQuery.isNotEmpty()) {
      // Clear saved state when user searches
      AryaPariwarPageState.clear()
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
    if (AryaPariwarPageState.hasData() && AryaPariwarPageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preserveFamilyPagination(AryaPariwarPageState.families, AryaPariwarPageState.paginationState)
      AryaPariwarPageState.hasLoadedOnce = true
    }

    // Load data (will be skipped if preserved)
    viewModel.loadFamiliesPaginated(pageSize = pageSize, resetPagination = true)
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
          viewModel.loadNextFamilyPage()
        }
      }
  }

  LaunchedEffect(uiState) {
    AryaPariwarPageState.saveState(uiState.families, uiState.paginationState, uiState.searchQuery)
    if (uiState.families.isNotEmpty() || uiState.paginationState.error != null) {
      AryaPariwarPageState.hasLoadedOnce = true
    }
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
        onValueChange = viewModel::searchFamiliesWithDebounce,
        modifier = if (isCompact) Modifier.weight(1f) else Modifier.widthIn(max = 600.dp),
        placeholder = { Text("परिवार का नाम") },
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
              Text("नया परिवार जोड़ें")
            }
          },
          state = rememberTooltipState()
        ) {
          IconButton(
            onClick = onNavigateToCreateFamily
          ) {
            Icon(
              modifier = Modifier.size(24.dp),
              imageVector = vectorResource(Res.drawable.family_add),
              contentDescription = "नया परिवार जोड़ें"
            )
          }
        }
      } else {
        Button(
          onClick = onNavigateToCreateFamily
        ) {
          Icon(
            modifier = Modifier.size(24.dp),
            imageVector = vectorResource(Res.drawable.family_add),
            contentDescription = null
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("नया परिवार जोड़ें")
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
              onClick = { viewModel.retryFamilyLoad() }
            ) {
              Text("पुनः प्रयास करें")
            }
          }
        }
      }
      return@Column
    }

    val familiesToShow = uiState.families

    if (familiesToShow.isEmpty() && !uiState.paginationState.isInitialLoading && !uiState.paginationState.isSearching && uiState.paginationState.error == null && AryaPariwarPageState.hasLoadedOnce) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text("कोई परिवार नहीं मिले")
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
            items(familiesToShow) { family ->
              FamilyItem(
                family = family,
                onFamilyClick = { onNavigateToFamilyDetail(family.id) },
                onEditFamily = { onEditFamily(family.id) },
                onDeleteFamily = { showDeleteDialog = family },
                modifier = Modifier.width(490.dp)
              )
            }
          } else {
            items(familiesToShow.chunked(2)) { chunk ->
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                chunk.forEach { family ->
                  FamilyItem(
                    family = family,
                    onFamilyClick = { onNavigateToFamilyDetail(family.id) },
                    onEditFamily = { onEditFamily(family.id) },
                    onDeleteFamily = { showDeleteDialog = family },
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

          if (uiState.paginationState.hasReachedEnd && !uiState.paginationState.hasNextPage && familiesToShow.isNotEmpty()) {
            item {
              val totalItemsDisplayed = "${uiState.families.size}".toDevanagariNumerals()
              Text(
                text = "सभी परिवार दिखाए गए(${totalItemsDisplayed})",
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

  showDeleteDialog?.let { family ->
    AlertDialog(
      onDismissRequest = { showDeleteDialog = null },
      title = { Text("परिवार हटाएँ") },
      text = {
        Text("क्या आप वाकई \"${family.name}\" परिवार को हटाना चाहते हैं? यह कार्रवाई पूर्ववत नहीं की जा सकती।")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteFamily(family.id)
            showDeleteDialog = null
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
        TextButton(onClick = { showDeleteDialog = null }) {
          Text("रद्द करें")
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

@Composable
private fun FamilyItem(
  family: FamilyShort,
  onFamilyClick: () -> Unit,
  onEditFamily: (String) -> Unit,
  onDeleteFamily: (String) -> Unit,
  modifier: Modifier = Modifier
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

  ElevatedCard(
    modifier =
      modifier
        .clickable { onFamilyClick() }
        .width(490.dp),
    shape = RoundedCornerShape(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Family Photo (square shape)
      val familyPhoto = family.photos.firstOrNull()
      if (familyPhoto != null) {
        AsyncImage(
          model = familyPhoto,
          contentDescription = "परिवार फोटो",
          modifier =
            Modifier
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
              onEditFamily(family.id)
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
            onDeleteFamily(family.id)
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
