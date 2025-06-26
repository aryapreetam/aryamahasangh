package org.aryamahasangh.features.admin

import kotlinx.datetime.LocalDateTime
import org.aryamahasangh.fragment.AryaSamajFields

data class MemberShort(
  val id: String,
  val name: String,
  val profileImage: String,
  val place: String = ""
)

data class MemberDetail(
  val id: String,
  val name: String,
  val profileImage: String,
  val phoneNumber: String,
  val educationalQualification: String,
  val email: String,
  val address: String,
  val district: String,
  val state: String,
  val pincode: String,
  val organisations: List<OrganisationInfo>,
  val activities: List<ActivityInfo>,
  val aryaSamaj: AryaSamajFields?
)

data class OrganisationInfo(
  val id: String,
  val name: String,
  val logo: String
)

data class ActivityInfo(
  val id: String,
  val name: String,
  val district: String,
  val state: String,
  val startDatetime: LocalDateTime,
  val endDatetime: LocalDateTime
)
