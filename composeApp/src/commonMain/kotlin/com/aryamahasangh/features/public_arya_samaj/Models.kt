package com.aryamahasangh.features.public_arya_samaj

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * Public AryaSamaj home item model for display in the home screen
 */
@Serializable
data class AryaSamajHomeListItem(
  val id: String,
  val name: String,
  val description: String,
  val mediaUrls: List<String>,
  val createdAt: LocalDateTime,
  val basicAddress: String?,
  val state: String?,
  val district: String?,
  val pincode: String?,
  val latitude: Double?,
  val longitude: Double?,
  val vidhansabha: String?
) {
  val formattedAddress: String
    get() = buildString {
      if (!basicAddress.isNullOrBlank()) {
        append(basicAddress)
        if (!district.isNullOrBlank() || !state.isNullOrBlank()) {
          append(", ")
        }
      }
      if (!vidhansabha.isNullOrBlank()) {
        append(vidhansabha)
        if (!district.isNullOrBlank() || !state.isNullOrBlank()) {
          append(", ")
        }
      }
      if (!district.isNullOrBlank()) {
        append(district)
        if (!state.isNullOrBlank()) {
          append(", ")
        }
      }
      if (!state.isNullOrBlank()) {
        append(state)
      }
      if (!pincode.isNullOrBlank()) {
        append(" - ")
        append(pincode)
      }
    }

  val primaryImage: String?
    get() = mediaUrls.firstOrNull()

  val initialsText: String
    get() = name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("")
}

/**
 * Pagination state for the AryaSamaj home screen
 */
@Serializable
data class AryaSamajHomePageState(
  val isLoading: Boolean = false,
  val error: String? = null,
  val items: List<AryaSamajHomeListItem> = emptyList(),
  val hasNextPage: Boolean = false,
  val endCursor: String? = null,
  val totalCount: Int = 0
) {
  val hasData: Boolean = items.isNotEmpty()
  val isEmpty: Boolean = items.isEmpty()
  val needsRefresh: Boolean = !hasData && !isLoading
}

/**
 * Filter state for address-based filtering
 */
@Serializable
data class AryaSamajHomeFilterState(
  val searchQuery: String = "",
  val selectedState: String = "",
  val selectedDistrict: String = "",
  val selectedVidhansabha: String = ""
) {
  val hasActiveFilters: Boolean =
    searchQuery.isNotBlank() ||
      selectedState.isNotBlank() ||
      selectedDistrict.isNotBlank() ||
      selectedVidhansabha.isNotBlank()
}

/**
 * UI state for the AryaSamaj home screen
 */
@Serializable
data class AryaSamajHomeUiState(
  val pageState: AryaSamajHomePageState = AryaSamajHomePageState(),
  val filterState: AryaSamajHomeFilterState = AryaSamajHomeFilterState(),
  val totalCount: Int = 0,
  val isRefreshing: Boolean = false
) {
  val isLoading: Boolean = pageState.isLoading
  val error: String? = pageState.error
  val items: List<AryaSamajHomeListItem> = pageState.items
  val hasNextPage: Boolean = pageState.hasNextPage
  val isEmpty: Boolean = pageState.isEmpty && !isLoading
  val hasData: Boolean = pageState.hasData
}
