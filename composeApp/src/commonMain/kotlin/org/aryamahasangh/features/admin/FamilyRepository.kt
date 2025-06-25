package org.aryamahasangh.features.admin

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.*
import org.aryamahasangh.DeleteFamilyMutation
import org.aryamahasangh.UpdateFamilyMutation
import org.aryamahasangh.fragment.FamilyFields
import org.aryamahasangh.fragment.MemberWithoutFamily
import org.aryamahasangh.type.FamilyMemberInsertInput
import org.aryamahasangh.type.FamilyRelation
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

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
    addressId: String?,
    aryaSamajId: String?,
    photos: List<String>
  ): Flow<Result<String>>

  suspend fun updateFamily(
    familyId: String,
    name: String,
    addressId: String?,
    aryaSamajId: String?,
    photos: List<String>
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
  val relationToHead: FamilyRelation?
)

class FamilyRepositoryImpl(private val apolloClient: ApolloClient) : FamilyRepository {
  override suspend fun getFamilies(
    first: Int,
    after: String?
  ): Flow<Result<List<FamilyFields>>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.query(
              GetFamiliesQuery(
                first = Optional.present(first),
                after = Optional.presentIfNotNull(after),
                filter = Optional.absent(),
                orderBy = Optional.absent()
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          response.data?.familyCollection?.edges?.map { it.node.familyFields } ?: emptyList()
        }
      emit(result)
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
      val result =
        safeCall {
          val response =
            apolloClient.query(
              GetFamilyDetailQuery(familyId)
            ).execute()

          if (response.hasErrors()) {
            throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
          }

          val familyNode = response.data?.familyCollection?.edges?.firstOrNull()?.node
          if (familyNode == null) {
            throw Exception("Family not found")
          }

          familyNode
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
          val allMembers = response.data?.memberNotInFamilyCollection?.edges?.map { it.node.memberWithoutFamily } ?: emptyList()
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
            throw Exception(response.errors?.firstOrNull()?.message ?: "Failed to fetch addresses")
          }

          response.data?.addressCollection?.edges?.map { it.node } ?: emptyList()
        }
      emit(result)
    }

  override suspend fun createFamily(
    name: String,
    addressId: String?,
    aryaSamajId: String?,
    photos: List<String>
  ): Flow<Result<String>> =
    flow {
      emit(Result.Loading)
      val result =
        safeCall {
          val response =
            apolloClient.mutation(
              CreateFamilyMutation(
                name = name,
                addressId = Optional.presentIfNotNull(addressId),
                aryaSamajId = Optional.presentIfNotNull(aryaSamajId),
                photos = Optional.present(photos.filterNotNull())
              )
            ).execute()

          if (response.hasErrors()) {
            throw Exception("Family creation failed: ${response.errors?.firstOrNull()?.message}")
          }

          val familyId = response.data?.insertIntoFamilyCollection?.records?.firstOrNull()?.id
          if (familyId == null) {
            throw Exception("Family creation failed - no ID returned")
          }

          familyId
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
