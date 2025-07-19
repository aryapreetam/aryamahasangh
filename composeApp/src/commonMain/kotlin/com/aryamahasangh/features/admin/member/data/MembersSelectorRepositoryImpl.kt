package com.aryamahasangh.features.admin.member.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.cacheInfo
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.aryamahasangh.RecentMembersForSelectorQuery
import com.aryamahasangh.RecentMembersNotInFamilyForSelectorQuery
import com.aryamahasangh.SearchMembersForSelectorQuery
import com.aryamahasangh.SearchMembersNotInFamilyForSelectorQuery
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.admin.member.MemberCollectionType
import com.aryamahasangh.features.admin.member.MembersSelectorRepository
import com.aryamahasangh.features.admin.member.PaginatedResult
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of MembersSelectorRepository using Apollo GraphQL client with infinite scroll support
 */
class MembersSelectorRepositoryImpl(
  private val apolloClient: ApolloClient
) : MembersSelectorRepository {

  override suspend fun getRecentMembers(
    limit: Int,
    cursor: String?,
    collectionType: MemberCollectionType
  ): Flow<Result<PaginatedResult<Member>>> = flow {
    emit(Result.Loading)

    when (collectionType) {
      MemberCollectionType.ALL_MEMBERS -> {
        apolloClient.query(
          RecentMembersForSelectorQuery(
            first = limit,
            after = if (cursor != null) Optional.Present(cursor) else Optional.Absent
          )
        )
          .fetchPolicy(FetchPolicy.CacheAndNetwork)
          .toFlow()
          .collect { response ->
            // Skip cache misses with empty data - wait for network response
            val isCacheMissWithEmptyData = response.isFromCache &&
              response.cacheInfo?.isCacheHit == false &&
              response.data?.memberCollection?.edges.isNullOrEmpty()

            if (isCacheMissWithEmptyData) {
              return@collect
            }

            val result = safeCall {
              if (response.hasErrors()) {
                throw Exception(
                  response.errors?.firstOrNull()?.message ?: "सदस्य लोड करने में त्रुटि"
                )
              }

              val edges = response.data?.memberCollection?.edges ?: emptyList()
              val pageInfo = response.data?.memberCollection?.pageInfo

              val members = edges.map { edge ->
                val memberData = edge.node.memberSelectorExtended
                Member(
                  id = memberData.id,
                  name = memberData.name,
                  profileImage = memberData.profileImage ?: "",
                  phoneNumber = memberData.phoneNumber ?: "",
                )
              }

              PaginatedResult(
                items = members,
                hasNextPage = pageInfo?.hasNextPage ?: false,
                endCursor = pageInfo?.endCursor
              )
            }

            emit(result)
          }
      }

      MemberCollectionType.MEMBERS_NOT_IN_FAMILY -> {
        apolloClient.query(
          RecentMembersNotInFamilyForSelectorQuery(
            first = limit,
            after = if (cursor != null) Optional.Present(cursor) else Optional.Absent
          )
        )
          .fetchPolicy(FetchPolicy.CacheAndNetwork)
          .toFlow()
          .collect { response ->
            // Skip cache misses with empty data - wait for network response
            val isCacheMissWithEmptyData = response.isFromCache &&
              response.cacheInfo?.isCacheHit == false &&
              response.data?.memberNotInFamilyCollection?.edges.isNullOrEmpty()

            if (isCacheMissWithEmptyData) {
              return@collect
            }

            val result = safeCall {
              if (response.hasErrors()) {
                throw Exception(
                  response.errors?.firstOrNull()?.message ?: "परिवार रहित सदस्य लोड करने में त्रुटि"
                )
              }

              val edges = response.data?.memberNotInFamilyCollection?.edges ?: emptyList()
              val pageInfo = response.data?.memberNotInFamilyCollection?.pageInfo

              val members = edges.map { edge ->
                val memberData = edge.node.memberNotInFamilySelectorExtended
                Member(
                  id = memberData.id!!,
                  name = memberData.name!!,
                  profileImage = memberData.profileImage ?: "",
                  phoneNumber = memberData.phoneNumber!!,
                  email = "" // MemberNotInFamily doesn't have email field
                )
              }

              PaginatedResult(
                items = members,
                hasNextPage = pageInfo?.hasNextPage ?: false,
                endCursor = pageInfo?.endCursor
              )
            }

            emit(result)
          }
      }
    }
  }

  override suspend fun searchMembers(
    query: String,
    limit: Int,
    cursor: String?,
    collectionType: MemberCollectionType
  ): Flow<Result<PaginatedResult<Member>>> = flow {
    emit(Result.Loading)

    val searchTerm = "%$query%"

    when (collectionType) {
      MemberCollectionType.ALL_MEMBERS -> {
        apolloClient.query(
          SearchMembersForSelectorQuery(
            searchTerm = searchTerm,
            first = limit,
            after = if (cursor != null) Optional.Present(cursor) else Optional.Absent
          )
        )
          .fetchPolicy(FetchPolicy.CacheAndNetwork)
          .toFlow()
          .collect { response ->
            // Skip cache misses with empty data - wait for network response
            val isCacheMissWithEmptyData = response.isFromCache &&
              response.cacheInfo?.isCacheHit == false &&
              response.data?.memberCollection?.edges.isNullOrEmpty()

            if (isCacheMissWithEmptyData) {
              return@collect
            }

            val result = safeCall {
              if (response.hasErrors()) {
                throw Exception(
                  response.errors?.firstOrNull()?.message ?: "खोज में त्रुटि"
                )
              }

              val edges = response.data?.memberCollection?.edges ?: emptyList()
              val pageInfo = response.data?.memberCollection?.pageInfo

              val members = edges.map { edge ->
                val memberData = edge.node.memberSelectorExtended
                Member(
                  id = memberData.id,
                  name = memberData.name,
                  profileImage = memberData.profileImage ?: "",
                  phoneNumber = memberData.phoneNumber ?: "",
                )
              }

              PaginatedResult(
                items = members,
                hasNextPage = pageInfo?.hasNextPage ?: false,
                endCursor = pageInfo?.endCursor
              )
            }

            emit(result)
          }
      }

      MemberCollectionType.MEMBERS_NOT_IN_FAMILY -> {
        apolloClient.query(
          SearchMembersNotInFamilyForSelectorQuery(
            searchTerm = searchTerm,
            first = limit,
            after = if (cursor != null) Optional.Present(cursor) else Optional.Absent
          )
        )
          .fetchPolicy(FetchPolicy.CacheAndNetwork)
          .toFlow()
          .collect { response ->
            // Skip cache misses with empty data - wait for network response
            val isCacheMissWithEmptyData = response.isFromCache &&
              response.cacheInfo?.isCacheHit == false &&
              response.data?.memberNotInFamilyCollection?.edges.isNullOrEmpty()

            if (isCacheMissWithEmptyData) {
              return@collect
            }

            val result = safeCall {
              if (response.hasErrors()) {
                throw Exception(
                  response.errors?.firstOrNull()?.message ?: "परिवार रहित सदस्य खोज में त्रुटि"
                )
              }

              val edges = response.data?.memberNotInFamilyCollection?.edges ?: emptyList()
              val pageInfo = response.data?.memberNotInFamilyCollection?.pageInfo

              val members = edges.map { edge ->
                val memberData = edge.node.memberNotInFamilySelectorExtended
                Member(
                  id = memberData.id!!,
                  name = memberData.name!!,
                  profileImage = memberData.profileImage ?: "",
                  phoneNumber = memberData.phoneNumber!!,
                  email = "" // MemberNotInFamily doesn't have email field
                )
              }

              PaginatedResult(
                items = members,
                hasNextPage = pageInfo?.hasNextPage ?: false,
                endCursor = pageInfo?.endCursor
              )
            }

            emit(result)
          }
      }
    }
  }
}
