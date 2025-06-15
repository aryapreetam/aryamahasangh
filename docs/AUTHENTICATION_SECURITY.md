# Authentication Security Implementation

## Overview

The Arya Mahasangh app implements secure authentication with platform-specific token storage to ensure user credentials
and session tokens are protected from unauthorized access.

## Security Features

### 1. PKCE Flow

- Uses Proof Key for Code Exchange (PKCE) flow for enhanced security
- Prevents authorization code interception attacks
- Especially important for mobile and web applications

### 2. Platform-Specific Secure Storage

#### Android

- **Storage**: EncryptedSharedPreferences
- **Security**: Uses Android Keystore system for encryption
- **Protection**: Tokens are encrypted at rest and protected by hardware security module when available
- **Access**: Only the app itself can access the encrypted tokens

#### iOS

- **Storage**: iOS Keychain Services
- **Security**: Hardware-encrypted storage
- **Protection**: Protected by device passcode/biometrics
- **Access**: Sandboxed to the app, inaccessible to other apps

#### Web

- **Storage**: In-memory only (no localStorage)
- **Security**: Tokens exist only in JavaScript memory
- **Protection**: Cleared on page refresh/close
- **Trade-off**: Users need to login again after closing the browser
- **Alternative**: For persistence, consider implementing secure httpOnly cookies with your backend

#### Desktop

- **Storage**: Platform-specific secure storage
    - Windows: Windows Credential Manager
    - macOS: Keychain Services
    - Linux: Secret Service API (e.g., GNOME Keyring)
- **Security**: OS-level encryption and access control

### 3. Automatic Session Management

- Sessions are automatically refreshed before expiry
- Invalid sessions are detected and user is prompted to re-authenticate
- Session state is reactive - UI updates automatically on auth state changes

## Implementation Details

### Supabase Client Configuration

```kotlin
install(Auth) {
    flowType = FlowType.PKCE
    autoLoadFromStorage = true
    alwaysAutoRefresh = true
}
```

### Session Manager

The `SessionManager` object provides:

- Centralized authentication state management
- Reactive authentication state via Kotlin Flow
- Secure token retrieval methods
- Session initialization on app startup

### App-Level Integration

- Session is initialized when the app starts
- Authentication state determines which UI to show
- Automatic navigation between login and main app screens

## Best Practices

1. **Never store tokens in plain text**
2. **Always use HTTPS in production**
3. **Implement token rotation and refresh**
4. **Clear sessions on logout**
5. **Handle session expiry gracefully**
6. **Use biometric authentication where available** (can be added as an enhancement)

## Future Enhancements

1. **Biometric Authentication**: Add fingerprint/face recognition for additional security
2. **Remember Me**: Implement secure "remember me" functionality with refresh tokens
3. **Session Timeout**: Add configurable session timeout for sensitive operations
4. **Multi-factor Authentication**: Implement 2FA for enhanced security

## Testing Security

To verify the security implementation:

1. **Android**: Use Android Studio's Device File Explorer - tokens should not be visible in shared_prefs
2. **iOS**: Use Xcode's device console - tokens should not be accessible
3. **Web**: Check browser's localStorage and sessionStorage - should be empty
4. **Desktop**: Check OS credential storage using platform-specific tools
