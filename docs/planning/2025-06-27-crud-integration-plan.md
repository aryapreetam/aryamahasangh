# CRUD Integration Development Plan
**Project**: Arya Mahasangh Organization Management Platform  
**Date Created**: June 27, 2025  
**Last Updated**: June 27, 2025

## Overview

This document tracks the integration of Supabase CRUD functions (Family, Member, Arya Samaj) into the Compose
Multiplatform client application.

---

## Phase 1: GraphQL Schema & Operations Setup

**Status**: ✅ **COMPLETED**  
**Estimated Time**: 2-3 hours  
**Actual Time**: 2 hours  
**Priority**: HIGH (Foundation)

### 1.1 Create GraphQL Operations for CRUD Functions

- [x] **Arya Samaj Operations**
    - [x] `InsertAryaSamajDetails` mutation (clean name)
    - [x] `UpdateAryaSamajDetails` mutation (clean name)
    - [x] `DeleteAryaSamajComprehensive` mutation (renamed to avoid conflict)

- [x] **Member Operations**
    - [x] `InsertMemberDetails` mutation (clean name)
    - [x] `UpdateMemberDetailsComprehensive` mutation (renamed to avoid conflict)
    - [x] `DeleteMemberComprehensive` mutation (renamed to avoid conflict)

- [x] **Family Operations**
    - [x] `InsertFamilyDetails` mutation (clean name)
    - [x] `UpdateFamilyDetails` mutation (clean name)
    - [x] `DeleteFamilyComprehensive` mutation (renamed to avoid conflict)

### 1.2 Update Existing GraphQL Files

- [x] Review and update existing family/member GraphQL files
- [x] Ensure consistency with new CRUD operations
- [x] Add any missing query operations for data fetching

### 1.3 Apollo Code Generation

- [x] Run Apollo code generation: `./gradlew generateApolloSources`
- [x] Verify generated Kotlin code compiles correctly
- [x] Fix any compilation errors

**Completion Criteria**: ✅ All GraphQL operations defined, tested, and generating clean Kotlin code

**Notes**:

- ✅ **CORRECTED APPROACH**: Found and used the actual comprehensive Supabase functions from the GraphQL schema (
  `insertAryaSamajDetails`, `insertFamilyDetails`, `insertMemberDetails`, etc.)
- ✅ **Conflict Resolution**: Only renamed conflicting mutations in our new files, preserved existing working code
- ✅ **Naming Strategy**: Clean names for non-conflicting mutations, "Comprehensive" suffix only where needed
- ✅ **Full Business Logic**: These mutations call the complete CRUD functions with proper validation, error
  handling, and business rules
- ✅ **Apollo Integration**: Successfully generating typed Kotlin code for all comprehensive operations
- ✅ **Build Verification**: Project compiles successfully with all new mutations

**Current Mutation State (9 total):**

- **Clean Names**: `InsertAryaSamajDetails`, `UpdateAryaSamajDetails`, `InsertMemberDetails`, `InsertFamilyDetails`,
  `UpdateFamilyDetails`
- **Comprehensive Names**: `DeleteAryaSamajComprehensive`, `UpdateMemberDetailsComprehensive`,
  `DeleteMemberComprehensive`, `DeleteFamilyComprehensive`

---

## Phase 2: Localization Infrastructure

**Status**: ✅ **COMPLETED**  
**Estimated Time**: 1-2 hours  
**Actual Time**: 1 hour  
**Priority**: HIGH (Required for user experience)

### 2.1 Create Message Localization System

- [x] Create `LocalizationManager.kt` in utils package
- [x] Define message code to Hindi translation mappings
- [x] Implement message resolution with fallback to English

### 2.2 Error Code Mappings

- [x] Create comprehensive Hindi translations for success codes:
    - [x] `ARYA_SAMAJ_CREATED_SUCCESSFULLY` → "आर्य समाज सफलतापूर्वक बनाया गया"
    - [x] `MEMBER_CREATED_SUCCESSFULLY` → "सदस्य सफलतापूर्वक जोड़ा गया"
    - [x] `FAMILY_CREATED_SUCCESSFULLY` → "परिवार सफलतापूर्वक बनाया गया"
    - [x] Add all update/delete success messages

