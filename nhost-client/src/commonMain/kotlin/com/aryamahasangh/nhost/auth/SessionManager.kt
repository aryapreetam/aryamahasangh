package com.aryamahasangh.nhost.auth

import com.aryamahasangh.nhost.models.Session
import com.aryamahasangh.nhost.storage.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Manages user session state and automatic token refresh
 */
internal class SessionManager(
  private val secureStorage: SecureStorage,
  private val refreshBeforeExpiry: Long = 60, // Refresh 60 seconds before expiry
  private val scope: CoroutineScope,
  private val onRefreshToken: suspend (String) -> Result<Session>
) {
  private val _currentSession = MutableStateFlow<Session?>(null)
  val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

  private var expiresAt: Instant? = null
  private var refreshJob: Job? = null

  /**
   * Get current access token
   */
  fun getAccessToken(): String? = _currentSession.value?.accessToken

  /**
   * Update session and schedule refresh
   */
  suspend fun updateSession(session: Session) {
    val now = Clock.System.now()
    expiresAt = session.getExpiresAt(now)

    // Save to secure storage
    secureStorage.saveString(SecureStorage.KEY_REFRESH_TOKEN, session.refreshToken)
    secureStorage.saveString(SecureStorage.KEY_ACCESS_TOKEN, session.accessToken)
    secureStorage.saveString(SecureStorage.KEY_EXPIRES_AT, expiresAt.toString())

    _currentSession.value = session

    // Schedule automatic token refresh
    scheduleTokenRefresh()
  }

  /**
   * Try to restore session from storage
   */
  suspend fun restoreSession(): Boolean {
    val refreshToken = secureStorage.getString(SecureStorage.KEY_REFRESH_TOKEN) ?: return false
    val expiresAtStr = secureStorage.getString(SecureStorage.KEY_EXPIRES_AT) ?: return false

    expiresAt = try {
      Instant.parse(expiresAtStr)
    } catch (e: Exception) {
      return false
    }

    val now = Clock.System.now()

    // Check if session is still valid or can be refreshed
    if (now >= expiresAt!!) {
      // Token expired, try to refresh
      return refreshTokenInternal(refreshToken)
    }

    // Token still valid, schedule refresh
    scheduleTokenRefresh()
    return true
  }

  /**
   * Clear session
   */
  suspend fun clearSession() {
    refreshJob?.cancel()
    refreshJob = null
    expiresAt = null
    _currentSession.value = null
    secureStorage.clear()
  }

  /**
   * Schedule automatic token refresh
   */
  private fun scheduleTokenRefresh() {
    refreshJob?.cancel()

    val expiry = expiresAt ?: return
    val now = Clock.System.now()
    val refreshAt = Instant.fromEpochSeconds(expiry.epochSeconds - refreshBeforeExpiry)

    if (refreshAt <= now) {
      // Already past refresh time, refresh immediately
      refreshJob = scope.launch {
        refreshTokenNow()
      }
    } else {
      // Schedule refresh
      val delayMillis = (refreshAt.epochSeconds - now.epochSeconds) * 1000
      refreshJob = scope.launch {
        delay(delayMillis)
        refreshTokenNow()
      }
    }
  }

  /**
   * Refresh token now
   */
  private suspend fun refreshTokenNow() {
    val refreshToken = secureStorage.getString(SecureStorage.KEY_REFRESH_TOKEN) ?: return
    refreshTokenInternal(refreshToken)
  }

  /**
   * Internal refresh implementation
   */
  private suspend fun refreshTokenInternal(refreshToken: String): Boolean {
    return try {
      val result = onRefreshToken(refreshToken)

      result.fold(
        onSuccess = { newSession ->
          updateSession(newSession)
          true
        },
        onFailure = {
          // Refresh failed, clear session
          clearSession()
          false
        }
      )
    } catch (e: Exception) {
      clearSession()
      false
    }
  }

  /**
   * Check if access token is expired or about to expire
   */
  fun isTokenExpiring(): Boolean {
    val session = _currentSession.value ?: return true
    val expiry = expiresAt ?: return true
    val now = Clock.System.now()
    return session.isExpiring(now, expiry, refreshBeforeExpiry)
  }
}

