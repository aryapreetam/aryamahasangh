# GitHub Copilot Instructions for AryaMahasangh

## Project Overview
AryaMahasangh is an organization management platform built with **Compose Multiplatform** targeting **Android, iOS, Web (WasmJS), and Desktop**. The backend is powered by **Supabase** accessed via **GraphQL** using **Apollo Kotlin**.

---

## Core Architecture Principles

### MVVM Architecture (MANDATORY)
- **Model**: Domain models and data layer (repositories, data sources)
- **View**: Compose UI components - MUST be dumb, stateless, and free of business logic
- **ViewModel**: All business logic, state management, and data transformation
- **Single State Pattern**: ViewModels MUST expose only a single combined `UiState` using `StateFlow`
- **No Direct Data Access**: UI screens NEVER perform GraphQL queries directly - all data access through repositories

### Testable Architecture
- Every ViewModel MUST be testable in `commonTest` using coroutine test utilities
- Use mock repositories for testing - never mock ViewModels
- All repositories MUST have interface definitions for easy mocking
- UI components should be testable with fake/preview states

### Multi-Platform First (Android, iOS, Web, Desktop)
- Use ONLY Kotlin & Jetpack Compose APIs supported across ALL platforms
- Build adaptive layouts - avoid `fillMaxWidth()` for all components
- Test on all targets, especially WasmJS where caching/serialization issues surface first
- Never use platform-specific APIs in common code without expect/actual

### Structured Concurrency
- **NEVER use `runBlocking()`** except in very rare cases like secrets loading
- Always prefer `suspend` functions and structured concurrency
- Use `viewModelScope` for ViewModel coroutines
- Use `Flow` and `StateFlow` for reactive data streams
- Properly handle coroutine cancellation and cleanup in `onCleared()`

---

## Code Organization

### Feature Module Structure
Organize code by feature using:
```
feature/<featureName>/
├── ui/              # Compose screens and components
├── viewmodel/       # ViewModels with state management
├── domain/          # Domain models, use cases
└── data/            # Repositories, data sources
```

### Navigation
- Centralize navigation using sealed class `Screen` or `NavGraph`
- Avoid inline navigation calls within Composables
- **TYPE-SAFE NAVIGATION**: Use `popUpTo<Screen.DataClass>()` (generic syntax) instead of `popUpTo(Screen.DataClass)` for data class routes to prevent serialization crashes

---

## GraphQL & Data Layer

### GraphQL Operations
- All GraphQL queries and mutations MUST be defined in `.graphql` files
- Generate code using Apollo Kotlin
- All operations MUST be exposed through repository interfaces
- Use `apolloClient.query().toFlow()` or `.watch()` for reactive queries

### Repository Pattern
- All data access MUST go through repository interfaces
- Implement repositories using Apollo Kotlin client
- For pagination, extend `PaginatedRepository<EntityType>` interface
- **Apollo Cache Management**: Clear cache after mutations with `apolloClient.apolloStore.clearAll()`
- Handle `FetchPolicy.CacheAndNetwork` properly - skip empty cache misses but show cache hits

### Query Watchers (PREFERRED)
- Use `.watch()` instead of `.toFlow()` for `FetchPolicy.CacheAndNetwork` to prevent duplication
- Query watchers automatically handle cache vs network responses
- Still handle empty cache misses: `val isCacheMissWithEmptyData = response.exception is CacheMissException && response.data?.collection?.edges.isNullOrEmpty()`

---

## ViewModel Guidelines

### State Management
- Expose all UI state via `StateFlow<UiState>` - single combined state
- Use `SnapshotStateList` for mutable lists if needed
- State should be immutable data classes
- Include states for: `isLoading`, `error`, `data`, `submitSuccess`, etc.

### Business Logic
- ALL business logic, data transformations, and caching in ViewModel or domain layer
- UI layer ONLY renders data from state
- Never perform calculations or transformations in Composables

### Deletion State Pattern
Include proper deletion state in ViewModels:
```kotlin
data class DeleteState(
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val deleteError: String? = null,
    val deletingId: String? = null
)
```

### Pagination Patterns
- Call `repository.getItemsPaginated()` and `repository.searchItemsPaginated()`
- Implement `preservePagination()` method to restore saved state
- Force list refresh with `resetPagination = true` after mutations
- Initial load logic: `val shouldReset = PageState.needsRefresh || !PageState.hasData()`
- Include bounds checking: `if (!resetPagination && currentState.hasNextPage == false) return@launch`

---

## UI Layer Guidelines

