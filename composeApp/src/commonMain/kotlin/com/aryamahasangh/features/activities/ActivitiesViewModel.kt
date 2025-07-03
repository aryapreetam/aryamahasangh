package com.aryamahasangh.features.activities

import androidx.lifecycle.viewModelScope
import com.apollographql.apollo.api.Optional
import com.aryamahasangh.fragment.OrganisationalActivityShort
import com.aryamahasangh.type.ActivitiesInsertInput
import com.aryamahasangh.util.Result
import com.aryamahasangh.viewmodel.AdmissionFormSubmissionState
import com.aryamahasangh.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
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
  val activities: List<OrganisationalActivityShort> = emptyList(),
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

  init {
    loadActivities()
  }

  /**
   * Load activities from the repository
   */
  fun loadActivities() {
    launch {
      activityRepository.getActivities().collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null) }
          }

          is Result.Success -> {
            updateState {
              it.copy(
                activities = result.data,
                isLoading = false,
                error = null
              )
            }
          }

          is Result.Error -> {
            updateState {
              it.copy(
                isLoading = false,
                error = result.message
              )
            }
          }
        }
      }
    }
  }

  /**
   * Delete an activity by ID
   */
  fun deleteActivity(id: String) {
    launch {
      val result = activityRepository.deleteActivity(id)
      when (result) {
        is Result.Success -> {
          // Remove the deleted activity from the list
          updateState { state ->
            state.copy(
              activities = state.activities.filter { it.id != id },
              error = null
            )
          }
        }

        is Result.Error -> {
          updateState { it.copy(error = result.message) }
        }

        is Result.Loading -> {
          // This shouldn't happen with the current implementation
        }
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
          loadActivities()
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
          loadActivities()
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