- [x] Create Hindi translations for error codes:
    - [x] `MEMBER_NOT_FOUND` → "सदस्य नहीं मिला"
    - [x] `ARYA_SAMAJ_NOT_FOUND` → "आर्य समाज नहीं मिला"
    - [x] `FAMILY_NOT_FOUND` → "परिवार नहीं मिला"
    - [x] Add all other error codes

### 2.3 Integration with Existing Error Handling

- [x] Update `ErrorHandling.kt` component to use localization
- [x] Test error message display in Hindi
- [x] Ensure proper Devanagari script rendering

**Completion Criteria**: ✅ All messages display in proper Hindi with Devanagari script

**Notes**:

- ✅ **LocalizationManager**: Created comprehensive message translation system with 93 total translations
- ✅ **CrudError Integration**: Added new AppError.CrudError type for handling CRUD function responses
- ✅ **Hindi Error Messages**: Updated all error handling components to display in Hindi
- ✅ **Fallback Support**: Implemented graceful fallback to English for unknown message codes
- ✅ **UI Translation**: Updated button text and suggestions to Hindi in error components
- ✅ **Build Verification**: All localization components compile and build successfully

**Message Coverage**:

- **Success Messages**: 13 translations for all CRUD operations
- **Error Messages**: 80 translations covering validation, business logic, system errors
- **UI Components**: All error dialogs, buttons, and suggestions now in Hindi

---

## Phase 3: Repository Layer Implementation

**Status**: ⏳ **PENDING**  
**Estimated Time**: 4-5 hours  
**Priority**: HIGH (Data Layer Foundation)

### 3.1 Create Family Feature Module
- [ ] Create directory structure: `features/family/{data, domain, ui, viewmodel}`
- [ ] **FamilyRepository Interface** (`features/family/data/FamilyRepository.kt`)
    - [ ] `suspend fun createFamily(familyDetails: CreateFamilyRequest): Flow<Result<FamilyResponse>>`
    - [ ] `suspend fun updateFamily(familyId: String, updates: UpdateFamilyRequest): Flow<Result<FamilyResponse>>`
    - [ ] `suspend fun deleteFamily(familyId: String): Flow<Result<DeleteResponse>>`
    - [ ] `fun getFamilies(): Flow<List<Family>>`
    - [ ] `fun getFamilyById(id: String): Flow<Family?>`

- [ ] **FamilyRepositoryImpl** (`features/family/data/FamilyRepositoryImpl.kt`)
    - [ ] Implement Apollo GraphQL operations
    - [ ] Handle error mapping from message codes
    - [ ] Implement proper exception handling
    - [ ] Add logging for debugging

### 3.2 Create Member Feature Module

- [ ] Create directory structure: `features/member/{data, domain, ui, viewmodel}`
- [ ] **MemberRepository Interface** (`features/member/data/MemberRepository.kt`)
    - [ ] `suspend fun createMember(memberDetails: CreateMemberRequest): Flow<Result<MemberResponse>>`
    - [ ] `suspend fun updateMember(memberId: String, updates: UpdateMemberRequest): Flow<Result<MemberResponse>>`
    - [ ] `suspend fun deleteMember(memberId: String): Flow<Result<DeleteResponse>>`
    - [ ] `fun getMembers(): Flow<List<Member>>`
    - [ ] `fun getMemberById(id: String): Flow<Member?>`

- [ ] **MemberRepositoryImpl** (`features/member/data/MemberRepositoryImpl.kt`)
    - [ ] Implement Apollo GraphQL operations
    - [ ] Handle complex member data with addresses
    - [ ] Implement search and filtering capabilities

