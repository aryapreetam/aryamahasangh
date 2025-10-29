package com.aryamahasangh.nhost

import com.apollographql.apollo.ApolloClient
import com.aryamahasangh.nhost.auth.NHostAuth
import com.aryamahasangh.nhost.auth.NHostAuthImpl
import com.aryamahasangh.nhost.graphql.NHostApolloInterceptor
import com.aryamahasangh.nhost.http.NHostHttpClientFactory
import com.aryamahasangh.nhost.storage.InMemorySecureStorage
import com.aryamahasangh.nhost.storage.NHostStorage
import com.aryamahasangh.nhost.storage.NHostStorageImpl
import com.aryamahasangh.nhost.storage.SecureStorage
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Main NHost client for authentication and storage operations
 */
class NHostClient internal constructor(
  val auth: NHostAuth,
  val storage: NHostStorage,
  private val httpClient: HttpClient,
  private val scope: CoroutineScope
) {
  /**
   * Create an Apollo GraphQL client with automatic authentication
   */
  fun createApolloClient(graphqlUrl: String): ApolloClient {
    return ApolloClient.Builder()
      .serverUrl(graphqlUrl)
      .addHttpInterceptor(NHostApolloInterceptor(auth))
      .build()
  }

  /**
   * Close the client and release resources
   */
  fun close() {
    httpClient.close()
  }
}

/**
 * Configuration for NHost client
 */
class NHostClientConfig {
  var autoRefreshToken: Boolean = true
  var autoLoadSession: Boolean = true
  var refreshBeforeExpiry: Long = 60 // seconds
  var secureStorage: SecureStorage? = null
}

/**
 * Create a new NHost client
 *
 * @param nhostUrl The base URL of your NHost instance (e.g., "https://your-app.nhost.run")
 * @param configure Configuration block for the client
 * @return Configured NHostClient instance
 */
fun createNHostClient(
  nhostUrl: String,
  configure: NHostClientConfig.() -> Unit = {}
): NHostClient {
  val config = NHostClientConfig().apply(configure)

  // Create coroutine scope for the client
  val scope = CoroutineScope(SupervisorJob())

  // Use provided secure storage or fallback to in-memory
  val secureStorage = config.secureStorage ?: InMemorySecureStorage()

  // Create HTTP client with auth interceptor
  val httpClient = NHostHttpClientFactory.create(
    baseUrl = nhostUrl,
    accessTokenProvider = {
      // This will be set by auth implementation
      null
    }
  )

  // Create auth module
  val auth = NHostAuthImpl(
    httpClient = httpClient,
    baseUrl = nhostUrl,
    secureStorage = secureStorage,
    scope = scope,
    autoLoadSession = config.autoLoadSession,
    refreshBeforeExpiry = config.refreshBeforeExpiry
  )

  // Create storage module
  val storage = NHostStorageImpl(
    httpClient = httpClient,
    baseUrl = nhostUrl
  )

  return NHostClient(
    auth = auth,
    storage = storage,
    httpClient = httpClient,
    scope = scope
  )
}

