# PaginatedListScreen - Reusable Pagination Component

## Overview

`PaginatedListScreen` is a generic, reusable Compose component that provides consistent pagination behavior across all
list screens in the application. It includes infinite scroll, search functionality, loading states, error handling, and
visual enhancements.

## Features

- ✅ **Infinite Scroll Pagination** with 90% threshold detection
- ✅ **Debounced Search** (1 second delay) with loading indicators
- ✅ **Adaptive Page Sizes** based on screen width (15/25/35 items)
- ✅ **Visual Scrollbar** matching app design
- ✅ **Error Handling** with retry buttons
- ✅ **Scroll State Persistence** across navigation
- ✅ **End-of-List Indicators** with Devanagari numerals
- ✅ **Responsive Layout** (compact/tablet/desktop)
- ✅ **Cache-First Loading** with proper empty state handling

## Usage

### 1. Repository Implementation

Your repository must implement `PaginatedRepository<T>`:

```kotlin
interface YourRepository : PaginatedRepository<YourDataType> {
  // Required methods will be inherited automatically
}

class YourRepositoryImpl : YourRepository {
  override suspend fun getItemsPaginated(
    pageSize: Int,
    cursor: String?,
    filter: Any?
  ): Flow<PaginationResult<YourDataType>> = flow {
    emit(PaginationResult.Loading())
    
    apolloClient.query(
      YourPaginationQuery(
        first = pageSize,
        after = Optional.presentIfNotNull(cursor),
        filter = Optional.presentIfNotNull(filter as? YourFilterType),
        orderBy = Optional.present(listOf(YourOrderBy(createdAt = Optional.present(OrderByDirection.DescNullsLast))))
      )
    )
    .fetchPolicy(FetchPolicy.CacheAndNetwork)
    .toFlow()
    .collect { response ->
      // IMPORTANT: Include cache handling to prevent empty state flashing
      if (response.isFromCache && response.cacheInfo?.isCacheHit == false && 
          response.data?.yourCollection?.edges.isNullOrEmpty()) {
        // Skip emitting empty cache miss - wait for network
      } else {
        val result = safeCall {
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          val items = response.data?.yourCollection?.edges?.map {
            it.node.yourFragment // Use your fragment here
          } ?: emptyList()

          val pageInfo = response.data?.yourCollection?.pageInfo

          PaginationResult.Success(
            data = items,
            hasNextPage = pageInfo?.hasNextPage ?: false,
            endCursor = pageInfo?.endCursor
          )
        }

        when (result) {
          is Result.Success -> emit(result.data)
          is Result.Error -> emit(PaginationResult.Error(result.exception?.message ?: "Unknown error"))
          is Result.Loading -> {} // Already emitted Loading above
        }
      }
    }
  }

  override suspend fun searchItemsPaginated(
    searchTerm: String,
    pageSize: Int,
    cursor: String?
  ): Flow<PaginationResult<YourDataType>> = flow {
    // Similar implementation for search
    emit(PaginationResult.Loading())
    val result = safeCall {
      val response = apolloClient.query(
        YourSearchQuery(
          searchTerm = "%$searchTerm%",
          first = pageSize,
          after = Optional.presentIfNotNull(cursor)
        )
      ).execute()

      // Process response and return PaginationResult.Success
    }

    when (result) {
      is Result.Success -> emit(result.data)
      is Result.Error -> emit(PaginationResult.Error(result.message))
      is Result.Loading -> {} // Already emitted Loading above
    }
  }
}
```

### 2. GraphQL Queries

Add paginated queries to your `queries.graphql`:

```graphql
query getYourItems($first: Int!, $after: Cursor, $filter: YourFilter, $orderBy: [YourOrderBy!]) {
  yourCollection(
    first: $first
    after: $after
    filter: $filter
    orderBy: $orderBy
  ) {
    edges {
      node {
        ...YourFragment
      }
      cursor
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
  }
}

query searchYourItems($first: Int!, $after: Cursor, $searchTerm: String!) {
  yourCollection(
    first: $first
    after: $after
    filter: {
      name: {ilike: $searchTerm}
    }
    orderBy: {createdAt: DescNullsLast}
  ) {
    edges {
      node {
        ...YourFragment
      }
      cursor
    }
    pageInfo {
      hasNextPage
      hasPreviousPage
      startCursor
      endCursor
    }
  }
}
```

### 3. ViewModel Implementation

Your ViewModel should include pagination methods:

