package org.aryamahasangh

import com.expediagroup.graphql.generator.scalars.ID
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Organisation(
  val name: String,
  val logo: String,
  val description: String,
  val keyPeople: List<OrganisationalMember> = listOf(),
  val activities: List<OrganisationalActivity> = listOf(),
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
  CAMPAIGN
}

@OptIn(ExperimentalUuidApi::class)
data class OrganisationalActivity(
  val id: ID,
  val name: String,
  val description: String,
  val activityType: ActivityType,
  val place: String,
  /**
   * list of organisations associated with this activity.
   * represented by orgId
   */
  val associatedOrganisation: List<Uuid> = listOf(),
  val startDateTime: LocalDateTime,
  val endDateTime: LocalDateTime,
  /**
   * links to images and videos
   */
  val mediaFiles: List<String> =  listOf(),
  val contactPeople: List<OrganisationalMember> = listOf(),
  val additionalInstructions: String = ""
)

data class OrganisationalMember(
  val member: Member,
  val post: String,
  val priority: Int
)

data class Member(
  val name: String,
  val educationalQualification: String? = "",
  val profileImage: String?,
  val phoneNumber: String? = "",
  val email: String? = ""
)

