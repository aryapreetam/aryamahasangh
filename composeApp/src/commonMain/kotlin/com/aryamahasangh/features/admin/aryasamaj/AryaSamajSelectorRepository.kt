package com.aryamahasangh.features.admin.aryasamaj

import com.aryamahasangh.components.AryaSamaj
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Paginated result wrapper for cursor-based pagination
 */
data class AryaSamajPaginatedResult<T>(
  val items: List<T>,
  val hasNextPage: Boolean,
  val endCursor: String?
)

interface AryaSamajSelectorRepository {
  /**
   * Get recent AryaSamajs with cursor-based pagination and optional location-based sorting
   * @param limit Maximum number of results to return (default 20)
   * @param cursor Cursor for pagination (null for first page)
   * @param latitude Optional latitude for proximity-based sorting
   * @param longitude Optional longitude for proximity-based sorting
   * @return Flow of Result containing PaginatedResult of AryaSamaj
   */
  suspend fun getRecentAryaSamajs(
    limit: Int = 20,
    cursor: String? = null,
    latitude: Double? = null,
    longitude: Double? = null
  ): Flow<Result<AryaSamajPaginatedResult<AryaSamaj>>>

  /**
   * Search AryaSamajs by name with cursor-based pagination
   * @param query Search term for AryaSamaj name
   * @param limit Maximum number of results to return (default 20)
   * @param cursor Cursor for pagination (null for first page)
   * @return Flow of Result containing PaginatedResult of AryaSamaj
   */
  suspend fun searchAryaSamajs(
    query: String,
    limit: Int = 20,
    cursor: String? = null
  ): Flow<Result<AryaSamajPaginatedResult<AryaSamaj>>>
}
