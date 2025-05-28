package org.aryamahasangh.config

/**
 * Application configuration that loads secrets from secrets.properties file during development
 * and from environment variables in production/CI.
 *
 * This approach eliminates the need for platform-specific configuration files.
 */
object AppConfig {
  private var config: Map<String, String> = emptyMap()
  private var isInitialized = false

  /**
   * Initialize configuration with provided values
   */
  fun initialize(configValues: Map<String, String>) {
    config = configValues
    isInitialized = true
  }

  /**
   * Legacy init method for backward compatibility
   */
  fun init(configValues: Map<String, String>) {
    initialize(configValues)
  }

  /**
   * Get current configuration (throws if not initialized)
   */
  val current: AppConfig
    get() {
      if (!isInitialized) {
        error("AppConfig not initialized. Call AppConfig.initialize() first.")
      }
      return this
    }

  val environment: String
    get() = config["environment"] ?: "dev"

  val supabaseUrl: String
    get() = get("supabase.url") ?: error("Supabase URL not configured")

  val supabaseKey: String
    get() = get("supabase.key") ?: error("Supabase key not configured")

  val serverUrl: String
    get() = get("server.url") ?: error("Server URL not configured")

  val graphqlUrl: String
    get() = "$serverUrl/graphql"

  val subscriptionsUrl: String
    get() = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/subscriptions"

  const val STORAGE_BUCKET = "documents"

  private fun get(key: String): String? {
    return config["$environment.$key"] ?: config[key]
  }

  fun getConfigInfo(): String =
    """
    Environment: $environment
    Supabase URL: ${supabaseUrl.take(20)}...
    Server URL: $serverUrl
    GraphQL URL: $graphqlUrl
    Subscriptions URL: $subscriptionsUrl
    """.trimIndent()
}
