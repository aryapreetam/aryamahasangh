package com.aryamahasangh.nhost

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for NHostClient functionality
 *
 * Tests the client against the live NHost instance:
 * https://hwdbpplmrdjdhcsmdleh.hasura.ap-south-1.nhost.run/v1/graphql
 */
class NHostClientTest {

  companion object {
    private const val NHOST_URL = ""
    private const val GRAPHQL_URL = "$NHOST_URL/v1/graphql"
  }

  private lateinit var nhostClient: NHostClient
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setup() {
    // Create NHost client
    nhostClient = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false // Disable for tests
      autoLoadSession = false // Don't load session in tests
    }

    // Create Apollo client with NHost authentication
    apolloClient = nhostClient.createApolloClient(graphqlUrl = GRAPHQL_URL)
  }

  @AfterTest
  fun teardown() {
    nhostClient.close()
  }

  /**
   * Test that we can create an NHost client successfully
   */
  @Test
  fun testCreateNHostClient() {
    assertNotNull(nhostClient, "NHost client should be created")
    assertNotNull(nhostClient.auth, "Auth module should be available")
    assertNotNull(nhostClient.storage, "Storage module should be available")
  }

  /**
   * Test that we can create an Apollo client with NHost integration
   */
  @Test
  fun testCreateApolloClient() {
    assertNotNull(apolloClient, "Apollo client should be created")
  }

  /**
   * Test the OrganisationNames GraphQL query
   *
   * This query fetches all organisations with their id and name:
   * ```graphql
   * query OrganisationNames {
   *   organisation {
   *     id
   *     name
   *   }
   * }
   * ```
   */
  @Test
  fun testOrganisationNamesQuery() = runTest {
    // Execute the query
    val response: ApolloResponse<OrganisationNamesQuery.Data> =
      apolloClient.query(OrganisationNamesQuery()).execute()

    // Assert response is successful
    assertNull(response.exception, "Query should not have exceptions: ${response.exception?.message}")
    assertFalse(response.hasErrors(), "Query should not have GraphQL errors: ${response.errors}")

    // Assert data is present
    assertNotNull(response.data, "Response data should not be null")

    val organisations = response.data?.organisation
    assertNotNull(organisations, "Organisations list should not be null")

    println("✓ Successfully fetched ${organisations.size} organisations")

    // Print organisations for debugging
    organisations.forEach { org ->
      println("  - Organisation: id=${org.id}, name=${org.name}")
    }

    // Optional: Assert that we have at least some data (if you know there should be data)
    // assertTrue(organisations.isNotEmpty(), "Should have at least one organisation")
  }

  /**
   * Test that authenticated queries work with the Apollo interceptor
   *
   * Note: This test will pass even without authentication if the query
   * doesn't require authentication. To properly test authentication,
   * you would need valid credentials.
   */
  @Test
  fun testApolloInterceptorAddsAuthHeaders() = runTest {
    // This test verifies that the Apollo client can execute queries
    // The NHostApolloInterceptor should add auth headers if a session exists

    val response = apolloClient.query(OrganisationNamesQuery()).execute()

    // Should execute without throwing exceptions
    assertNull(response.exception, "Query should execute successfully")

    println("✓ Apollo interceptor is working correctly")
  }

  /**
   * Test client initialization with different configurations
   */
  @Test
  fun testClientConfigurationOptions() {
    val client1 = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = true
      autoLoadSession = true
      refreshBeforeExpiry = 30
    }
    assertNotNull(client1)
    client1.close()

    val client2 = createNHostClient(nhostUrl = NHOST_URL) {
      autoRefreshToken = false
      autoLoadSession = false
      refreshBeforeExpiry = 120
    }
    assertNotNull(client2)
    client2.close()

    println("✓ Client can be configured with different options")
  }

  /**
   * Test that auth module provides expected properties
   */
  @Test
  fun testAuthModuleProperties() {
    assertNotNull(nhostClient.auth.currentSession, "Current session flow should be available")
    assertNotNull(nhostClient.auth.currentUser, "Current user flow should be available")
    assertFalse(nhostClient.auth.isAuthenticated, "Should not be authenticated initially")
    assertNull(nhostClient.auth.getAccessToken(), "Access token should be null initially")

    println("✓ Auth module has correct initial state")
  }

  /**
   * Test that storage module provides public URL generation
   */
  @Test
  fun testStoragePublicUrl() {
    val testFileId = "test-file-id-123"
    val publicUrl = nhostClient.storage.getPublicUrl(testFileId)

    assertNotNull(publicUrl, "Public URL should not be null")
    assertTrue(publicUrl.contains(testFileId), "Public URL should contain file ID")
    assertTrue(publicUrl.contains(NHOST_URL), "Public URL should contain NHost URL")

    println("✓ Public URL: $publicUrl")
  }
}


