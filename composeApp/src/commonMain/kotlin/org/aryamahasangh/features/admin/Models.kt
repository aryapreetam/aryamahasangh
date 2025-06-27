package org.aryamahasangh.features.admin

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.aryamahasangh.components.Gender
import org.aryamahasangh.fragment.AddressFields
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
  val dob: LocalDate?,
  val joiningDate: LocalDate?,
  val gender: Gender?,
  val introduction: String,
  val occupation: String,
  val referrerId: String?,
  val referrer: ReferrerInfo?,
  val address: String, // Formatted address string for display
  val addressFields: AddressFields?, // Full address details for editing
  val tempAddress: String, // Formatted temp address string
  val tempAddressFields: AddressFields?, // Full temp address details for editing
  val district: String,
  val state: String,
  val pincode: String,
  val organisations: List<OrganisationInfo>,
  val activities: List<ActivityInfo>,
  val aryaSamaj: AryaSamajFields?,
  val samajPositions: List<SamajPositionInfo> // New field for Arya Samaj positions
)

data class ReferrerInfo(
  val id: String,
  val name: String,
  val profileImage: String
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

data class SamajPositionInfo(
  val id: String,
  val post: String,
  val priority: Int,
  val aryaSamaj: AryaSamajFields
)