### Compose Best Practices
- Follow **Material Design 3** guidelines (Expressive Design if possible)
- Build adaptive layouts for mobile, tablet, desktop, and browser
- Use `rememberSaveable()` for form inputs (config-change resilience)
- Add `testTag` or `semantics` to EVERY UI component for testing

### State Handling
- Collect state with `val uiState by viewModel.uiState.collectAsState()`
- React to state changes with `LaunchedEffect(uiState.property)`
- Never mutate state directly - always through ViewModel methods
- No logic in UI - just pure rendering based on state

### Global Message Manager (MANDATORY)
- Use `GlobalMessageManager` for ALL user-facing notifications
- **Success Messages**: `GlobalMessageManager.showSuccess()` in ViewModels
- **Error Messages**: `GlobalMessageManager.showError()` for critical errors
- **Immediate Navigation**: Navigate immediately on `submitSuccess`/`updateSuccess` without waiting for snackbar
- Local form validation can still use `snackbarHostState` directly

---

## Real-Time Subscriptions

### Subscription Management
- Initialize subscriptions in ViewModel, expose as `StateFlow`
- Never start/stop subscriptions in Composable directly
- Use `SubscriptionManager` abstraction for Supabase channel lifecycles
- Surface errors via `UiEffect` like `ShowErrorSnackbar`
- Ensure subscriptions are disposed in `onCleared()`

---

## Pagination

### Pagination Architecture
- Handle pagination in ViewModel with explicit `loadNextPage()` function
- Use sealed class `LoadState` for: loading, success, error, end-of-list
- Track: current page, total items, `hasNextPage` in UI state
- Display separate loading indicators for initial and paginated loads
- Throttle or debounce scroll events - avoid concurrent `loadNextPage()` calls

### List Screens (STANDARDIZED)
- Use `PaginatedListScreen` component - never create custom LazyColumn implementations
- Companion global state object (e.g., `EkalAryaPageState`) with:
  - `items: List<T>`
  - `paginationState: PaginationState`
  - `lastSearchQuery: String?`
  - `needsRefresh: Boolean`
  - Methods: `clear()`, `saveState()`, `hasData()`, `markForRefresh()`
- Auto-refresh using `refreshKey = Clock.System.now().toEpochMilliseconds()`
- Preserve scroll position for view-only operations

---

## Error Handling

### User-Facing Messages
- ALL user-facing messages MUST be in **pure Sanskrit/Hindi with Devanagari script**
- **NO Urdu/Persian loanwords** - use Sanskrit roots only
- Examples:
  - ✅ "परीक्षण" ❌ "टेस्ट"
  - ✅ "उत्तम" ❌ "बेहतरीन"
  - ✅ "पुनः प्रयास करें" ❌ "फिर कोशिश करें"
  - ✅ "प्रस्तुत करें" ❌ "सबमिट करें"

### Repository Error Handling
- Catch GraphQL errors and convert to Hindi messages
- Handle database constraint violations with specific messages
- Check `affectedCount` and throw exceptions for zero affected records
- NEVER let database errors be silently logged - always surface to UI

### Backend Message Codes
- Supabase functions return English message codes (not localized text)
- Success: `message_code` field (e.g., `RECORD_CREATED_SUCCESSFULLY`)
- Error: `error_code` field (e.g., `RECORD_NOT_FOUND`, `ERROR_CREATING_RECORD`)
- Always include `success: Boolean` field
- Client handles localization based on codes

---

## Testing

### ViewModel Testing
- Test in `commonTest` with coroutine test utilities
- Use mock repositories - never mock ViewModels
- Test all state transitions
- Test error scenarios

### UI Testing
- Use Apollo Kotlin MockServer for integration tests
- Add `testTag`/`semantics` to all UI components
- Only use test APIs supported across all platforms
- Test adaptive layouts on different screen sizes

### E2E Testing Strategy
- Use **Maestro** for E2E tests on all platforms
- **Text Selector Quoting**: Wrap regex patterns in single quotes for Hindi/Devanagari
  - ✅ `- assertVisible: 'सनातन धर्म.*'`
  - ❌ `- assertVisible: "सनातन धर्म.*"`

---

## Engineering Excellence

### 10X Engineer Mentality
Before implementation, ask:
- What's the **business impact**?
- What's the **opportunity cost**?
- What's the **blast radius** if this breaks?
- Can we achieve **80% benefit with 20% effort**?
- Prioritize **shipping speed** over technical perfectionism
- Push back on over-engineering

