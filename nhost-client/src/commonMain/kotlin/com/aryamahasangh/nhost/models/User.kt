package com.aryamahasangh.nhost.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a user in NHost
 */
@Serializable
data class User(
  val id: String,
  val email: String? = null,
  val displayName: String,
  val avatarUrl: String,
  val createdAt: Instant,
  val locale: String,
  val isAnonymous: Boolean,
  val defaultRole: String,
  val roles: List<String>,
  val emailVerified: Boolean,
  val phoneNumber: String? = null,
  val phoneNumberVerified: Boolean,
  val activeMfaType: String? = null,
  val metadata: Map<String, String>? = null
)