```kotlin
class YourViewModel(private val repository: YourRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(YourUiState())
  val uiState: StateFlow<YourUiState> = _uiState.asStateFlow()
  
  private var searchJob: Job? = null
  private var shouldPreservePagination = false

  // Add pagination state to your UI state
  data class YourUiState(
    val items: List<YourDisplayItem> = emptyList(),
    val paginationState: PaginationState<YourDataType> = PaginationState(),
    val searchQuery: String = ""
  )

  // Required pagination methods
  fun loadItemsPaginated(pageSize: Int = 30, resetPagination: Boolean = false) {
    viewModelScope.launch {
      val currentState = _uiState.value.paginationState
      val shouldReset = resetPagination
      val cursor = if (shouldReset) null else currentState.endCursor

      // Set loading state
      _uiState.value = _uiState.value.copy(
        paginationState = currentState.copy(
          isInitialLoading = shouldReset || currentState.items.isEmpty(),
          isLoadingNextPage = !shouldReset && currentState.items.isNotEmpty(),
          error = null
        )
      )

      repository.getItemsPaginated(pageSize = pageSize, cursor = cursor).collect { result ->
        when (result) {
          is PaginationResult.Loading -> {
            // Loading state already set above
          }
          is PaginationResult.Success -> {
            val newItems = if (shouldReset) {
              result.data
            } else {
              currentState.items + result.data
            }

            // Convert to display items if needed
            val displayItems = newItems.map { /* convert to display format */ }

            _uiState.value = _uiState.value.copy(
              items = displayItems,
              paginationState = currentState.copy(
                items = newItems,
                isInitialLoading = false,
                isLoadingNextPage = false,
                hasNextPage = result.hasNextPage,
                endCursor = result.endCursor,
                hasReachedEnd = !result.hasNextPage,
                error = null
              )
            )
          }
          is PaginationResult.Error -> {
            _uiState.value = _uiState.value.copy(
              paginationState = currentState.copy(
                isInitialLoading = false,
                isLoadingNextPage = false,
                error = result.message,
                showRetryButton = true
              )
            )
          }
        }
      }
    }
  }

  fun searchItemsWithDebounce(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      if (query.isBlank()) {
        loadItemsPaginated(resetPagination = true)
        return@launch
      }

      delay(1000) // 1 second debounce

      // Implement search pagination similar to loadItemsPaginated
    }
  }

  fun loadNextPage() {
    val currentState = _uiState.value.paginationState
    if (currentState.hasNextPage && !currentState.isLoadingNextPage) {
      if (currentState.currentSearchTerm.isNotBlank()) {
        // Load next search page
      } else {
        loadItemsPaginated(resetPagination = false)
      }
    }
  }

  fun retryLoad() {
    val currentState = _uiState.value.paginationState
    _uiState.value = _uiState.value.copy(
      paginationState = currentState.copy(showRetryButton = false)
    )

    if (currentState.currentSearchTerm.isNotBlank()) {
      // Retry search
    } else {
      loadItemsPaginated(resetPagination = currentState.items.isEmpty())
    }
  }

  fun calculatePageSize(screenWidthDp: Float): Int {
    return when {
      screenWidthDp < 600f -> 15      // Mobile portrait
      screenWidthDp < 840f -> 25      // Tablet, mobile landscape
      else -> 35                      // Desktop, large tablets
    }
  }
}
```

### 4. Screen Implementation

Create your screen using `PaginatedListScreen`:

```kotlin
// Global object for state persistence (optional but recommended)
private object YourPageState {
  var items: List<YourDataType> = emptyList()
  var paginationState: PaginationState<YourDataType> = PaginationState()
  var lastSearchQuery: String = ""

  fun clear() {
    items = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
  }

  fun saveState(
    newItems: List<YourDataType>,
    newPaginationState: PaginationState<YourDataType>,
    searchQuery: String
  ) {
    items = newItems
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = items.isNotEmpty()
}

@Composable
fun YourListScreen(
  viewModel: YourViewModel,
  onNavigateToAdd: () -> Unit = {},
  onNavigateToDetail: (String) -> Unit = {},
  onEdit: (String) -> Unit = {},
  onDelete: (String) -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
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

  LaunchedEffect(Unit) {
    // State preservation logic (optional)
    if (YourPageState.hasData() && YourPageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preservePagination(YourPageState.items, YourPageState.paginationState)
    }

    viewModel.loadItemsPaginated(pageSize = pageSize, resetPagination = true)
  }

  LaunchedEffect(uiState) {
    YourPageState.saveState(uiState.paginationState.items, uiState.paginationState, uiState.searchQuery)
  }

  PaginatedListScreen(
    items = uiState.items,
    paginationState = uiState.paginationState,
    searchQuery = uiState.searchQuery,
    onSearchChange = viewModel::searchItemsWithDebounce,
    onLoadMore = viewModel::loadNextPage,
    onRetry = viewModel::retryLoad,
    searchPlaceholder = "आपका प्लेसहोल्डर",
    emptyStateText = "कोई आइटम नहीं मिले",
    endOfListText = { count -> "सभी आइटम दिखाए गए(${count.toString().toDevanagariNumerals()})" },
    addButtonText = "नया आइटम जोड़ें",
    onAddClick = onNavigateToAdd,
    isCompactLayout = isCompact,
    itemsPerRow = if (isCompact) 1 else 2,
    itemContent = { item ->
      YourItemComponent(
        item = item,
        onItemClick = { onNavigateToDetail(item.id) },
        onEditClick = { onEdit(item.id) },
        onDeleteClick = { onDelete(item.id) }
      )
    }
  )
}
```

