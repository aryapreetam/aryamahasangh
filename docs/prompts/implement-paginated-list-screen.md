# Prompt: Implement Paginated List Screen Using Reusable Components

## Context

This project has a comprehensive pagination system implemented using reusable components. The system includes:

- **PaginatedListScreen<T>**: Generic UI component for infinite scroll pagination
- **PaginatedRepository<T>**: Repository interface for paginated data access
- **PaginationState<T>** and **PaginationResult<T>**: State management classes
- **Comprehensive cache handling** to prevent empty state flashing
- **Scroll state persistence** across navigation
- **Visual scrollbar** matching app design
- **Adaptive page sizes** based on screen width
- **Debounced search** functionality
- **Error handling** with retry buttons

## Reference Implementation

**AryaSamajListScreen** serves as the complete reference implementation demonstrating all features:

- File: `composeApp/src/commonMain/kotlin/com/aryamahasangh/features/admin/AryaSamajListScreen.kt`
- Repository: `composeApp/src/commonMain/kotlin/com/aryamahasangh/features/admin/data/AryaSamajRepository.kt`
- ViewModel: `composeApp/src/commonMain/kotlin/com/aryamahasangh/features/admin/data/AryaSamajViewModel.kt`
- GraphQL Queries: `composeApp/src/commonMain/graphql/queries.graphql` (search for `getAryaSamajs` and
  `searchAryaSamajs`)

## Task

Please implement a paginated list screen for [ENTITY_NAME] following the exact same pattern as AryaSamajListScreen. This
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
  .toFlow()
  .collect { response ->
    // CRITICAL: Include cache handling to prevent empty state flashing
    if (response.isFromCache && response.cacheInfo?.isCacheHit == false && 
        response.data?.[entityName]Collection?.edges.isNullOrEmpty()) {
      // Skip emitting empty cache miss - wait for network
    } else {
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
}

override suspend fun searchItemsPaginated(
  searchTerm: String,
  pageSize: Int,
  cursor: String?
): Flow<PaginationResult<[EntityType]>> = flow {
  emit(PaginationResult.Loading())
  val result = safeCall {
    val response = apolloClient.query(
      Search[EntityName]sQuery(
        searchTerm = "%$searchTerm%",
        first = pageSize,
        after = Optional.presentIfNotNull(cursor)
      )
    ).execute()

    if (response.hasErrors()) {
      throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
    }

    val edges = response.data?.[entityName]Collection?.edges ?: emptyList()
    val items = edges.map { it.node as [EntityType] }
    val hasNextPage = response.data?.[entityName]Collection?.pageInfo?.hasNextPage ?: false
    val endCursor = response.data?.[entityName]Collection?.pageInfo?.endCursor

    PaginationResult.Success(data = items, hasNextPage = hasNextPage, endCursor = endCursor)
  }

  when (result) {
    is Result.Success -> emit(result.data)
    is Result.Error -> emit(PaginationResult.Error(result.message))
    is Result.Loading -> {} // Already emitted Loading above
  }
}
```

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
    paginationState = savedPaginationState
  )
  shouldPreservePagination = true
}

fun load[EntityPlural]Paginated(pageSize: Int = 30, resetPagination: Boolean = false) {
  viewModelScope.launch {
    val currentState = _uiState.value.paginationState

    // Preserve pagination logic
    val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExisting[EntityName]Data()
    if (shouldPreservePagination) {
      shouldPreservePagination = false
    }
    if (shouldPreserveExistingData) {
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
          val newItems = if (shouldReset) {
            result.data
          } else {
            currentState.items + result.data
          }

          // Convert to display items
          val displayItems = newItems.map { /* convert to display format */ }

          _uiState.value = _uiState.value.copy(
            [entityPlural] = displayItems,
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

fun search[EntityPlural]Paginated(searchTerm: String, pageSize: Int = 30, resetPagination: Boolean = true) {
  // Similar implementation to load[EntityPlural]Paginated but using searchItemsPaginated
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
```

### 4. Screen Implementation

**Create global state object:**

```kotlin
private object [EntityName]PageState {
  var [entityPlural]: List<[EntityType]> = emptyList()
  var paginationState: PaginationState<[EntityType]> = PaginationState()
  var lastSearchQuery: String = ""

  fun clear() {
    [entityPlural] = emptyList()
    paginationState = PaginationState()
    lastSearchQuery = ""
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
    // State preservation
    if ([EntityName]PageState.hasData() && [EntityName]PageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preserve[EntityName]Pagination([EntityName]PageState.[entityPlural], [EntityName]PageState.paginationState)
    }

    viewModel.load[EntityPlural]Paginated(pageSize = pageSize, resetPagination = true)
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
        onDeleteClick = { onDelete([entitySingular].id) }
      )
    }
  )
}
```

### 5. Required Dependencies

Ensure these are available in `Models.kt`:

- `PaginatedRepository<T>` interface
- `PaginationState<T>` data class
- `PaginationResult<T>` sealed class

### 6. Extension Functions (if needed)

Add address formatting or other utility functions to the Models file:

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
```

## Critical Implementation Notes

### 1. Cache Handling

**MUST INCLUDE** the cache handling logic in repository to prevent empty state flashing:

```kotlin
if (response.isFromCache && response.cacheInfo?.isCacheHit == false && 
    response.data?.[entityName]Collection?.edges.isNullOrEmpty()) {
  // Skip emitting empty cache miss - wait for network
}
```

### 2. LaunchedEffect Dependencies

**CRITICAL**: In PaginatedListScreen, the scroll detection LaunchedEffect must depend on both `listState` and
`paginationState`:

```kotlin
LaunchedEffect(listState, paginationState) {
  // scroll detection logic
}
```

### 3. State Preservation

Implement global state object for scroll position persistence across navigation.

### 4. Hindi Text

All UI text should be in pure Hindi (Sanskrit-based) with Devanagari script. Avoid Urdu/Persian loanwords.

### 5. Compilation

After implementation, run:

```bash
./gradlew :composeApp:generateApolloSources
./gradlew :composeApp:compileCommonMainKotlinMetadata
```

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
