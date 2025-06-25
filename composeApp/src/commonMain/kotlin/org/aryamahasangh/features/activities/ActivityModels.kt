package org.aryamahasangh.features.activities

import kotlinx.datetime.*
import kotlinx.serialization.SerialName
import org.aryamahasangh.OrganisationalActivityDetailByIdQuery
import org.aryamahasangh.fragment.OrganisationalActivityShort
import org.aryamahasangh.type.ActivityType
import org.aryamahasangh.type.ActivityType.*

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
  val email: String = "",
  val address: String = "",
  val district: String = "",
  val state: String = "",
  val pincode: String = "",
  val addressId: String = ""
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

enum class ActivityStatus {
  PAST,
  ONGOING,
  UPCOMING;

  fun toDisplayName(): String {
    return when (this) {
      PAST -> "समाप्त"
      ONGOING -> "चल रही है"
      UPCOMING -> "आगामी"
    }
  }
}

fun ActivityType.toDisplayName(): String {
  return when (this) {
    SESSION -> "सत्र"
    CAMP -> "शिविर"
    COURSE -> "कक्षा"
    EVENT -> "कार्यक्रम"
    CAMPAIGN -> "अभियान"
    UNKNOWN__ -> "कार्यक्रम"
  }
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
  val associatedOrganisations: List<AssociatedOrganisation>,
  val overviewDescription: String? = null,
  val overviewMediaUrls: List<String> = emptyList()
) {
  companion object
}

fun OrganisationalActivity.getStatus(): ActivityStatus {
  val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  return when {
    currentTime < startDatetime -> ActivityStatus.UPCOMING
    currentTime > endDatetime -> ActivityStatus.PAST
    else -> ActivityStatus.ONGOING
  }
}

fun OrganisationalActivity.isFromPast() = getStatus() == ActivityStatus.PAST

fun OrganisationalActivity.isUpcoming() = getStatus() == ActivityStatus.UPCOMING

fun OrganisationalActivity.hasOverview(): Boolean {
  return !overviewDescription.isNullOrEmpty() || overviewMediaUrls.isNotEmpty()
}

fun OrganisationalActivityShort.getStatus(): ActivityStatus {
  val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  return when {
    currentTime < startDatetime.toLocalDateTime() -> ActivityStatus.UPCOMING
    currentTime > endDatetime.toLocalDateTime() -> ActivityStatus.PAST
    else -> ActivityStatus.ONGOING
  }
}

fun Instant.toLocalDateTime(): LocalDateTime {
  return this.toLocalDateTime(TimeZone.currentSystemDefault())
}

fun OrganisationalActivity.Companion.camelCased(
  node: OrganisationalActivityDetailByIdQuery.Node
): OrganisationalActivity {
  val organisationalActivityShort = node.organisationalActivityShort
  return OrganisationalActivity(
    id = organisationalActivityShort.id,
    name = organisationalActivityShort.name,
    type = organisationalActivityShort.type,
    shortDescription = organisationalActivityShort.shortDescription,
    district = organisationalActivityShort.district ?: "",
    state = node.state!!,
    address = node.address!!,
    capacity = node.capacity!!,
    latitude = node.latitude,
    longitude = node.longitude,
    mediaFiles = node.mediaFiles.map { it ?: "" },
    endDatetime = organisationalActivityShort.endDatetime.toLocalDateTime(),
    allowedGender = node.allowedGender!!.name,
    startDatetime = organisationalActivityShort.startDatetime.toLocalDateTime(),
    longDescription = node.longDescription!!,
    additionalInstructions = node.additionalInstructions ?: "",
    contactPeople =
      node.activityMemberCollection?.edges?.map {
        val member = it.node.member!!
        ActivityMember(
          id = it.node.id.toString(),
          post = it.node.post.toString(),
          member =
            Member(
              id = member.id,
              name = member.name!!,
              phoneNumber = member.phoneNumber ?: "",
              profileImage = member.profileImage ?: ""
            ),
          priority = it.node.priority!!
        )
      }!!,
    associatedOrganisations =
      node.organisationalActivityCollection?.edges?.map {
        val (id, name) = it.node.organisation!!
        AssociatedOrganisation(
          id = it.node.id,
          organisation = Organisation(id = id, name = name!!)
        )
      }!!,
    overviewDescription = node.overviewDescription ?: null,
    overviewMediaUrls = node.overviewMediaUrls.map { it ?: "" }
  )
}

fun getLocalDateTime(timestamptzStr: Any): LocalDateTime {
  val dateTimeStr = timestamptzStr as String
  // Parse as Instant to handle timezone information
  val instant = Instant.parse(dateTimeStr)
  // Convert to LocalDateTime in the system's default timezone
  return instant.toLocalDateTime(TimeZone.currentSystemDefault())
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
  val allowedGender: GenderAllowed = GenderAllowed.ANY,
  val mediaFiles: List<String>,
  val shortDescription: String,
  val longDescription: String,
  val startDatetime: LocalDateTime,
  val endDatetime: LocalDateTime,
  val additionalInstructions: String,
  val contactPeople: List<ActivityMember>,
  val associatedOrganisations: List<Organisation>
)
