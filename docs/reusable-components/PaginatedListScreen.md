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
- ✅ **Query Watchers** for automatic cache/network deduplication

## Usage

### 1. Repository Implementation

Your repository must implement `PaginatedRepository<T>` using **Query Watchers**:

```kotlin
import com.apollographql.apollo.exception.CacheMissException

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
    .watch() // ✅ PREFERRED: Query Watchers handle cache/network automatically
    .collect { response ->
      // ✅ CRITICAL: Still need to handle empty cache misses
      val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                     response.data?.yourCollection?.edges.isNullOrEmpty()
      
      if (isCacheMissWithEmptyData) {
        return@collect // Skip empty cache miss - prevents empty list flashing
      }

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

  override suspend fun searchItemsPaginated(
    searchTerm: String,
    pageSize: Int,
    cursor: String?
  ): Flow<PaginationResult<YourDataType>> = flow {
    emit(PaginationResult.Loading())
    
    apolloClient.query(
      YourSearchQuery(
        searchTerm = "%$searchTerm%",
        first = pageSize,
        after = Optional.presentIfNotNull(cursor)
      )
    )
    .fetchPolicy(FetchPolicy.CacheAndNetwork)
    .watch() // ✅ PREFERRED: Query Watchers handle cache/network automatically
    .collect { response ->
      // ✅ CRITICAL: Handle empty cache misses in search too
      val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                     response.data?.yourCollection?.edges.isNullOrEmpty()
      
      if (isCacheMissWithEmptyData) {
        return@collect
      }

      val result = safeCall {
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }

        val items = response.data?.yourCollection?.edges?.map {
          it.node.yourFragment
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

  // Method to preserve pagination state when navigating back
  fun preservePagination(savedItems: List<YourDataType>, savedPaginationState: PaginationState<YourDataType>) {
    val displayItems = savedItems.map { /* convert to display format */ }
    _uiState.value = _uiState.value.copy(
      items = displayItems,
      paginationState = savedPaginationState.copy(items = savedItems) // CRITICAL: Ensure consistency
    )
    shouldPreservePagination = true
  }

  // Required pagination methods
  fun loadItemsPaginated(pageSize: Int = 30, resetPagination: Boolean = false) {
    viewModelScope.launch {
      val currentState = _uiState.value.paginationState

      // CRITICAL: Check if we should preserve existing data (e.g., navigating back from detail screen)
      val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExistingData()

      // Reset the preservation flag after checking
      if (shouldPreservePagination) {
        shouldPreservePagination = false

        // CRITICAL: If preserving data, don't make API call
        if (shouldPreserveExistingData) {
          return@launch
        }
      }

      // CRITICAL: For pagination (resetPagination=false), check if we already have the data
      if (!resetPagination && currentState.hasNextPage == false) {
        // No more pages to load
        return@launch
      }

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
            // ✅ Query Watchers prevent duplication automatically - no distinctBy needed
            val existingItems = if (shouldReset) emptyList() else currentState.items
            val newItems = existingItems + result.data

            // Convert to display items if needed
            val displayItems = newItems.map { /* convert to display format */ }

            _uiState.value = _uiState.value.copy(
              items = displayItems,
              paginationState = currentState.copy(
                items = newItems, // ✅ Clean items - Query Watchers handle deduplication
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

      searchItemsPaginated(searchTerm = query.trim(), resetPagination = true)
    }
  }

  fun searchItemsPaginated(searchTerm: String, pageSize: Int = 30, resetPagination: Boolean = true) {
    viewModelScope.launch {
      val currentState = _uiState.value.paginationState
      val cursor = if (resetPagination) null else currentState.endCursor

      // Set loading state
      _uiState.value = _uiState.value.copy(
        searchQuery = searchTerm,
        paginationState = currentState.copy(
          isSearching = resetPagination,
          isLoadingNextPage = !resetPagination,
          error = null,
          currentSearchTerm = searchTerm
        )
      )

      repository.searchItemsPaginated(
        searchTerm = searchTerm,
        pageSize = pageSize,
        cursor = cursor
      ).collect { result ->
        when (result) {
          is PaginationResult.Loading -> {
            // Loading state already set above
          }
          is PaginationResult.Success -> {
            // ✅ Query Watchers prevent duplication in search too
            val existingItems = if (resetPagination) emptyList() else currentState.items
            val newItems = existingItems + result.data

            val displayItems = newItems.map { /* convert to display format */ }

            _uiState.value = _uiState.value.copy(
              items = displayItems,
              paginationState = currentState.copy(
                items = newItems, // ✅ Clean items - Query Watchers handle deduplication
                isSearching = false,
                isLoadingNextPage = false,
                hasNextPage = result.hasNextPage,
                endCursor = result.endCursor,
                hasReachedEnd = !result.hasNextPage,
                error = null,
                currentSearchTerm = searchTerm
              )
            )
          }
          is PaginationResult.Error -> {
            _uiState.value = _uiState.value.copy(
              paginationState = currentState.copy(
                isSearching = false,
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

  // ... rest of methods remain the same
}

### 4. Screen Implementation

Create your screen using `PaginatedListScreen`:

```kotlin
// Global object for state persistence (optional but recommended)
private object YourPageState {
  var items: List<YourDataType> = emptyList()
  var paginationState: PaginationState<YourDataType> = PaginationState()
  var lastSearchQuery: String = ""
  var needsRefresh: Boolean = false // Add refresh tracking

