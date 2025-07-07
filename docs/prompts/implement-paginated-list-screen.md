# Prompt: Implement Paginated List Screen Using Reusable Components

## Context

This project has a comprehensive pagination system implemented using reusable components. The system includes:

- **PaginatedListScreen<T>**: Generic UI component for infinite scroll pagination
- **PaginatedRepository<T>**: Repository interface for paginated data access
- **PaginationState<T>** and **PaginationResult<T>**: State management classes
- **Query Watchers**: Apollo cache/network handling to prevent duplication
- **Scroll state persistence** across navigation
- **Visual scrollbar** matching app design
- **Adaptive page sizes** based on screen width
- **Debounced search** functionality
- **Error handling** with retry buttons

## Reference Implementation

**FamilyRepository** serves as the complete reference implementation demonstrating Query Watchers:

- Repository: `composeApp/src/commonMain/kotlin/com/aryamahasangh/features/admin/FamilyRepository.kt`
- ViewModel: `composeApp/src/commonMain/kotlin/com/aryamahasangh/features/admin/FamilyViewModel.kt`
- Screen: `composeApp/src/commonMain/kotlin/com/aryamahasangh/features/admin/AryaPariwarListScreen.kt`
- GraphQL Queries: `composeApp/src/commonMain/graphql/queries.graphql` (search for `getFamilies` and
  `searchFamilies`)

## Task

Please implement a paginated list screen for [ENTITY_NAME] following the exact same pattern as the Family
implementation. This
should include:

### 1. GraphQL Queries Implementation

Add these queries to `composeApp/src/commonMain/graphql/queries.graphql`:

