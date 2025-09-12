package com.aryamahasangh.features.activities

import com.aryamahasangh.type.ActivitiesWithStatusFilter
import com.aryamahasangh.type.ActivityTypeFilter
import com.aryamahasangh.type.ActivityType
import com.aryamahasangh.type.GenderFilterFilter
import com.aryamahasangh.type.GenderFilter
import com.aryamahasangh.type.StringFilter
import com.apollographql.apollo.api.Optional

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
 * Builds activity type filters with smart SESSION combination logic
 */
private fun buildActivityTypeFilter(selectedFilters: Set<ActivityFilterOption>): ActivitiesWithStatusFilter? {
  val sessionFilters = selectedFilters.filterIsInstance<ActivityFilterOption>().filter {
    it is ActivityFilterOption.AryaTraining || it is ActivityFilterOption.BodhSession
  }

  val otherFilters = selectedFilters.filterIsInstance<ActivityFilterOption>().filterNot {
    it is ActivityFilterOption.AryaTraining || it is ActivityFilterOption.BodhSession
  }

  val filterClauses = mutableListOf<ActivitiesWithStatusFilter>()

  // Handle SESSION type filters with smart combination
  when {
    // Both SESSION types selected → just SESSION type (no name filter needed)
    sessionFilters.size >= 2 -> {
      filterClauses.add(
        ActivitiesWithStatusFilter(
          type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.SESSION)))
        )
      )
    }
    // Only AryaTraining selected → SESSION type + name does NOT contain "बोध"
    sessionFilters.contains(ActivityFilterOption.AryaTraining) -> {
      filterClauses.add(
        ActivitiesWithStatusFilter(
          and = Optional.present(
            listOf(
              ActivitiesWithStatusFilter(
                type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.SESSION)))
              ),
              ActivitiesWithStatusFilter(
                not = Optional.present(
                  ActivitiesWithStatusFilter(
                    name = Optional.present(StringFilter(ilike = Optional.present("%बोध%")))
                  )
                )
              )
            )
          )
        )
      )
    }
    // Only BodhSession selected → SESSION type + name contains "बोध"
    sessionFilters.contains(ActivityFilterOption.BodhSession) -> {
      filterClauses.add(
        ActivitiesWithStatusFilter(
          and = Optional.present(
            listOf(
              ActivitiesWithStatusFilter(
                type = Optional.present(ActivityTypeFilter(eq = Optional.present(ActivityType.SESSION)))
              ),
              ActivitiesWithStatusFilter(
                name = Optional.present(StringFilter(ilike = Optional.present("%बोध%")))
              )
            )
          )
        )
      )
    }
  }

  // Handle other filter types
  otherFilters.forEach { filter ->
    when (filter) {
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

      else -> { /* ShowAll and unknown types ignored */
      }
    }
  }

  return when {
    filterClauses.isEmpty() -> null
    filterClauses.size == 1 -> filterClauses.first()
    else -> ActivitiesWithStatusFilter(
      or = Optional.present(filterClauses)
    )
  }
}
