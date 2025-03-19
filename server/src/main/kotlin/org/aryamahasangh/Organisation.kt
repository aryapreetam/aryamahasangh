package org.aryamahasangh

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organisation(
  val id: String,
  val name: String,
  val logo: String = "",
  val description: String = "",
  val keyPeople: List<OrganisationalMember> = listOf(),
)


enum class ActivityType {
  /**
   * Karyakram:
   */
  EVENT,
  /**
   * Satr:
   */
  SESSION,
  /**
   * Abhiyan:
   */
  CAMPAIGN,
  /**
   * कक्षा
   */
  COURSE
}

@Serializable
data class ActivityInput(
  val name: String,
  @SerialName("short_description")
  val shortDescription: String,
  @SerialName("long_description")
  val longDescription: String,
  @SerialName("type")
  val activityType: ActivityType,
  val address: String = "",
  val state: String = "",
  val district: String = "",
  val pincode: Int,
  @Serializable(with = LocalDateTimeSerializer::class)
  @SerialName("start_datetime")
  val startDateTime: LocalDateTime,
  @Serializable(with = LocalDateTimeSerializer::class)
  @SerialName("end_datetime")
  val endDateTime: LocalDateTime,
  @SerialName("media_files")
  val mediaFiles: List<String> =  listOf(),
  @SerialName("additional_instructions")
  val additionalInstructions: String = ""
)

@Serializable
data class Organisational_Activity(
  val activity_id: String,
  val organisation_id: String
)

@Serializable
data class Activity_Member(
  val activity_id: String,
  val member_id: String,
  val post: String,
  val priority: Int
)

@Serializable
data class OrganisationActivityInput(
  val name: String,
  val shortDescription: String,
  val longDescription: String,
  val activityType: ActivityType,
  val address: String = "",
  val state: String = "",
  val district: String = "",
  val pincode: Int,
  val associatedOrganisations: List<String> = listOf(),
  @Serializable(with = LocalDateTimeSerializer::class)
  val startDateTime: LocalDateTime,
  @Serializable(with = LocalDateTimeSerializer::class)
  val endDateTime: LocalDateTime,
  /**
   * links to images and videos
   */
  val mediaFiles: List<String> =  listOf(),
  val contactPeople: List<List<String>> = listOf(),
  val additionalInstructions: String = ""
)

@Serializable
data class OrganisationalActivity(
  val id: String,
  val name: String,
  @SerialName("short_description")
  val shortDescription: String,
  @SerialName("long_description")
  val longDescription: String,
  @SerialName("type")
  val activityType: ActivityType,
  val address: String = "",
  val state: String = "",
  val district: String = "",
  val pincode: Int,
  /**
   * list of organisations associated with this activity.
   * represented by orgId
   */
  @SerialName("associatedOrganisations")
  val associatedOrganisations: List<Organisation> = listOf(),
  @Serializable(with = LocalDateTimeSerializer::class)
  @SerialName("start_datetime")
  val startDateTime: LocalDateTime,
  @Serializable(with = LocalDateTimeSerializer::class)
  @SerialName("end_datetime")
  val endDateTime: LocalDateTime,
  /**
   * links to images and videos
   */
  @SerialName("media_files")
  val mediaFiles: List<String> =  listOf(),
  val contactPeople: List<OrganisationalMember> = listOf(),
  @SerialName("additional_instructions")
  val additionalInstructions: String = ""
)


@Serializable
data class OrganisationalActivityWrapper(
  val organisation: Organisation
)

@Serializable
data class ActivityMemberWrapper(
  val member: OrganisationalMember
)

@Serializable
data class OrganisationalMember(
  val id: String = "",
  val member: Member,
  val post: String = "",
  val priority: Int = 0
)

@Serializable
data class Member(
  val id: String = "",
  val name: String,
  val educationalQualification: String = "",
  @SerialName("profile_image")
  val profileImage: String = "",
  @SerialName("phone_number")
  val phoneNumber: String = "",
  val email: String = ""
)

