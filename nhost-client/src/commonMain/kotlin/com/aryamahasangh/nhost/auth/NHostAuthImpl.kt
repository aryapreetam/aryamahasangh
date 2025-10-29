package com.aryamahasangh.nhost.auth

import com.aryamahasangh.nhost.models.*
import com.aryamahasangh.nhost.storage.SecureStorage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Implementation of NHost authentication
 */
internal class NHostAuthImpl(
  private val httpClient: HttpClient,
  private val baseUrl: String,
  private val secureStorage: SecureStorage,
  private val scope: CoroutineScope,
  private val autoLoadSession: Boolean = true,
  refreshBeforeExpiry: Long = 60
) : NHostAuth {

  private val sessionManager = SessionManager(
    secureStorage = secureStorage,
    refreshBeforeExpiry = refreshBeforeExpiry,
    scope = scope,
    onRefreshToken = ::refreshTokenInternal
  )

  override val currentSession: StateFlow<Session?> = sessionManager.currentSession

  override val currentUser: StateFlow<User?> = currentSession
    .map { it?.user }
    .stateIn(scope, SharingStarted.Eagerly, null)

  override val isAuthenticated: Boolean
    get() = currentSession.value != null

  init {
    if (autoLoadSession) {
      scope.launch {
        sessionManager.restoreSession()
      }
    }
  }

  override suspend fun signIn(email: String, password: String): Result<Session> {
    return try {
      val response = httpClient.post("$baseUrl/v1/signin/email-password") {
        contentType(ContentType.Application.Json)
        setBody(SignInRequest(email, password))
      }

      if (response.status.isSuccess()) {
        val signInResponse = response.body<SignInResponse>()
        sessionManager.updateSession(signInResponse.session)
        Result.success(signInResponse.session)
      } else {
        val errorResponse = try {
          response.body<ErrorResponse>()
        } catch (e: Exception) {
          ErrorResponse("Sign in failed", response.status.value)
        }
        Result.failure(Exception(errorResponse.message))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun signOut(): Result<Unit> {
    return try {
      // Call sign out endpoint
      val response = httpClient.post("$baseUrl/v1/signout") {
        contentType(ContentType.Application.Json)
      }

      // Clear session regardless of response
      sessionManager.clearSession()

      if (response.status.isSuccess()) {
        Result.success(Unit)
      } else {
        // Still return success since session is cleared
        Result.success(Unit)
      }
    } catch (e: Exception) {
      // Clear session even on error
      sessionManager.clearSession()
      Result.success(Unit)
    }
  }

  override fun getAccessToken(): String? {
    return sessionManager.getAccessToken()
  }

  override suspend fun refreshToken(): Result<Session> {
    val refreshToken = secureStorage.getString(SecureStorage.KEY_REFRESH_TOKEN)
      ?: return Result.failure(Exception("No refresh token available"))

    return refreshTokenInternal(refreshToken)
  }

  /**
   * Internal token refresh implementation
   */
  private suspend fun refreshTokenInternal(refreshToken: String): Result<Session> {
    return try {
      val response = httpClient.post("$baseUrl/v1/token") {
        contentType(ContentType.Application.Json)
        setBody(RefreshTokenRequest(refreshToken))
      }

      if (response.status.isSuccess()) {
        val refreshResponse = response.body<RefreshTokenResponse>()
        sessionManager.updateSession(refreshResponse.session)
        Result.success(refreshResponse.session)
      } else {
        val errorResponse = try {
          response.body<ErrorResponse>()
        } catch (e: Exception) {
          ErrorResponse("Token refresh failed", response.status.value)
        }
        Result.failure(Exception(errorResponse.message))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

