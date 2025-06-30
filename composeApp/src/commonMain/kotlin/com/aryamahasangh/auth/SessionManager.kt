package com.aryamahasangh.auth

import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.aryamahasangh.network.supabaseClient

/**
 * Manages authentication session state across the application.
 * Handles secure session persistence across app restarts on all platforms.
 */
object SessionManager {
  /**
   * Flow that emits true when user is authenticated, false otherwise
   */
  val isAuthenticated: Flow<Boolean> =
    supabaseClient.auth.sessionStatus.map { status ->
      status is SessionStatus.Authenticated
    }

  /**
   * Get the current session synchronously (may be null if not authenticated)
   */
  val currentSession get() = supabaseClient.auth.currentSessionOrNull()

  /**
   * Get the current user (may be null if not authenticated)
   */
  val currentUser get() = currentSession?.user

  /**
   * Check if user is currently authenticated
   */
  suspend fun isUserAuthenticated(): Boolean {
    return try {
      // This will attempt to refresh the session if needed
      supabaseClient.auth.refreshCurrentSession()
      currentSession != null
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Clear the local session without making a network request
   * Useful for offline scenarios or when the network request fails
   */
  suspend fun clearLocalSession() {
    try {
      // This only clears the local session without hitting the server
      supabaseClient.auth.signOut(SignOutScope.LOCAL)
    } catch (e: Exception) {
      println("Error clearing local session: ${e.message}")
    }
  }

  /**
   * Sign out the current user and clear the session
   * @param scope The scope of the sign out (default is GLOBAL which invalidates the session on server)
   * @return Result indicating success or failure
   *
   * Note: GLOBAL logout only invalidates the current session token, not all sessions for the user.
   * This means if the user is logged in on multiple devices/platforms, only the current one is logged out.
   */
  suspend fun signOut(scope: SignOutScope = SignOutScope.GLOBAL): Result<Unit> {
    return try {
      // First check if there's an active session
      if (currentSession == null) {
        // No active session, just return success
        return Result.success(Unit)
      }

      // Try to sign out with the specified scope
      // GLOBAL: Invalidates the session on Supabase server (recommended)
      // LOCAL: Only clears local storage (offline mode)
      // OTHERS: Logs out all other sessions except current one
      supabaseClient.auth.signOut(scope)
      Result.success(Unit)
    } catch (e: Exception) {
      // If network request fails, we have two options:
      // 1. Keep the session (user stays logged in) - NOT recommended
      // 2. Clear local session anyway - SAFER option

      // We choose option 2 for security reasons
      try {
        // Force clear local session even if server request fails
        // This ensures the user is logged out locally even if network is down
        supabaseClient.auth.signOut(SignOutScope.LOCAL)
      } catch (localError: Exception) {
        // Log the error but don't throw
        println("Failed to clear local session: ${localError.message}")
      }

      // Return the original error wrapped in Result
      // The UI can decide whether to show an error or success message
      Result.failure(e)
    }
  }

  /**
   * Get the access token for authenticated requests
   * Returns null if not authenticated
   */
  fun getAccessToken(): String? {
    return currentSession?.accessToken
  }

  /**
   * Initialize session management
   * This should be called once at app startup
   */
  suspend fun initialize() {
    // The SDK will automatically try to restore the session from secure storage
    // We just need to trigger it
    try {
      supabaseClient.auth.retrieveUserForCurrentSession()

      // If we have a session, validate it's still good
      if (currentSession != null) {
        try {
          // This will refresh the session and validate the token
          supabaseClient.auth.refreshCurrentSession()
        } catch (e: Exception) {
          // Session is invalid, clear it
          println("Session validation failed, clearing: ${e.message}")
          clearLocalSession()
        }
      }
    } catch (e: Exception) {
      // Session restoration failed, user needs to login again
      println("Session restoration failed: ${e.message}")
    }
  }

  /**
   * Validates if the current session is still valid on the server
   * Useful before making authenticated requests
   */
  suspend fun validateSession(): Boolean {
    return try {
      if (currentSession == null) return false

      // This will throw if the session is invalid
      supabaseClient.auth.refreshCurrentSession()
      true
    } catch (e: Exception) {
      // Session is invalid, clear local storage
      clearLocalSession()
      false
    }
  }

  /**
   * Handle login with proper session cleanup
   * This ensures any orphaned sessions are handled properly
   */
  suspend fun ensureCleanLogin() {
    // If there's a current session locally, try to sign out properly
    if (currentSession != null) {
      try {
        // Try global signout first
        supabaseClient.auth.signOut(SignOutScope.GLOBAL)
      } catch (e: Exception) {
        // If that fails, at least clear locally
        clearLocalSession()
      }
    }
  }
}
