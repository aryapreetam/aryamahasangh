package com.aryamahasangh.features.public_arya_samaj

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.aryamahasangh.AryaSamajPublicCountQuery
import com.aryamahasangh.GetAryaSamajsPublicQuery
import com.aryamahasangh.SearchAryaSamajsPublicQuery
import com.aryamahasangh.features.admin.PaginatedRepository
import com.aryamahasangh.features.admin.PaginationResult
import com.aryamahasangh.fragment.AryaSamajPublicListItem
import com.aryamahasangh.type.AryaSamajWithAddressFilter
import com.aryamahasangh.type.AryaSamajWithAddressOrderBy
import com.aryamahasangh.type.OrderByDirection
import com.aryamahasangh.type.StringFilter
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Repository interface for AryaSamaj home screen functionality
 * Extends PaginatedRepository for proper caching and scroll persistence
 */
interface AryaSamajHomeRepository : PaginatedRepository<AryaSamajHomeListItem> {
    /**
     * Get total count of AryaSamaj items
     */
    suspend fun getAryaSamajCount(): Flow<Result<Int>>
    
    /**
     * Search AryaSamaj items by address with pagination
     */
    suspend fun searchAryaSamajsByAddress(
        state: String? = null,
        district: String? = null,
        vidhansabha: String? = null,
        pageSize: Int,
        cursor: String? = null
    ): Flow<PaginationResult<AryaSamajHomeListItem>>
    
    /**
     * Search AryaSamaj items by both name and address with pagination
     */
    suspend fun searchAryaSamajsByNameAndAddress(
        searchTerm: String,
        state: String? = null,
        district: String? = null,
        vidhansabha: String? = null,
        pageSize: Int,
        cursor: String? = null
    ): Flow<PaginationResult<AryaSamajHomeListItem>>
}

/**
 * Implementation of AryaSamajHomeRepository using Apollo GraphQL client
 * Provides proper caching, scroll persistence, and pagination support
 */
