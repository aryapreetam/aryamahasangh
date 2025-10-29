package com.aryamahasangh.nhost.example

import com.aryamahasangh.nhost.createNHostClient
import com.aryamahasangh.nhost.models.PresignedUrlParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Example usage of NHost client
 *
 * This demonstrates:
 * - Creating a client
 * - Authentication
 * - Storage operations
 * - Apollo GraphQL integration
 *
 * Note: This is a suspend function that should be called from a coroutine scope.
 *
 * Example usage:
 * ```kotlin
 * // From a ViewModel or repository
 * viewModelScope.launch {
 *     runNHostExample(this)
 * }
 * ```
 */
suspend fun runNHostExample(scope: CoroutineScope) {
  // 1. Create NHost client
  val nhostClient = createNHostClient(
    nhostUrl = "https://your-app.nhost.run"
  ) {
    autoRefreshToken = true
    autoLoadSession = true
    refreshBeforeExpiry = 60
  }

  // 2. Observe authentication state
  nhostClient.auth.currentUser
    .onEach { user ->
      if (user != null) {
        println("Authenticated as: ${user.email}")
      } else {
        println("Not authenticated")
      }
    }
    .launchIn(scope)

  // 3. Sign in
  val signInResult = nhostClient.auth.signIn(
    email = "user@example.com",
    password = "password123"
  )

  signInResult.fold(
    onSuccess = { session ->
      println("✓ Signed in successfully")
      println("  User ID: ${session.user.id}")
      println("  Email: ${session.user.email}")
      println("  Roles: ${session.user.roles.joinToString()}")
      println("  Token expires in: ${session.accessTokenExpiresIn} seconds")
    },
    onFailure = { error ->
      println("✗ Sign in failed: ${error.message}")
      return
    }
  )

  // 4. Upload a file
  val fileContent = "Hello, NHost!".encodeToByteArray()
  val uploadResult = nhostClient.storage.upload(
    file = fileContent,
    name = "hello.txt",
    bucketId = "default",
    mimeType = "text/plain"
  )

  uploadResult.fold(
    onSuccess = { uploadResponse ->
      println("✓ File uploaded successfully")
      println("  File ID: ${uploadResponse.id}")
      println("  File name: ${uploadResponse.name}")
      println("  Bucket: ${uploadResponse.bucketId}")

      // 5. Get presigned URL for the file
      val presignedResult = nhostClient.storage.getPresignedUrl(
        PresignedUrlParams(fileId = uploadResponse.id)
      )

      presignedResult.fold(
        onSuccess = { presignedUrl ->
          println("✓ Presigned URL generated")
          println("  URL: ${presignedUrl.url}")
          println("  Expires at: ${presignedUrl.expiration}")
        },
        onFailure = { error ->
          println("✗ Failed to get presigned URL: ${error.message}")
        }
      )

      // 6. Get public URL
      val publicUrl = nhostClient.storage.getPublicUrl(uploadResponse.id)
      println("Public URL: $publicUrl")

      // 7. Delete the file
      val deleteResult = nhostClient.storage.delete(uploadResponse.id)
      deleteResult.fold(
        onSuccess = {
          println("✓ File deleted successfully")
        },
        onFailure = { error ->
          println("✗ Failed to delete file: ${error.message}")
        }
      )
    },
    onFailure = { error ->
      println("✗ File upload failed: ${error.message}")
    }
  )

  // 8. Create Apollo GraphQL client
  val apolloClient = nhostClient.createApolloClient(
    graphqlUrl = "https://your-app.nhost.run/v1/graphql"
  )
  println("✓ Apollo client created: ${apolloClient.hashCode()}")

  // 9. Sign out
  val signOutResult = nhostClient.auth.signOut()
  signOutResult.fold(
    onSuccess = {
      println("✓ Signed out successfully")
    },
    onFailure = { error ->
      println("✗ Sign out failed: ${error.message}")
    }
  )

  // 10. Clean up
  nhostClient.close()
  println("✓ NHost client closed")
}

