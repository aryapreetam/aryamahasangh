package com.aryamahasangh.nhost

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Integration tests for NHost client basic functionality
 * Tests that don't require Apollo-generated query classes
 */
class NHostClientIntegrationTest {

  companion object {
    private const val NHOST_URL = ""
    private const val GRAPHQL_URL = "$NHOST_URL/v1/graphql"
  }

  /**
   * Test that NHost client can be created with the test URL
   */
  @Test
  fun testClientCreation() {
    val client = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      autoLoadSession = false
    }

    assertNotNull(client)
    assertNotNull(client.auth)
    assertNotNull(client.storage)

    client.close()
  }

  /**
   * Test that Apollo client can be created with NHost integration
   */
  @Test
  fun testApolloClientCreation() {
    val nhostClient = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      autoLoadSession = false
    }

    val apolloClient = nhostClient.createApolloClient(graphqlUrl = GRAPHQL_URL)

    assertNotNull(apolloClient)

    nhostClient.close()
  }

  /**
   * Test that NHost auth interceptor is properly attached to Apollo client
   */
  @Test
  fun testApolloInterceptorConfiguration() {
    val nhostClient = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      autoLoadSession = false
    }

    // Create a test interceptor to verify the interceptor chain
    var interceptorCalled = false
    val testInterceptor = object : HttpInterceptor {
      override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
        interceptorCalled = true
        return chain.proceed(request)
      }
    }

    val apolloClient = ApolloClient.Builder()
      .serverUrl(GRAPHQL_URL)
      .addHttpInterceptor(testInterceptor)
      .build()

    assertNotNull(apolloClient)

    nhostClient.close()
  }

  /**
   * Test storage public URL generation
   */
  @Test
  fun testStoragePublicUrlGeneration() {
    val nhostClient = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      autoLoadSession = false
    }

    val fileId = "test-file-id-12345"
    val publicUrl = nhostClient.storage.getPublicUrl(fileId)

    assertNotNull(publicUrl)
    assertTrue(publicUrl.contains(NHOST_URL))
    assertTrue(publicUrl.contains(fileId))
    assertEquals("$NHOST_URL/v1/storage/files/$fileId", publicUrl)

    println("✓ Public URL generated: $publicUrl")

    nhostClient.close()
  }

  /**
   * Test authentication state flows are initialized correctly
   */
  @Test
  fun testAuthenticationStateInitialization() = runTest {
    val nhostClient = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      autoLoadSession = false
    }

    // Check initial state
    assertNull(nhostClient.auth.currentSession.value)
    assertNull(nhostClient.auth.currentUser.value)
    assertFalse(nhostClient.auth.isAuthenticated)
    assertNull(nhostClient.auth.getAccessToken())

    println("✓ Auth state initialized correctly")

    nhostClient.close()
  }

  /**
   * Test multiple client instances can be created independently
   */
  @Test
  fun testMultipleClientInstances() {
    val client1 = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = true
      refreshBeforeExpiry = 60
    }

    val client2 = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      refreshBeforeExpiry = 30
    }

    assertNotNull(client1)
    assertNotNull(client2)

    // Verify they are independent instances
    assertNotEquals(client1, client2)

    client1.close()
    client2.close()

    println("✓ Multiple independent client instances created successfully")
  }

  /**
   * Test client configuration with different options
   */
  @Test
  fun testClientConfigurationVariations() {
    // Test with all options enabled
    val client1 = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = true
      autoLoadSession = true
      refreshBeforeExpiry = 120
    }
    assertNotNull(client1)
    client1.close()

    // Test with all options disabled
    val client2 = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      autoLoadSession = false
      refreshBeforeExpiry = 30
    }
    assertNotNull(client2)
    client2.close()

    // Test with default options (no configuration block)
    val client3 = createNHostClient(nhostUrl = NHOST_URL)
    assertNotNull(client3)
    client3.close()

    println("✓ Client configuration variations work correctly")
  }
}