class AryaSamajHomeRepositoryImpl(
    private val apolloClient: ApolloClient
) : AryaSamajHomeRepository {

    override suspend fun getItemsPaginated(
        pageSize: Int,
        cursor: String?,
        filter: Any?
    ): Flow<PaginationResult<AryaSamajHomeListItem>> = flow {
        emit(PaginationResult.Loading())
        
        try {
            println("Repository Debug: Starting getItemsPaginated - pageSize: $pageSize, cursor: $cursor")
            
            val addressFilter = filter as? AryaSamajWithAddressFilter
            val orderBy = listOf(
                AryaSamajWithAddressOrderBy.Builder()
                    .createdAt(OrderByDirection.DescNullsLast)
                    .build()
            )

            val response = apolloClient.query(
                GetAryaSamajsPublicQuery(
                    first = pageSize,
                    after = Optional.presentIfNotNull(cursor),
                    filter = Optional.presentIfNotNull(addressFilter),
                    orderBy = Optional.presentIfNotNull(orderBy)
                )
            ).execute()
            
            println("Repository Debug: Query executed - hasErrors: ${response.hasErrors()}, data null: ${response.data == null}")
            
            if (response.hasErrors()) {
                val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown GraphQL error"
                println("Repository Debug: GraphQL Error - $errorMessage")
                emit(PaginationResult.Error(errorMessage))
                return@flow
            }
            
            val collection = response.data?.aryaSamajWithAddressCollection
            println("Repository Debug: Collection - null: ${collection == null}, edges count: ${collection?.edges?.size}")
            
            if (collection == null) {
                emit(PaginationResult.Error("No data returned from GraphQL"))
                return@flow
            }
            
            val items = collection.edges.mapNotNull { edge ->
                try {
                    val fragment = edge.node.aryaSamajPublicListItem
                    println("Repository Debug: Processing fragment - id: ${fragment.id}, name: ${fragment.name}")
                    convertToHomeListItem(fragment)
                } catch (e: Exception) {
                    println("Repository Debug: Error converting item - ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
            
            println("Repository Debug: Successfully converted ${items.size} items")
            
            val pageInfo = collection.pageInfo
            
            emit(PaginationResult.Success(
                data = items,
                hasNextPage = pageInfo.hasNextPage,
                endCursor = pageInfo.endCursor
            ))
            
        } catch (e: Exception) {
            println("Repository Debug: Exception in getItemsPaginated - ${e.message}")
            e.printStackTrace()
            emit(PaginationResult.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun searchItemsPaginated(
        searchTerm: String,
        pageSize: Int,
        cursor: String?
    ): Flow<PaginationResult<AryaSamajHomeListItem>> = flow {
        emit(PaginationResult.Loading())
        
        try {
            val response = apolloClient.query(
                SearchAryaSamajsPublicQuery(
                    first = pageSize,
                    after = Optional.presentIfNotNull(cursor),
                    searchTerm = "%$searchTerm%"
                )
            ).execute()
            
            if (response.hasErrors()) {
                emit(PaginationResult.Error(response.errors?.firstOrNull()?.message ?: "Search error"))
                return@flow
            }
            
            val collection = response.data?.aryaSamajWithAddressCollection
            if (collection == null) {
                emit(PaginationResult.Error("No data returned from search"))
                return@flow
            }
            
            val items = collection.edges.mapNotNull { edge ->
                try {
                    convertToHomeListItem(edge.node.aryaSamajPublicListItem)
                } catch (e: Exception) {
                    println("Repository Debug: Error converting search item - ${e.message}")
                    null
                }
            }
            
            val pageInfo = collection.pageInfo
            
            emit(PaginationResult.Success(
                data = items,
                hasNextPage = pageInfo.hasNextPage,
                endCursor = pageInfo.endCursor
            ))
            
        } catch (e: Exception) {
            emit(PaginationResult.Error(e.message ?: "Search error"))
        }
    }

    override suspend fun searchAryaSamajsByAddress(
        state: String?,
        district: String?,
        vidhansabha: String?,
        pageSize: Int,
        cursor: String?
    ): Flow<PaginationResult<AryaSamajHomeListItem>> = flow {
        emit(PaginationResult.Loading())
        
        val filter = buildAddressFilter(state, district, vidhansabha)
        
        // Use getItemsPaginated with address filter for consistency
        getItemsPaginated(pageSize, cursor, filter).collect { result ->
            emit(result)
        }
    }

    override suspend fun searchAryaSamajsByNameAndAddress(
        searchTerm: String,
        state: String?,
        district: String?,
        vidhansabha: String?,
        pageSize: Int,
        cursor: String?
    ): Flow<PaginationResult<AryaSamajHomeListItem>> = flow {
        emit(PaginationResult.Loading())
        
        // For combined search, we need to build a complex filter
        val addressFilters = mutableListOf<AryaSamajWithAddressFilter>()
        
        // Add name search filter
        addressFilters.add(
            AryaSamajWithAddressFilter.Builder()
                .name(StringFilter.Builder().ilike("%$searchTerm%").build())
                .build()
        )
        
        // Add address filters if provided
        state?.let { s ->
            addressFilters.add(
                AryaSamajWithAddressFilter.Builder()
                    .state(StringFilter.Builder().eq(s).build())
                    .build()
            )
        }
        
        district?.let { d ->
            addressFilters.add(
                AryaSamajWithAddressFilter.Builder()
                    .district(StringFilter.Builder().eq(d).build())
                    .build()
            )
        }
        
        vidhansabha?.let { v ->
            addressFilters.add(
                AryaSamajWithAddressFilter.Builder()
                    .vidhansabha(StringFilter.Builder().eq(v).build())
                    .build()
            )
        }
        
        val combinedFilter = if (addressFilters.size == 1) {
            addressFilters.first()
        } else {
            AryaSamajWithAddressFilter.Builder()
                .and(addressFilters)
                .build()
        }
        
        // Use getItemsPaginated with combined filter
        getItemsPaginated(pageSize, cursor, combinedFilter).collect { result ->
            emit(result)
        }
    }

    override suspend fun getAryaSamajCount(): Flow<Result<Int>> = flow {
        emit(Result.Loading)
        
        try {
            val response = apolloClient.query(AryaSamajPublicCountQuery()).execute()
            
            if (response.hasErrors()) {
                emit(Result.Error(response.errors?.firstOrNull()?.message ?: "Count error"))
                return@flow
            }
            
            val count = response.data?.aryaSamajWithAddressCollection?.totalCount ?: 0
            emit(Result.Success(count))
            
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Count error"))
        }
    }

    /**
     * Build address filter for GraphQL query
     */
    private fun buildAddressFilter(
        state: String?,
        district: String?,
        vidhansabha: String?
    ): AryaSamajWithAddressFilter? {
        val filters = mutableListOf<AryaSamajWithAddressFilter>()
        
        state?.let { s ->
            filters.add(
                AryaSamajWithAddressFilter.Builder()
                    .state(StringFilter.Builder().eq(s).build())
                    .build()
            )
        }
        
        district?.let { d ->
            filters.add(
                AryaSamajWithAddressFilter.Builder()
                    .district(StringFilter.Builder().eq(d).build())
                    .build()
            )
        }
        
        vidhansabha?.let { v ->
            filters.add(
                AryaSamajWithAddressFilter.Builder()
                    .vidhansabha(StringFilter.Builder().eq(v).build())
                    .build()
            )
        }
        
        return when {
            filters.isEmpty() -> null
            filters.size == 1 -> filters.first()
            else -> AryaSamajWithAddressFilter.Builder()
                .and(filters)
                .build()
        }
    }

    /**
     * Convert GraphQL fragment to domain model
     */
    private fun convertToHomeListItem(fragment: AryaSamajPublicListItem): AryaSamajHomeListItem {
        // Handle date conversion safely
        val createdAt = try {
            when (val dateValue = fragment.createdAt) {
                is String -> Instant.parse(dateValue).toLocalDateTime(TimeZone.currentSystemDefault())
                else -> {
                  println("Repository Debug: Unexpected date type: ${dateValue?.toString()}")
                    kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }
        } catch (e: Exception) {
            println("Repository Debug: Date conversion error - ${e.message}, value: ${fragment.createdAt}")
            kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        }
        
        return AryaSamajHomeListItem(
            id = fragment.id ?: "",
            name = fragment.name ?: "",
            description = fragment.description ?: "",
            mediaUrls = fragment.mediaUrls?.filterNotNull() ?: emptyList(),
            createdAt = createdAt,
            basicAddress = fragment.basicAddress,
            state = fragment.state,
            district = fragment.district,
            pincode = fragment.pincode,
            latitude = fragment.latitude,
            longitude = fragment.longitude,
            vidhansabha = fragment.vidhansabha
        )
    }
}
