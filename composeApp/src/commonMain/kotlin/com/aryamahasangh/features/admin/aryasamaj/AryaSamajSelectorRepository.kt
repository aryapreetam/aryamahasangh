package com.aryamahasangh.features.admin.aryasamaj

import com.aryamahasangh.components.AryaSamaj
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.Flow

interface AryaSamajSelectorRepository {
  /**
   * Get recent AryaSamajs with optional location-based sorting
   * @param limit Maximum number of results to return (default 10)
   * @param latitude Optional latitude for proximity-based sorting
   * @param longitude Optional longitude for proximity-based sorting
   * @return Flow of Result containing list of AryaSamaj
   */
  suspend fun getRecentAryaSamajs(
    limit: Int = 10,
    latitude: Double? = null,
    longitude: Double? = null
  ): Flow<Result<List<AryaSamaj>>>

  /**
   * Search AryaSamajs by name
   * @param query Search term for AryaSamaj name
   * @param limit Maximum number of results to return (default 20)
   * @return Flow of Result containing list of AryaSamaj
   */
  suspend fun searchAryaSamajs(
    query: String,
    limit: Int = 20
  ): Flow<Result<List<AryaSamaj>>>
}
