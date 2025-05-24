package org.aryamahasangh.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
  @Serializable
  data object OrgsSection: Screen()
  @Serializable
  data object Orgs: Screen()
  @Serializable
  data object ActivitiesSection: Screen()
  @Serializable
  data object Activities: Screen()
  @Serializable
  data class ActivityDetails(val id: String): Screen()
  @Serializable
  data object JoinUs: Screen()
  @Serializable
  data object Learning: Screen()
  @Serializable
  data object LearningSection: Screen()

  @Serializable
  data object AryaGurukulSection: Screen()

  @Serializable
  data object AryaaGurukulSection: Screen()
  @Serializable
  data object AryaGurukulCollege: Screen()
  @Serializable
  data object AryaaGurukulCollege: Screen()
  @Serializable
  data object BookSection: Screen()
  @Serializable
  data object BookOrderForm: Screen()
  @Serializable
  data class BookOrderDetails(val bookOrderId: String): Screen()
  @Serializable
  data object AryaNirmanSection: Screen()
  @Serializable
  data object AryaNirmanHome: Screen()

  @Serializable
  data object AryaPariwarSection: Screen()

  @Serializable
  data object AryaPariwarHome: Screen()
  @Serializable
  data object AryaSamajSection: Screen()
  @Serializable
  data object AryaSamajHome: Screen()
  @Serializable
  data class VideoDetails(val learningItemId: String): Screen()
  @Serializable
  data class OrgDetails(val name: String): Screen()
  @Serializable
  data object AboutSection: Screen()
  @Serializable
  data object AboutUs: Screen()
  @Serializable
  data object AboutUsDetails: Screen()
  @Serializable
  data object AdmissionForm: Screen()
}