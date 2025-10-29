package com.aryamahasangh.nhost.auth

import com.aryamahasangh.nhost.models.Session
import com.aryamahasangh.nhost.models.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication module for NHost
 */
interface NHostAuth {
  /**
   * Current user session
   */
  val currentSession: StateFlow<Session?>

  /**
   * Current user
   */
  val currentUser: StateFlow<User?>

  /**
   * Check if user is authenticated
   */
  val isAuthenticated: Boolean

  /**
   * Sign in with email and password
   */
  suspend fun signIn(email: String, password: String): Result<Session>

  /**
   * Sign out current user
   */
  suspend fun signOut(): Result<Unit>

  /**
   * Get current access token
   */
  fun getAccessToken(): String?

  /**
   * Manually refresh the current token
   */
  suspend fun refreshToken(): Result<Session>
}

