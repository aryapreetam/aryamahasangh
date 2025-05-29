package org.aryamahasangh.features.activities

import com.apollographql.apollo.ApolloClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import org.aryamahasangh.*
import org.aryamahasangh.network.supabaseClient
import org.aryamahasangh.type.ActivitiesInsertInput
import org.aryamahasangh.type.Gender_filter
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling activity-related operations
 */
interface ActivityRepository {
  /**
   * Get all activities
   */
  fun getActivities(): Flow<Result<List<OrganisationalActivityShort>>>

  /**
   * Get activity details by ID
   */
  suspend fun getActivityDetail(id: String): Result<OrganisationalActivity>

  /**
   * Delete an activity by ID
   */
  suspend fun deleteActivity(id: String): Result<Boolean>

  /**
   * Create a new activity
   */
  suspend fun createActivity(
    activityInputData: ActivitiesInsertInput,
    activityMembers: List<ActivityMember>,
    associatedOrganisations: List<Organisation>
  ): Result<Boolean>

  /**
   * Get organizations and members
   */
  suspend fun getOrganisationsAndMembers(): Result<OrganisationsAndMembers>

  suspend fun getRegisteredUsers(id: String): Result<List<UserProfile>>
}

/**
 * Implementation of ActivityRepository that uses Apollo GraphQL client
 */
class ActivityRepositoryImpl(private val apolloClient: ApolloClient) : ActivityRepository {
  override fun getActivities(): Flow<Result<List<OrganisationalActivityShort>>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(OrganisationalActivitiesQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.activitiesCollection?.edges?.map { it.node.organisationalActivityShort.camelCased() }
            ?: emptyList()
        }

      emit(result)
    }

  override suspend fun getActivityDetail(id: String): Result<OrganisationalActivity> {
    return safeCall {
      val response = apolloClient.query(OrganisationalActivityDetailByIdQuery(id)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.activitiesCollection?.edges?.map {
        OrganisationalActivity.camelCased(it.node)
      }[0] ?: throw Exception("Activity not found")
    }
  }

  override suspend fun deleteActivity(id: String): Result<Boolean> {
    return safeCall {
      val response = apolloClient.mutation(DeleteActivityMutation(id)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.deleteFromactivitiesCollection?.affectedCount!! > 0
    }
  }

  override suspend fun createActivity(
    activityInputData: ActivitiesInsertInput,
    activityMembers: List<ActivityMember>,
    associatedOrganisations: List<Organisation>
  ): Result<Boolean> {
    return safeCall {
      val response = apolloClient.mutation(AddOrganisationActivityMutation(activityInputData)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      if (response.data?.insertIntoactivitiesCollection?.affectedCount!! > 0) {
        val activityId = response.data?.insertIntoactivitiesCollection?.records?.first()?.activityFields?.id!!
        val organisations =
          associatedOrganisations.map {
            OrganisationalActivityInsertData(organisation_id = it.id, activity_id = activityId)
          }
        try {
          supabaseClient.from("organisational_activity").insert(organisations)
          val members =
            activityMembers.map {
              ActivityMemberInsertData(
                activity_id = activityId,
                member_id = it.member.id,
                post = it.post,
                priority = it.priority
              )
            }
          supabaseClient.from("activity_member").insert(members)
        } catch (e: Exception) {
          // FIXME notify about the error to the user
          throw Exception("Unknown error occurred ${e.message}")
        }
      }
      true
    }
  }

  override suspend fun getOrganisationsAndMembers(): Result<OrganisationsAndMembers> {
    return safeCall {
      val response = apolloClient.query(OrganisationsAndMembersQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data.let {
        OrganisationsAndMembers(
          members =
            it?.memberCollection?.edges?.map {
              Member(
                id = it.node.id,
                name = it.node.name!!,
                profileImage = it.node.profile_image ?: ""
              )
            }!!,
          organisations =
            it.organisationCollection?.edges?.map {
              Organisation(
                id = it.node.id,
                name = it.node.name!!
              )
            }!!
        )
      }
    }
  }

  override suspend fun getRegisteredUsers(id: String): Result<List<UserProfile>> {
    return safeCall {
      val response = apolloClient.query(RegistrationsForActivityQuery(id)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.satr_registrationCollection?.edges?.map {
        UserProfile(
          id = it.node.id as String,
          fullname = it.node.fullname!!,
          mobile = it.node.mobile ?: "",
          gender = (it.node.gender ?: Gender_filter.any) as String,
          address = it.node.address ?: ""
        )
      }!!
    }
  }
}

@Serializable
data class OrganisationalActivityInsertData(
  val organisation_id: String,
  val activity_id: String
)

@Serializable
data class ActivityMemberInsertData(
  val activity_id: String,
  val member_id: String,
  val post: String = "",
  val priority: Int = 1
)
