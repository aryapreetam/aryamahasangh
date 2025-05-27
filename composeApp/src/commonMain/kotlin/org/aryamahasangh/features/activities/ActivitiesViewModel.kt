package org.aryamahasangh.features.activities

import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.aryamahasangh.type.ActivitiesInsertInput
import org.aryamahasangh.type.Gender_filter
import org.aryamahasangh.util.Result
import org.aryamahasangh.viewmodel.AdmissionFormSubmissionState
import org.aryamahasangh.viewmodel.BaseViewModel

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

  // Separate state for activity details
  private val _activityDetailUiState = MutableStateFlow(ActivityDetailUiState())
  val activityDetailUiState: StateFlow<ActivityDetailUiState> = _activityDetailUiState.asStateFlow()

  // Separate state for form submission
  private val _formSubmissionState = MutableStateFlow(AdmissionFormSubmissionState())
  val activityFormSubmissionState: StateFlow<AdmissionFormSubmissionState> = _formSubmissionState.asStateFlow()

  // Separate state for organizations and members
  private val _organisationsAndMembersState = MutableStateFlow(OrganisationsAndMembersUiState())
  val organisationsAndMembersState: StateFlow<OrganisationsAndMembersUiState> = _organisationsAndMembersState.asStateFlow()

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
            updateState { it.copy(
              activities = result.data,
              isLoading = false,
              error = null
            )}
          }
          is Result.Error -> {
            updateState { it.copy(
              isLoading = false,
              error = result.message
            )}
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

      when (val result = activityRepository.getActivityDetail(id)) {
        is Result.Success -> {
          _activityDetailUiState.value = ActivityDetailUiState(
            activity = result.data,
            isLoading = false,
            error = null
          )
        }
        is Result.Error -> {
          _activityDetailUiState.value = ActivityDetailUiState(
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
   * Create a new activity
   */
  fun createActivity(input: ActivityInputData) {
    val activity = ActivitiesInsertInput(
      name = Optional.present(input.name),
      type = Optional.present(input.type.name),
      short_description = Optional.present(input.shortDescription),
      long_description = Optional.present(input.longDescription),
      address = Optional.present(input.address),
      state = Optional.present(input.state),
      district = Optional.present(input.district),
      start_datetime = Optional.present(input.startDatetime.convertToInstant()),
      end_datetime = Optional.present(input.endDatetime.convertToInstant()),
      media_files= Optional.present(input.mediaFiles),
      additional_instructions = Optional.present(input.additionalInstructions),
      capacity = Optional.present(input.capacity),
      latitude = Optional.present(input.latitude),
      longitude = Optional.present(input.longitude),
      allowed_gender = Optional.present(Gender_filter.valueOf(input.allowedGender)),
    )
    launch {
      _formSubmissionState.value = AdmissionFormSubmissionState(isSubmitting = true)

      when (val result = activityRepository.createActivity(activity, input.contactPeople, input.associatedOrganisations)) {
        is Result.Success -> {
          _formSubmissionState.value = AdmissionFormSubmissionState(
            isSubmitting = false,
            isSuccess = result.data,
            error = null
          )
          // Reload activities to include the new one
          loadActivities()
        }
        is Result.Error -> {
          _formSubmissionState.value = AdmissionFormSubmissionState(
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

  /**
   * Load organizations and members from the repository
   */
  fun loadOrganisationsAndMembers() {
    launch {
      _organisationsAndMembersState.value = OrganisationsAndMembersUiState(isLoading = true)

      when (val result = activityRepository.getOrganisationsAndMembers()) {
        is Result.Success -> {
          _organisationsAndMembersState.value = OrganisationsAndMembersUiState(
            organisations = result.data.organisations ?: emptyList(),
            members = result.data.members ?: emptyList(),
            isLoading = false,
            error = null
          )
        }
        is Result.Error -> {
          _organisationsAndMembersState.value = OrganisationsAndMembersUiState(
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
}

fun LocalDateTime.convertToInstant(): Instant {
  return toInstant(TimeZone.currentSystemDefault())
}