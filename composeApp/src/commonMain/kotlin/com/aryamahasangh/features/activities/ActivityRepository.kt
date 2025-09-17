package com.aryamahasangh.features.activities

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.*
import com.apollographql.apollo.exception.CacheMissException
import com.aryamahasangh.*
import com.aryamahasangh.features.admin.PaginatedRepository
import com.aryamahasangh.features.admin.PaginationResult
import com.aryamahasangh.fragment.ActivityWithStatus
import com.aryamahasangh.fragment.OrganisationalActivityShort
import com.aryamahasangh.network.supabaseClient
import com.aryamahasangh.type.*
import com.aryamahasangh.util.GlobalMessageManager
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
interface ActivityRepository : PaginatedRepository<ActivityWithStatus> {
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
    associatedOrganisations: List<Organisation>,
    address: CreateAddressMutation.Builder
  ): Result<String>

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
    mediaUrls: List<String>
  ): Flow<Result<Boolean>>

  /**
   * Get activities with combined filter options and search
   */
  suspend fun getItemsPaginatedWithFilters(
    pageSize: Int,
    cursor: String?,
    searchTerm: String? = null,
    filterOptions: Set<ActivityFilterOption> = emptySet()
  ): Flow<PaginationResult<ActivityWithStatus>>
}

/**
 * Implementation of ActivityRepository that uses Apollo GraphQL client
 */