```graphql
query get[EntityName]s($first: Int!, $after: Cursor, $filter: [EntityName]Filter, $orderBy: [[EntityName]OrderBy!]) {
  [entityName]Collection(
    first: $first
    after: $after
    filter: $filter
    orderBy: $orderBy
  ) {
    edges {
      node {
        ...[EntityName]WithAddress  # or appropriate fragment
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

query search[EntityName]s($first: Int!, $after: Cursor, $searchTerm: String!) {
  [entityName]Collection(
    first: $first
    after: $after
    filter: {
      name: {ilike: $searchTerm}
    }
    orderBy: {createdAt: DescNullsLast}
  ) {
    edges {
      node {
        ...[EntityName]WithAddress
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

### 2. Repository Enhancement

Update the existing repository to implement `PaginatedRepository<[EntityType]>`:

**Required imports to add:**

```kotlin
import com.aryamahasangh.features.admin.PaginatedRepository
import com.aryamahasangh.features.admin.PaginationResult
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo.exception.CacheMissException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
```

**Interface update:**

```kotlin
interface [EntityName]Repository : PaginatedRepository<[EntityType]> {
  // existing methods...
}
```

**Implementation methods to add:**

```kotlin
override suspend fun getItemsPaginated(
  pageSize: Int,
  cursor: String?,
  filter: Any?
): Flow<PaginationResult<[EntityType]>> = flow {
  emit(PaginationResult.Loading())

  apolloClient.query(
    Get[EntityName]sQuery(
      first = pageSize,
      after = Optional.presentIfNotNull(cursor),
      filter = Optional.presentIfNotNull(filter as? [EntityName]Filter),
      orderBy = Optional.present(listOf([EntityName]OrderBy(createdAt = Optional.present(OrderByDirection.DescNullsLast))))
    )
  )
  .fetchPolicy(FetchPolicy.CacheAndNetwork)
  .watch() // ✅ PREFERRED: Query Watchers handle cache/network automatically
  .collect { response ->
    // ✅ CRITICAL: Handle empty cache misses to prevent empty list flashing
    val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                   response.data?.[entityName]Collection?.edges.isNullOrEmpty()
    
    if (isCacheMissWithEmptyData) {
      return@collect // Skip empty cache miss - wait for network
    }

    val result = safeCall {
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }

      val items = response.data?.[entityName]Collection?.edges?.map {
        it.node.[entityFragment] // Use appropriate fragment access
      } ?: emptyList()

      val pageInfo = response.data?.[entityName]Collection?.pageInfo

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
): Flow<PaginationResult<[EntityType]>> = flow {
  emit(PaginationResult.Loading())
  
  apolloClient.query(
    Search[EntityName]sQuery(
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
                                   response.data?.[entityName]Collection?.edges.isNullOrEmpty()
    
    if (isCacheMissWithEmptyData) {
      return@collect
    }

    val result = safeCall {
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }

      val items = response.data?.[entityName]Collection?.edges?.map {
        it.node.[entityFragment]
      } ?: emptyList()

      val pageInfo = response.data?.[entityName]Collection?.pageInfo

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

override suspend fun create[EntityName](formData: [EntityName]FormData): Flow<Result<String>> = flow {
  emit(Result.Loading)
  val result = safeCall {
    val response = apolloClient.mutation(Create[EntityName]Mutation(formData)).execute()
    if (response.hasErrors()) {
      throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
    }
    val createdId = response.data?.insertInto[EntityName]Collection?.records?.firstOrNull()?.id
      ?: throw Exception("Created record ID not found")
    
    // CRITICAL: Clear Apollo cache after successful creation
    apolloClient.apolloStore.clearAll()
    createdId
  }
  emit(result)
}

override suspend fun update[EntityName](id: String, formData: [EntityName]FormData): Flow<Result<Boolean>> = flow {
  emit(Result.Loading)
  val result = safeCall {
    val response = apolloClient.mutation(Update[EntityName]Mutation(id, formData)).execute()
    if (response.hasErrors()) {
      throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
    }
    val success = response.data?.update[EntityName]Collection?.affectedCount?.let { it > 0 } ?: false
    if (success) {
      // CRITICAL: Clear Apollo cache after successful update
      apolloClient.apolloStore.clearAll()
    }
    success
  }
  emit(result)
}

override suspend fun delete[EntityName](id: String): Flow<Result<Boolean>> = flow {
  emit(Result.Loading)
  val result = safeCall {
    val response = apolloClient.mutation(Delete[EntityName]Mutation(id)).execute()
    if (response.hasErrors()) {
      throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
    }
    val success = response.data?.deleteFrom[EntityName]Collection?.affectedCount?.let { it > 0 } ?: false
    if (success) {
      // CRITICAL: Clear Apollo cache after successful deletion
      apolloClient.apolloStore.clearAll()
    }
    success
  }
  emit(result)
}

### 3. ViewModel Enhancement

Update the ViewModel UI state and add pagination methods:

**UI State update:**

```kotlin
data class [EntityName]ListUiState(
  val [entityPlural]: List<[EntityListItem]> = emptyList(),
  // ... existing fields ...
  val paginationState: PaginationState<[EntityType]> = PaginationState()
)
```

**Required imports to add:**

```kotlin
import com.aryamahasangh.features.admin.PaginationState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
```

**Methods to add:**

```kotlin
private var searchJob: Job? = null
private var shouldPreservePagination = false

fun hasExisting[EntityName]Data(): Boolean {
  return _uiState.value.[entityPlural].isNotEmpty()
}

fun preserve[EntityName]Pagination(
  saved[EntityPlural]: List<[EntityType]>,
  savedPaginationState: PaginationState<[EntityType]>
) {
  // Convert EntityType to EntityListItem for display if needed
  val listItems = saved[EntityPlural].map { entity ->
    // Convert to display format
  }

  _uiState.value = _uiState.value.copy(
    [entityPlural] = listItems,
    paginationState = savedPaginationState.copy(items = saved[EntityPlural]) // Ensure consistency
  )
  shouldPreservePagination = true
}

fun load[EntityPlural]Paginated(pageSize: Int = 30, resetPagination: Boolean = false) {
  viewModelScope.launch {
    val currentState = _uiState.value.paginationState

    // Check if we should preserve existing data (e.g., navigating back from detail screen)
    val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExisting[EntityName]Data()

    // Reset the preservation flag after checking
    if (shouldPreservePagination) {
      shouldPreservePagination = false

      // If preserving data, don't make API call
      if (shouldPreserveExistingData) {
        return@launch
      }
    }

    // For pagination (resetPagination=false), check if we already have the data
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

          // Convert to display items
          val displayItems = newItems.map { /* convert to display format */ }

          _uiState.value = _uiState.value.copy(
            [entityPlural] = displayItems,
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

fun search[EntityPlural]Paginated(searchTerm: String, pageSize: Int = 30, resetPagination: Boolean = true) {
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
            [entityPlural] = displayItems,
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

fun loadNext[EntityName]Page() {
  val currentState = _uiState.value.paginationState
  if (currentState.hasNextPage && !currentState.isLoadingNextPage) {
    if (currentState.currentSearchTerm.isNotBlank()) {
      search[EntityPlural]Paginated(
        searchTerm = currentState.currentSearchTerm,
        resetPagination = false
      )
    } else {
      load[EntityPlural]Paginated(resetPagination = false)
    }
  }
}

fun retry[EntityName]Load() {
  val currentState = _uiState.value.paginationState
  _uiState.value = _uiState.value.copy(
    paginationState = currentState.copy(showRetryButton = false)
  )

  if (currentState.currentSearchTerm.isNotBlank()) {
    search[EntityPlural]Paginated(
      searchTerm = currentState.currentSearchTerm,
      resetPagination = currentState.items.isEmpty()
    )
  } else {
    load[EntityPlural]Paginated(resetPagination = currentState.items.isEmpty())
  }
}

fun search[EntityPlural]WithDebounce(query: String) {
  _uiState.value = _uiState.value.copy(searchQuery = query)

  searchJob?.cancel()
  searchJob = viewModelScope.launch {
    if (query.isBlank()) {
      load[EntityPlural]Paginated(resetPagination = true)
      return@launch
    }

    delay(1000) // 1 second debounce

    search[EntityPlural]Paginated(searchTerm = query.trim(), resetPagination = true)
  }
}

fun calculatePageSize(screenWidthDp: Float): Int {
  return when {
    screenWidthDp < 600f -> 15      // Mobile portrait
    screenWidthDp < 840f -> 25      // Tablet, mobile landscape
    else -> 35                      // Desktop, large tablets
  }
}

### 4. Screen Implementation

**Create global state object:**

```kotlin
private object [EntityName]PageState {
  var [entityPlural]: List<[EntityType]> = emptyList()
  var paginationState: PaginationState<[EntityType]> = PaginationState()
  var lastSearchQuery: String = ""
  var needsRefresh: Boolean = false // Add refresh tracking

  fun clear() {
    [entityPlural] = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    needsRefresh = false
  }

  fun saveState(
    new[EntityPlural]: List<[EntityType]>,
    newPaginationState: PaginationState<[EntityType]>,
    searchQuery: String
  ) {
    [entityPlural] = new[EntityPlural]
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = [entityPlural].isNotEmpty()
  
  fun markForRefresh() {
    needsRefresh = true
  }
}
```

**Replace screen content with PaginatedListScreen:**

```kotlin
@Composable
fun [EntityName]ListScreen(
  viewModel: [EntityName]ViewModel,
  onNavigateToAdd[EntityName]: () -> Unit = {},
  onNavigateToDetail: (String) -> Unit = {},
  onEdit: (String) -> Unit = {},
  onDelete: (String) -> Unit = {},
  onDataChanged: () -> Unit = {} // Add callback for count updates
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
  val refreshKey = remember([EntityName]PageState.needsRefresh) {
    if ([EntityName]PageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
  }

  LaunchedEffect(refreshKey) {
    // Clear preserved state if refresh is requested
    if ([EntityName]PageState.needsRefresh) {
      [EntityName]PageState.clear()
    }

    // Preserve pagination only if not refreshing
    if (![EntityName]PageState.needsRefresh && [EntityName]PageState.hasData() && 
        [EntityName]PageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preserve[EntityName]Pagination([EntityName]PageState.[entityPlural], [EntityName]PageState.paginationState)
    }

    // Load data (resetPagination = true when refreshing)
    val shouldReset = [EntityName]PageState.needsRefresh || ![EntityName]PageState.hasData() 
    viewModel.load[EntityPlural]Paginated(pageSize = pageSize, resetPagination = shouldReset)
    [EntityName]PageState.needsRefresh = false
  }

  LaunchedEffect(uiState) {
    [EntityName]PageState.saveState(uiState.paginationState.items, uiState.paginationState, uiState.searchQuery)
  }

  PaginatedListScreen(
    items = uiState.[entityPlural],
    paginationState = uiState.paginationState,
    searchQuery = uiState.searchQuery,
    onSearchChange = viewModel::search[EntityPlural]WithDebounce,
    onLoadMore = viewModel::loadNext[EntityName]Page,
    onRetry = viewModel::retry[EntityName]Load,
    searchPlaceholder = "[EntityName] का नाम",
    emptyStateText = "कोई [EntityName] नहीं मिले",
    endOfListText = { count -> "सभी [EntityName] दिखाए गए(${count.toString().toDevanagariNumerals()})" },
    addButtonText = "नया [EntityName] जोड़ें",
    onAddClick = onNavigateToAdd[EntityName],
    isCompactLayout = isCompact,
    itemsPerRow = if (isCompact) 1 else 2,
    itemContent = { [entitySingular] ->
      [EntityName]Item(
        [entitySingular] = [entitySingular],
        onItemClick = { onNavigateToDetail([entitySingular].id) },
        onEditClick = { onEdit([entitySingular].id) },
        onDeleteClick = {
          // Mark for refresh and delete
          [EntityName]PageState.markForRefresh()
          viewModel.delete[EntityName]([entitySingular].id) {
            onDataChanged() // Trigger parent screen updates (like count refresh)
          }
        }
      )
    }
  )
}

### 5. Required Dependencies

Ensure these are available in `Models.kt`:

- `PaginatedRepository<T>` interface
- `PaginationState<T>` data class
- `PaginationResult<T>` sealed class

### 6. Extension Functions (if needed)

Add these to the Models file:

```kotlin
fun [EntityType].getFormattedAddress(): String {
  return address?.addressFields?.let { addr ->
    val parts = listOfNotNull(
      addr.basicAddress?.takeIf { it.isNotBlank() },
      addr.district?.takeIf { it.isNotBlank() },
      addr.state?.takeIf { it.isNotBlank() }
    )
    parts.joinToString(", ")
  } ?: ""
}

## Critical Implementation Notes

### 1. Use Query Watchers (MANDATORY)

**MUST USE** Query Watchers in repository to handle cache/network responses automatically and eliminate duplication:

```kotlin
apolloClient.query(YourQuery(...))
  .fetchPolicy(FetchPolicy.CacheAndNetwork)
  .watch() // ✅ PREFERRED: Handles cache vs network automatically
  .collect { response ->
    // ✅ CRITICAL: Handle only empty cache misses to prevent empty list flashing
    val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                   response.data?.collection?.edges.isNullOrEmpty()
    if (isCacheMissWithEmptyData) return@collect
    
    // Process normally - NO manual deduplication needed
    // Query Watchers eliminate duplicates automatically
  }
```

**Key Benefits:**

- ✅ Eliminates need for manual `distinctBy { it.id }` calls
- ✅ Prevents 6→12→18 item duplication on config changes
- ✅ Works consistently across Android, iOS, Web, Desktop
- ✅ Uses Apollo's intended patterns for optimal performance

### 2. Prevent Empty List on Initial Load (MANDATORY)

**CRITICAL**: Wrong reset logic causes empty lists on first screen load:

```kotlin
// ❌ WRONG - causes empty list on initial load
val shouldReset = PageState.needsRefresh  // false on initial load

// ✅ CORRECT - handles initial load properly  
val shouldReset = PageState.needsRefresh || !PageState.hasData()
```

### 3. Clear Apollo Cache After Mutations (MANDATORY)

**CRITICAL**: Clear cache after successful create/update/delete operations:

```kotlin
// In all mutation methods
if (success) {
  apolloClient.apolloStore.clearAll() // Essential for WasmJS/Web
}
```

### 4. State Consistency (MANDATORY)

Keep UI state and pagination state synchronized:

```kotlin
// ✅ CORRECT - ensures consistency
_uiState.value = _uiState.value.copy(
  items = displayItems,  
  paginationState = savedPaginationState.copy(items = sourceItems) // Synced
)
```

## Key Architecture Principles

### Query Watchers: The Root Solution

❌ **Old Pattern (Causes Issues):**

```kotlin
.toFlow().collect { response ->
  val items = existingItems + newItems.distinctBy { it.id } // Manual deduplication
}
```

✅ **New Pattern (Prevents Issues):**

```kotlin
.watch().collect { response ->
  val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                 response.data?.collection?.edges.isNullOrEmpty()
  if (isCacheMissWithEmptyData) return@collect
  
  val items = existingItems + newItems // No deduplication needed
}
```

### The "If You Need distinctBy, You're Doing It Wrong" Rule

- **Query Watchers** eliminate duplication at the source
- **Manual deduplication** is a band-aid that indicates incorrect Apollo usage
- **Proper cache handling** makes the code simpler and more reliable

### Prevention Rules (MANDATORY)

1. **MANDATORY QUERY WATCHERS**: Use `.watch()` for CacheAndNetwork policy
2. **MANDATORY CACHE MISS HANDLING**: Check `CacheMissException` for empty responses
3. **MANDATORY RESET LOGIC**: Always check `!hasData()` for initial load handling
4. **MANDATORY STATE SYNC**: Keep UI state and pagination state in sync
5. **MANDATORY CACHE CLEARING**: Clear Apollo cache after mutations
6. **MANDATORY TESTING**: Test on all platforms, especially WasmJS/Web

## Implementation Checklist (MANDATORY)

**Use this checklist for EVERY paginated list implementation to prevent common issues:**

### ✅ **Repository Requirements (MANDATORY)**

- [ ] **Query Watchers**: Use `.watch()` instead of `.toFlow()` for CacheAndNetwork
- [ ] **Cache Miss Handling**: Check `response.exception is CacheMissException`
- [ ] **Cache clearing**: `apolloClient.apolloStore.clearAll()` after mutations
- [ ] **Error handling**: Proper error states in `PaginationResult.Error`
- [ ] **GraphQL structure**: Correct pageInfo with `hasNextPage`, `endCursor`
- [ ] **NO Manual Deduplication**: Remove any `distinctBy { it.id }` calls

### ✅ **ViewModel Requirements (MANDATORY)**

- [ ] **State preservation**: Proper early returns for existing data
- [ ] **State consistency**: `paginationState.copy(items = sourceItems)` in preserve method
- [ ] **Bounds checking**: `if (!resetPagination && currentState.hasNextPage == false) return`
- [ ] **Simple concatenation**: `existingItems + newItems` (no deduplication needed)
- [ ] **Delete callback**: Accept `onSuccess: (() -> Unit)?` parameter for parent updates

### ✅ **Screen Requirements (MANDATORY)**

- [ ] **Correct reset logic**: `shouldReset = needsRefresh || !hasData()` (NOT just `needsRefresh`)
- [ ] **Global state object**: With `markForRefresh()`, `hasData()`, `clear()` methods
- [ ] **Unique refresh key**: `Clock.System.now().toEpochMilliseconds()` when refresh needed
- [ ] **State saving**: Save pagination state in `LaunchedEffect(uiState)`
- [ ] **Parent callback**: `onDataChanged: () -> Unit` for count updates

### ✅ **Auto-Refresh Integration (MANDATORY)**

- [ ] **Mark for refresh**: Call `PageState.markForRefresh()` after create/edit operations
- [ ] **Delete integration**: Call `markForRefresh()` and `onDataChanged()` in delete handler
- [ ] **Navigation updates**: Mark for refresh in navigation callbacks
- [ ] **Cache consistency**: Ensure mutations clear Apollo cache

### ✅ **Testing Requirements (MANDATORY)**

**Test all these scenarios to prevent production issues:**

- [ ] **Initial load**: Verify list shows items on first load (not empty)
- [ ] **Configuration changes**: Rotate device - NO 6→12→18 duplication should occur
- [ ] **Navigation back**: Scroll position preserved from detail screens
- [ ] **Search pagination**: Search + infinite scroll works without duplication
- [ ] **CRUD refresh**: Create/edit/delete automatically refreshes list
- [ ] **Parent updates**: Count updates work in parent screens (if applicable)
- [ ] **Platform testing**: Test especially on WasmJS/Web target (most sensitive to caching issues)

## Common Issues Prevention

**If you encounter these symptoms, check the corresponding fixes:**

| **Issue**           | **Symptom**                    | **Root Cause**                          | **Fix**                                   |
|---------------------|--------------------------------|-----------------------------------------|-------------------------------------------|
| **Duplication**     | 6→12→18 items on rotation      | Using `.toFlow()` instead of `.watch()` | Use Query Watchers                        |
| **Empty List**      | Nothing shows on initial load  | Wrong reset logic                       | Use `needsRefresh \|\| !hasData()`        |
| **State Drift**     | UI shows wrong counts          | UI/pagination state not synced          | Use `paginationState.copy(items = ...)`   |
| **Cache Issues**    | Old data after CRUD operations | Cache not cleared after mutations       | Add `apolloClient.apolloStore.clearAll()` |
| **Web-Only Issues** | Works on native, fails on web  | Platform-specific caching behavior      | Test on WasmJS/Web target                 |

## Expected Outcome

After implementation, the screen should have:

- ✅ Infinite scroll pagination with 90% threshold
- ✅ Debounced search (1 second delay)
- ✅ Proper cache handling (no empty state flashing)
- ✅ Scroll state persistence across navigation
- ✅ Visual scrollbar matching app design
- ✅ Adaptive page sizes (15/25/35 based on screen width)
- ✅ Error handling with retry buttons
- ✅ Loading indicators for initial and next page loads
- ✅ End-of-list indicators with Devanagari numerals
- ✅ Responsive layout (compact/tablet/desktop)

## Replacements Needed

Replace the following placeholders with actual values:

- `[ENTITY_NAME]` - Entity name (e.g., "Family", "AryaSamaj")
- `[EntityName]` - Pascal case entity name (e.g., "Family", "AryaSamaj")
- `[entityName]` - camelCase entity name (e.g., "family", "aryaSamaj")
- `[EntityType]` - Generated GraphQL type (e.g., "FamilyWithAddress", "AryaSamajWithAddress")
- `[EntityListItem]` - Display item type (e.g., "FamilyListItem", "AryaSamajListItem")
- `[entityPlural]` - Plural form (e.g., "families", "aryaSamajs")
- `[EntityPlural]` - Pascal case plural (e.g., "Families", "AryaSamajs")
- `[entitySingular]` - Singular variable name (e.g., "family", "aryaSamaj")
- `[entityFragment]` - GraphQL fragment access (e.g., "familyFields", "aryaSamajWithAddress")

## Compilation Instructions

After implementation, run:

```bash
./gradlew :composeApp:generateApolloSources
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

## Hindi Text Guidelines

All UI text should be in pure Hindi (Sanskrit-based) with Devanagari script. Avoid Urdu/Persian loanwords.

Examples:

- Use 'परीक्षण' not 'टेस्ट'
- Use 'उत्तम' not 'बेहतरीन'
- Use 'पुनः प्रयास करें' not 'फिर कोशिश करें'
- Use 'प्रस्तुत करें' not 'सबमिट करें'

## Testing

Test with realistic data (50+ records) to verify:

- Initial loading works
- Infinite scroll triggers at 90%
- Search resets pagination
- Navigation preserves scroll position
- Error states show retry buttons
- All loading states work correctly

---

**Reference Files:**

- Complete example: `AryaSamajListScreen.kt`
- Documentation: `docs/reusable-components/PaginatedListScreen.md`
- Development log: `docs/development-logs/2025-07-03-pagination-implementation.md`

## Auto-Refresh for Create/Edit/Delete Operations

**CRITICAL**: To ensure the paginated list automatically refreshes after create, edit, or delete operations, and that
dependent counts update properly, follow this pattern:

### 1. Add Refresh Tracking to Global State

Update the global state object to include refresh tracking:

```kotlin
private object [EntityName]PageState {
  var [entityPlural]: List<[EntityType]> = emptyList()
  var paginationState: PaginationState<[EntityType]> = PaginationState()
  var lastSearchQuery: String = ""
  var needsRefresh: Boolean = false // Add refresh tracking

  fun clear() {
    [entityPlural] = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
    needsRefresh = false
  }

  fun saveState(
    new[EntityPlural]: List<[EntityType]>,
    newPaginationState: PaginationState<[EntityType]>,
    searchQuery: String
  ) {
    [entityPlural] = new[EntityPlural]
    paginationState = newPaginationState
    lastSearchQuery = searchQuery
  }

  fun hasData(): Boolean = [entityPlural].isNotEmpty()
  
  fun markForRefresh() {
    needsRefresh = true
  }
}
```

### 2. Add Cache Clearing to Repository Mutations

**CRITICAL**: Add cache clearing after successful create/edit/delete operations:

```kotlin
// In create[EntityName] method
override suspend fun create[EntityName](formData: [EntityName]FormData): Flow<Result<String>> = flow {
  // ... creation logic...
  
  // CRITICAL: Clear Apollo cache after successful creation
  apolloClient.apolloStore.clearAll()
  
  emit(result)
}

// In update[EntityName] method  
override suspend fun update[EntityName](id: String, formData: [EntityName]FormData): Flow<Result<Boolean>> = flow {
  // ... update logic...
  
  val result = safeCall {
    // ... update operations...
    
    // CRITICAL: Clear Apollo cache after successful update
    apolloClient.apolloStore.clearAll()
    true
  }
  emit(result)
}

// In delete[EntityName] method
override suspend fun delete[EntityName](id: String): Flow<Result<Boolean>> = flow {
  emit(Result.Loading)
  val result = safeCall {
    val response = apolloClient.mutation(Delete[EntityName]Mutation(id)).execute()
    if (response.hasErrors()) {
      throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
    }
    val success = response.data?.deleteFrom[EntityName]Collection?.affectedCount?.let { it > 0 } ?: false
    if (success) {
      // CRITICAL: Clear Apollo cache after successful deletion
      apolloClient.apolloStore.clearAll()
    }
    success
  }
  emit(result)
}
```

### 3. Update ViewModel Delete Method

Modify the delete method to accept a success callback:

```kotlin
fun delete[EntityName](id: String, onSuccess: (() -> Unit)? = null) {
  viewModelScope.launch {
    repository.delete[EntityName](id).collect { result ->
      result.handleResult(
        onLoading = {
          // Could add a loading state for delete if needed
        },
        onSuccess = { success ->
          // Refresh the list
          load[EntityPlural]Paginated(resetPagination = true)
          // Call the success callback for parent updates
          onSuccess?.invoke()
        },
        onError = { appError ->
          ErrorHandler.logError(appError, "[EntityName]ViewModel.delete[EntityName]")
          _listUiState.value = _listUiState.value.copy(
            error = appError.getUserMessage(),
            appError = appError
          )
        }
      )
    }
  }
}
```

### 4. Mark for Refresh After Create/Edit Operations

In navigation callbacks after successful create/edit:

```kotlin
// In form screens, navigation callbacks after successful operations
onNavigateToItemDetails = { itemId ->
  [EntityName]PageState.markForRefresh() // Mark list for refresh
  navController.navigate(Screen.[EntityName]Detail(itemId)) {
    popUpTo(Screen.[EntityName]Form) { inclusive = true }
  }
}
```

### 5. Parent Screen Integration (For Count Updates)

If your list is embedded in a parent screen with counts (like tabs), add the callback:

```kotlin
// In parent screen (e.g., AdminContainerScreen with tabs showing counts)
[EntityName]ListScreen(
  viewModel = [entityName]ViewModel,
  onDataChanged = {
    // Refresh counts or other parent data when entity data changes
    parentViewModel.loadCounts() // or loadAdminCounts()
  }
  // ... other parameters
)
```

### 6. LaunchedEffect Pattern for Auto-Refresh

The screen implementation should use this pattern:

```kotlin
// Generate unique key when refresh is needed
val refreshKey = remember([EntityName]PageState.needsRefresh) {
  if ([EntityName]PageState.needsRefresh) Clock.System.now().toEpochMilliseconds() else 0L
}

LaunchedEffect(refreshKey) {
  // Clear preserved state if refresh is requested
  if ([EntityName]PageState.needsRefresh) {
    [EntityName]PageState.clear()
  }

  // Preserve pagination only if not refreshing
  if (![EntityName]PageState.needsRefresh && [EntityName]PageState.hasData() && 
      [EntityName]PageState.lastSearchQuery == uiState.searchQuery) {
    viewModel.preserve[EntityName]Pagination([EntityName]PageState.[entityPlural], [EntityName]PageState.paginationState)
  }

  // Load data (resetPagination = true when refreshing)
  val shouldReset = [EntityName]PageState.needsRefresh || ![EntityName]PageState.hasData() 
  viewModel.load[EntityPlural]Paginated(pageSize = pageSize, resetPagination = shouldReset)
  [EntityName]PageState.needsRefresh = false
}
```

### 7. Required Imports for Auto-Refresh

Add these imports to your screen file:

```kotlin
import kotlinx.datetime.Clock
```

### Auto-Refresh Benefits

This pattern ensures:

- ✅ **Scroll Preservation**: Normal navigation preserves scroll position
- ✅ **Auto-Refresh**: Create/edit/delete operations automatically refresh the list
- ✅ **Cache Consistency**: Apollo cache is cleared to ensure fresh data
- ✅ **Parent Updates**: Callbacks allow parent screens to update counts/stats
- ✅ **Minimal Changes**: Only 5-6 lines of code per screen to implement

### Auto-Refresh Checklist

When implementing, ensure:

- ✅ Add `needsRefresh` flag and `markForRefresh()` to global state
- ✅ Clear Apollo cache in repository after successful mutations
- ✅ Use unique `refreshKey` based on `needsRefresh` in LaunchedEffect
- ✅ Call `markForRefresh()` after create/edit operations in navigation
- ✅ Add success callback to delete method for parent updates
- ✅ Add `onDataChanged` callback to screen for count updates
- ✅ Reset `needsRefresh` flag after processing
