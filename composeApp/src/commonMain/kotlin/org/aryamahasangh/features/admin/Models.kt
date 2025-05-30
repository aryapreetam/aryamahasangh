package org.aryamahasangh.features.admin

data class MemberShort(
  val id: String,
  val name: String,
  val profileImage: String,
  val place: String = "",
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
  val activities: List<ActivityInfo>
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
  val startDatetime: String,
  val endDatetime: String
)