### 3.3 Create Arya Samaj Feature Module
- [ ] Create directory structure: `features/aryasamaj/{data, domain, ui, viewmodel}`
- [ ] **AryaSamajRepository Interface** (`features/aryasamaj/data/AryaSamajRepository.kt`)
    - [ ] `suspend fun createAryaSamaj(samajDetails: CreateAryaSamajRequest): Flow<Result<AryaSamajResponse>>`
  - [ ] `suspend fun updateAryaSamaj(samajId: String, updates: UpdateAryaSamajRequest): Flow<Result<AryaSamajResponse>>`
    - [ ] `suspend fun deleteAryaSamaj(samajId: String): Flow<Result<DeleteResponse>>`
    - [ ] `fun getAryaSamajs(): Flow<List<AryaSamaj>>`
    - [ ] `fun getAryaSamajById(id: String): Flow<AryaSamaj?>`

- [ ] **AryaSamajRepositoryImpl** (`features/aryasamaj/data/AryaSamajRepositoryImpl.kt`)
    - [ ] Implement Apollo GraphQL operations
    - [ ] Handle address and media URL management
    - [ ] Implement member assignment logic

### 3.4 Update Dependency Injection
- [ ] Update `RepositoryModule.kt` to provide new repositories
- [ ] Add repository bindings to Koin configuration
- [ ] Test DI resolution works correctly

**Completion Criteria**: All repositories implemented, tested, and properly injected

---

## Phase 4: Domain Layer & Models
**Status**: ⏳ **PENDING**  
**Estimated Time**: 3-4 hours  
**Priority**: HIGH (Business Logic)

### 4.1 Create Domain Models
- [ ] **Family Domain Models** (`features/family/domain/`)
    - [ ] `Family.kt` - Core family data class
    - [ ] `FamilyMember.kt` - Family member relationship
    - [ ] `CreateFamilyRequest.kt` - Input model for creation
    - [ ] `UpdateFamilyRequest.kt` - Input model for updates
    - [ ] `FamilyResponse.kt` - API response wrapper

- [ ] **Member Domain Models** (`features/member/domain/`)
    - [ ] `Member.kt` - Core member data class with all fields
    - [ ] `CreateMemberRequest.kt` - Input model for creation
    - [ ] `UpdateMemberRequest.kt` - Input model for updates
    - [ ] `MemberResponse.kt` - API response wrapper

- [ ] **Arya Samaj Domain Models** (`features/aryasamaj/domain/`)
    - [ ] `AryaSamaj.kt` - Core Arya Samaj data class
    - [ ] `CreateAryaSamajRequest.kt` - Input model for creation
    - [ ] `UpdateAryaSamajRequest.kt` - Input model for updates
    - [ ] `AryaSamajResponse.kt` - API response wrapper

### 4.2 Create Use Cases
- [ ] **Family Use Cases** (`features/family/domain/`)
    - [ ] `CreateFamilyUseCase.kt` - Business logic for family creation
    - [ ] `UpdateFamilyUseCase.kt` - Business logic for family updates
    - [ ] `DeleteFamilyUseCase.kt` - Business logic for family deletion
    - [ ] `GetFamiliesUseCase.kt` - Business logic for family listing

- [ ] **Member Use Cases** (`features/member/domain/`)
    - [ ] `CreateMemberUseCase.kt` - Business logic + validation
    - [ ] `UpdateMemberUseCase.kt` - Business logic + validation
    - [ ] `DeleteMemberUseCase.kt` - Business logic for member deletion
    - [ ] `GetMembersUseCase.kt` - Business logic for member listing

- [ ] **Arya Samaj Use Cases** (`features/aryasamaj/domain/`)
    - [ ] `CreateAryaSamajUseCase.kt` - Business logic + validation
    - [ ] `UpdateAryaSamajUseCase.kt` - Business logic + validation
    - [ ] `DeleteAryaSamajUseCase.kt` - Business logic for deletion
    - [ ] `GetAryaSamajsUseCase.kt` - Business logic for listing

### 4.3 Validation Logic
- [ ] Implement input validation in use cases
- [ ] Add business rule validations (e.g., family head constraints)
- [ ] Create validation error messages in Hindi

### 4.4 Response/Result Wrappers
- [ ] Create consistent `Result<T>` wrapper for all operations
- [ ] Implement `LoadState` sealed class for loading states
- [ ] Add proper error handling with localized messages

