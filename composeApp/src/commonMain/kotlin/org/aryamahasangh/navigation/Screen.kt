package org.aryamahasangh.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
  @Serializable
  data object Orgs: Screen()
  @Serializable
  data object Activities: Screen()
  @Serializable
  data class ActivityDetails(val id: String): Screen()
  @Serializable
  object Navigation: Screen()
  @Serializable
  data object JoinUs: Screen()
  @Serializable
  data object Learning: Screen()
  @Serializable
  data class OrgDetails(val name: String): Screen()
  @Serializable
  data object AboutUs: Screen()
  @Serializable
  data object ContactUs: Screen()
}