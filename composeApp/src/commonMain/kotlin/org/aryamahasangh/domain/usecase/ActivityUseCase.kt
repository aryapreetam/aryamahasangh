package org.aryamahasangh.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.aryamahasangh.domain.error.ErrorHandler
import org.aryamahasangh.features.activities.ActivityRepository
import org.aryamahasangh.features.activities.OrganisationalActivity
import org.aryamahasangh.features.activities.OrganisationalActivityShort
import org.aryamahasangh.util.Result

/**
 * Use case for getting all activities with additional business logic
 */
class GetActivitiesUseCase(
  private val activityRepository: ActivityRepository
) {
  operator fun invoke(): Flow<Result<List<OrganisationalActivityShort>>> {
    return activityRepository.getActivities()
      .map { result ->
        when (result) {
          is Result.Success -> {
            // Apply business logic: sort activities by date (newest first)
            val sortedActivities =
              result.data.sortedByDescending {
                it.startDatetime.toString()
              }
            Result.Success(sortedActivities)
          }
          else -> result
        }
      }
      .catch { exception ->
        val appError = ErrorHandler.handleException(exception)
        emit(Result.Error(appError.message, exception))
      }
  }
}

/**
 * Use case for getting activity details with validation
 */
class GetActivityDetailUseCase(
  private val activityRepository: ActivityRepository
) {
  suspend operator fun invoke(id: String): Result<OrganisationalActivity> {
    return if (id.isBlank()) {
      Result.Error("Activity ID cannot be empty")
    } else {
      try {
        activityRepository.getActivityDetail(id)
      } catch (exception: Exception) {
        val appError = ErrorHandler.handleException(exception)
        Result.Error(appError.message, exception)
      }
    }
  }
}

/**
 * Use case for deleting an activity with validation
 */
class DeleteActivityUseCase(
  private val activityRepository: ActivityRepository
) {
  suspend operator fun invoke(id: String): Result<Boolean> {
    return if (id.isBlank()) {
      Result.Error("Activity ID cannot be empty")
    } else {
      try {
        activityRepository.deleteActivity(id)
      } catch (exception: Exception) {
        val appError = ErrorHandler.handleException(exception)
        Result.Error(appError.message, exception)
      }
    }
  }
}

/**
 * Use case for creating an activity with validation
 */
class CreateActivityUseCase(
  private val activityRepository: ActivityRepository
) {
//  suspend operator fun invoke(
//    activityInputData: ActivitiesInsertInput,
//    activityMembers: List<ActivityMember>,
//    associatedOrganisations: List<Organisation>
//  ): Result<Boolean> {
//    return try {
//      // Validate input
//      val validationResult = validateActivityInput(activityInputData)
//      if (validationResult != null) {
//        return Result.Error(validationResult)
//      }
//
//      activityRepository.createActivity(activityInputData, activityMembers, associatedOrganisations)
//    } catch (exception: Exception) {
//      val appError = ErrorHandler.handleException(exception)
//      Result.Error(appError.message, exception)
//    }
//  }

//  private fun validateActivityInput(input: ActivitiesInsertInput): String? {
//    return when {
//      input.name.getOrNull().isNullOrBlank() -> "Activity name is required"
//      (input.name.getOrNull()?.length ?: 0) < 3 -> "Activity name must be at least 3 characters"
//      (input.name.getOrNull()?.length ?: 0) > 100 -> "Activity name must not exceed 100 characters"
//      input.short_description.getOrNull().isNullOrBlank() -> "Activity description is required"
//      (input.short_description.getOrNull()?.length ?: 0) < 10 -> "Activity description must be at least 10 characters"
//      (input.short_description.getOrNull()?.length ?: 0) > 500 -> "Activity description must not exceed 500 characters"
//      input.type.getOrNull() == null -> "Activity type is required"
//      input.district.getOrNull().isNullOrBlank() -> "District is required"
//      input.start_datetime.getOrNull() == null -> "Start date and time is required"
//      input.end_datetime.getOrNull() == null -> "End date and time is required"
//      else -> null
//    }
//  }
//}
//
///**
// * Use case for getting organizations and members with caching logic
// */
//class GetOrganisationsAndMembersUseCase(
//  private val activityRepository: ActivityRepository
//) {
//  private var cachedData: OrganisationsAndMembers? = null
//  private var lastFetchTime: Long = 0
//  private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes
//
//  @OptIn(ExperimentalTime::class)
//  suspend operator fun invoke(forceRefresh: Boolean = false): Result<OrganisationsAndMembers> {
//    val currentTime = Clock.System.now().toEpochMilliseconds()
//
//    // Return cached data if it's still valid and not forcing refresh
//    if (!forceRefresh && cachedData != null && (currentTime - lastFetchTime) < cacheValidityDuration) {
//      return Result.Success(cachedData!!)
//    }
//
//    return try {
//      val result = activityRepository.getOrganisationsAndMembers()
//      if (result is Result.Success) {
//        cachedData = result.data
//        lastFetchTime = currentTime
//      }
//      result
//    } catch (exception: Exception) {
//      val appError = ErrorHandler.handleException(exception)
//      Result.Error(appError.message, exception)
//    }
//  }
//
//  fun clearCache() {
//    cachedData = null
//    lastFetchTime = 0
//  }
//}
//
///**
// * Combined use case for activity management operations
// */
//class ActivityManagementUseCase(
//  private val getActivitiesUseCase: GetActivitiesUseCase,
//  private val getActivityDetailUseCase: GetActivityDetailUseCase,
//  private val createActivityUseCase: CreateActivityUseCase,
//  private val deleteActivityUseCase: DeleteActivityUseCase,
//  private val getOrganisationsAndMembersUseCase: GetOrganisationsAndMembersUseCase
//) {
//  fun getActivities() = getActivitiesUseCase()
//
//  suspend fun getActivityDetail(id: String) = getActivityDetailUseCase(id)
//
//  suspend fun createActivity(
//    activityInputData: ActivitiesInsertInput,
//    activityMembers: List<ActivityMember>,
//    associatedOrganisations: List<Organisation>
//  ) = createActivityUseCase(activityInputData, activityMembers, associatedOrganisations)
//
//  suspend fun deleteActivity(id: String) = deleteActivityUseCase(id)
//
//  suspend fun getOrganisationsAndMembers(forceRefresh: Boolean = false) =
//    getOrganisationsAndMembersUseCase(forceRefresh)
//
//  fun clearOrganisationsCache() = getOrganisationsAndMembersUseCase.clearCache()
}