**Completion Criteria**: All domain models and use cases implemented with proper validation

---

## Phase 5: ViewModel Implementation
**Status**: ⏳ **PENDING**  
**Estimated Time**: 4-5 hours  
**Priority**: HIGH (State Management)

### 5.1 Family ViewModel
- [ ] **FamilyViewModel** (`features/family/viewmodel/FamilyViewModel.kt`)
    - [ ] `StateFlow<List<Family>> families`
    - [ ] `StateFlow<LoadState> loadState`
    - [ ] `StateFlow<String?> errorMessage`
    - [ ] `fun createFamily(request: CreateFamilyRequest)`
    - [ ] `fun updateFamily(id: String, request: UpdateFamilyRequest)`
    - [ ] `fun deleteFamily(id: String)`
    - [ ] `fun loadFamilies()`

- [ ] **Family Form ViewModel** (`features/family/viewmodel/FamilyFormViewModel.kt`)
    - [ ] Form state management with `rememberSaveable()`
    - [ ] Input validation
    - [ ] Form submission handling
    - [ ] Error state management

### 5.2 Member ViewModel
- [ ] **MemberViewModel** (`features/member/viewmodel/MemberViewModel.kt`)
    - [ ] `StateFlow<List<Member>> members`
    - [ ] `StateFlow<LoadState> loadState`
    - [ ] Pagination support with `loadNextPage()`
    - [ ] Search/filter functionality
    - [ ] CRUD operations

- [ ] **Member Form ViewModel** (`features/member/viewmodel/MemberFormViewModel.kt`)
    - [ ] Complex form state for all member fields
    - [ ] Address component integration
    - [ ] Image picker integration
    - [ ] Validation logic

### 5.3 Arya Samaj ViewModel
- [ ] **AryaSamajViewModel** (`features/aryasamaj/viewmodel/AryaSamajViewModel.kt`)
    - [ ] `StateFlow<List<AryaSamaj>> aryaSamajs`
    - [ ] `StateFlow<LoadState> loadState`
    - [ ] CRUD operations
    - [ ] Media URL management

- [ ] **Arya Samaj Form ViewModel** (`features/aryasamaj/viewmodel/AryaSamajFormViewModel.kt`)
    - [ ] Form state with address integration
    - [ ] Media upload handling
    - [ ] Member assignment logic

### 5.4 Update Dependency Injection
- [ ] Update `ViewModelModule.kt` with new ViewModels
- [ ] Ensure proper ViewModel factory setup
- [ ] Test ViewModel injection works correctly

### 5.5 ViewModel Testing
- [ ] Create test files in `commonTest`
- [ ] Write unit tests for all ViewModels using `kotlinx.coroutines.test`
- [ ] Mock repositories for isolated testing
- [ ] Test error scenarios and edge cases

**Completion Criteria**: All ViewModels implemented with comprehensive state management and testing

---

## Phase 6: UI Implementation
**Status**: ⏳ **PENDING**  
**Estimated Time**: 6-8 hours  
**Priority**: MEDIUM (User Interface)

### 6.1 Family UI Components
- [ ] **Family List Screen** (`features/family/ui/FamilyListScreen.kt`)
    - [ ] Adaptive layout (mobile/tablet/desktop)
    - [ ] LazyColumn with family cards
    - [ ] Pull-to-refresh functionality
    - [ ] Search/filter UI
    - [ ] Floating Action Button for adding families

- [ ] **Family Detail Screen** (`features/family/ui/FamilyDetailScreen.kt`)
    - [ ] Family information display
    - [ ] Family member list
    - [ ] Edit/Delete actions
    - [ ] Photo gallery view

- [ ] **Family Form Screen** (`features/family/ui/FamilyFormScreen.kt`)
    - [ ] Form fields using existing `FormComponents.kt`
    - [ ] Head member selection
    - [ ] Address integration using `AddressComponent.kt`
    - [ ] Photo picker integration using `ImagePickerComponent.kt`
    - [ ] Validation error display

