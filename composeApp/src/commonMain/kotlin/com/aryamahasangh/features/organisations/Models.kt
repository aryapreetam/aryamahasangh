package com.aryamahasangh.features.organisations

import com.aryamahasangh.features.activities.Member

data class OrganisationDetail(
  val id: String,
  val name: String,
  val description: String,
  val logo: String? = null,
  val members: List<OrganisationalMember> = emptyList()
)

data class OrganisationWithDescription(
  val id: String,
  val name: String,
  val description: String
)

data class OrganisationalMember(
  val id: String = "",
  val member: Member,
  val post: String = "",
  val priority: Int = 0
)
