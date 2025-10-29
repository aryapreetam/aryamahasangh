package com.aryamahasangh.nhost.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents an authentication session in NHost
 */
@Serializable
data class Session(
  val accessToken: String,
  val refreshToken: String,
  val accessTokenExpiresIn: Long, // in seconds
  val refreshTokenId: String? = null,
  val user: User
) {
  /**
   * Calculate when the access token expires
   */
  fun getExpiresAt(issuedAt: Instant): Instant {
    return Instant.fromEpochSeconds(issuedAt.epochSeconds + accessTokenExpiresIn)
  }

  /**
   * Check if access token is expired or about to expire
   */
  fun isExpiring(now: Instant, expiresAt: Instant, bufferSeconds: Long = 60): Boolean {
    val expiryWithBuffer = Instant.fromEpochSeconds(expiresAt.epochSeconds - bufferSeconds)
    return now >= expiryWithBuffer
  }
}