### 6.2 Member UI Components
- [ ] **Member List Screen** (`features/member/ui/MemberListScreen.kt`)
    - [ ] Adaptive grid layout
    - [ ] Member cards with photos
    - [ ] Pagination handling
    - [ ] Search functionality
    - [ ] Filter by Arya Samaj/Family

- [ ] **Member Detail Screen** (`features/member/ui/MemberDetailScreen.kt`)
    - [ ] Complete member profile
    - [ ] Address display
    - [ ] Family relationships
    - [ ] Arya Samaj associations

- [ ] **Member Form Screen** (`features/member/ui/MemberFormScreen.kt`)
    - [ ] Comprehensive member form
    - [ ] Address component integration
    - [ ] Profile image picker
    - [ ] Date pickers for DOB/joining date
    - [ ] Dropdown for gender/occupation

### 6.3 Arya Samaj UI Components
- [ ] **Arya Samaj List Screen** (`features/aryasamaj/ui/AryaSamajListScreen.kt`)
    - [ ] Card-based layout with images
    - [ ] Location information
    - [ ] Member count display
    - [ ] Search by location/name

- [ ] **Arya Samaj Detail Screen** (`features/aryasamaj/ui/AryaSamajDetailScreen.kt`)
    - [ ] Complete Samaj information
    - [ ] Address and map integration
    - [ ] Photo gallery
    - [ ] Associated members list

- [ ] **Arya Samaj Form Screen** (`features/aryasamaj/ui/AryaSamajFormScreen.kt`)
    - [ ] Samaj information form
    - [ ] Address component integration
    - [ ] Media upload component
    - [ ] Member assignment interface

### 6.4 Common UI Components
- [ ] **Delete Confirmation Dialog** - Reusable deletion confirmation
- [ ] **Loading States** - Consistent loading indicators
- [ ] **Error States** - Error display with retry options
- [ ] **Success Messages** - Hindi success notifications using SnackBar

### 6.5 Responsive Design
- [ ] Test all screens on mobile viewport
- [ ] Test all screens on tablet viewport
- [ ] Test all screens on desktop viewport
- [ ] Ensure proper navigation on all screen sizes
- [ ] Verify Material 3 design compliance

**Completion Criteria**: All UI screens implemented with proper responsive design and Hindi localization

---

## Phase 7: Navigation & Integration
**Status**: ⏳ **PENDING**  
**Estimated Time**: 2-3 hours  
**Priority**: HIGH (App Integration)

### 7.1 Update Navigation System
- [ ] **Update Screen.kt** with new screen definitions:
    - [ ] `FamilyList`, `FamilyDetail`, `FamilyForm`
    - [ ] `MemberList`, `MemberDetail`, `MemberForm`
    - [ ] `AryaSamajList`, `AryaSamajDetail`, `AryaSamajForm`

- [ ] **Update RootNavGraph.kt** with new routes:
    - [ ] Add all new screen composables
    - [ ] Configure navigation arguments
    - [ ] Set up proper back stack handling

### 7.2 Update App Drawer
- [ ] **Update AppDrawer.kt** with new menu items:
    - [ ] "परिवार प्रबंधन" (Family Management)
    - [ ] "सदस्य प्रबंधन" (Member Management)
    - [ ] "आर्य समाज प्रबंधन" (Arya Samaj Management)

- [ ] Add proper icons for new menu items
- [ ] Test drawer navigation to new screens

### 7.3 Deep Linking Support
- [ ] Configure deep links for entity detail screens
- [ ] Test navigation from notifications
- [ ] Handle navigation state persistence

### 7.4 Navigation Testing
- [ ] Test all navigation paths
- [ ] Verify back button behavior
- [ ] Test navigation on all platforms (Android/iOS/Desktop/Web)

**Completion Criteria**: Seamless navigation between all new and existing screens

---

## Phase 8: Testing & Quality Assurance
**Status**: ⏳ **PENDING**  
**Estimated Time**: 3-4 hours  
**Priority**: HIGH (Quality)

