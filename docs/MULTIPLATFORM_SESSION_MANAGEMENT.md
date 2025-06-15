# Multi-Platform Session Management

## Overview

The Arya Mahasangh app supports multiple platforms (Android, iOS, Web, Desktop) and allows users to be logged in on
multiple devices simultaneously. This document explains how session management works across platforms.

## How Supabase Session Management Works

### Session Tokens

- Each platform/device gets its own unique session token when the user logs in
- These tokens are independent - logging out on one device doesn't affect others
- Each token has its own expiry time and refresh cycle

### SignOut Scopes Explained

1. **`SignOutScope.GLOBAL`** (Our Default)
    - Invalidates **only the current session token** on the server
    - Other devices/platforms remain logged in with their own valid tokens
    - Supabase knows about the logout event
    - The invalidated token cannot be used for API calls anymore
    - **This is what we use by default**

2. **`SignOutScope.LOCAL`**
    - Only clears the session from local storage
    - The token remains valid on the server
    - Supabase doesn't know about the logout
    - Used as fallback when network is unavailable

3. **`SignOutScope.OTHERS`**
    - Logs out all OTHER sessions except the current one
    - Useful for "log out other devices" feature

## Our Implementation

### Default Behavior

```kotlin
// User clicks logout on Android
SessionManager.signOut() // Uses GLOBAL by default

// What happens:
// ✅ Android: Session cleared, token invalidated on server
// ✅ iOS: Still logged in with its own valid token
// ✅ Web: Still logged in with its own valid token  
// ✅ Desktop: Still logged in with its own valid token
```

### Network Failure Handling

If the network request to Supabase fails:

1. We still clear the local session (security first)
2. The UI shows "Logged out successfully"
3. The error is logged for debugging
4. User experience is not affected

## Session Storage by Platform

### Android

- Uses `EncryptedSharedPreferences` (hardware-backed encryption)
- Survives app restarts
- Cleared on app uninstall

### iOS

- Uses iOS Keychain (hardware encryption)
- Survives app restarts
- Can survive app reinstalls (depending on keychain settings)

### Web

- Uses **in-memory storage only** (no localStorage)
- Session lost when browser tab/window is closed
- Most secure but less convenient

### Desktop

- Windows: Credential Manager
- macOS: Keychain
- Linux: Secret Service API
- Survives app restarts

## Multi-Device Scenarios

### Scenario 1: Normal Logout

```
User has app on: Android, iOS, Web
User logs out from Android

Result:
- Android: Logged out, must login again
- iOS: Still logged in, can continue using app
- Web: Still logged in, can continue using app
- Supabase: Knows Android session was terminated
```

### Scenario 2: Network Issue During Logout

```
User tries to logout from Android with no internet

Result:
- Android: Local session cleared, appears logged out
- Server: Still has valid token (but app can't use it)
- Other devices: Unaffected
- When network returns: Token will fail on next use
```

### Scenario 3: Security - Logout All Devices

```kotlin
// If you need to implement "logout everywhere" feature:
SessionManager.signOut(SignOutScope.OTHERS) // Logs out other devices
// Then
SessionManager.signOut(SignOutScope.LOCAL) // Log out current device
```

## Security Considerations

1. **Token Invalidation**: Using GLOBAL logout ensures tokens are invalidated server-side
2. **Fallback Safety**: Even if network fails, local session is cleared
3. **No Token Leakage**: Tokens are stored in platform-specific secure storage
4. **Session Isolation**: Each platform's session is independent

## Best Practices

1. **Always use GLOBAL logout** (default) for proper session tracking
2. **Handle network errors gracefully** (already implemented)
3. **Don't share tokens between platforms** (each gets its own)
4. **Monitor logout errors** in logs for debugging

## FAQ

**Q: If I logout on my phone, will I be logged out on my laptop?**
A: No, each device maintains its own session. Logging out on one device doesn't affect others.

**Q: What happens if I'm offline when I logout?**
A: The app will clear your local session. You'll appear logged out. The server token remains valid but unused.

**Q: Can I see all my active sessions?**
A: This would require a custom implementation. Supabase doesn't provide this out of the box.

**Q: Is it secure to stay logged in on multiple devices?**
A: Yes, each device has its own secure token storage. Compromising one device doesn't affect others.
