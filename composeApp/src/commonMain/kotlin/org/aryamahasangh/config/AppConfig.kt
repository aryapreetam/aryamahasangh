package org.aryamahasangh.config

/**
 * Application configuration that loads secrets from secrets.properties file during development
 * and from environment variables in production/CI.
 * 
 * This approach eliminates the need for platform-specific configuration files.
 */
object AppConfig {

    private var config: Map<String, String> = emptyMap()

    fun init(configValues: Map<String, String>) {
        config = configValues
    }

    private val environment: String = config["environment"] ?: "dev"

    val supabaseUrl: String by lazy {
        get("supabase.url") ?: error("Supabase URL not configured")
    }

    val supabaseKey: String by lazy {
        get("supabase.key") ?: error("Supabase key not configured")
    }

    val serverUrl: String by lazy {
        get("server.url") ?: error("Server URL not configured")
    }

    val graphqlUrl: String by lazy {
        "$serverUrl/graphql"
    }

    val subscriptionsUrl: String by lazy {
        serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/subscriptions"
    }

    const val STORAGE_BUCKET = "documents"

    private fun get(key: String): String? {
        return config["$environment.$key"] ?: config[key]
    }

    fun getConfigInfo(): String = """
        Environment: $environment
        Supabase URL: ${supabaseUrl.take(20)}...
        Server URL: $serverUrl
        GraphQL URL: $graphqlUrl
        Subscriptions URL: $subscriptionsUrl
    """.trimIndent()
}