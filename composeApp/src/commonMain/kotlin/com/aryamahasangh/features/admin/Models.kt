package com.aryamahasangh.features.admin

import com.aryamahasangh.components.Gender
import com.aryamahasangh.fragment.AddressFields
import com.aryamahasangh.fragment.AryaSamajFields
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class MemberShort(
  val id: String,
  val name: String,
  val profileImage: String,
  val place: String = ""
)

/**
 * Pagination state for lists
 */
data class PaginationState<T>(
  val items: List<T> = emptyList(),
  val isInitialLoading: Boolean = false,
  val isLoadingNextPage: Boolean = false,
  val isSearching: Boolean = false,
  val hasNextPage: Boolean = false,
  val hasReachedEnd: Boolean = false,
  val error: String? = null,
  val nextPageError: String? = null,
  val showRetryButton: Boolean = false,
  val endCursor: String? = null,
  val currentSearchTerm: String = ""
)

/**
 * Generic repository interface for paginated data
 */
interface PaginatedRepository<T> {
  suspend fun getItemsPaginated(
    pageSize: Int,
    cursor: String? = null,
    filter: Any? = null
  ): Flow<PaginationResult<T>>

  suspend fun searchItemsPaginated(
    searchTerm: String,
    pageSize: Int,
    cursor: String? = null
  ): Flow<PaginationResult<T>>
}

/**
 * Base interface for paginated ViewModels
 */
interface PaginatedViewModel<T> {
  val searchQuery: String
  val paginationState: PaginationState<T>

  fun loadItemsPaginated(pageSize: Int = 30, resetPagination: Boolean = false)
  fun searchItemsWithDebounce(query: String)
  fun loadNextPage()
  fun retryLoad()
  fun calculatePageSize(screenWidthDp: Float): Int
}

/**
 * Generic component state for pagination screens
 */
data class PaginatedUiState<T>(
  val items: List<T> = emptyList(),
  val paginationState: PaginationState<T> = PaginationState(),
  val searchQuery: String = ""
)

/**
 * Result wrapper for pagination operations
 */
sealed class PaginationResult<T> {
  data class Success<T>(val data: List<T>, val hasNextPage: Boolean, val endCursor: String?) : PaginationResult<T>()
  data class Error<T>(val message: String) : PaginationResult<T>()
  class Loading<T> : PaginationResult<T>()
}

/**
 * Retry policy configuration
 */
data class RetryConfig(
  val maxRetries: Int = 3,
  val baseDelayMs: Long = 1000L,
  val currentRetryCount: Int = 0
) {
  fun nextRetry(): RetryConfig = copy(currentRetryCount = currentRetryCount + 1)
  fun canRetry(): Boolean = currentRetryCount < maxRetries
  fun getDelayMs(): Long = baseDelayMs * (2 * currentRetryCount) // Exponential backoff
}

data class MemberDetail(
  val id: String,
  val name: String,
  val profileImage: String,
  val phoneNumber: String,
  val educationalQualification: String,
  val email: String,
  val dob: LocalDate?,
  val joiningDate: LocalDate?,
  val gender: Gender?,
  val introduction: String,
  val occupation: String,
  val referrerId: String?,
  val referrer: ReferrerInfo?,
  val address: String, // Formatted address string for display
  val addressFields: AddressFields?, // Full address details for editing
  val tempAddress: String, // Formatted temp address string
  val tempAddressFields: AddressFields?, // Full temp address details for editing
  val district: String,
  val state: String,
  val pincode: String,
  val organisations: List<OrganisationInfo>,
  val activities: List<ActivityInfo>,
  val aryaSamaj: AryaSamajFields?,
  val samajPositions: List<SamajPositionInfo> // New field for Arya Samaj positions
)

data class ReferrerInfo(
  val id: String,
  val name: String,
  val profileImage: String
)

data class OrganisationInfo(
  val id: String,
  val name: String,
  val logo: String
)

data class ActivityInfo(
  val id: String,
  val name: String,
  val district: String,
  val state: String,
  val startDatetime: LocalDateTime,
  val endDatetime: LocalDateTime
)

data class SamajPositionInfo(
  val id: String,
  val post: String,
  val priority: Int,
  val aryaSamaj: AryaSamajFields
)
