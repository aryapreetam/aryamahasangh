package org.aryamahasangh.features.activities

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.aryamahasangh.*
import org.aryamahasangh.fragment.OrganisationalActivityShort
import org.aryamahasangh.network.supabaseClient
import org.aryamahasangh.type.ActivitiesInsertInput
import org.aryamahasangh.type.ActivitiesUpdateInput
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall
import org.aryamahasangh.type.GenderFilter as ApolloGenderFilter

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
  ): Result<String>

  /**
   * Update an activity
   */
  suspend fun updateActivity(
    id: String,
    input: ActivitiesInsertInput,
    contactMembers: List<ActivityMember>,
    organisations: List<Organisation>
  ): Result<Boolean>

  /**
   * Get organizations and members
   */
  suspend fun getOrganisationsAndMembers(): Result<OrganisationsAndMembers>

  suspend fun getRegisteredUsers(id: String): Result<List<UserProfile>>

  /**
   * Listen to real-time registration updates for an activity
   */
  fun listenToRegistrations(activityId: String): Flow<List<UserProfile>>

  fun addActivityOverview(
    activityId: String,
    overview: String,
    mediaUrls: List<String> = emptyList()
  ): Flow<Result<Boolean>>
}

/**
 * Implementation of ActivityRepository that uses Apollo GraphQL client
 */
class ActivityRepositoryImpl(
  private val apolloClient: ApolloClient
) : ActivityRepository {
  override fun getActivities(): Flow<Result<List<OrganisationalActivityShort>>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(OrganisationalActivitiesQuery()).execute()
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }
          response.data?.activitiesCollection?.edges?.map { it.node.organisationalActivityShort }
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
      response.data?.deleteFromActivitiesCollection?.affectedCount!! > 0
    }
  }

  override suspend fun createActivity(
    activityInputData: ActivitiesInsertInput,
    activityMembers: List<ActivityMember>,
    associatedOrganisations: List<Organisation>
  ): Result<String> {
    return safeCall {
      val response = apolloClient.mutation(AddOrganisationActivityMutation(activityInputData)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      if (response.data?.insertIntoActivitiesCollection?.affectedCount!! > 0) {
        val activityId = response.data?.insertIntoActivitiesCollection?.records?.first()?.activityFields?.id!!
        val organisations =
          associatedOrganisations.map {
            OrganisationalActivityInsertData(organisationId = it.id, activityId = activityId)
          }
        try {
          supabaseClient.from("organisational_activity").insert(organisations)
          val members =
            activityMembers.map {
              ActivityMemberInsertData(
                activityId = activityId,
                memberId = it.member.id,
                post = it.post,
                priority = it.priority
              )
            }
          supabaseClient.from("activity_member").insert(members)
        } catch (e: Exception) {
          // FIXME notify about the error to the user
          throw Exception("Unknown error occurred ${e.message}")
        }
        return@safeCall activityId
      }
      throw Exception("Failed to create activity")
    }
  }

  override suspend fun updateActivity(
    id: String,
    input: ActivitiesInsertInput,
    contactMembers: List<ActivityMember>,
    organisations: List<Organisation>
  ): Result<Boolean> {
    return try {
      // Create update input using Optional fields
      val updateInput =
        ActivitiesUpdateInput(
          name = input.name,
          type = input.type,
          shortDescription = input.shortDescription,
          longDescription = input.longDescription,
          address = input.address,
          state = input.state,
          district = input.district,
          startDatetime = input.startDatetime,
          endDatetime = input.endDatetime,
          mediaFiles = input.mediaFiles,
          additionalInstructions = input.additionalInstructions,
          capacity = input.capacity,
          latitude = input.latitude,
          longitude = input.longitude,
          allowedGender = input.allowedGender
        )

      // Update the activity
      val updateMutation =
        UpdateActivityMutation(
          id = id,
          input = updateInput
        )

      val updateResponse = apolloClient.mutation(updateMutation).execute()
      if (updateResponse.hasErrors()) {
        return Result.Error(updateResponse.errors?.first()?.message ?: "Failed to update activity")
      }

      // Delete existing associations
      supabaseClient.from("organisational_activity")
        .delete {
          filter {
            eq("activityId", id)
          }
        }

      supabaseClient.from("activity_member")
        .delete {
          filter {
            eq("activityId", id)
          }
        }

      // Add new associations
      val organisationData =
        organisations.map {
          OrganisationalActivityInsertData(
            activityId = id,
            organisationId = it.id
          )
        }

      if (organisationData.isNotEmpty()) {
        supabaseClient.from("organisational_activity")
          .insert(organisationData)
      }

      val contactData =
        contactMembers.map {
          ActivityMemberInsertData(
            activityId = id,
            memberId = it.member.id,
            post = it.post,
            priority = it.priority
          )
        }

      if (contactData.isNotEmpty()) {
        supabaseClient.from("activity_member")
          .insert(contactData)
      }

      Result.Success(true)
    } catch (e: Exception) {
      Result.Error(e.message ?: "Failed to update activity")
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
                profileImage = it.node.profileImage ?: ""
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
      response.data?.satrRegistrationCollection?.edges?.map {
        UserProfile(
          id = it.node.id as String,
          fullname = it.node.fullname!!,
          mobile = it.node.mobile ?: "",
          gender = (it.node.gender ?: ApolloGenderFilter.ANY).toString(),
          address = it.node.address ?: ""
        )
      }!!
    }
  }

  @OptIn(SupabaseExperimental::class)
  override fun listenToRegistrations(activityId: String): Flow<List<UserProfile>> {
    return supabaseClient
      .from("satr_registration")
      .selectAsFlow(
        primaryKeys = listOf(SatrRegistration::id),
        filter = FilterOperation("activity_id", FilterOperator.EQ, activityId)
      ).map { registrations ->
        // Convert to UserProfile - no need to filter since it's already filtered at DB level
        registrations.map { registration ->
          UserProfile(
            id = registration.id,
            fullname = registration.fullname,
            mobile = registration.mobile,
            gender = registration.gender,
            address = registration.address
          )
        }
      }
  }

  override fun addActivityOverview(
    activityId: String,
    overview: String,
    mediaUrls: List<String>
  ): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              AddActivityOverviewMutation(
                activityId,
                overview,
                Optional.present(mediaUrls)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          response.data?.updateActivitiesCollection?.affectedCount!! > 0
        }
      emit(result)
    }
  }
}

@Serializable
data class SatrRegistration(
  val id: String,
  @SerialName("activity_id")
  val activityId: String,
  val fullname: String,
  val mobile: String,
  val gender: String,
  val address: String
)

@Serializable
data class ActivityMemberInsertData(
  @SerialName("activity_id")
  val activityId: String,
  @SerialName("member_id")
  val memberId: String,
  val post: String = "",
  val priority: Int = 1
)

@Serializable
data class OrganisationalActivityInsertData(
  @SerialName("organisation_id")
  val organisationId: String,
  @SerialName("activity_id")
  val activityId: String
)