  fun clear() {
    items = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    needsRefresh = false
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
  
  fun markForRefresh() {
    needsRefresh = true
  }
}

@Composable
fun YourListScreen(
  viewModel: YourViewModel,
  onNavigateToAdd: () -> Unit = {},
  onNavigateToDetail: (String) -> Unit = {},
  onEdit: (String) -> Unit = {},
  onDelete: (String) -> Unit = {},
  onDataChanged: () -> Unit = {} // Callback for when data changes (for count updates)
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

  // Generate unique key when refresh is needed
  val refreshKey = remember(YourPageState.needsRefresh) {
    if (YourPageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
  }

  LaunchedEffect(refreshKey) {
    // Clear preserved state if refresh is requested
    if (YourPageState.needsRefresh) {
      YourPageState.clear()
    }

    // Preserve pagination only if not refreshing
    if (!YourPageState.needsRefresh && YourPageState.hasData() && 
        YourPageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preservePagination(YourPageState.items, YourPageState.paginationState)
    }

    // Load data: Reset pagination if refresh needed OR no existing data (initial load)
    val shouldReset = YourPageState.needsRefresh || !YourPageState.hasData()
    viewModel.loadItemsPaginated(pageSize = pageSize, resetPagination = shouldReset)
    YourPageState.needsRefresh = false
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
        onDeleteClick = {
          // Mark for refresh and delete
          YourPageState.markForRefresh()
          viewModel.deleteItem(item.id) {
            onDataChanged() // Trigger parent screen updates (like count refresh)
          }
        }
      )
    }
  )
}

## Auto-Refresh for Create/Edit/Delete Operations

To ensure the paginated list automatically refreshes after create, edit, or delete operations, follow this pattern:

### 1. Add Refresh Flag to Global State

```kotlin
private object YourPageState {
  // ... existing properties
  var needsRefresh: Boolean = false

  fun markForRefresh() {
    needsRefresh = true
  }

  fun clear() {
    // ... existing clear logic
    needsRefresh = false
  }
}
```

### 2. Modify LaunchedEffect to Handle Refresh

```kotlin
// Generate unique key when refresh is needed
val refreshKey = remember(YourPageState.needsRefresh) {
  if (YourPageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
}

LaunchedEffect(refreshKey) {
  // Clear preserved state if refresh is requested
  if (YourPageState.needsRefresh) {
    YourPageState.clear()
  }

  // Preserve pagination only if not refreshing
  if (!YourPageState.needsRefresh && YourPageState.hasData() && 
      YourPageState.lastSearchQuery == uiState.searchQuery) {
    viewModel.preservePagination(YourPageState.items, YourPageState.paginationState)
  }

  // Load data: Reset pagination if refresh needed OR no existing data (initial load)
  val shouldReset = YourPageState.needsRefresh || !YourPageState.hasData()
  viewModel.loadItemsPaginated(pageSize = pageSize, resetPagination = shouldReset)
  YourPageState.needsRefresh = false
}
```

### 3. Mark for Refresh After Operations

#### After Create/Edit (in navigation callbacks):

