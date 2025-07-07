package com.aryamahasangh.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.aryamahasangh.features.activities.toDevanagariNumerals
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> PaginatedListScreen(
    // Data & State
    items: List<T>,
    paginationState: PaginationState<*>,
    searchQuery: String,
    
    // Actions
    onSearchChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    
    // UI Customization
    searchPlaceholder: String,
    emptyStateText: String,
    endOfListText: (Int) -> String,
    addButtonText: String,
    onAddClick: () -> Unit,
    
    // Item Rendering
    itemContent: @Composable (T) -> Unit,
    
    // Layout Configuration
    isCompactLayout: Boolean = true,
    itemsPerRow: Int = 1,
    modifier: Modifier = Modifier
) {
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT
    
    val listState = rememberSaveable(
        key = "paginated_list_state",
        saver = LazyListState.Saver
    ) {
        LazyListState()
    }

    // Reset scroll to top when user searches
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
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

  // Debug: Track pagination state changes
  // Removed debug logging

  // Scroll detection for pagination
  LaunchedEffect(listState, paginationState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            val threshold = (totalItems * 0.9).toInt()
            lastVisibleItem >= threshold && totalItems > 0
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
              if (shouldLoadMore && paginationState.hasNextPage && !paginationState.isLoadingNextPage) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        // Search bar and add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCompact) Arrangement.SpaceBetween else Arrangement.Start
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = if (isCompact) Modifier.weight(1f) else Modifier.width(490.dp),
                placeholder = { Text(searchPlaceholder) },
                leadingIcon = if (searchQuery.isEmpty()) {
                  { Icon(Icons.Default.Search, contentDescription = "Search") }
                } else null,
                trailingIcon = {
                    if (paginationState.isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }else if(searchQuery.isNotEmpty()){
                      // close icon that when clicked, clears the input field
                      IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                          Icons.Default.Close,
                          contentDescription = "हटाएँ",
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }else{
                      null
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
                            Text(addButtonText)
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Default.Add,
                            contentDescription = addButtonText
                        )
                    }
                }
            } else {
                Button(onClick = onAddClick) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(addButtonText)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading state
        if (paginationState.isInitialLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // Error state
        paginationState.error?.let { error ->
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
                    if (paginationState.showRetryButton) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text("पुनः प्रयास करें")
                        }
                    }
                }
            }
            return@Column
        }

        // Empty state
        if (items.isEmpty() && !paginationState.isInitialLoading && !paginationState.isSearching && paginationState.error == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(emptyStateText)
            }
        } else {
            // List content with scrollbar
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isCompactLayout || itemsPerRow == 1) {
                        items(items) { item ->
                            itemContent(item)
                        }
                    } else {
                        items(items.chunked(itemsPerRow)) { chunk ->
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { item ->
                                    itemContent(item)
                                }
                            }
                        }
                    }

                    // Loading next page indicator
                    if (paginationState.isLoadingNextPage) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // End of list indicator
                    if (paginationState.hasReachedEnd && !paginationState.hasNextPage && items.isNotEmpty()) {
                        item {
                            val totalItemsDisplayed = "${items.size}".toDevanagariNumerals()
                            Text(
                                text = endOfListText(items.size),
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Custom scrollbar
                CustomScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                    scrollState = listState
                )
            }
        }
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
