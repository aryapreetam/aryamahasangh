package org.aryamahasangh.features.admin

data class MemberShort(
  val id: String,
  val name: String,
  val profileImage: String,
  val place: String = "",
)
