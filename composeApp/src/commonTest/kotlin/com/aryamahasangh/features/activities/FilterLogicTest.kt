package com.aryamahasangh.features.activities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FilterLogicTest {

  @Test
  fun buildCombinedFilter_withShowAllFilter_returnsNull() {
    val filters = setOf(ActivityFilterOption.ShowAll)
    val result = buildCombinedFilter(filters)

    assertNull(result, "ShowAll filter should return null (no filtering)")
  }

  @Test
  fun buildCombinedFilter_withEmptyFilters_returnsNull() {
    val filters = emptySet<ActivityFilterOption>()
    val result = buildCombinedFilter(filters)

    assertNull(result, "Empty filters should return null (no filtering)")
  }

  @Test
  fun buildCombinedFilter_withSingleFilter_returnsNonNull() {
    val filters = setOf(ActivityFilterOption.Campaign)
    val result = buildCombinedFilter(filters)

    assertNotNull(result, "Single filter should return non-null filter")
  }

  @Test
  fun buildCombinedFilter_withSearchTerm_returnsNonNull() {
    val filters = emptySet<ActivityFilterOption>()
    val result = buildCombinedFilter(filters, "test")

    assertNotNull(result, "Search term should return non-null filter")
  }

  @Test
  fun buildCombinedFilter_withBothSessionFilters_returnsNonNull() {
    val filters = setOf(
      ActivityFilterOption.AryaTraining,
      ActivityFilterOption.BodhSession
    )
    val result = buildCombinedFilter(filters)

    assertNotNull(result, "Both SESSION type filters should return non-null filter")
  }

  @Test
  fun buildCombinedFilter_withMixedFilters_returnsNonNull() {
    val filters = setOf(
      ActivityFilterOption.Campaign,
      ActivityFilterOption.Course,
      ActivityFilterOption.MaleTraining
    )
    val result = buildCombinedFilter(filters)

    assertNotNull(result, "Mixed filters should return non-null filter")
  }

  @Test
  fun buildCombinedFilter_withSearchAndFilters_returnsNonNull() {
    val filters = setOf(ActivityFilterOption.BodhSession)
    val result = buildCombinedFilter(filters, "search term")

    assertNotNull(result, "Search + filters should return non-null filter")
  }
}
