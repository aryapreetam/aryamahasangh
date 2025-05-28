package org.aryamahasangh.features.activities

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import org.aryamahasangh.OrganisationalActivityDetailByIdQuery
import org.aryamahasangh.fragment.OrganisationalActivityShort

data class ActivityResponse(
  val data: ActivityData?
)

data class ActivityData(
  val activitiesCollection: ActivitiesCollection?
)

data class ActivitiesCollection(
  val edges: List<ActivityEdge>?
)

data class ActivityEdge(
  val node: Activity?
)

data class Activity(
  val id: String,
  val name: String,
  val type: String,
  val state: String,
  val address: String,
  val capacity: Int,
  val district: String,
  val latitude: Double?,
  val longitude: Double?,
  val mediaFiles: List<String>,
  val endDatetime: String,
  val allowedGender: String,
  val startDatetime: String,
  val longDescription: String,
  val shortDescription: String,
  val additionalInstructions: String,
  val activityMemberCollection: ActivityMemberCollection?,
  val organisationalActivityCollection: OrganisationalActivityCollection?
)

data class ActivityMemberCollection(
  val edges: List<ActivityMemberEdge>?
)

data class ActivityMemberEdge(
  val node: ActivityMember?
)

data class ActivityMember(
  val id: String,
  val post: String,
  val member: Member,
  val priority: Int
)

data class Member(
  val id: String,
  val name: String,
  @SerialName("educational_qualification")
  val educationalQualification: String = "",
  @SerialName("profile_image")
  val profileImage: String = "",
  @SerialName("phone_number")
  val phoneNumber: String = "",
  val email: String = ""
)

data class OrganisationalActivityCollection(
  val edges: List<OrganisationalActivityEdge>?
)

data class OrganisationalActivityEdge(
  val node: AssociatedOrganisation?
)

data class AssociatedOrganisation(
  val id: String,
  val organisation: Organisation
)

data class Organisation(
  val id: String,
  val name: String
)

enum class ActivityType {
  COURSE,
  EVENT,
  CAMPAIGN,
  SESSION
}

data class OrganisationalActivity(
  val id: String,
  val name: String,
  val type: ActivityType,
  val state: String,
  val address: String,
  val capacity: Int,
  val district: String,
  val latitude: Double?,
  val longitude: Double?,
  val mediaFiles: List<String>,
  val endDatetime: LocalDateTime,
  val allowedGender: String,
  val startDatetime: LocalDateTime,
  val longDescription: String,
  val shortDescription: String,
  val additionalInstructions: String,
  val contactPeople: List<ActivityMember>,
  val associatedOrganisations: List<AssociatedOrganisation>
) {
  companion object
}

fun OrganisationalActivity.Companion.camelCased(node: OrganisationalActivityDetailByIdQuery.Node): OrganisationalActivity {
  val organisationalActivityShort = node.organisationalActivityShort.camelCased()
  return OrganisationalActivity(
    id = organisationalActivityShort.id,
    name = organisationalActivityShort.name,
    type = organisationalActivityShort.type,
    shortDescription = organisationalActivityShort.shortDescription,
    district = organisationalActivityShort.district,
    state = node.state!!,
    address = node.address!!,
    capacity = node.capacity!!,
    latitude = node.latitude,
    longitude = node.longitude,
    mediaFiles = node.media_files.map { it ?: "" },
    endDatetime = organisationalActivityShort.endDatetime,
    allowedGender = node.allowed_gender!!.name,
    startDatetime = organisationalActivityShort.startDatetime,
    longDescription = node.long_description!!,
    additionalInstructions = node.additional_instructions ?: "",
    contactPeople =
      node.activity_memberCollection?.edges?.map {
        val member = it.node.member!!
        ActivityMember(
          id = it.node.id.toString(),
          post = it.node.post.toString(),
          member =
            Member(
              id = member.id,
              name = member.name!!,
              phoneNumber = member.phone_number ?: "",
              profileImage = member.profile_image ?: ""
            ),
          priority = it.node.priority!!
        )
      }!!,
    associatedOrganisations =
      node.organisational_activityCollection?.edges?.map {
        val (id, name) = it.node.organisation!!
        AssociatedOrganisation(
          id = it.node.id,
          organisation = Organisation(id = id, name = name!!)
        )
      }!!
  )
}

fun getLocalDateTime(timestamptzStr: Any): LocalDateTime {
  val dateTimeStr = timestamptzStr as String
  // Parse as Instant to handle timezone information
  val instant = Instant.parse(dateTimeStr)
  // Convert to LocalDateTime in the system's default timezone
  return instant.toLocalDateTime(TimeZone.currentSystemDefault())
}

data class OrganisationalActivityShort(
  val id: String,
  val name: String,
  val shortDescription: String,
  val startDatetime: LocalDateTime,
  val endDatetime: LocalDateTime,
  val type: ActivityType,
  val district: String,
)

fun OrganisationalActivityShort.camelCased(): org.aryamahasangh.features.activities.OrganisationalActivityShort {
  return OrganisationalActivityShort(
    id = this.id,
    name = this.name!!,
    shortDescription = this.short_description!!,
    startDatetime = this.start_datetime!!.toLocalDateTime(TimeZone.currentSystemDefault()),
    endDatetime = this.end_datetime!!.toLocalDateTime(TimeZone.currentSystemDefault()),
    type = ActivityType.valueOf(this.type!!),
    district = this.district!!
  )
}

data class OrganisationsAndMembers(
  val members: List<Member>,
  val organisations: List<Organisation>
)

data class ActivityInputData(
  val name: String,
  val type: ActivityType,
  val state: String,
  val address: String,
  val capacity: Int,
  val district: String,
  val latitude: Double?,
  val longitude: Double?,
  val allowedGender: String = "any",
  val mediaFiles: List<String>,
  val shortDescription: String,
  val longDescription: String,
  val startDatetime: LocalDateTime,
  val endDatetime: LocalDateTime,
  val additionalInstructions: String,
  val contactPeople: List<ActivityMember>,
  val associatedOrganisations: List<Organisation>
)
