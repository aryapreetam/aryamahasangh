package com.aryamahasangh.features.activities

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.cacheInfo
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.aryamahasangh.*
import com.aryamahasangh.fragment.OrganisationalActivityShort
import com.aryamahasangh.network.supabaseClient
import com.aryamahasangh.type.ActivitiesInsertInput
import com.aryamahasangh.type.ActivitiesUpdateInput
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
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
import kotlinx.serialization.json.Json
import com.aryamahasangh.type.GenderFilter as ApolloGenderFilter

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
  suspend fun getActivityDetail(id: String): Flow<Result<OrganisationalActivity>>

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
   * Smart update an activity using differential updates
   */
  suspend fun updateActivitySmart(
    activityId: String,
    originalActivity: OrganisationalActivity?,
    newActivityData: ActivityInputData
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

      apolloClient.query(OrganisationalActivitiesQuery())
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val cameFromEmptyCache =
            response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if(!cameFromEmptyCache) {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
              }
            }
            response.data?.activitiesCollection?.edges?.map { it.node.organisationalActivityShort }
              ?: emptyList()
          }
          emit(result)
        }
    }

  override suspend fun getActivityDetail(id: String): Flow<Result<OrganisationalActivity>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(OrganisationalActivityDetailByIdQuery(id))
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val cameFromEmptyCache =
            response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if(!cameFromEmptyCache) {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
              }
            }
            val activities = response.data?.activitiesCollection?.edges?.map {
              OrganisationalActivity.camelCased(it.node)
            }
            if (activities.isNullOrEmpty() && !cameFromEmptyCache) {
              throw Exception("Activity not found")
            }
            activities?.firstOrNull() ?: throw Exception("Activity not found")
          }
          if (!cameFromEmptyCache || (result is Result.Success)) {
            emit(result)
          }
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
            eq("activity_id", id)
          }
        }

      supabaseClient.from("activity_member")
        .delete {
          filter {
            eq("activity_id", id)
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

  override suspend fun updateActivitySmart(
    activityId: String,
    originalActivity: OrganisationalActivity?,
    newActivityData: ActivityInputData
  ): Result<Boolean> {
    return safeCall {
      // Build the update request with only changed fields
      val updateRequest = buildActivityUpdateRequest(
        activityId = activityId,
        originalActivity = originalActivity,
        newActivityData = newActivityData
      )

      // If no changes detected, return success without making API call
      if (isActivityUpdateRequestEmpty(updateRequest)) {
        return@safeCall true
      }

      // Convert to JSON string
      val requestJson = Json.encodeToString(updateRequest)

      // Execute the mutation
      val response = apolloClient.mutation(
        UpdateActivityDetailsSmartMutation(requestJson)
      ).execute()

      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }

      // Parse the response
      val responseData = response.data?.updateActivityDetails
      if (responseData == null) {
        throw Exception("No response data received")
      }

      // Parse the JSON response
      try {
        val jsonConfig = Json {
          ignoreUnknownKeys = true
          encodeDefaults = true
          isLenient = true
        }
        val jsonString = responseData.toString()
        val parsedResponse = jsonConfig.decodeFromString<ActivityUpdateResponse>(jsonString)

        if (parsedResponse.success) {
          true
        } else {
          throw Exception("Update failed: ${parsedResponse.error_code ?: "UNKNOWN_ERROR"}${parsedResponse.error_details?.let { " - $it" } ?: ""}")
        }
      } catch (e: Exception) {
        // Fallback: check for success in raw response string
        val responseString = responseData.toString()
        if (responseString.contains("\"success\":true")) {
          true
        } else {
          throw Exception("Activity update failed: $responseString")
        }
      }
    }
  }

  /**
   * Build update request with only changed fields
   */
  private fun buildActivityUpdateRequest(
    activityId: String,
    originalActivity: OrganisationalActivity?,
    newActivityData: ActivityInputData
  ): ActivityUpdateRequest {
    // If no original data, send all fields
    if (originalActivity == null) {
      return ActivityUpdateRequest(
        activityId = activityId,
        name = newActivityData.name,
        type = newActivityData.type.name,
        shortDescription = newActivityData.shortDescription,
        longDescription = newActivityData.longDescription,
        address = newActivityData.address,
        state = newActivityData.state,
        district = newActivityData.district,
        startDatetime = newActivityData.startDatetime.convertToInstant().toString(),
        endDatetime = newActivityData.endDatetime.convertToInstant().toString(),
        mediaFiles = newActivityData.mediaFiles,
        additionalInstructions = newActivityData.additionalInstructions,
        capacity = newActivityData.capacity,
        latitude = newActivityData.latitude,
        longitude = newActivityData.longitude,
        allowedGender = newActivityData.allowedGender.name,
        organisations = newActivityData.associatedOrganisations.map { it.id },
        members = newActivityData.contactPeople.map { member ->
          ActivityMemberUpdateRequest(
            memberId = member.member.id,
            post = member.post,
            priority = member.priority
          )
        }
      )
    }

    // Compare and include only changed fields
    return ActivityUpdateRequest(
      activityId = activityId,
      name = if (originalActivity.name != newActivityData.name) newActivityData.name else null,
      type = if (originalActivity.type.name != newActivityData.type.name) newActivityData.type.name else null,
      shortDescription = if (originalActivity.shortDescription != newActivityData.shortDescription) newActivityData.shortDescription else null,
      longDescription = if (originalActivity.longDescription != newActivityData.longDescription) newActivityData.longDescription else null,
      address = if (originalActivity.address != newActivityData.address) newActivityData.address else null,
      state = if (originalActivity.state != newActivityData.state) newActivityData.state else null,
      district = if (originalActivity.district != newActivityData.district) newActivityData.district else null,
      startDatetime = if (originalActivity.startDatetime != newActivityData.startDatetime.convertToInstant()) newActivityData.startDatetime.convertToInstant().toString() else null,
      endDatetime = if (originalActivity.endDatetime != newActivityData.endDatetime.convertToInstant()) newActivityData.endDatetime.convertToInstant().toString() else null,
      mediaFiles = if (originalActivity.mediaFiles != newActivityData.mediaFiles) newActivityData.mediaFiles else null,
      additionalInstructions = if (originalActivity.additionalInstructions != newActivityData.additionalInstructions) newActivityData.additionalInstructions else null,
      capacity = if (originalActivity.capacity != newActivityData.capacity) newActivityData.capacity else null,
      latitude = if (originalActivity.latitude != newActivityData.latitude) newActivityData.latitude else null,
      longitude = if (originalActivity.longitude != newActivityData.longitude) newActivityData.longitude else null,
      allowedGender = if (originalActivity.allowedGender != newActivityData.allowedGender.name) newActivityData.allowedGender.name else null,
      organisations = if (!areOrganisationsEqual(
          originalActivity.associatedOrganisations.map { it.organisation },
          newActivityData.associatedOrganisations
        )
      ) {
        newActivityData.associatedOrganisations.map { it.id }
      } else null,
      members = if (!areMembersEqual(originalActivity.contactPeople, newActivityData.contactPeople)) {
        newActivityData.contactPeople.map { member ->
          ActivityMemberUpdateRequest(
            memberId = member.member.id,
            post = member.post,
            priority = member.priority
          )
        }
      } else null
    )
  }

  /**
   * Check if update request has any changes
   */
  private fun isActivityUpdateRequestEmpty(request: ActivityUpdateRequest): Boolean {
    return request.name == null &&
      request.type == null &&
      request.shortDescription == null &&
      request.longDescription == null &&
      request.address == null &&
      request.state == null &&
      request.district == null &&
      request.startDatetime == null &&
      request.endDatetime == null &&
      request.mediaFiles == null &&
      request.additionalInstructions == null &&
      request.capacity == null &&
      request.latitude == null &&
      request.longitude == null &&
      request.allowedGender == null &&
      request.organisations == null &&
      request.members == null
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

  override suspend fun getOrganisationsAndMembers(): Result<OrganisationsAndMembers> {
    return safeCall {
      val response = apolloClient.query(OrganisationsAndMembersQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data.let { it ->
        OrganisationsAndMembers(
          members =
            it?.memberInOrganisationCollection?.edges?.map {
              Member(
                id = it.node.memberInOrganisationShort.id!!,
                name = it.node.memberInOrganisationShort.name!!,
                profileImage = it.node.memberInOrganisationShort.profileImage ?: ""
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

/**
 * Response type for smart activity updates
 */
@Serializable
data class ActivityUpdateResponse(
  val success: Boolean,
  val message_code: String? = null,
  val error_code: String? = null,
  val error_details: String? = null
)

/**
 * Data class for smart activity update requests
 */
@Serializable
data class ActivityUpdateRequest(
  @SerialName("activity_id")
  val activityId: String,
  val name: String? = null,
  val type: String? = null,
  @SerialName("shortDescription")
  val shortDescription: String? = null,
  @SerialName("longDescription")
  val longDescription: String? = null,
  val address: String? = null,
  val state: String? = null,
  val district: String? = null,
  @SerialName("startDatetime")
  val startDatetime: String? = null,
  @SerialName("endDatetime")
  val endDatetime: String? = null,
  @SerialName("mediaFiles")
  val mediaFiles: List<String>? = null,
  @SerialName("additionalInstructions")
  val additionalInstructions: String? = null,
  val capacity: Int? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  @SerialName("allowedGender")
  val allowedGender: String? = null,
  val organisations: List<String>? = null,
  val members: List<ActivityMemberUpdateRequest>? = null
)

/**
 * Data class for activity member update requests
 */
@Serializable
data class ActivityMemberUpdateRequest(
  @SerialName("memberId")
  val memberId: String,
  val post: String = "",
  val priority: Int = 1
)

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
