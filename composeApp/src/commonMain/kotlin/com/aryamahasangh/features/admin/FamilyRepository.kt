package com.aryamahasangh.features.admin

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.cacheInfo
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.aryamahasangh.*
import com.aryamahasangh.fragment.FamilyFields
import com.aryamahasangh.fragment.MemberWithoutFamily
import com.aryamahasangh.type.FamilyMemberInsertInput
import com.aryamahasangh.type.FamilyRelation
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FamilyMemberJson(
  val member_id: String,
  val is_head: Boolean,
  val relation_to_head: String
)

interface FamilyRepository {
  // Family listing and search
  suspend fun getFamilies(
    first: Int = 50,
    after: String? = null
  ): Flow<Result<List<FamilyFields>>>

  suspend fun searchFamilies(
    query: String,
    first: Int = 50
  ): Flow<Result<List<FamilyFields>>>

  // Family detail
  suspend fun getFamilyDetail(familyId: String): Flow<Result<GetFamilyDetailQuery.Node>>

  // Count methods
  suspend fun getFamilyAndMembersCount(): Flow<Result<Pair<Int,Int>>>

  // Member search for family creation
  suspend fun searchMembersWithoutFamily(
    query: String,
    first: Int = 50
  ): Flow<Result<List<MemberWithoutFamily>>>

  suspend fun getAllFamilyMemberIds(): Flow<Result<List<String>>>

  // Address fetching
  suspend fun getAddressesByIds(addressIds: List<String>): Flow<Result<List<GetAddressesByIdsQuery.Node>>>

  // Family creation and updates
  suspend fun createFamily(
    name: String,
    aryaSamajId: String,
    photos: List<String> = emptyList(),
    familyMembers: List<FamilyMemberData>,
    addressId: String? = null,
    basicAddress: String? = null,
    state: String? = null,
    district: String? = null,
    pincode: String? = null,
    vidhansabha: String? = null,
    latitude: Double? = null,
    longitude: Double? = null
  ): Flow<Result<String>>

  suspend fun updateFamily(
    familyId: String,
    name: String,
    addressId: String? = null,
    aryaSamajId: String? = null,
    photos: List<String> = emptyList()
  ): Flow<Result<Boolean>>

  suspend fun addMembersToFamily(
    familyId: String,
    members: List<FamilyMemberData>
  ): Flow<Result<Boolean>>

  suspend fun updateMemberAddresses(
    memberIds: List<String>,
    addressId: String
  ): Flow<Result<Boolean>>

  // Family deletion
  suspend fun deleteFamily(familyId: String): Flow<Result<Boolean>>
}

data class FamilyMemberData(
  val memberId: String,
  val isHead: Boolean,
  val relationToHead: FamilyRelation? = null
)

class FamilyRepositoryImpl(private val apolloClient: ApolloClient) : FamilyRepository {
  override suspend fun getFamilies(
    first: Int,
    after: String?
  ): Flow<Result<List<FamilyFields>>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(
        GetFamiliesQuery(
          first = Optional.present(first),
          after = Optional.presentIfNotNull(after),
          filter = Optional.absent(),
          orderBy = Optional.absent()
        )
      )
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

