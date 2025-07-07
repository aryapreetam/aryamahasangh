package com.aryamahasangh.features.activities

import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.aryamahasangh.features.admin.PaginationResult
import com.aryamahasangh.features.admin.PaginationState
import com.aryamahasangh.fragment.ActivityWithStatus
import com.aryamahasangh.type.ActivitiesInsertInput
import com.aryamahasangh.util.Result
import com.aryamahasangh.viewmodel.AdmissionFormSubmissionState
import com.aryamahasangh.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * UI state for the Activities screen
 */
data class ActivitiesUiState(
  val activities: List<ActivityWithStatus> = emptyList(),
  val searchQuery: String = "",
  val paginationState: PaginationState<ActivityWithStatus> = PaginationState(),
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * UI state for the Activity Detail screen
 */
data class ActivityDetailUiState(
  val activity: OrganisationalActivity? = null,
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * UI state for form submission
 */
data class FormSubmissionState(
  val isSubmitting: Boolean = false,
  val isSuccess: Boolean = false,
  val error: String? = null
)

/**
 * UI state for organizations and members
 */
data class OrganisationsAndMembersUiState(
  val organisations: List<Organisation> = emptyList(),
  val members: List<Member> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * ViewModel for the Activities screen
 */
class ActivitiesViewModel(
  private val activityRepository: ActivityRepository
) : BaseViewModel<ActivitiesUiState>(ActivitiesUiState()) {

  // Direct state management for Activities (not using BaseViewModel's generic state)
  private val _activitiesUiState = MutableStateFlow(ActivitiesUiState())
  val activitiesUiState: StateFlow<ActivitiesUiState> = _activitiesUiState.asStateFlow()

  private val _registeredUsers = MutableStateFlow(emptyList<UserProfile>())
  val registeredUsers: StateFlow<List<UserProfile>> = _registeredUsers.asStateFlow()

  // Separate state for activity details
  private val _activityDetailUiState = MutableStateFlow(ActivityDetailUiState())
  val activityDetailUiState: StateFlow<ActivityDetailUiState> = _activityDetailUiState.asStateFlow()

  // Separate state for form submission
  private val _formSubmissionState = MutableStateFlow(AdmissionFormSubmissionState())
  val activityFormSubmissionState: StateFlow<AdmissionFormSubmissionState> = _formSubmissionState.asStateFlow()

  // Store the newly created activity ID
  private val _createdActivityId = MutableStateFlow<String?>(null)
  val createdActivityId: StateFlow<String?> = _createdActivityId.asStateFlow()

  // Separate state for organizations and members
  private val _organisationsAndMembersState = MutableStateFlow(OrganisationsAndMembersUiState())
  val organisationsAndMembersState: StateFlow<OrganisationsAndMembersUiState> =
    _organisationsAndMembersState.asStateFlow()

  // Track the current activity being viewed for real-time updates
  private var currentActivityId: String? = null
  private var registrationListenerJob: Job? = null

  // Store original activity data for smart updates
  private val _originalActivityData = MutableStateFlow<OrganisationalActivity?>(null)
  val originalActivityData: StateFlow<OrganisationalActivity?> = _originalActivityData.asStateFlow()

  // Add required properties and methods for pagination
  private var searchJob: Job? = null
  private var shouldPreservePagination = false

  init {
    loadActivitiesPaginated()
  }

  fun hasExistingActivityData(): Boolean {
    return _activitiesUiState.value.activities.isNotEmpty()
  }

  fun preserveActivityPagination(
    savedActivities: List<ActivityWithStatus>,
    savedPaginationState: PaginationState<ActivityWithStatus>
  ) {
    _activitiesUiState.value = _activitiesUiState.value.copy(
      activities = savedActivities,
      paginationState = savedPaginationState.copy(items = savedActivities) // Ensure consistency
    )
    shouldPreservePagination = true
  }

  fun loadActivitiesPaginated(pageSize: Int = 30, resetPagination: Boolean = false) {
    viewModelScope.launch {
      val currentState = _activitiesUiState.value.paginationState

      // Check if we should preserve existing data
      val shouldPreserveExistingData = shouldPreservePagination && resetPagination && hasExistingActivityData()

      if (shouldPreservePagination) {
        shouldPreservePagination = false
        if (shouldPreserveExistingData) {
          return@launch
        }
      }

      // Bounds checking
      if (!resetPagination && currentState.hasNextPage == false) {
        return@launch
      }

      val shouldReset = resetPagination
      val cursor = if (shouldReset) null else currentState.endCursor

      // Update loading states
      _activitiesUiState.value = _activitiesUiState.value.copy(
        paginationState = currentState.copy(
          isInitialLoading = shouldReset && currentState.items.isEmpty(),
          isLoadingNextPage = !shouldReset && currentState.items.isNotEmpty()
        )
      )

      activityRepository.getItemsPaginated(pageSize = pageSize, cursor = cursor).collect { result ->
        when (result) {
          is PaginationResult.Success -> {
            // Query Watchers prevent duplication - no distinctBy needed
            val existingActivities = if (shouldReset) emptyList() else currentState.items
            val newActivities = existingActivities + result.data

            _activitiesUiState.value = _activitiesUiState.value.copy(
              activities = newActivities,
              paginationState = currentState.copy(
                items = newActivities, // Keep UI and pagination state in sync
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
            _activitiesUiState.value = _activitiesUiState.value.copy(
              paginationState = currentState.copy(
                isInitialLoading = false,
                isLoadingNextPage = false,
                error = result.message,
                showRetryButton = true
              )
            )
          }

          is PaginationResult.Loading -> {
            // Loading state handled by pagination state
          }
        }
      }
    }
  }

  fun searchActivitiesWithDebounce(query: String) {
    _activitiesUiState.value = _activitiesUiState.value.copy(searchQuery = query)

    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      if (query.isBlank()) {
        loadActivitiesPaginated(resetPagination = true)
        return@launch
      }

      delay(1000) // 1 second debounce
      searchActivitiesPaginated(searchTerm = query.trim(), resetPagination = true)
    }
  }

  fun searchActivitiesPaginated(searchTerm: String, pageSize: Int = 30, resetPagination: Boolean = true) {
    viewModelScope.launch {
      val currentState = _activitiesUiState.value.paginationState
      val cursor = if (resetPagination) null else currentState.endCursor

      // Update loading states
      _activitiesUiState.value = _activitiesUiState.value.copy(
        paginationState = currentState.copy(
          isSearching = resetPagination,
          isLoadingNextPage = !resetPagination
        )
      )

      activityRepository.searchItemsPaginated(
        searchTerm = searchTerm,
        pageSize = pageSize,
        cursor = cursor
      ).collect { result ->
        when (result) {
          is PaginationResult.Success -> {
            val existingActivities = if (resetPagination) emptyList() else currentState.items
            val newActivities = existingActivities + result.data

            _activitiesUiState.value = _activitiesUiState.value.copy(
              activities = newActivities,
              paginationState = currentState.copy(
                items = newActivities,
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
            _activitiesUiState.value = _activitiesUiState.value.copy(
              paginationState = currentState.copy(
                isSearching = false,
                isLoadingNextPage = false,
                error = result.message,
                showRetryButton = true
              )
            )
          }

          is PaginationResult.Loading -> {
            // Loading state handled by pagination state
          }
        }
      }
    }
  }

  fun loadNextActivityPage() {
    val currentState = _activitiesUiState.value.paginationState
    if (currentState.hasNextPage && !currentState.isLoadingNextPage) {
      if (currentState.currentSearchTerm.isNotBlank()) {
        searchActivitiesPaginated(
          searchTerm = currentState.currentSearchTerm,
          resetPagination = false
        )
      } else {
        loadActivitiesPaginated(resetPagination = false)
      }
    }
  }

  fun retryActivityLoad() {
    val currentState = _activitiesUiState.value.paginationState
    _activitiesUiState.value = _activitiesUiState.value.copy(
      paginationState = currentState.copy(showRetryButton = false)
    )

    if (currentState.currentSearchTerm.isNotBlank()) {
      searchActivitiesPaginated(
        searchTerm = currentState.currentSearchTerm,
        resetPagination = currentState.items.isEmpty()
      )
    } else {
      loadActivitiesPaginated(resetPagination = currentState.items.isEmpty())
    }
  }

  fun calculatePageSize(screenWidthDp: Float): Int {
    return when {
      screenWidthDp < 600f -> 15      // Mobile portrait
      screenWidthDp < 840f -> 25      // Tablet, mobile landscape  
      else -> 35                      // Desktop, large tablets
    }
  }

  /**
   * Load activities from the repository (legacy method - kept for compatibility)
   */
  fun loadActivities() {
    launch {
      activityRepository.getActivities().collect { result ->
        when (result) {
          is Result.Loading -> {
            _activitiesUiState.value = _activitiesUiState.value.copy(isLoading = true, error = null)
          }

          is Result.Success -> {
            _activitiesUiState.value =
              _activitiesUiState.value.copy(
                isLoading = false,
                error = null
              )
          }

          is Result.Error -> {
            _activitiesUiState.value =
              _activitiesUiState.value.copy(
                isLoading = false,
                error = result.message
              )
          }
        }
      }
    }
  }

  /**
   * Delete an activity by ID
   */
  fun deleteActivity(id: String, onSuccess: (() -> Unit)? = null) {
    launch {
      when (val result = activityRepository.deleteActivity(id)) {
        is Result.Success -> {
          // Refresh the list
          loadActivitiesPaginated(resetPagination = true)
          onSuccess?.invoke()
        }
        is Result.Error -> {
          _activitiesUiState.value = _activitiesUiState.value.copy(error = result.message)
        }
        is Result.Loading -> {}
      }
    }
  }

  /**
   * Load activity details by ID
   */
  fun loadActivityDetail(id: String) {
    launch {
      _activityDetailUiState.value = ActivityDetailUiState(isLoading = true)

      // Clear previous registered users to avoid showing stale data
      _registeredUsers.value = emptyList()

      // Update current activity ID and start listening for real-time updates
      currentActivityId = id

      // Load registered users initially
      try {
        loadRegisteredUsers(id)
      } catch (e: Exception) {
        println("Error loading initial registered users: ${e.message}")
      }

      // Start listening for real-time updates
      startListeningForRegistrations(id)

      // Load activity detail
      launch {
        activityRepository.getActivityDetail(id).collect { result ->
          when (result) {
            is Result.Success -> {
              _activityDetailUiState.update {
                ActivityDetailUiState(
                  activity = result.data,
                  isLoading = false,
                  error = null
                )
              }
              _originalActivityData.value = result.data
            }

            is Result.Error -> {
              _activityDetailUiState.value =
                ActivityDetailUiState(
                  isLoading = false,
                  error = result.message
                )
            }

            is Result.Loading -> {
              _activityDetailUiState.value =
                ActivityDetailUiState(
                  isLoading = true,
                  error = null
                )
            }
          }
        }
      }
    }
  }

  /**
   * Create a new activity
   */
  fun createActivity(input: ActivityInputData) {
    val activity =
      ActivitiesInsertInput(
        name = Optional.present(input.name),
        type = Optional.present(input.type),
        shortDescription = Optional.present(input.shortDescription),
        longDescription = Optional.present(input.longDescription),
        address = Optional.present(input.address),
        state = Optional.present(input.state),
        district = Optional.present(input.district),
        startDatetime = Optional.present(input.startDatetime.convertToInstant()),
        endDatetime = Optional.present(input.endDatetime.convertToInstant()),
        mediaFiles = Optional.present(input.mediaFiles),
        additionalInstructions = Optional.present(input.additionalInstructions),
        capacity = Optional.present(input.capacity),
        latitude = Optional.present(input.latitude),
        longitude = Optional.present(input.longitude),
        allowedGender = Optional.present(input.allowedGender.toApollo()),
        overviewDescription = Optional.absent(),
        overviewMediaUrls = Optional.absent()
      )
    launch {
      _formSubmissionState.value = AdmissionFormSubmissionState(isSubmitting = true)

      when (
        val result =
          activityRepository.createActivity(
            activity,
            input.contactPeople,
            input.associatedOrganisations
          )
      ) {
        is Result.Success -> {
          _createdActivityId.value = result.data
          _formSubmissionState.value =
            AdmissionFormSubmissionState(
              isSubmitting = false,
              isSuccess = true,
              error = null
            )
          // Reload activities to include the new one
          loadActivitiesPaginated(resetPagination = true)
        }

        is Result.Error -> {
          _formSubmissionState.value =
            AdmissionFormSubmissionState(
              isSubmitting = false,
              isSuccess = false,
              error = result.message
            )
        }

        is Result.Loading -> {
          // This shouldn't happen with the current implementation
        }
      }
    }
  }

  /**
   * Update an existing activity
   */
  fun updateActivity(
    id: String,
    input: ActivityInputData
  ) {
    val activity =
      ActivitiesInsertInput(
        name = Optional.present(input.name),
        type = Optional.present(input.type),
        shortDescription = Optional.present(input.shortDescription),
        longDescription = Optional.present(input.longDescription),
        address = Optional.present(input.address),
        state = Optional.present(input.state),
        district = Optional.present(input.district),
        startDatetime = Optional.present(input.startDatetime.convertToInstant()),
        endDatetime = Optional.present(input.endDatetime.convertToInstant()),
        mediaFiles = Optional.present(input.mediaFiles),
        additionalInstructions = Optional.present(input.additionalInstructions),
        capacity = Optional.present(input.capacity),
        latitude = Optional.present(input.latitude),
        longitude = Optional.present(input.longitude),
        allowedGender = Optional.present(input.allowedGender.toApollo())
      )
    launch {
      _formSubmissionState.value = AdmissionFormSubmissionState(isSubmitting = true)

      when (
        val result =
          activityRepository.updateActivity(
            id,
            activity,
            input.contactPeople,
            input.associatedOrganisations
          )
      ) {
        is Result.Success -> {
          _formSubmissionState.value =
            AdmissionFormSubmissionState(
              isSubmitting = false,
              isSuccess = result.data,
              error = null
            )
          // Reload activities to reflect the update
          loadActivityDetail(id)
        }

        is Result.Error -> {
          _formSubmissionState.value =
            AdmissionFormSubmissionState(
              isSubmitting = false,
              isSuccess = false,
              error = result.message
            )
        }

        is Result.Loading -> {
          // This shouldn't happen with the current implementation
        }
      }
    }
  }

  /**
   * Smart update an existing activity using differential updates
   */
  fun updateActivitySmart(
    id: String,
    input: ActivityInputData
  ) {
    launch {
      _formSubmissionState.value = AdmissionFormSubmissionState(isSubmitting = true)

      when (
        val result = activityRepository.updateActivitySmart(
          activityId = id,
          originalActivity = _originalActivityData.value,
          newActivityData = input
        )
      ) {
        is Result.Success -> {
          _formSubmissionState.value =
            AdmissionFormSubmissionState(
              isSubmitting = false,
              isSuccess = result.data,
              error = null
            )
          // Reload activity detail to reflect the update
          loadActivityDetail(id)
        }

        is Result.Error -> {
          _formSubmissionState.value =
            AdmissionFormSubmissionState(
              isSubmitting = false,
              isSuccess = false,
              error = result.message
            )
        }

        is Result.Loading -> {
          // This shouldn't happen with the current implementation
        }
      }
    }
  }

  /**
   * Check if activity data has changed compared to original
   */
  fun hasActivityDataChanged(newData: ActivityInputData): Boolean {
    val original = _originalActivityData.value ?: return true

    return original.name != newData.name ||
      original.type.name != newData.type.name ||
      original.shortDescription != newData.shortDescription ||
      original.longDescription != newData.longDescription ||
      original.address != newData.address ||
      original.state != newData.state ||
      original.district != newData.district ||
      original.startDatetime != newData.startDatetime ||
      original.endDatetime != newData.endDatetime ||
      original.mediaFiles != newData.mediaFiles ||
      original.additionalInstructions != newData.additionalInstructions ||
      original.capacity != newData.capacity ||
      original.latitude != newData.latitude ||
      original.longitude != newData.longitude ||
      original.allowedGender != newData.allowedGender.name ||
      !areOrganisationsEqual(
        original.associatedOrganisations.map { it.organisation },
        newData.associatedOrganisations
      ) ||
      !areMembersEqual(original.contactPeople, newData.contactPeople)
  }

  /**
   * Compare organisations for equality
   */
  private fun areOrganisationsEqual(
    original: List<Organisation>,
    new: List<Organisation>
  ): Boolean {
    if (original.size != new.size) return false
    val originalIds = original.map { it.id }.toSet()
    val newIds = new.map { it.id }.toSet()
    return originalIds == newIds
  }

  /**
   * Compare members for equality
   */
  private fun areMembersEqual(
    original: List<ActivityMember>,
    new: List<ActivityMember>
  ): Boolean {
    if (original.size != new.size) return false

    // Create comparable lists
    val originalComparable = original.map { Triple(it.member.id, it.post, it.priority) }.sortedBy { it.first }
    val newComparable = new.map { Triple(it.member.id, it.post, it.priority) }.sortedBy { it.first }

    return originalComparable == newComparable
  }

  /**
   * Reset form submission state
   */
  fun resetFormSubmissionState() {
    _formSubmissionState.value = AdmissionFormSubmissionState()
  }

  suspend fun loadRegisteredUsers(id: String) {
    println("Loading registered users for activity: $id")
    when (val result = activityRepository.getRegisteredUsers(id)) {
      is Result.Success -> {
        println("Loaded ${result.data.size} registered users")
        _registeredUsers.value = result.data
      }

      is Result.Error -> {
        println("Error loading registered users: ${result.message}")
        // Do nothing
      }

      is Result.Loading -> {
        // Do nothing
      }
    }
  }

  /**
   * Load organizations and members from the repository
   */
  fun loadOrganisationsAndMembers() {
    launch {
      _organisationsAndMembersState.value = OrganisationsAndMembersUiState(isLoading = true)

      when (val result = activityRepository.getOrganisationsAndMembers()) {
        is Result.Success -> {
          _organisationsAndMembersState.value =
            OrganisationsAndMembersUiState(
              organisations = result.data.organisations ?: emptyList(),
              members = result.data.members ?: emptyList(),
              isLoading = false,
              error = null
            )
        }

        is Result.Error -> {
          _organisationsAndMembersState.value =
            OrganisationsAndMembersUiState(
              isLoading = false,
              error = result.message
            )
        }

        is Result.Loading -> {
          // This shouldn't happen with the current implementation
        }
      }
    }
  }

  /**
   * Start listening for real-time registration updates
   */
  private fun startListeningForRegistrations(activityId: String) {
    // Cancel previous listener if exists
    stopListeningForRegistrations()

    println("Starting real-time listening for activity: $activityId")

    registrationListenerJob =
      viewModelScope.launch {
        try {
          activityRepository.listenToRegistrations(activityId)
            .collect { users ->
              println("Real-time update: ${users.size} registered users")
              _registeredUsers.value = users
            }
        } catch (e: Exception) {
          println("Error in real-time listener: ${e.message}")
        }
      }

    println("Real-time listener job created: ${registrationListenerJob != null}")
  }

  /**
   * Add overview to an activity
   */
  fun addActivityOverview(
    activityId: String,
    description: String,
    mediaUrls: List<String>
  ) = activityRepository.addActivityOverview(activityId, description, mediaUrls)

  /**
   * Stop listening for real-time registration updates
   */
  fun stopListeningForRegistrations() {
    println("Stopping real-time listener for activity: $currentActivityId")
    registrationListenerJob?.cancel()
    registrationListenerJob = null
    currentActivityId = null
  }

  override fun onCleared() {
    stopListeningForRegistrations()
    super.onCleared()
  }
}

fun LocalDateTime.convertToInstant(): Instant {
  return toInstant(TimeZone.currentSystemDefault())
}
