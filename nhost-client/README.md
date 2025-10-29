# NHost Client for Kotlin Multiplatform

A comprehensive Kotlin Multiplatform client library for [NHost](https://nhost.io/) with automatic token management,
authentication, and storage support across Android, iOS, Web (WasmJS), and Desktop platforms.

## Features

✅ **Authentication**

- Sign in with email and password
- Automatic token refresh before expiration
- Secure session management with persistent storage
- Sign out functionality

✅ **Storage**

- Upload files to NHost storage
- Delete files
- Get presigned URLs with image transformations
- Get public URLs

✅ **Apollo GraphQL Integration**

- Automatic authentication headers
- Seamless integration with Apollo Kotlin

✅ **Multiplatform**

- Android
- iOS
- Web (WasmJS)
- Desktop (JVM)

## Installation

Add the `nhost-client` module to your dependencies:

```kotlin
commonMain {
    dependencies {
        implementation(project(":nhost-client"))
    }
}
```

## Quick Start

### 1. Create NHost Client

```kotlin
val nhostClient = createNHostClient(
    nhostUrl = "https://your-app.nhost.run"
) {
    autoRefreshToken = true
    autoLoadSession = true
    refreshBeforeExpiry = 60 // Refresh 60 seconds before token expires
}
```

### 2. Authentication

#### Sign In

```kotlin
val result = nhostClient.auth.signIn(
    email = "user@example.com",
    password = "password123"
)

result.fold(
    onSuccess = { session ->
        println("Signed in: ${session.user.email}")
    },
    onFailure = { error ->
        println("Sign in failed: ${error.message}")
    }
)
```

#### Observe Authentication State

```kotlin
// Collect current session
nhostClient.auth.currentSession.collect { session ->
    if (session != null) {
        println("User: ${session.user.email}")
    } else {
        println("Not authenticated")
    }
}

// Collect current user
nhostClient.auth.currentUser.collect { user ->
    user?.let {
        println("User ID: ${it.id}")
        println("Email: ${it.email}")
        println("Roles: ${it.roles}")
    }
}

// Check authentication status
if (nhostClient.auth.isAuthenticated) {
    println("User is authenticated")
}
```

#### Sign Out

```kotlin
val result = nhostClient.auth.signOut()

result.fold(
    onSuccess = {
        println("Signed out successfully")
    },
    onFailure = { error ->
        println("Sign out failed: ${error.message}")
    }
)
```

### 3. Storage Operations

#### Upload File

```kotlin
val fileBytes: ByteArray = // ... your file content
val result = nhostClient.storage.upload(
    file = fileBytes,
    name = "profile.jpg",
    bucketId = "default",
    mimeType = "image/jpeg"
)

result.fold(
    onSuccess = { uploadResponse ->
        println("File uploaded: ${uploadResponse.id}")
        println("File name: ${uploadResponse.name}")
    },
    onFailure = { error ->
        println("Upload failed: ${error.message}")
    }
)
```

#### Delete File

```kotlin
val result = nhostClient.storage.delete(fileId = "file-id-here")

result.fold(
    onSuccess = {
        println("File deleted successfully")
    },
    onFailure = { error ->
        println("Delete failed: ${error.message}")
    }
)
```

#### Get Presigned URL

```kotlin
val params = PresignedUrlParams(
    fileId = "file-id-here",
    quality = 80,
    width = 300,
    height = 300
)

val result = nhostClient.storage.getPresignedUrl(params)

result.fold(
    onSuccess = { presignedUrl ->
        println("URL: ${presignedUrl.url}")
        println("Expires at: ${presignedUrl.expiration}")
    },
    onFailure = { error ->
        println("Failed to get presigned URL: ${error.message}")
    }
)
```

#### Get Public URL

```kotlin
val publicUrl = nhostClient.storage.getPublicUrl(fileId = "file-id-here")
println("Public URL: $publicUrl")
```

### 4. Apollo GraphQL Integration

```kotlin
// Create Apollo client with automatic authentication
val apolloClient = nhostClient.createApolloClient(
    graphqlUrl = "https://your-app.nhost.run/v1/graphql"
)

// Use Apollo client for GraphQL queries/mutations
val response = apolloClient.query(YourQuery()).execute()
```

The Apollo client automatically:

- Adds authentication headers to all requests
- Uses the current access token from the session
- Updates when the token is refreshed

## Architecture

### Automatic Token Refresh

The client automatically handles token refresh:

1. **Scheduled Refresh**: Tokens are refreshed 60 seconds (configurable) before expiration
2. **Transparent**: Your app continues working without interruption
3. **Failure Handling**: If refresh fails, the session is cleared and user must sign in again

```kotlin
// Token refresh happens automatically in the background
// You can also manually trigger a refresh:
val result = nhostClient.auth.refreshToken()
```

### Session Persistence

Sessions are automatically saved to secure storage:

- **Android**: Encrypted SharedPreferences (when platform-specific storage is implemented)
- **iOS**: Keychain (when platform-specific storage is implemented)
- **Web/Desktop**: Currently uses in-memory storage (fallback)

The session is automatically restored when the app restarts (if `autoLoadSession = true`).

### Secure Storage

By default, the client uses `InMemorySecureStorage` for development and testing. For production, you should provide a
platform-specific secure storage implementation:

```kotlin
// Example: Provide custom secure storage
val customSecureStorage = object : SecureStorage {
    override suspend fun saveString(key: String, value: String) {
        // Your platform-specific implementation
    }
    
    override suspend fun getString(key: String): String? {
        // Your platform-specific implementation
        return null
    }
    
    override suspend fun remove(key: String) {
        // Your platform-specific implementation
    }
    
    override suspend fun clear() {
        // Your platform-specific implementation
    }
}

val nhostClient = createNHostClient(nhostUrl = "...") {
    secureStorage = customSecureStorage
}
```

## API Reference

### NHostClient

Main client instance providing access to auth and storage modules.

```kotlin
class NHostClient {
    val auth: NHostAuth
    val storage: NHostStorage
    fun createApolloClient(graphqlUrl: String): ApolloClient
    fun close()
}
```

### NHostAuth

Authentication module for user sign in/out and session management.

```kotlin
interface NHostAuth {
    val currentSession: StateFlow<Session?>
    val currentUser: StateFlow<User?>
    val isAuthenticated: Boolean
    
    suspend fun signIn(email: String, password: String): Result<Session>
    suspend fun signOut(): Result<Unit>
    fun getAccessToken(): String?
    suspend fun refreshToken(): Result<Session>
}
```

### NHostStorage

Storage module for file operations.

```kotlin
interface NHostStorage {
    suspend fun upload(
        file: ByteArray,
        name: String,
        bucketId: String = "default",
        mimeType: String? = null
    ): Result<FileUploadResponse>
    
    suspend fun delete(fileId: String): Result<Unit>
    
    suspend fun getPresignedUrl(params: PresignedUrlParams): Result<PresignedUrl>
    
    fun getPublicUrl(fileId: String): String
}
```

## Configuration Options

```kotlin
createNHostClient(nhostUrl = "https://your-app.nhost.run") {
    // Enable automatic token refresh (default: true)
    autoRefreshToken = true
    
    // Load session from storage on startup (default: true)
    autoLoadSession = true
    
    // Refresh token this many seconds before expiry (default: 60)
    refreshBeforeExpiry = 60
    
    // Provide custom secure storage implementation
    secureStorage = customSecureStorage
}
```

## Error Handling

All operations return `Result<T>` for safe error handling:

```kotlin
val result = nhostClient.auth.signIn(email, password)

result.fold(
    onSuccess = { session ->
        // Handle success
    },
    onFailure = { error ->
        // Handle error
        when (error) {
            is IOException -> println("Network error")
            else -> println("Error: ${error.message}")
        }
    }
)
```

## Testing

For testing, use the in-memory storage (default) and mock responses:

```kotlin
@Test
fun testAuthentication() = runTest {
    val nhostClient = createNHostClient(
        nhostUrl = "https://test.nhost.run"
    ) {
        autoLoadSession = false
    }
    
    // Your test code
}
```

## Best Practices

1. **Single Instance**: Create one NHostClient instance and share it across your app
2. **Close on Exit**: Call `nhostClient.close()` when your app exits to release resources
3. **Observe State**: Use StateFlow observers to react to authentication state changes
4. **Error Handling**: Always handle Result failure cases
5. **Secure Storage**: Implement platform-specific secure storage for production

## Comparison with Supabase-KT

This NHost client is designed with similar principles to
the [Supabase-KT Apollo plugin](https://github.com/supabase-community/supabase-kt-plugins/tree/main/ApolloGraphQL):

| Feature                 | NHost Client | Supabase-KT |
|-------------------------|--------------|-------------|
| Automatic token refresh | ✅            | ✅           |
| Session persistence     | ✅            | ✅           |
| Apollo integration      | ✅            | ✅           |
| Storage operations      | ✅            | ✅           |
| Multiplatform           | ✅            | ✅           |
| Email/Password auth     | ✅            | ✅           |

## License

This library is part of the AryaMahasangh project.

## Contributing

Contributions are welcome! Please follow the project's coding guidelines.