### 8.1 Unit Testing
- [ ] **Repository Tests** (`commonTest`)
    - [ ] Test all CRUD operations
    - [ ] Test error handling scenarios
    - [ ] Mock Apollo client responses

- [ ] **ViewModel Tests** (`commonTest`)
    - [ ] Test state transitions
    - [ ] Test error states
    - [ ] Test form validation
    - [ ] Use `kotlinx.coroutines.test` for async testing

- [ ] **Use Case Tests** (`commonTest`)
    - [ ] Test business logic validation
    - [ ] Test error scenarios
    - [ ] Mock repository dependencies

### 8.2 Integration Testing
- [ ] **End-to-End Workflows**
    - [ ] Test complete family creation workflow
    - [ ] Test member creation with family assignment
    - [ ] Test Arya Samaj creation with member assignment

- [ ] **Error Handling Testing**
    - [ ] Test network error scenarios
    - [ ] Test validation error display
    - [ ] Test error message localization

### 8.3 UI Testing
- [ ] **Compose UI Tests** (`androidInstrumentedTest`)
    - [ ] Test form interactions
    - [ ] Test list scrolling and pagination
    - [ ] Test navigation flows

### 8.4 Manual Testing
- [ ] Test on Android device/emulator
- [ ] Test on iOS simulator (if Mac available)
- [ ] Test desktop application
- [ ] Test web application (WASM)
- [ ] Test different screen sizes and orientations

### 8.5 Performance Testing
- [ ] Test large data sets (100+ families/members)
- [ ] Test pagination performance
- [ ] Monitor memory usage
- [ ] Test image loading performance

**Completion Criteria**: All tests passing with comprehensive coverage

---

## Phase 9: Documentation & Polish
**Status**: ⏳ **PENDING**  
**Estimated Time**: 2-3 hours  
**Priority**: MEDIUM (Documentation)

### 9.1 Code Documentation
- [ ] Add KDoc comments to all public APIs
- [ ] Document complex business logic
- [ ] Add usage examples for components

### 9.2 User Documentation
- [ ] Create user guide for family management
- [ ] Create user guide for member management
- [ ] Create user guide for Arya Samaj management
- [ ] Add screenshots to documentation

### 9.3 Developer Documentation
- [ ] Update README with new features
- [ ] Document API integration patterns
- [ ] Create troubleshooting guide

### 9.4 Final Polish
- [ ] Review all Hindi text for accuracy
- [ ] Ensure consistent UI styling
- [ ] Optimize performance where needed
- [ ] Add loading animations/transitions

**Completion Criteria**: Complete, polished features with comprehensive documentation

---

## Progress Tracking

### Completed Phases

- **Phase 1**: ✅ GraphQL Schema & Operations Setup (2 hours)
- **Phase 2**: ✅ Localization Infrastructure (1 hour)

### Current Phase

- **Phase 3**: Repository Layer Implementation

### Next Planned Phase

- **Phase 4**: Domain Layer & Models

### Issues/Blockers
- None currently identified

### Time Tracking
- **Total Estimated Time**: 25-30 hours
- **Time Spent**: 3 hours
- **Remaining Time**: 22-27 hours

---

## Notes & Decisions

### Technical Decisions
- Using existing FormComponents.kt for consistency
- Leveraging AddressComponent.kt for address handling
- Using ImagePickerComponent.kt for media uploads
- Following existing repository pattern with Flow/StateFlow
- **Phase 1 Approach**: Created simplified GraphQL mutations using existing table operations; comprehensive CRUD
  functions will be called via `supabase.rpc()` in repository layer

### UI/UX Decisions

- All user-facing text in Hindi (Devanagari script)
- Material 3 design system compliance
- Responsive design for all screen sizes
- Consistent error handling and success messaging

### Future Enhancements
- Real-time synchronization using Supabase Realtime
- Offline support with local caching
- Advanced search and filtering capabilities
- Bulk operations for mass data management
- Export functionality for reports

---

**Last Updated**: June 27, 2025  
**Next Review**: After Phase 3 completion