## Required Dependencies

### Models.kt Additions

Make sure these interfaces and classes are available:

```kotlin
interface PaginatedRepository<T> {
  suspend fun getItemsPaginated(
    pageSize: Int,
    cursor: String? = null,
    filter: Any? = null
  ): Flow<PaginationResult<T>>

  suspend fun searchItemsPaginated(
    searchTerm: String,
    pageSize: Int,
    cursor: String? = null
  ): Flow<PaginationResult<T>>
}

data class PaginationState<T>(
  val items: List<T> = emptyList(),
  val isInitialLoading: Boolean = false,
  val isLoadingNextPage: Boolean = false,
  val isSearching: Boolean = false,
  val hasNextPage: Boolean = false,
  val hasReachedEnd: Boolean = false,
  val endCursor: String? = null,
  val error: String? = null,
  val showRetryButton: Boolean = false,
  val currentSearchTerm: String = ""
)

sealed class PaginationResult<T> {
  data class Success<T>(val data: List<T>, val hasNextPage: Boolean, val endCursor: String?) : PaginationResult<T>()
  data class Error<T>(val message: String) : PaginationResult<T>()
  class Loading<T> : PaginationResult<T>()
}
```

## Configuration Options

### PaginatedListScreen Parameters

| Parameter | Type | Description | Required |
|-----------|------|-------------|----------|
| `items` | `List<T>` | List of items to display | ✅ |
| `paginationState` | `PaginationState<*>` | Current pagination state | ✅ |
| `searchQuery` | `String` | Current search query | ✅ |
| `onSearchChange` | `(String) -> Unit` | Search input handler | ✅ |
| `onLoadMore` | `() -> Unit` | Load next page handler | ✅ |
| `onRetry` | `() -> Unit` | Retry on error handler | ✅ |
| `searchPlaceholder` | `String` | Search field placeholder | ✅ |
| `emptyStateText` | `String` | Empty state message | ✅ |
| `endOfListText` | `(Int) -> String` | End of list message function | ✅ |
| `addButtonText` | `String` | Add button label | ✅ |
| `onAddClick` | `() -> Unit` | Add button handler | ✅ |
| `itemContent` | `@Composable (T) -> Unit` | Item rendering function | ✅ |
| `isCompactLayout` | `Boolean` | Use compact layout | ❌ (default: true) |
| `itemsPerRow` | `Int` | Items per row in non-compact | ❌ (default: 1) |
| `modifier` | `Modifier` | Component modifier | ❌ |

### Adaptive Page Sizes

The component automatically calculates page sizes based on screen width:

- **Mobile (< 600dp)**: 15 items per page
- **Tablet (600dp - 840dp)**: 25 items per page
- **Desktop (> 840dp)**: 35 items per page

You can override this by implementing your own `calculatePageSize()` method.

### Visual Indicators

- **Loading States**: Shows circular progress indicators
- **Search Loading**: Shows progress in search field trailing icon
- **End of List**: Shows total count in Devanagari numerals
- **Scrollbar**: Custom scrollbar matching app design
- **Error States**: Cards with retry buttons

## Best Practices

### 1. Cache Handling

Always implement proper cache handling in your repository to prevent empty state flashing:

```kotlin
if (response.isFromCache && response.cacheInfo?.isCacheHit == false && 
    response.data?.yourCollection?.edges.isNullOrEmpty()) {
  // Skip emitting empty cache miss
} else {
  // Process and emit result
}
```

### 2. State Preservation

Implement global state objects for scroll position persistence across navigation.

### 3. Error Handling

Provide meaningful error messages in Hindi with Devanagari script.

### 4. Search Debouncing

Use 1-second debounce to prevent excessive API calls during search.

### 5. Loading States

Distinguish between initial loading and next page loading for better UX.

## Troubleshooting

### Common Issues

#### 1. Pagination Not Working

- Ensure `LaunchedEffect(listState, paginationState)` includes both dependencies
- Check that `hasNextPage` is properly set in repository
- Verify GraphQL query returns correct `pageInfo`

#### 2. Empty State Flashing

- Implement proper cache handling in repository
- Skip cache misses with empty data

#### 3. State Not Preserved

- Implement global state object for your screen
- Save/restore state in `LaunchedEffect(uiState)`

#### 4. Search Not Working

- Check search query formatting (`%$searchTerm%`)
- Verify search GraphQL query is correct
- Ensure debounce logic is implemented

## Examples

### Complete Implementation

See `AryaSamajListScreen.kt` for a complete working example that demonstrates all features of the pagination system.

### Integration with Existing Screens

The EkalAryaListScreen and AryaPariwarListScreen can be refactored to use this component for consistency.

---

**Version**: 1.0  
**Last Updated**: July 3, 2025  
**Compatible With**: Compose Multiplatform (Android, iOS, Web, Desktop)
