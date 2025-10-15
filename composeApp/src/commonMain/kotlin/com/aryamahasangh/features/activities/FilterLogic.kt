package com.aryamahasangh.features.activities

import com.apollographql.apollo.api.Optional
import com.aryamahasangh.type.*

/**
 * Builds a combined GraphQL filter based on selected filter options and search term.
 * Handles smart combinations and search integration.
 *
 * @param selectedFilters Set of selected filter options
 * @param searchTerm Optional search term to combine with filters
 * @return GraphQL filter or null if no filtering needed
 */
fun buildCombinedFilter(
  selectedFilters: Set<ActivityFilterOption>,
  searchTerm: String? = null
): ActivitiesWithStatusFilter? {
  // If ShowAll is selected or no filters, only apply search filter
  if (selectedFilters.contains(ActivityFilterOption.ShowAll) || selectedFilters.isEmpty()) {
    return buildSearchFilter(searchTerm)
  }

  val filterClauses = mutableListOf<ActivitiesWithStatusFilter>()

  // Add search filter if present
  buildSearchFilter(searchTerm)?.let { searchFilter ->
    filterClauses.add(searchFilter)
  }

  // Build activity type filters
  val activityTypeFilter = buildActivityTypeFilter(selectedFilters)
  activityTypeFilter?.let { typeFilter ->
    filterClauses.add(typeFilter)
  }

  return when {
    filterClauses.isEmpty() -> null
    filterClauses.size == 1 -> filterClauses.first()
    else -> ActivitiesWithStatusFilter(
      and = Optional.present(filterClauses)
    )
  }
}

/**
 * Builds search filter for name, shortDescription, district, and state
 */
private fun buildSearchFilter(searchTerm: String?): ActivitiesWithStatusFilter? {
  if (searchTerm.isNullOrBlank()) return null

  val searchPattern = "%${searchTerm.trim()}%"

  return ActivitiesWithStatusFilter(
    or = Optional.present(
      listOf(
        ActivitiesWithStatusFilter(
          name = Optional.present(StringFilter(ilike = Optional.present(searchPattern)))
        ),
        ActivitiesWithStatusFilter(
          shortDescription = Optional.present(StringFilter(ilike = Optional.present(searchPattern)))
        ),
        ActivitiesWithStatusFilter(
          district = Optional.present(StringFilter(ilike = Optional.present(searchPattern)))
        ),
        ActivitiesWithStatusFilter(
          state = Optional.present(StringFilter(ilike = Optional.present(searchPattern)))
        )
      )
    )
  )
}

/**
 * Builds activity type filters with direct type mapping.
 * Each filter option maps to exactly one ActivityType.
 * Multiple selections use OR logic.
 */
private fun buildActivityTypeFilter(selectedFilters: Set<ActivityFilterOption>): ActivitiesWithStatusFilter? {
  val filterClauses = mutableListOf<ActivitiesWithStatusFilter>()

  selectedFilters.forEach { filter ->
    when (filter) {
      // Session type filters - each maps to its own ActivityType
      is ActivityFilterOption.AryaTraining -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.SESSION)))
          )
        )
      }

      is ActivityFilterOption.ProtectorTraining -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.PROTECTION_SESSION)))
          )
        )
      }

      is ActivityFilterOption.BodhSession -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.BODH_SESSION)))
          )
        )
      }

      // Camp type filters with gender restrictions
      is ActivityFilterOption.MaleTraining -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            and = Optional.present(
              listOf(
                ActivitiesWithStatusFilter(
                  type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.CAMP)))
                ),
                ActivitiesWithStatusFilter(
                  allowedGender = Optional.present(GenderFilterFilter(eq = Optional.present(GenderFilter.MALE)))
                )
              )
            )
          )
        )
      }

      is ActivityFilterOption.FemaleTraining -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            and = Optional.present(
              listOf(
                ActivitiesWithStatusFilter(
                  type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.CAMP)))
                ),
                ActivitiesWithStatusFilter(
                  allowedGender = Optional.present(GenderFilterFilter(eq = Optional.present(GenderFilter.FEMALE)))
                )
              )
            )
          )
        )
      }

      // Other activity type filters
      is ActivityFilterOption.Campaign -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.CAMPAIGN)))
          )
        )
      }

      is ActivityFilterOption.Course -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.COURSE)))
          )
        )
      }

      is ActivityFilterOption.Event -> {
        filterClauses.add(
          ActivitiesWithStatusFilter(
            type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.EVENT)))
          )
        )
      }

      // ShowAll is handled at the top level, ignore here
      is ActivityFilterOption.ShowAll -> { /* Ignored - handled in buildCombinedFilter */ }
    }
  }

  return when {
    filterClauses.isEmpty() -> null
    filterClauses.size == 1 -> filterClauses.first()
    // Multiple filters use OR logic - show activities matching ANY selected filter
    else -> ActivitiesWithStatusFilter(
      or = Optional.present(filterClauses)
    )
  }
}