### Systematic Problem Analysis
For every technical decision, analyze:
1. What are we optimizing for? (speed/maintainability/performance)
2. How will this evolve over time?
3. What's the blast radius if this breaks?
4. How do other major systems solve this?
5. What would the solution look like in 2-3 years?
6. Can we solve the root cause instead of symptoms?

### Architecture Exploration (MANDATORY)
**Before implementing any solution**, present at least **3 different approaches** with explicit trade-offs:
```
Approach 1: [name]
✅ Pros: [...]
❌ Cons: [...]
Best for: [scenario]
```

Consider: maintainability, scalability, performance, developer experience, long-term evolution.

### Design Patterns
Always consider established patterns:
- Repository pattern
- Factory pattern
- Strategy pattern
- Observer pattern
- Dependency Injection

Reference how companies like Netflix, Spotify, Google architect similar problems.

---

## Security

### Secrets Management
- NEVER store secrets in the app
- Use Supabase Auth securely
- Load secrets only when absolutely necessary (rare `runBlocking()` use case)

### Development Logs
- DO NOT include sensitive information (passwords, keys, secrets) in logs
- Always use `git diff` to check for changes before committing

---

## MCP Servers Available

The project has the following MCP servers configured:
- **supabase_dev/staging/prod**: Direct Supabase access for each environment
- **github**: GitHub operations
- **AutoMobile**: Mobile automation
- **mobile-mcp**: Mobile-specific MCP
- **sequential-thinking**: Advanced reasoning
- **context7**: Context management
- **memory**: Knowledge graph memory
- **maestro**: E2E testing

Always check `firebender.json` for available MCP servers before providing manual instructions.

---

## Platform-Specific Considerations

### WasmJS (Web)
- Cache clearing is especially critical - always clear after mutations
- Serialization issues surface more on WasmJS - test navigation thoroughly
- Query watchers help prevent duplication issues

### Apollo Cache (All Platforms)
- Clear cache after ALL mutations: `apolloClient.apolloStore.clearAll()`
- Critical for WasmJS/Web but benefits all platforms
- Ensures fresh data after create/update/delete operations

---

## Prompts & Communication

### Prompt Generation
When asked to provide a prompt, ensure it:
- Can be copied and pasted into next chat window
- Retains formatting
- Includes all necessary context

### Clarification
- If clarity is needed, ASK before proceeding
- Do not proceed with implementation unless everything is clear
- Be specific about unknowns or ambiguities

---

## File-Specific Rules

### *ListScreen.kt
- MUST use standardized `PaginatedListScreen` component
- MUST have companion global state object
- MUST implement auto-refresh on data changes
- MUST preserve scroll position for view-only operations
- MUST implement snackbar error display for deletion feedback

### *Repository.kt
- Interface MUST extend `PaginatedRepository<EntityType>` for pagination
- Implement `getItemsPaginated()` and `searchItemsPaginated()` with real GraphQL
- MUST support filter parameter for future extensibility
- MUST clear Apollo cache after mutations
- MUST have comprehensive error handling with Hindi messages

### *ViewModel.kt
- MUST include proper deletion state management
- Call repository pagination methods (not old methods)
- Implement `preservePagination()` method
- PREFER Query Watchers over manual deduplication
- Always clear Apollo cache after mutations
- Include proper early return logic in pagination methods

### *NavGraph.kt
- MUST call appropriate `markForRefresh()` methods in CRUD callbacks
- Use type-safe navigation syntax: `popUpTo<Screen.DataClass>()`

### *Test.kt
- Only use test APIs supported across all platforms (Android, iOS, Web, Desktop)

### maestro-tests/*.yaml
- Wrap regex patterns in single quotes for Devanagari/Hindi text
- Example: `- assertVisible: 'सनातन धर्म.*'`

---

## Summary Checklist

Before submitting any code, verify:
- ✅ No business logic in UI layer
- ✅ Single combined `UiState` exposed from ViewModel
- ✅ All data access through repository interfaces
- ✅ Structured concurrency (no `runBlocking()`)
- ✅ Works on all 4 platforms (Android, iOS, Web, Desktop)
- ✅ User-facing messages in pure Sanskrit/Hindi
- ✅ Apollo cache cleared after mutations
- ✅ Type-safe navigation syntax used
- ✅ GlobalMessageManager used for notifications
- ✅ Test tags added to UI components
- ✅ ViewModels are testable with mock repositories
- ✅ Adaptive layouts (no hardcoded `fillMaxWidth()`)
- ✅ No secrets committed to repo
- ✅ Explored multiple architectural approaches

---

**Think like a Staff/Principal Engineer. Build for scale, maintainability, and long-term evolution.**

