package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.AddOrganisationActivityMutation
import org.aryamahasangh.DeleteActivityMutation
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.OrganisationalActivityDetailQuery
import org.aryamahasangh.OrganisationsAndMembersQuery
import org.aryamahasangh.type.OrganisationActivityInput
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling activity-related operations
 */
interface ActivityRepository {
  /**
   * Get all activities
   */
  fun getActivities(): Flow<Result<List<OrganisationalActivitiesQuery.OrganisationalActivity>>>

  /**
   * Get activity details by ID
   */
  suspend fun getActivityDetail(id: String): Result<OrganisationalActivityDetailQuery.OrganisationalActivity>

  /**
   * Delete an activity by ID
   */
  suspend fun deleteActivity(id: String): Result<Boolean>

  /**
   * Create a new activity
   */
  suspend fun createActivity(input: OrganisationActivityInput): Result<Boolean>

  /**
   * Get organizations and members
   */
  suspend fun getOrganisationsAndMembers(): Result<OrganisationsAndMembersQuery.Data>
}

/**
 * Implementation of ActivityRepository that uses Apollo GraphQL client
 */
class ActivityRepositoryImpl(private val apolloClient: ApolloClient) : ActivityRepository {

  override fun getActivities(): Flow<Result<List<OrganisationalActivitiesQuery.OrganisationalActivity>>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(OrganisationalActivitiesQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.organisationalActivities ?: emptyList()
    }

    emit(result)
  }

  override suspend fun getActivityDetail(id: String): Result<OrganisationalActivityDetailQuery.OrganisationalActivity> {
    return safeCall {
      val response = apolloClient.query(OrganisationalActivityDetailQuery(id)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.organisationalActivity ?: throw Exception("Activity not found")
    }
  }

  override suspend fun deleteActivity(id: String): Result<Boolean> {
    return safeCall {
      val response = apolloClient.mutation(DeleteActivityMutation(id)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      true
    }
  }

  override suspend fun createActivity(input: OrganisationActivityInput): Result<Boolean> {
    return safeCall {
      val response = apolloClient.mutation(AddOrganisationActivityMutation(input)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      true
    }
  }

  override suspend fun getOrganisationsAndMembers(): Result<OrganisationsAndMembersQuery.Data> {
    return safeCall {
      val response = apolloClient.query(OrganisationsAndMembersQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data ?: throw Exception("No data returned")
    }
  }
}
