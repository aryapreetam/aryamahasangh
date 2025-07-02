package com.aryamahasangh.features.admin

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import com.aryamahasangh.components.Gender
import com.aryamahasangh.fragment.AddressFields
import com.aryamahasangh.fragment.AryaSamajFields

data class MemberShort(
  val id: String,
  val name: String,
  val profileImage: String,
  val place: String = ""
)

// Pagination state for managing paginated data
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

// Result wrapper for pagination operations
sealed class PaginationResult<T> {
  data class Success<T>(val data: List<T>, val hasNextPage: Boolean, val endCursor: String?) : PaginationResult<T>()
  data class Error<T>(val message: String) : PaginationResult<T>()
  class Loading<T> : PaginationResult<T>()
}

// Retry policy configuration
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
