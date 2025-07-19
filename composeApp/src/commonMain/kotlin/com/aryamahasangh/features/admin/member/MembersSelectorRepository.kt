package com.aryamahasangh.features.admin.member

import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Paginated result wrapper for cursor-based pagination
 */
data class PaginatedResult<T>(
  val items: List<T>,
  val hasNextPage: Boolean,
  val endCursor: String?
)

/**
 * Collection type for member queries
 */
enum class MemberCollectionType {
  ALL_MEMBERS,
  MEMBERS_NOT_IN_FAMILY
}

/**
 * Repository interface for member selector functionality
 */
interface MembersSelectorRepository {
  /**
   * Get recent members with cursor-based pagination
   * @param limit Maximum number of members to fetch (default: 20)
   * @param cursor Cursor for pagination (null for first page)
   * @param collectionType Type of collection to query (default: ALL_MEMBERS)
   * @return Flow of Result containing PaginatedResult of members
   */
  suspend fun getRecentMembers(
    limit: Int = 20,
    cursor: String? = null,
    collectionType: MemberCollectionType = MemberCollectionType.ALL_MEMBERS
  ): Flow<Result<PaginatedResult<Member>>>

  /**
   * Search members by query string with cursor-based pagination
   * @param query Search query for name and phone number
   * @param limit Maximum number of results (default: 20)
   * @param cursor Cursor for pagination (null for first page)
   * @param collectionType Type of collection to query (default: ALL_MEMBERS)
   * @return Flow of Result containing PaginatedResult of members
   */
  suspend fun searchMembers(
    query: String,
    limit: Int = 20,
    cursor: String? = null,
    collectionType: MemberCollectionType = MemberCollectionType.ALL_MEMBERS
  ): Flow<Result<PaginatedResult<Member>>>
}