```kotlin
// In form screens after successful create/edit
onNavigateToItemDetails = { itemId ->
  YourPageState.markForRefresh() // Mark list for refresh
  navController.navigate(Screen.ItemDetail(itemId)) {
    popUpTo(Screen.ItemForm) { inclusive = true }
  }
}
```

#### After Delete (in item component):

```kotlin
onDeleteClick = {
  YourPageState.markForRefresh() // Mark list for refresh
  viewModel.deleteItem(item.id) {
    onDataChanged() // Callback to parent for additional updates (e.g., count refresh)
  }
}
```

### 4. Update ViewModel Delete Method

```kotlin
fun deleteItem(id: String, onSuccess: (() -> Unit)? = null) {
  viewModelScope.launch {
    repository.deleteItem(id).collect { result ->
      result.handleResult(
        onSuccess = { success ->
          // Refresh the list
          loadItemsPaginated(resetPagination = true)
          // Call success callback
          onSuccess?.invoke()
        },
        onError = { appError ->
          // Handle error
        }
      )
    }
  }
}
```

### 5. Clear Cache After Mutations (Repository Level)

```kotlin
// In repository delete method
override suspend fun deleteItem(id: String): Flow<Result<Boolean>> = flow {
  emit(Result.Loading)
  val result = safeCall {
    val response = apolloClient.mutation(DeleteItemMutation(id)).execute()
    if (response.hasErrors()) {
      throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
    }
    val success = response.data?.deleteFromItemCollection?.affectedCount?.let { it > 0 } ?: false
    if (success) {
      // Clear Apollo cache after successful deletion
      apolloClient.apolloStore.clearAll()
    }
    success
  }
  emit(result)
}

// Similar for create/update operations
override suspend fun createItem(formData: ItemFormData): Flow<Result<String>> = flow {
  // ... creation logic
  
  // Clear cache after successful creation
  apolloClient.apolloStore.clearAll()
  
  emit(result)
}
```

### 6. Add Data Change Callback to Parent Screen

If your list is embedded in a parent screen (like tabs), add a callback to refresh parent data:

```kotlin
// In parent screen (e.g., TabScreen with counts)
YourListScreen(
  viewModel = yourViewModel,
  onDataChanged = {
    // Refresh counts or other parent data
    parentViewModel.loadCounts()
  }
)
```

### Benefits of This Pattern

1. **Scroll Preservation**: Normal navigation preserves scroll position
2. **Auto-Refresh**: Create/edit/delete operations automatically refresh the list
3. **Cache Consistency**: Apollo cache is cleared to ensure fresh data
4. **Parent Updates**: Callbacks allow parent screens to update counts/stats
5. **Minimal Changes**: Only 4-5 lines of code per screen to implement
6. **No Duplication**: Query Watchers prevent cache/network response duplication

### Key Points

- ✅ Use `markForRefresh()` after create/edit/delete operations
- ✅ Clear Apollo cache in repository after successful mutations
- ✅ Use unique `refreshKey` to trigger LaunchedEffect when needed
- ✅ Preserve scroll position for view-only navigation
- ✅ Add `onDataChanged` callback for parent screen updates
- ✅ Reset `needsRefresh` flag after processing
- ✅ **Query Watchers eliminate need for manual deduplication**

This pattern ensures consistent behavior across all paginated screens while maintaining optimal performance and user
experience.

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

### 1. Use Query Watchers

**PREFERRED**: Use Apollo Query Watchers for automatic cache/network response handling:

```kotlin
apolloClient.query(YourQuery(...))
  .fetchPolicy(FetchPolicy.CacheAndNetwork)
  .watch() // ✅ Handles cache vs network automatically
  .collect { response ->
    // Handle only empty cache misses
    val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                   response.data?.collection?.edges.isNullOrEmpty()
    if (isCacheMissWithEmptyData) return@collect
    
    // Process normally - no deduplication needed
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

- Use Query Watchers: `.watch()` instead of `.toFlow()`
- Handle empty cache misses: `response.exception is CacheMissException`

#### 3. State Not Preserved

- Implement global state object for your screen
- Save/restore state in `LaunchedEffect(uiState)`

#### 4. Search Not Working

- Check search query formatting (`%$searchTerm%`)
- Verify search GraphQL query is correct
- Ensure debounce logic is implemented

#### 5. **Empty List on Initial Load**

**Symptoms:**

- Screen shows empty list on first load
- Loading indicators don't appear
- Works fine after manual refresh

**Root Cause:**
Wrong pagination reset logic - trying to load "next page" when no first page exists.

**Fix:**

```kotlin
// WRONG - causes empty list on initial load
val shouldReset = YourPageState.needsRefresh  // false on initial load

