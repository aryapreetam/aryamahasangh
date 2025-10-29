package com.aryamahasangh.nhost.models

import kotlinx.serialization.Serializable

/**
 * Request body for sign in with email and password
 */
@Serializable
data class SignInRequest(
  val email: String,
  val password: String
)

/**
 * Response from sign in endpoint
 */
@Serializable
data class SignInResponse(
  val session: Session
)

/**
 * Request body for token refresh
 */
@Serializable
data class RefreshTokenRequest(
  val refreshToken: String
)

/**
 * Response from token refresh endpoint
 */
@Serializable
data class RefreshTokenResponse(
  val session: Session
)

/**
 * Represents an error response from NHost API
 */
@Serializable
data class ErrorResponse(
  val message: String,
  val status: Int? = null,
  val error: String? = null
)

