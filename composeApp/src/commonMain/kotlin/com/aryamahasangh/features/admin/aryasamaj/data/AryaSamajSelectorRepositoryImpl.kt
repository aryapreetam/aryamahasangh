package com.aryamahasangh.features.admin.aryasamaj.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.cacheInfo
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.isFromCache
import com.aryamahasangh.RecentAryaSamajsQuery
import com.aryamahasangh.SearchAryaSamajsQuery
import com.aryamahasangh.components.AryaSamaj
import com.aryamahasangh.features.admin.aryasamaj.AryaSamajPaginatedResult
import com.aryamahasangh.features.admin.aryasamaj.AryaSamajSelectorRepository
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.*

class AryaSamajSelectorRepositoryImpl(
  private val apolloClient: ApolloClient
) : AryaSamajSelectorRepository {

  override suspend fun getRecentAryaSamajs(
    limit: Int,
    cursor: String?,
    latitude: Double?,
    longitude: Double?
  ): Flow<Result<AryaSamajPaginatedResult<AryaSamaj>>> = flow {
    emit(Result.Loading)

    apolloClient.query(
      RecentAryaSamajsQuery(
        first = limit,
        after = if (cursor != null) Optional.Present(cursor) else Optional.Absent,
        lat = Optional.presentIfNotNull(latitude),
        lng = Optional.presentIfNotNull(longitude)
      )
    )
      .fetchPolicy(FetchPolicy.CacheAndNetwork)
      .toFlow()
      .collect { response ->
        // Skip cache misses with empty data - wait for network response
        val isCacheMissWithEmptyData = response.isFromCache && 
          response.cacheInfo?.isCacheHit == false && 
          response.data?.aryaSamajCollection?.edges.isNullOrEmpty()
        
        if (isCacheMissWithEmptyData) {
          return@collect
        }

        val result = safeCall {
          if (response.hasErrors()) {
            throw Exception(
              response.errors?.firstOrNull()?.message ?: "Failed to load recent AryaSamajs"
            )
          }

          val edges = response.data?.aryaSamajCollection?.edges ?: emptyList()
          val pageInfo = response.data?.aryaSamajCollection?.pageInfo

          val aryaSamajs = edges.mapNotNull { edge ->
            val node = edge.node
            val aryaSamajFields = node.aryaSamajFields
            val address = node.address

            // Create AryaSamaj object
            val aryaSamaj = AryaSamaj(
              id = aryaSamajFields.id,
              name = aryaSamajFields.name ?: "",
              address = buildAddressString(
                basicAddress = address?.addressFields?.basicAddress,
                district = address?.addressFields?.district,
                state = address?.addressFields?.state,
                pincode = address?.addressFields?.pincode
              ),
              district = address?.addressFields?.district ?: ""
            )

            // If lat/lng provided, calculate distance for sorting
            if (latitude != null && longitude != null && 
                address?.addressFields?.latitude != null && 
                address.addressFields.longitude != null) {
              val distance = calculateDistance(
                latitude, longitude,
                address.addressFields.latitude, address.addressFields.longitude
              )
              // Return pair of AryaSamaj and distance for sorting
              aryaSamaj to distance
            } else {
              // No location-based sorting
              aryaSamaj to 0.0
            }
          }

          // Sort by distance if location provided, otherwise keep order from query
          val sortedAryaSamajs = if (latitude != null && longitude != null) {
            aryaSamajs.sortedBy { it.second }.map { it.first }
          } else {
            aryaSamajs.map { it.first }
          }

          AryaSamajPaginatedResult(
            items = sortedAryaSamajs,
            hasNextPage = pageInfo?.hasNextPage ?: false,
            endCursor = pageInfo?.endCursor
          )
        }

        emit(result)
      }
  }

  override suspend fun searchAryaSamajs(
    query: String,
    limit: Int,
    cursor: String?
  ): Flow<Result<AryaSamajPaginatedResult<AryaSamaj>>> = flow {
    emit(Result.Loading)

    apolloClient.query(
      SearchAryaSamajsQuery(
        searchTerm = "%$query%",
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
          response.data?.aryaSamajCollection?.edges.isNullOrEmpty()
        
        if (isCacheMissWithEmptyData) {
          return@collect
        }

        val result = safeCall {
          if (response.hasErrors()) {
            throw Exception(
              response.errors?.firstOrNull()?.message ?: "Search failed"
            )
          }

          val edges = response.data?.aryaSamajCollection?.edges ?: emptyList()
          val pageInfo = response.data?.aryaSamajCollection?.pageInfo

          val aryaSamajs = edges.map { edge ->
            val node = edge.node
            val aryaSamajWithAddress = node.aryaSamajWithAddress
            val address = aryaSamajWithAddress.address

            AryaSamaj(
              id = aryaSamajWithAddress.aryaSamajFields.id,
              name = aryaSamajWithAddress.aryaSamajFields.name ?: "",
              address = buildAddressString(
                basicAddress = address?.addressFields?.basicAddress,
                district = address?.addressFields?.district,
                state = address?.addressFields?.state,
                pincode = address?.addressFields?.pincode
              ),
              district = address?.addressFields?.district ?: ""
            )
          }

          AryaSamajPaginatedResult(
            items = aryaSamajs,
            hasNextPage = pageInfo?.hasNextPage ?: false,
            endCursor = pageInfo?.endCursor
          )
        }

        emit(result)
      }
  }

  /**
   * Build a formatted address string from components
   */
  private fun buildAddressString(
    basicAddress: String?,
    district: String?,
    state: String?,
    pincode: String?
  ): String {
    return listOfNotNull(
      basicAddress?.takeIf { it.isNotBlank() },
      district?.takeIf { it.isNotBlank() },
      state?.takeIf { it.isNotBlank() },
      pincode?.takeIf { it.isNotBlank() }
    ).joinToString(", ").trim()
  }

  /**
   * Calculate distance between two points using Haversine formula
   * @return Distance in kilometers
   */
  private fun calculateDistance(
    lat1: Double, lng1: Double,
    lat2: Double, lng2: Double
  ): Double {
    val earthRadius = 6371.0 // Earth's radius in kilometers
    
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLng = (lng2 - lng1) * PI / 180.0
    
    val a = sin(dLat / 2).pow(2) + 
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * 
            sin(dLng / 2).pow(2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return earthRadius * c
  }
}
