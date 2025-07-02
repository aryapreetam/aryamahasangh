# Scroll Position Persistence in Paginated Lists - Complete Solution

**Date:** January 2, 2025  
**Feature:** EkalAryaListScreen scroll position persistence  
**Status:** ✅ Implemented and Working

## Problem Statement

Users expect that when they scroll down in a paginated list (e.g., to item 100+), navigate to a detail screen, and then
navigate back, they should return to the same scroll position. However, this was not working for items beyond the
initial page size (~25-30 items).

### Root Cause Analysis

The issue had two components:

1. **Scroll Position**: ✅ Was being saved correctly with `rememberSaveable(LazyListState.Saver)`
2. **Pagination Data**: ❌ Was being lost during navigation

**The Real Problem:** The `AdminViewModel` is configured as a `factory` in Koin DI, meaning each navigation creates a
fresh ViewModel instance. When you navigate back, the new ViewModel only has the initial page of data (~30 items), so
trying to scroll to item 100+ fails because item 100 doesn't exist.

## How This Solution Was Derived

### The Debugging Process

The breakthrough came from analyzing the actual user logs during scroll testing:

```
🔍 Scroll trigger: shouldLoadMore=true
🔍 loadNextEkalAryaPage called
🔍 Loading next page for regular list
🔍 loadEkalAryaMembersPaginated called: pageSize=30, resetPagination=false
🔍 shouldPreservePagination=false, hasExistingData=true, shouldReset=false
🔍 Skipping load because data should be preserved
```

**Key Insight #1**: The scroll position (index: 11, offset: 54) was being saved and restored correctly, but only for
items within the initial page size (~25-30 items).

**Key Insight #2**: When the user said "I tried to click on 100th item and came back from detail. I was shown items from
initial page size and not the 100th one", it revealed the real issue wasn't scroll position - it was missing pagination
data.

### The Investigation Trail

1. **Initial Hypothesis**: Thought `rememberSaveable` wasn't working properly
2. **Debug Logging**: Added extensive logging to track scroll position, pagination state, and ViewModel calls
3. **Root Cause Discovery**: Realized that scroll position was correct, but the ViewModel was being recreated
4. **DI Configuration Check**: Discovered `AdminViewModel` was configured as `factory` in `ViewModelModule.kt`
5. **The "Aha!" Moment**: Each navigation creates a fresh ViewModel instance with only initial page data

### Solution Design Philosophy

**Why Global State Instead of Other Approaches:**

1. **Singleton ViewModel**: Rejected because it could cause memory leaks and state pollution across different contexts
2. **Navigation Arguments**: Too complex and limited by argument size restrictions
3. **Persistent Storage**: Overkill for session-based data that should be cleared on app restart
4. **Global State Object**: ✅ Perfect fit - survives ViewModel recreation but clears on app restart

**Key Design Decisions:**

- **Memory Scoped**: Global object lives in memory (not persistent storage) so it naturally clears when app restarts
- **Search Aware**: Clears state when search context changes to prevent stale data restoration
- **Lazy Evaluation**: Only restores state if search query matches, ensuring context relevance
- **Automatic Saving**: Continuously saves state on every UI update, ensuring no data loss

### The Pattern Recognition

This solution emerged from recognizing that **the problem wasn't scroll position (which Compose handles well), but data
availability**. The pattern is:

```
Scroll Position (Compose) + Data Persistence (Custom) = Complete Solution
```

This insight led to separating concerns:

- Let Compose handle scroll position with `rememberSaveable`
- Handle pagination data persistence with a custom global state holder

The solution is elegant because it leverages Compose's built-in capabilities while solving only the specific problem
that Compose doesn't handle automatically.

## Solution: Global State Persistence

### Implementation

**1. Create a Global State Holder**

```kotlin
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
```

**2. Update the Composable Screen**

```kotlin
@Composable
fun EkalAryaListScreen(/*...*/) {
  // Scroll position preserved across navigation using rememberSaveable
  val listState = rememberSaveable(
    key = "admin_ekal_arya_list_state",
    saver = LazyListState.Saver
  ) {
    LazyListState()
  }
  
  // Reset scroll and clear saved state when user searches
  LaunchedEffect(uiState.searchQuery) {
    if (uiState.searchQuery.isNotEmpty()) {
      EkalAryaPageState.clear()
      listState.animateScrollToItem(0)
    }
  }

  LaunchedEffect(Unit) {
    // Restore pagination data if available and search query matches
    if (EkalAryaPageState.hasData() && EkalAryaPageState.lastSearchQuery == uiState.searchQuery) {
      viewModel.preserveEkalAryaPagination(EkalAryaPageState.members, EkalAryaPageState.paginationState)
    }
    
    // Load data (will be skipped if preserved)
    viewModel.loadEkalAryaMembersPaginated(pageSize = pageSize, resetPagination = true)
  }

  // Continuously save state as it changes
  LaunchedEffect(uiState) {
    EkalAryaPageState.saveState(uiState.members, uiState.paginationState, uiState.searchQuery)
  }
}
```

**3. Update the ViewModel**

```kotlin
class AdminViewModel(/*...*/) {
  private var shouldPreservePagination = false

  fun preserveEkalAryaPagination(savedMembers: List<MemberShort>, savedPaginationState: PaginationState<MemberShort>) {
    _ekalAryaUiState.value = _ekalAryaUiState.value.copy(
      members = savedMembers,
      paginationState = savedPaginationState
    )
    shouldPreservePagination = true
  }

  fun loadEkalAryaMembersPaginated(pageSize: Int = 30, resetPagination: Boolean = false) {
    viewModelScope.launch {
      val currentState = _ekalAryaUiState.value.paginationState

      // Only preserve pagination when explicitly requested AND it's a reset operation
      val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExistingEkalAryaData()
      
      // Reset the preservation flag after checking
      if (shouldPreservePagination) {
        shouldPreservePagination = false
      }

      // Only skip loading if we're preserving existing data from navigation
      if (shouldPreserveExistingData) {
        return@launch
      }

      // Continue with normal pagination loading...
    }
  }
}
```

## How It Works

1. **State Persistence**: `EkalAryaPageState` object stores pagination data in memory outside the ViewModel scope
2. **Automatic Saving**: Every UI state change triggers `saveState()` to keep the global object updated
3. **Smart Restoration**: On screen load, checks if saved data exists and search query matches, then restores the
   complete pagination state
4. **Search Handling**: Clears saved state when user searches to prevent restoring outdated data
5. **Scroll Position**: `rememberSaveable` handles scroll position independently of pagination data

## Key Benefits

- ✅ **Survives ViewModel Recreation**: Works despite `factory` scoped ViewModels in Koin DI
- ✅ **Complete Data Preservation**: Restores all paginated items (100+), not just scroll position
- ✅ **Cross-Platform Compatible**: Works on Android, iOS, Web, and Desktop
- ✅ **Memory Efficient**: Only holds current session data, clears on search
- ✅ **Search-Aware**: Intelligently clears state when search context changes

## Usage Pattern for Other Screens

This pattern can be applied to any paginated list screen:

1. Create a global state holder object (`XxxPageState`)
2. Add state saving/restoration logic in the Composable
3. Update ViewModel to accept restored state
4. Handle search/filter scenarios by clearing saved state

## Files Modified

- `EkalAryaListScreen.kt`: Added global state holder and restoration logic
- `AdminViewModel.kt`: Added state preservation method and logic
- No changes needed to Repository or navigation files

This solution provides a robust, reusable pattern for maintaining scroll position and pagination data across navigation
in Compose Multiplatform applications with factory-scoped ViewModels.