// CORRECT - handles initial load properly  
val shouldReset = YourPageState.needsRefresh || !YourPageState.hasData()
```

**Prevention:**

- ALWAYS check `!hasData()` for initial load reset logic
- Never rely solely on `needsRefresh` flag for determining reset behavior

#### 6. **State Inconsistency Between UI and Pagination**

**Symptoms:**

- Display shows wrong count vs actual data
- Navigation back doesn't preserve scroll correctly
- State gets out of sync after operations

**Root Cause:**
UI state items and pagination state items are not kept in sync.

**Fix:**

```kotlin
// WRONG - can get out of sync
_uiState.value = _uiState.value.copy(
  items = displayItems,
  paginationState = savedPaginationState  // Original state
)

// CORRECT - ensures consistency
_uiState.value = _uiState.value.copy(
  items = displayItems,  
  paginationState = savedPaginationState.copy(items = sourceItems) // Synced state
)
```

#### 7. **Platform-Specific Issues (WasmJS/Web)**

**Symptoms:**

- Issues appear only on web target (WasmJS)
- Works fine on Android/iOS but fails on web
- Cache behaves differently

**Root Cause:**
Different platforms have different caching behaviors. WasmJS has more persistent/aggressive caching.

**Fix:**

- Always clear Apollo cache after mutations: `apolloClient.apolloStore.clearAll()`
- Use Query Watchers for consistent cross-platform behavior
- Test extensively on all target platforms

### Pagination Implementation Checklist

Use this checklist when implementing any paginated list to prevent common issues:

#### Repository Requirements

- ✅ **Query Watchers**: Use `.watch()` instead of `.toFlow()`
- ✅ **Cache Miss Handling**: Check `response.exception is CacheMissException`
- ✅ **Cache clearing**: Clear Apollo cache after successful mutations
- ✅ **Error handling**: Proper error states in pagination results
- ✅ **GraphQL structure**: Correct pageInfo with `hasNextPage`, `endCursor`

#### ViewModel Requirements

- ✅ **State preservation**: Proper early returns for existing data
- ✅ **State consistency**: `paginationState.copy(items = items)`
- ✅ **Bounds checking**: Check for existing data before API calls
- ✅ **Simple concatenation**: No manual deduplication needed with Query Watchers

#### Screen Requirements

- ✅ **Correct reset logic**: `shouldReset = needsRefresh || !hasData()`
- ✅ **Global state object**: With `markForRefresh()` method
- ✅ **Unique refresh key**: `Clock.System.now().toEpochMilliseconds()`
- ✅ **State saving**: Save pagination state in LaunchedEffect

#### Testing Requirements

- ✅ **Initial load**: Verify list loads correctly (not empty)
- ✅ **Configuration changes**: Test device rotation
- ✅ **Navigation flows**: Back navigation preserves scroll/data correctly
- ✅ **Search functionality**: Search + pagination works without issues
- ✅ **CRUD operations**: Lists refresh after create/edit/delete
- ✅ **Platform testing**: Test especially on WasmJS/Web target

### Prevention Rules

To prevent issues in future implementations:

1. **MANDATORY QUERY WATCHERS**: Use `.watch()` for CacheAndNetwork policy
2. **MANDATORY CACHE MISS HANDLING**: Check `CacheMissException` for empty responses
3. **MANDATORY RESET LOGIC**: Always check `!hasData()` for initial load handling
4. **MANDATORY STATE SYNC**: Keep UI state and pagination state in sync
5. **MANDATORY CACHE CLEARING**: Clear Apollo cache after mutations
6. **MANDATORY TESTING**: Test on all platforms, especially WasmJS/Web

## Examples

### Complete Implementation

See `FamilyRepository.kt` for a complete working example that demonstrates Query Watchers implementation.

### Integration with Existing Screens

The EkalAryaListScreen and AryaPariwarListScreen can be refactored to use this component for consistency.

---

**Version**: 2.0  
**Last Updated**: July 7, 2025  
**Compatible With**: Compose Multiplatform (Android, iOS, Web, Desktop)  
**Uses**: Apollo Query Watchers for optimal cache handling
