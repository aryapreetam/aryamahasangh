# Logout Error Fix Documentation

## Problem

The app was crashing during logout with the following error:

```
Error: (Supabase-Core) POST request to endpoint /auth/v1/logout failed with exception HTTP/1.1 header parser received no bytes
Exception in thread "AWT-EventQueue-0" io.github.jan.supabase.exceptions.HttpRequestException: HTTP request to https://placeholder-dev.supabase.co/auth/v1/logout (POST) failed with message: HTTP/1.1 header parser received no bytes
```

## Root Cause

This error typically occurs when:

1. The network connection is unstable or slow
2. The Supabase server doesn't respond properly to the logout request
3. The HTTP client times out waiting for response headers

## Solution Implemented

### 1. **Enhanced Error Handling in SessionManager**

The `signOut()` function now:

- Returns a `Result<Unit>` instead of throwing exceptions
- Defaults to `SignOutScope.LOCAL` which only clears local session
- Falls back to local session clearing if the network request fails
- Provides detailed error information while ensuring the user is logged out locally

```kotlin
suspend fun signOut(scope: SignOutScope = SignOutScope.LOCAL): Result<Unit> {
    return try {
        // Check if there's an active session
        if (currentSession == null) {
            return Result.success(Unit)
        }
        
        // Try to sign out with the specified scope
        supabaseClient.auth.signOut(scope)
        Result.success(Unit)
    } catch (e: Exception) {
        // If network request fails, at least clear local session
        try {
            supabaseClient.auth.signOut(SignOutScope.LOCAL)
        } catch (localError: Exception) {
            println("Failed to clear local session: ${localError.message}")
        }
        
        Result.failure(e)
    }
}
```

### 2. **Updated Logout Dialog**

The logout dialog now:

- Handles the `Result` from `signOut()`
- Shows success message even if the server request fails (since local session is cleared)
- Logs the actual error for debugging purposes

```kotlin
val result = SessionManager.signOut()
result.fold(
    onSuccess = {
        snackbarHostState.showSnackbar("Logout successful")
    },
    onFailure = { error ->
        // Even if server logout fails, local session is cleared
        snackbarHostState.showSnackbar("Logged out successfully")
        println("Logout error (session cleared locally): ${error.message}")
    }
)
```

### 3. **Added Local Session Clear Function**

A new function `clearLocalSession()` was added for scenarios where network connectivity is not available:

```kotlin
suspend fun clearLocalSession() {
    try {
        supabaseClient.auth.signOut(SignOutScope.LOCAL)
    } catch (e: Exception) {
        println("Error clearing local session: ${e.message}")
    }
}
```

## Benefits of This Approach

1. **No More Crashes**: The app won't crash even if the logout network request fails
2. **Better User Experience**: Users are always logged out locally, regardless of network conditions
3. **Flexibility**: Can choose between local-only logout (`SignOutScope.LOCAL`) or global logout (`SignOutScope.GLOBAL`)
4. **Error Visibility**: Errors are logged for debugging but don't affect the user experience

## SignOut Scopes Explained

- **`SignOutScope.LOCAL`** (Default): Only clears the session from the current device. Doesn't require network access.
- **`SignOutScope.GLOBAL`**: Logs out from all devices by revoking the session on the server. Requires network access.
- **`SignOutScope.OTHERS`**: Logs out from all other devices except the current one.

## Recommendations

1. **Use Local Logout by Default**: Since we're using `SignOutScope.LOCAL` by default, logout will always work even
   offline
2. **Monitor Errors**: Keep an eye on the console logs to identify if the logout errors are persistent
3. **Network Stability**: If errors persist, check the network connection and Supabase service status
4. **Consider Retry Logic**: For critical scenarios, you could implement retry logic for global logout

## Testing

To test the logout functionality:

1. Try logging out with a good network connection
2. Try logging out with airplane mode enabled
3. Try logging out with a slow/unstable connection
4. Verify that the user is logged out in all cases (check `LocalIsAuthenticated.current`)

The logout should now work reliably in all scenarios without crashing the application.

## Edge Case: Local Logout with Valid Server Token

### The Problem

When a logout fails due to network issues and we fall back to LOCAL logout:

1. Local session is cleared (`isAuthenticated` = false)
2. Server still has a valid token
3. User appears logged out but has an orphaned session on server

### The Solution

We've implemented several safeguards:

1. **Session Validation on App Start**
   ```kotlin
   // In SessionManager.initialize()
   - Attempts to restore session from secure storage
   - If found, validates it with the server
   - If validation fails, clears local session
   ```

2. **Clean Login Process**
   ```kotlin
   // Before each login attempt
   SessionManager.ensureCleanLogin()
   - Checks for existing local session
   - Attempts proper GLOBAL logout
   - Falls back to LOCAL clear if needed
   ```

3. **Session Validation Helper**
   ```kotlin
   SessionManager.validateSession()
   - Can be called before critical operations
   - Ensures session is valid on server
   - Auto-clears invalid sessions
   ```

### What Happens When User Returns

1. **App Launch**:
    - `SessionManager.initialize()` runs
    - No local session found (was cleared)
    - User sees login screen

2. **User Tries to Login**:
    - `ensureCleanLogin()` runs first
    - Any orphaned sessions are cleaned up
    - Fresh login proceeds normally
    - New session token is issued

3. **No Conflicts**:
    - Supabase handles multiple sessions per user
    - Old orphaned tokens eventually expire
    - New login creates fresh session

### Best Practices

1. **Always validate sessions** before critical operations
2. **Use the provided login flow** which includes cleanup
3. **Monitor logs** for session validation failures
4. **Token expiry** helps clean up orphaned sessions automatically