            response.data?.familyCollection?.edges?.map { it.node.familyFields } ?: emptyList()
          }
          emit(result)
        }
    }

  override suspend fun searchFamilies(
    query: String,
    first: Int
  ): Flow<Result<List<FamilyFields>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.query(
              SearchFamiliesQuery(
                searchTerm = "%$query%",
                first = Optional.present(first)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          response.data?.familyCollection?.edges?.map { it.node.familyFields } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun getFamilyDetail(familyId: String): Flow<Result<GetFamilyDetailQuery.Node>> =
    flow {
      emit(Result.Loading)

      apolloClient.query(
        GetFamilyDetailQuery(familyId)
      )
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .toFlow()
        .collect { response ->
          val cameFromEmptyCache =
            response.isFromCache && response.cacheInfo?.isCacheHit == false
          val result = safeCall {
            if (!cameFromEmptyCache) {
              if (response.hasErrors()) {
                throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
              }
            }

            val familyNode = response.data?.familyCollection?.edges?.firstOrNull()?.node
            if (familyNode == null && !cameFromEmptyCache) {
              throw Exception("Family not found")
            }

            familyNode ?: throw Exception("Family not found")
          }
          if (!cameFromEmptyCache || (result is Result.Success)) {
            emit(result)
          }
        }
    }

  /**
   * Fetches total family and family member counts from the database via GraphQL.
   *
   * Executes [GetTotalFamilyAndFamilyMemberCountQuery] to retrieve the total number of families
   * and associated family members. Returns a [Result] Flow containing a pair of integers:
   * (familyCount, memberCount). This function is designed for cross-platform use in Compose Multiplatform
   * and uses [ApolloClient] to communicate with the backend.
   *
   * - The [Result] will be:
   *   - [Result.Success] with the count pair on successful query execution
   *   - [Result.Error] with a wrapper containing backend error_code messages (using English message codes)
   *     when the GraphQL response contains errors or when the query execution fails
   *   - [Result.Loading] emitted first to indicate query in progress
   *
   * This method should be called by the domain layer or ViewModel to handle data state transitions.
   * Errors should be translated to user-facing messages using client-side localization (Hindi).
   */
  override suspend fun getFamilyAndMembersCount(): Flow<Result<Pair<Int,Int>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.query(
              GetTotalFamilyAndFamilyMemberCountQuery()
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to fetch family member count")
          }
          Pair(
          response.data?.familyCollection?.totalCount ?: 0,
          response.data?.familyMemberCollection?.totalCount ?: 0)
        }
      emit(result)
    }

  override suspend fun searchMembersWithoutFamily(
    query: String,
    first: Int
  ): Flow<Result<List<MemberWithoutFamily>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.query(
              GetMembersWithoutFamilyQuery(
                searchTerm = if (query.isBlank()) Optional.present("%%") else Optional.present("%$query%"),
                first = Optional.present(first)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          // Get all family member IDs to filter out members who are already in families
          val familyMemberIdsResponse = apolloClient.query(GetAllFamilyMemberIdsQuery()).execute()
          val existingFamilyMemberIds =
            familyMemberIdsResponse.data?.familyMemberCollection?.edges?.map {
              it.node.memberId
            }?.toSet() ?: emptySet()

          // Filter out members who are already in families
          val allMembers =
            response.data?.memberNotInFamilyCollection?.edges?.map { it.node.memberWithoutFamily } ?: emptyList()
          allMembers.filter { member -> member.id !in existingFamilyMemberIds }
        }
      emit(result)
    }

  override suspend fun getAllFamilyMemberIds(): Flow<Result<List<String>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response = apolloClient.query(GetAllFamilyMemberIdsQuery()).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to fetch addresses")
          }

          response.data?.familyMemberCollection?.edges?.map { it.node.memberId } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun getAddressesByIds(addressIds: List<String>): Flow<Result<List<GetAddressesByIdsQuery.Node>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.query(
              GetAddressesByIdsQuery(
                ids = Optional.present(addressIds)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception("Failed to fetch addresses: ${response.errors?.firstOrNull()?.message}")
          }

          response.data?.addressCollection?.edges?.map { it.node } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun createFamily(
    name: String,
    aryaSamajId: String,
    photos: List<String>,
    familyMembers: List<FamilyMemberData>,
    addressId: String?,
    basicAddress: String?,
    state: String?,
    district: String?,
    pincode: String?,
    vidhansabha: String?,
    latitude: Double?,
    longitude: Double?
  ): Flow<Result<String>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          // Convert family members to JSON format expected by the function
          val familyMembersJson = familyMembers.map { memberData ->
            FamilyMemberJson(
              member_id = memberData.memberId,
              is_head = memberData.isHead,
              relation_to_head = memberData.relationToHead?.name ?: "SELF"
            )
          }

          val jsonConfig = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
          }
          val familyMembersJsonString = jsonConfig.encodeToString(familyMembersJson)

          val response =
            apolloClient.mutation(
              InsertFamilyDetailsMutation(
                name = name,
                aryaSamajId = aryaSamajId,
                photos = photos,
                familyMembers = familyMembersJsonString,
                addressId = Optional.presentIfNotNull(addressId),
                basicAddress = Optional.presentIfNotNull(basicAddress),
                state = Optional.presentIfNotNull(state),
                district = Optional.presentIfNotNull(district),
                pincode = Optional.presentIfNotNull(pincode),
                vidhansabha = Optional.presentIfNotNull(vidhansabha),
                latitude = Optional.presentIfNotNull(latitude),
                longitude = Optional.presentIfNotNull(longitude)
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to create family")
          }

          // Parse the JSON response to extract family_id
          val responseData = response.data?.insertFamilyDetails
          if (responseData == null) {
            throw Exception("No response data received")
          }

          try {
            val jsonString = responseData.toString()
            // Parse using the same jsonConfig instance
            val jsonResponse =
              jsonConfig.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(jsonString)

            val successElement = jsonResponse["success"]?.toString()
            val success = successElement?.contains("true") == true

            if (success) {
              val familyIdElement = jsonResponse["family_id"]?.toString()
              val familyId =
                familyIdElement?.removeSurrounding("\"")?.removeSurrounding("JsonPrimitive(")?.removeSurrounding(")")
              familyId ?: throw Exception("Family created but no family_id returned")
            } else {
              val errorCodeElement = jsonResponse["error_code"]?.toString()
              val errorCode =
                errorCodeElement?.removeSurrounding("\"")?.removeSurrounding("JsonPrimitive(")?.removeSurrounding(")")
              throw Exception(errorCode ?: "Failed to create family")
            }
          } catch (e: Exception) {
            // Fallback: treat response as string and try manual parsing
            val responseString = responseData.toString()
            if (responseString.contains("\"success\":true") && responseString.contains("\"family_id\"")) {
              // Extract family_id using regex
              val familyIdRegex = "\"family_id\"\\s*:\\s*\"([^\"]+)\"".toRegex()
              val matchResult = familyIdRegex.find(responseString)
              matchResult?.groupValues?.get(1) ?: throw Exception("Could not extract family_id from response")
            } else {
              throw Exception("Failed to create family: ${responseString}")
            }
          }
        }
      emit(result)
    }

  override suspend fun updateFamily(
    familyId: String,
    name: String,
    addressId: String?,
    aryaSamajId: String?,
    photos: List<String>
  ): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              UpdateFamilyMutation(
                id = familyId,
                name = Optional.present(name),
                addressId = Optional.presentIfNotNull(addressId),
                aryaSamajId = Optional.presentIfNotNull(aryaSamajId),
                photos = Optional.presentIfNotNull(photos.takeIf { it.isNotEmpty() })
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception("Family update failed: ${response.errors?.firstOrNull()?.message}")
          }

          val updated = response.data?.updateFamilyCollection?.affectedCount
          if (updated == null || updated <= 0) {
            throw Exception("Family update failed - no records updated")
          }

          true
        }
      emit(result)
    }

  override suspend fun addMembersToFamily(
    familyId: String,
    members: List<FamilyMemberData>
  ): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val familyMemberInputs =
            members.map { memberData ->
              FamilyMemberInsertInput(
                familyId = Optional.present(familyId),
                memberId = Optional.present(memberData.memberId),
                isHead = Optional.present(memberData.isHead),
                relationToHead = Optional.presentIfNotNull(memberData.relationToHead)
              )
            }

          val response =
            apolloClient.mutation(
              AddMultipleMembersToFamilyMutation(familyMemberInputs)
            ).execute()

          if (response.hasErrors()) {
            throw Exception("Failed to add members to family: ${response.errors?.firstOrNull()?.message}")
          }

          true
        }
      emit(result)
    }

  override suspend fun updateMemberAddresses(
    memberIds: List<String>,
    addressId: String
  ): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          // Update each member's address (preserve tempAddressId)
          memberIds.forEach { memberId ->
            val response =
              apolloClient.mutation(
                UpdateMemberAddressMutation(
                  memberId = memberId,
                  addressId = addressId
                )
              ).execute()

            if (response.hasErrors()) {
              throw Exception(
                "Failed to update address for member $memberId: ${response.errors?.firstOrNull()?.message}"
              )
            }
          }

          true
        }
      emit(result)
    }

  override suspend fun deleteFamily(familyId: String): Flow<Result<Boolean>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              DeleteFamilyMutation(
                id = familyId
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception("Failed to delete family: ${response.errors?.firstOrNull()?.message}")
          }

          val deleted = response.data?.deleteFromFamilyCollection?.affectedCount
          if (deleted == null || deleted <= 0) {
            throw Exception("Failed to delete family - no records deleted")
          }

          true
        }
      emit(result)
    }

  // Extension function to convert GetFamilyDetailQuery.Node to FamilyShort
  fun GetFamilyDetailQuery.Node.toFamilyShort(): FamilyShort {
    val familyData = this.familyFields
    return FamilyShort(
      id = familyData.id,
      name = familyData.name ?: "",
      photos = familyData.photos?.filterNotNull() ?: emptyList(),
      address =
        familyData.address?.let { addr ->
          "${addr.basicAddress ?: ""}, ${addr.district ?: ""}, ${addr.state ?: ""}"
        }?.trim()?.let { if (it == ", , ") "" else it } ?: "",
      aryaSamajName = familyData.aryaSamaj?.name ?: ""
    )
  }
}
