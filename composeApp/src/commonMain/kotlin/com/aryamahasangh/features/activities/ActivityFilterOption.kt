package com.aryamahasangh.features.activities

/**
 * Sealed class representing all available activity filter options.
 * Each option maps to specific GraphQL filter criteria.
 */
sealed class ActivityFilterOption(val displayName: String) {
  /**
   * Default option - shows all activities without any filtering
   */
  object ShowAll : ActivityFilterOption("सभी गतिविधियाँ दिखाएं")

  /**
   * Shows SESSION type activities that do NOT contain "बोध" in the name
   * GraphQL: type == SESSION && name does NOT contain "बोध"
   */
  object AryaTraining : ActivityFilterOption("आर्य प्रशिक्षण सत्र")

  /**
   * Shows SESSION type activities that contain "बोध" in the name
   * GraphQL: type == SESSION && name contains "बोध"
   */
  object BodhSession : ActivityFilterOption("बोध सत्र")

  /**
   * Shows CAMP type activities for male participants
   * GraphQL: type == CAMP && allowedGender == MALE
   */
  object MaleTraining : ActivityFilterOption("क्षात्र प्रशिक्षण")

  /**
   * Shows CAMP type activities for female participants
   * GraphQL: type == CAMP && allowedGender == FEMALE
   */
  object FemaleTraining : ActivityFilterOption("आर्य वीरांगना प्रशिक्षण")

  /**
   * Shows CAMPAIGN type activities
   * GraphQL: type == CAMPAIGN
   */
  object Campaign : ActivityFilterOption("अभियान")

  /**
   * Shows COURSE type activities
   * GraphQL: type == COURSE
   */
  object Course : ActivityFilterOption("कक्षा")

  /**
   * Shows EVENT type activities
   * GraphQL: type == EVENT
   */
  object Event : ActivityFilterOption("कार्यक्रम")

  companion object {
    /**
     * Returns all available filter options
     */
    fun getAllOptions(): List<ActivityFilterOption> = listOf(
      ShowAll,
      AryaTraining,
      BodhSession,
      MaleTraining,
      FemaleTraining,
      Campaign,
      Course,
      Event
    )

    /**
     * Returns all filter options except ShowAll
     */
    fun getFilterableOptions(): List<ActivityFilterOption> =
      getAllOptions().filterNot { it is ShowAll }
  }
}
