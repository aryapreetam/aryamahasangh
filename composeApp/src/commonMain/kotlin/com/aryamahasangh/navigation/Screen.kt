package com.aryamahasangh.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
  @Serializable
  data object OrgsSection : Screen()

  @Serializable
  data object Orgs : Screen()

  @Serializable
  data object ActivitiesSection : Screen()

  @Serializable
  data class Activities(val initialFilter: String? = null) : Screen()
  @Serializable
  data object CreateActivity : Screen()
  @Serializable
  data class ActivityDetails(val id: String) : Screen()

  @Serializable
  data class EditActivity(val id: String) : Screen()

  @Serializable
  data class CreateOverviewForm(
    val activityId: String,
    val existingOverview: String? = null,
    val existingMediaUrls: List<String> = emptyList()
  ) : Screen()

  @Serializable
  data object JoinUs : Screen()

  @Serializable
  data object Learning : Screen()

  @Serializable
  data object LearningSection : Screen()

  @Serializable
  data object AryaGurukulSection : Screen()

  @Serializable
  data object AryaaGurukulSection : Screen()

  @Serializable
  data object AryaGurukulCollege : Screen()

  @Serializable
  data object AryaaGurukulCollege : Screen()

  @Serializable
  data object BookSection : Screen()

  @Serializable
  data object BookOrderForm : Screen()

  @Serializable
  data class BookOrderDetails(val bookOrderId: String) : Screen()

  @Serializable
  data object AryaNirmanSection : Screen()

  @Serializable
  data object AryaNirmanHome : Screen()

  @Serializable
  data class AryaNirmanRegistrationForm(val activityId: String, val capacity: Int) : Screen()

  @Serializable
  data object AryaPariwarSection : Screen()

  @Serializable
  data object AryaPariwarHome : Screen()

  @Serializable
  data object AryaSamajSection : Screen()

  @Serializable
  data object AryaSamajHome : Screen()

  @Serializable
  data object AddAryaSamajForm : Screen()

  @Serializable
  data class EditAryaSamajForm(val aryaSamajId: String) : Screen()

  @Serializable
  data class AryaSamajDetail(val aryaSamajId: String) : Screen()

  @Serializable
  data class VideoDetails(val learningItemId: String) : Screen()

  @Serializable
  data class OrgDetails(val organisationId: String) : Screen()

  @Serializable
  data class NewOrganisationForm(val priority: Int) : Screen()

  @Serializable
  data object CreateOrganisation : Screen()

  @Serializable
  data object AboutSection : Screen()

  @Serializable
  data object AboutUs : Screen()

  @Serializable
  data class AboutUsDetails(val organisationId: String) : Screen()

  @Serializable
  data object AdmissionForm : Screen()

  @Serializable
  data object AdminSection : Screen()

  @Serializable
  data class AdminContainer(val id: Int = 0) : Screen()

  @Serializable
  data class MemberDetail(val memberId: String) : Screen()

  @Serializable
  data object Members : Screen()

  @Serializable
  data object AddMemberForm : Screen()

  @Serializable
  data class EditMemberForm(val memberId: String) : Screen()

  @Serializable
  data object CreateFamilyForm : Screen()

  @Serializable
  data class EditFamilyForm(val familyId: String) : Screen()

  @Serializable
  data class FamilyDetail(val familyId: String) : Screen()

  @Serializable
  data object KshatraTrainingSection : Screen()

  @Serializable
  data object KshatraTrainingHome : Screen()

  @Serializable
  data object ChatraTrainingSection : Screen()

  @Serializable
  data object ChatraTrainingHome : Screen()
}