class ActivityRepositoryImpl(
  private val apolloClient: ApolloClient
) : ActivityRepository {

  override suspend fun getItemsPaginated(
    pageSize: Int,
    cursor: String?,
    filter: Any?
  ): Flow<PaginationResult<ActivityWithStatus>> = flow {
    emit(PaginationResult.Loading())

    apolloClient.query(
      GetActivitiesQuery(
        first = pageSize,
        after = Optional.presentIfNotNull(cursor),
        filter = Optional.presentIfNotNull(filter as? ActivitiesWithStatusFilter),
        orderBy = Optional.present(listOf(
          ActivitiesWithStatusOrderBy(statusPriority = Optional.present(OrderByDirection.AscNullsLast)),
          ActivitiesWithStatusOrderBy(startDatetime = Optional.present(OrderByDirection.AscNullsLast)),
          ActivitiesWithStatusOrderBy(id = Optional.present(OrderByDirection.AscNullsLast))
        ))
      )
    )
    .fetchPolicy(FetchPolicy.CacheAndNetwork)
    .watch() 
    .collect { response ->
      val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                     response.data?.activitiesWithStatusCollection?.edges.isNullOrEmpty()
      
      if (isCacheMissWithEmptyData) {
        return@collect
      }

      val result = safeCall {
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }

        val activities = response.data?.activitiesWithStatusCollection?.edges?.map {
          it.node.activityWithStatus
        } ?: emptyList()

        val pageInfo = response.data?.activitiesWithStatusCollection?.pageInfo

        PaginationResult.Success(
          data = activities,
          hasNextPage = pageInfo?.hasNextPage ?: false,
          endCursor = pageInfo?.endCursor
        )
      }

      when (result) {
        is Result.Success -> emit(result.data)
        is Result.Error -> emit(PaginationResult.Error(result.exception?.message ?: "Unknown error"))
        is Result.Loading -> {}
      }
    }
  }

  override suspend fun searchItemsPaginated(
    searchTerm: String,
    pageSize: Int,
    cursor: String?
  ): Flow<PaginationResult<ActivityWithStatus>> = flow {
    emit(PaginationResult.Loading())
    
    apolloClient.query(
      SearchActivitiesQuery(
        searchTerm = "%$searchTerm%",
        first = pageSize,
        after = Optional.presentIfNotNull(cursor)
      )
    )
    .fetchPolicy(FetchPolicy.CacheAndNetwork)
    .watch() 
    .collect { response ->
      val isCacheMissWithEmptyData = response.exception is CacheMissException && 
                                     response.data?.activitiesWithStatusCollection?.edges.isNullOrEmpty()
      
      if (isCacheMissWithEmptyData) {
        return@collect
      }

      val result = safeCall {
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }

        val activities = response.data?.activitiesWithStatusCollection?.edges?.map {
          it.node.activityWithStatus
        } ?: emptyList()

        val pageInfo = response.data?.activitiesWithStatusCollection?.pageInfo

        PaginationResult.Success(
          data = activities,
          hasNextPage = pageInfo?.hasNextPage ?: false,
          endCursor = pageInfo?.endCursor
        )
      }

      when (result) {
        is Result.Success -> emit(result.data)
        is Result.Error -> emit(PaginationResult.Error(result.exception?.message ?: "Unknown error"))
        is Result.Loading -> {}
      }
    }
  }

  override fun getActivities(): Flow<Result<List<OrganisationalActivityShort>>> =
    flow {
      emit(Result.Loading)

//      apolloClient.query(OrganisationalActivitiesQuery())
//        .fetchPolicy(FetchPolicy.CacheAndNetwork)
//        .toFlow()
//        .collect { response ->
//          val cameFromEmptyCache =
//            response.isFromCache && response.cacheInfo?.isCacheHit == false
//          val result = safeCall {
//            if(!cameFromEmptyCache) {
//              if (response.hasErrors()) {
//                throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
//              }
//            }
//            response.data?.activitiesCollection?.edges?.map { it.node.organisationalActivityShort }
//              ?: emptyList()
//          }
//          emit(result)
//        }
    }

  override suspend fun getActivityDetail(id: String): Flow<Result<OrganisationalActivity>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(OrganisationalActivityDetailByIdQuery(id))
        .fetchPolicy(FetchPolicy.NetworkFirst)
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
        val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
        // Check for database constraint violations
        if (errorMessage.contains("constraint", ignoreCase = true) ||
          errorMessage.contains("foreign key", ignoreCase = true) ||
          errorMessage.contains("violates", ignoreCase = true)
        ) {
          throw Exception("CONSTRAINT_VIOLATION: $errorMessage")
        }
        throw Exception(errorMessage)
      }

      val affectedCount = response.data?.deleteFromActivitiesCollection?.affectedCount ?: 0
      if (affectedCount == 0) {
        throw Exception("RECORD_NOT_FOUND: No activity found with the specified ID")
      }

      val success = affectedCount > 0
      if (success) {
        apolloClient.apolloStore.clearAll()
      }
      success
    }
  }

  override suspend fun createActivity(
    activityInputData: ActivitiesInsertInput,
    activityMembers: List<ActivityMember>,
    associatedOrganisations: List<Organisation>,
    address: CreateAddressMutation.Builder
  ): Result<String> {
    return safeCall {
      
      var inputWithAddressData = activityInputData
      address.pincode("")
      val addressMutation = address.build()
      if(addressMutation.state.isNotEmpty() && addressMutation.district.isNotEmpty()) {
        val addressResp = apolloClient.mutation(addressMutation).execute()
        if (addressResp.hasErrors()) {
          throw Exception(addressResp.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        if (addressResp.data?.insertIntoAddressCollection?.affectedCount!! != 1) {
          throw Exception(addressResp.errors?.firstOrNull()?.message ?: "Couldn't create address. please try again")
        }
        val addressId = addressResp.data?.insertIntoAddressCollection?.records?.first()?.id!!
        inputWithAddressData = activityInputData.copy(addressId = Optional.present(addressId))
      }
       
      val response = apolloClient.mutation(AddOrganisationActivityMutation(inputWithAddressData)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      if (response.data?.insertIntoActivitiesCollection?.affectedCount!! > 0) {
        val activityId = response.data?.insertIntoActivitiesCollection?.records?.first()?.id!!
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
          throw Exception("Unknown error occurred ${e.message}")
        }
        apolloClient.apolloStore.clearAll()
        return@safeCall activityId
      }
      throw Exception("Failed to create activity")
    }
  }

  override suspend fun updateActivitySmart(
    activityId: String,
    originalActivity: OrganisationalActivity?,
    newActivityData: ActivityInputData
  ): Result<Boolean> = safeCall {
    // Address mutation block
    val origAddr = originalActivity?.address
    val origAddrId = originalActivity?.addressId
    val newAddr = newActivityData.address
    var addressIdForUpdate = origAddrId

    // If adding a new address
    if (originalActivity?.addressId == null && (newActivityData.address.isNotBlank() || newActivityData.state.isNotBlank() || newActivityData.district.isNotBlank())) {
      println("Adding a new address")
      val resp = apolloClient.mutation(
            CreateAddressMutation.Builder()
              .basicAddress(newActivityData.address)
                .state(newActivityData.state)
                .district(newActivityData.district)
                .latitude(newActivityData.latitude ?: 0.0)
                .longitude(newActivityData.longitude ?: 0.0)
                .pincode("")
                .vidhansabha("")
                .build()
        ).execute()
        val newId = resp.data?.insertIntoAddressCollection?.records?.firstOrNull()?.id
        if (resp.hasErrors() || newId == null) {
            GlobalMessageManager.showError("पता जोड़ने में त्रुटि, पुनः प्रयास करें")
            return@safeCall false
        }
        apolloClient.mutation(AddAddressToActivityMutation(activityId, newId)).execute()
        addressIdForUpdate = newId
    }
    // If removing address
    else if (origAddr != null && (newActivityData.addressId == null || (newActivityData.address.isBlank() && newActivityData.state.isBlank() && newActivityData.district.isBlank()))) {
      println("Removing address")

      val removeAddressResp = apolloClient.mutation(RemoveAddressFromActivityMutation(activityId)).execute()

      // original addressId non-null guaranteed
        val delResp = apolloClient.mutation(DeleteAddressMutation(origAddrId!!)).execute()
      println(delResp)
        if (delResp.hasErrors()) {
          println("Address removal failed ${delResp.errors?.firstOrNull()?.message}")
          return@safeCall false
            GlobalMessageManager.showError("पता हटाने में त्रुटि, पुनः प्रयास करें")
            return@safeCall false
        }
        val deleteAddressSuccess = delResp.data?.deleteFromAddressCollection?.affectedCount ?: 0
      if(deleteAddressSuccess > 0){
        println("Address removed successfully")
      }else{
        println("Address removal failed")
      }

        addressIdForUpdate = null
    }
    // If address has changed
    else if (origAddr != null && newActivityData.addressId != null && (
        originalActivity.address != newActivityData.address ||
          originalActivity.state != newActivityData.state ||
          originalActivity.district != newActivityData.district ||
        (originalActivity.latitude ?: 0.0) != (newActivityData.latitude ?: 0.0) ||
        (originalActivity.longitude ?: 0.0) != (newActivityData.longitude ?: 0.0)
    )) {
      println("Updating address")
        // Only update changed address fields
        val updResp = apolloClient.mutation(
            UpdateAddressMutation.Builder()
                .id(origAddrId!!)
              .basicAddress(newActivityData.address)
                .state(newActivityData.state)
                .district(newActivityData.district)
                .latitude(newActivityData.latitude ?: 0.0)
                .longitude(newActivityData.longitude ?: 0.0)
                .pincode("")
                .vidhansabha("")
                .build()
        ).execute()
        if (updResp.hasErrors() || (updResp.data?.updateAddressCollection?.affectedCount ?: 0) == 0) {
            GlobalMessageManager.showError("पता अद्यतन करने में त्रुटि, पुनः प्रयास करें")
            return@safeCall false
        }
        // keep addressIdForUpdate as origAddrId
    }
    // Else, if no address at all (both null or empty), just leave addressIdForUpdate as is

    // Now perform the main update mutation with addressIdForUpdate set
    val updateRequest = buildActivityUpdateRequest(
        activityId = activityId,
        originalActivity = originalActivity,
        newActivityData = newActivityData.copy(addressId = addressIdForUpdate)
    )

    if (isActivityUpdateRequestEmpty(updateRequest)) {
        return@safeCall true
    }

    val requestJson = Json.encodeToString(updateRequest)
    val response = apolloClient.mutation(
        UpdateActivityDetailsSmartMutation(requestJson)
    ).execute()
    if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
    }
    val responseData = response.data?.updateActivityDetails
    if (responseData == null) {
        throw Exception("No response data received")
    }
    try {
        val jsonConfig = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
        val jsonString = responseData.toString()
        val parsedResponse = jsonConfig.decodeFromString<ActivityUpdateResponse>(jsonString)
        if (parsedResponse.success) {
            // After main activity update (and address if required) succeed:
            val originalMembers = originalActivity?.contactPeople ?: emptyList()
            val newMembers = newActivityData.contactPeople

            // Diff
            val toAdd = newMembers.filter { nm -> originalMembers.none { om -> om.member.id == nm.member.id } }
            val toRemove = originalMembers.filter { om -> newMembers.none { nm -> nm.member.id == om.member.id } }
            val toUpdate = newMembers.filter { nm ->
                originalMembers.any { om ->
                    om.member.id == nm.member.id && (om.post != nm.post || om.priority != nm.priority)
                }
            }

            //println("toAdd: $toAdd")
            //println("toRemove: $toRemove")
            //println("toUpdate: $toUpdate")

            // Add
            for (member in toAdd) {

                val input = ActivityMemberInsertInput(
                  activityId = Optional.present(originalActivity?.id),
                  memberId = Optional.present(member.member.id),
                  post = Optional.present(member.post),
                  priority = Optional.present(member.priority)
                )
                val resp = apolloClient.mutation(
                    AddActivityMemberMutation.Builder().input(input).build()
                ).execute()
              println(resp)
                if (resp.hasErrors() || (resp.data?.insertIntoActivityMemberCollection?.affectedCount ?: 0) <= 0) {
                    GlobalMessageManager.showError("संपर्क व्यक्ति जोड़ने में त्रुटि, पुनः प्रयास करें")
                    return@safeCall false
                }
            }
            // Remove
            for (member in toRemove) {
                // We need activityMemberId, assuming stored in member.id
                val resp = apolloClient.mutation(
                    RemoveActivityMemberMutation.Builder().activityMemberId(member.id).build()
                ).execute()
              println(resp)
                if (resp.hasErrors() || (resp.data?.deleteFromActivityMemberCollection?.affectedCount ?: 0) <= 0) {
                    GlobalMessageManager.showError("संपर्क व्यक्ति हटाने में त्रुटि, पुनः प्रयास करें")
                    return@safeCall false
                }
            }
            // Update (assuming member.id is activityMemberId for update, adjust if another id is needed)
            for (member in toUpdate) {
                val resp = apolloClient.mutation(
                    UpdateActivityMemberMutation.Builder()
                      .activityMemberId(member.id)
                      .activityMemberUpdateInput(
                        ActivityMemberUpdateInput(
                          post = Optional.present(member.post),
                          priority = Optional.present(member.priority)
                        )
                      )
                      .build()
                ).execute()
                if (resp.hasErrors() || (resp.data?.updateActivityMemberCollection?.affectedCount ?: 0) <= 0) {
                    GlobalMessageManager.showError("संपर्क व्यक्ति अद्यतन करने में त्रुटि, पुनः प्रयास करें")
                    return@safeCall false
                }
            }
            apolloClient.apolloStore.clearAll()
            true
        } else {
            throw Exception(
              ("Update failed: ${parsedResponse.error_code ?: "UNKNOWN_ERROR"}" +
                parsedResponse.error_details?.let { " - $it" }) ?: ""
            )
        }
    } catch (e: Exception) {
        println("Activity update failed: $e")
        val responseString = responseData.toString()
        if (responseString.contains("\"success\":true")) {
            apolloClient.apolloStore.clearAll()
            true
        } else {
            throw Exception("Activity update failed: $responseString")
        }
    }
  }

  private fun buildActivityUpdateRequest(
    activityId: String,
    originalActivity: OrganisationalActivity?,
    newActivityData: ActivityInputData
  ): ActivityUpdateRequest {
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

  private fun areOrganisationsEqual(
    original: List<Organisation>,
    new: List<Organisation>
  ): Boolean {
    if (original.size != new.size) return false
    val originalIds = original.map { it.id }.toSet()
    val newIds = new.map { it.id }.toSet()
    return originalIds == newIds
  }

  private fun areMembersEqual(
    original: List<ActivityMember>,
    new: List<ActivityMember>
  ): Boolean {
    if (original.size != new.size) return false
    
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

  override suspend fun getItemsPaginatedWithFilters(
    pageSize: Int,
    cursor: String?,
    searchTerm: String?,
    filterOptions: Set<ActivityFilterOption>
  ): Flow<PaginationResult<ActivityWithStatus>> = flow {
    emit(PaginationResult.Loading())

    // Build filter logic here
    val filter = buildFilter(searchTerm, filterOptions)
    apolloClient.query(
      GetActivitiesQuery(
        first = pageSize,
        after = Optional.presentIfNotNull(cursor),
        filter = Optional.presentIfNotNull(filter),
        orderBy = Optional.present(
          listOf(
            ActivitiesWithStatusOrderBy(statusPriority = Optional.present(OrderByDirection.AscNullsLast)),
            ActivitiesWithStatusOrderBy(startDatetime = Optional.present(OrderByDirection.AscNullsLast)),
            ActivitiesWithStatusOrderBy(id = Optional.present(OrderByDirection.AscNullsLast))
          )
        )
      )
    )
      .fetchPolicy(FetchPolicy.CacheAndNetwork)
      .watch()
      .collect { response ->
        val isCacheMissWithEmptyData = response.exception is CacheMissException &&
          response.data?.activitiesWithStatusCollection?.edges.isNullOrEmpty()

        if (isCacheMissWithEmptyData) {
          return@collect
        }

        val result = safeCall {
          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          val activities = response.data?.activitiesWithStatusCollection?.edges?.map {
            it.node.activityWithStatus
          } ?: emptyList()

          val pageInfo = response.data?.activitiesWithStatusCollection?.pageInfo

          PaginationResult.Success(
            data = activities,
            hasNextPage = pageInfo?.hasNextPage ?: false,
            endCursor = pageInfo?.endCursor
          )
        }

        when (result) {
          is Result.Success -> emit(result.data)
          is Result.Error -> emit(PaginationResult.Error(result.exception?.message ?: "Unknown error"))
          is Result.Loading -> {}
        }
      }
  }

  private fun buildFilter(searchTerm: String?, filterOptions: Set<ActivityFilterOption>): ActivitiesWithStatusFilter? {
    return buildCombinedFilter(filterOptions, searchTerm)
  }
}

@Serializable
data class ActivityUpdateResponse(
  val success: Boolean,
  val message_code: String? = null,
  val error_code: String? = null,
  val error_details: String? = null
)

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
