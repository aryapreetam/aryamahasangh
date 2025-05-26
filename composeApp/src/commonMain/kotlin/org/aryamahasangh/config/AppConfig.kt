package org.aryamahasangh.config

/**
 * Application configuration that loads secrets from secrets.properties file during development
 * and from environment variables in production/CI.
 * 
 * This approach eliminates the need for platform-specific configuration files.
 */
object AppConfig {
    
    // Environment detection
    private val isProduction = System.getenv("ENVIRONMENT") == "prod" || 
                              System.getProperty("environment") == "prod"
    
    private val environment = if (isProduction) "prod" else "dev"
    
    // Configuration loading with fallback chain:
    // 1. Environment variables (production/CI)
    // 2. secrets.properties file (development)
    // 3. Throw error if not found
    
    val supabaseUrl: String by lazy {
        getConfigValue("supabase.url") ?: error("Supabase URL not configured")
    }
    
    val supabaseKey: String by lazy {
        getConfigValue("supabase.key") ?: error("Supabase key not configured")
    }
    
    val serverUrl: String by lazy {
        getConfigValue("server.url") ?: error("Server URL not configured")
    }
    
    val graphqlUrl: String by lazy {
        "$serverUrl/graphql"
    }
    
    val subscriptionsUrl: String by lazy {
        serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/subscriptions"
    }
    
    // Storage configuration
    const val STORAGE_BUCKET = "documents"
    
    /**
     * Gets configuration value with the following priority:
     * 1. Environment variable (SUPABASE_URL, SUPABASE_KEY, etc.)
     * 2. secrets.properties file with environment prefix (dev.supabase.url, prod.supabase.url)
     * 3. null if not found
     */
    private fun getConfigValue(key: String): String? {
        // 1. Try environment variable (uppercase with underscores)
        val envKey = key.replace(".", "_").uppercase()
        System.getenv(envKey)?.let { return it }
        
        // 2. Try secrets.properties file
        return loadFromSecretsFile("$environment.$key")
    }
    
    /**
     * Loads configuration from secrets.properties file
     */
    private fun loadFromSecretsFile(key: String): String? {
        return try {
            val properties = java.util.Properties()
            val secretsFile = java.io.File("secrets.properties")
            
            if (secretsFile.exists()) {
                secretsFile.inputStream().use { properties.load(it) }
                properties.getProperty(key)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Warning: Could not load secrets.properties: ${e.message}")
            null
        }
    }
    
    /**
     * Debug information (without exposing secrets)
     */
    fun getConfigInfo(): String {
        return """
            Environment: $environment
            Supabase URL: ${supabaseUrl.take(20)}...
            Server URL: $serverUrl
            GraphQL URL: $graphqlUrl
            Subscriptions URL: $subscriptionsUrl
        """.trimIndent()
    }
}