package org.aryamahasangh.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.OrganisationalActivityDetailQuery
import org.aryamahasangh.OrganisationsAndMembersQuery
import org.aryamahasangh.repository.ActivityRepository
import org.aryamahasangh.type.OrganisationActivityInput
import org.aryamahasangh.util.Result

/**
 * Use case for getting all activities with additional business logic
 */
class GetActivitiesUseCase(
    private val activityRepository: ActivityRepository
) {
    operator fun invoke(): Flow<Result<List<OrganisationalActivitiesQuery.OrganisationalActivity>>> {
        return activityRepository.getActivities()
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        // Apply business logic: sort activities by date (newest first)
                        val sortedActivities = result.data.sortedByDescending { 
                            it.createdAt ?: ""
                        }
                        Result.Success(sortedActivities)
                    }
                    else -> result
                }
            }
            .catch { exception ->
                emit(Result.Error("Failed to load activities: ${exception.message}", exception))
            }
    }
}

/**
 * Use case for getting activity details with validation
 */
class GetActivityDetailUseCase(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(id: String): Result<OrganisationalActivityDetailQuery.OrganisationalActivity> {
        return if (id.isBlank()) {
            Result.Error("Activity ID cannot be empty")
        } else {
            try {
                activityRepository.getActivityDetail(id)
            } catch (exception: Exception) {
                Result.Error("Failed to load activity details: ${exception.message}", exception)
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
                Result.Error("Failed to delete activity: ${exception.message}", exception)
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
    suspend operator fun invoke(input: OrganisationActivityInput): Result<Boolean> {
        return try {
            // Validate input
            val validationResult = validateActivityInput(input)
            if (validationResult != null) {
                return Result.Error(validationResult)
            }
            
            activityRepository.createActivity(input)
        } catch (exception: Exception) {
            Result.Error("Failed to create activity: ${exception.message}", exception)
        }
    }
    
    private fun validateActivityInput(input: OrganisationActivityInput): String? {
        return when {
            input.title.isNullOrBlank() -> "Activity title is required"
            input.title!!.length < 3 -> "Activity title must be at least 3 characters"
            input.title!!.length > 100 -> "Activity title must not exceed 100 characters"
            input.description.isNullOrBlank() -> "Activity description is required"
            input.description!!.length < 10 -> "Activity description must be at least 10 characters"
            input.description!!.length > 1000 -> "Activity description must not exceed 1000 characters"
            input.organisationId.isNullOrBlank() -> "Organisation ID is required"
            else -> null
        }
    }
}

/**
 * Use case for getting organizations and members with caching logic
 */
class GetOrganisationsAndMembersUseCase(
    private val activityRepository: ActivityRepository
) {
    private var cachedData: OrganisationsAndMembersQuery.Data? = null
    private var lastFetchTime: Long = 0
    private val cacheValidityDuration = 5 * 60 * 1000L // 5 minutes
    
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<OrganisationsAndMembersQuery.Data> {
        val currentTime = System.currentTimeMillis()
        
        // Return cached data if it's still valid and not forcing refresh
        if (!forceRefresh && cachedData != null && (currentTime - lastFetchTime) < cacheValidityDuration) {
            return Result.Success(cachedData!!)
        }
        
        return try {
            val result = activityRepository.getOrganisationsAndMembers()
            if (result is Result.Success) {
                cachedData = result.data
                lastFetchTime = currentTime
            }
            result
        } catch (exception: Exception) {
            Result.Error("Failed to load organizations and members: ${exception.message}", exception)
        }
    }
    
    fun clearCache() {
        cachedData = null
        lastFetchTime = 0
    }
}

/**
 * Combined use case for activity management operations
 */
class ActivityManagementUseCase(
    private val getActivitiesUseCase: GetActivitiesUseCase,
    private val getActivityDetailUseCase: GetActivityDetailUseCase,
    private val createActivityUseCase: CreateActivityUseCase,
    private val deleteActivityUseCase: DeleteActivityUseCase,
    private val getOrganisationsAndMembersUseCase: GetOrganisationsAndMembersUseCase
) {
    fun getActivities() = getActivitiesUseCase()
    
    suspend fun getActivityDetail(id: String) = getActivityDetailUseCase(id)
    
    suspend fun createActivity(input: OrganisationActivityInput) = createActivityUseCase(input)
    
    suspend fun deleteActivity(id: String) = deleteActivityUseCase(id)
    
    suspend fun getOrganisationsAndMembers(forceRefresh: Boolean = false) = 
        getOrganisationsAndMembersUseCase(forceRefresh)
    
    fun clearOrganisationsCache() = getOrganisationsAndMembersUseCase.clearCache()
}