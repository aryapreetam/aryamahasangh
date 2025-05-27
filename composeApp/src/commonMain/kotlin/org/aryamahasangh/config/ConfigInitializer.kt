package org.aryamahasangh.config

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Cross-platform configuration initializer
 * Handles loading secrets and initializing AppConfig
 */
object ConfigInitializer {
    private var isInitialized = false
    
    /**
     * Initialize configuration for the current platform
     * This should be called from each platform's main entry point
     */
    fun initialize() {
        if (isInitialized) {
            println("⚠️ Configuration already initialized")
            return
        }
        GlobalScope.launch {
            try {
                println("🔧 Initializing configuration...")

                // Load secrets using platform-specific loader
                val secretsLoader = SecretsLoaderFactory.create()
                val secrets = secretsLoader.loadSecrets()

                // Initialize AppConfig with loaded secrets
                AppConfig.initialize(secrets)

                isInitialized = true
                println("✅ Configuration initialized successfully")

                // Log current configuration (without sensitive values)
                logConfiguration()

            } catch (e: Exception) {
                println("❌ Failed to initialize configuration: ${e.message}")
                e.printStackTrace()

                // Initialize with empty config as fallback
                AppConfig.initialize(emptyMap())
                isInitialized = true
            }
        }
    }
    
    /**
     * Check if configuration is initialized
     */
    fun isInitialized(): Boolean = isInitialized
    
    /**
     * Reset initialization state (for testing)
     */
    fun reset() {
        isInitialized = false
    }
    
    private fun logConfiguration() {
        try {
            val config = AppConfig.current
            println("📋 Current configuration:")
            println("   Environment: ${config.environment}")
            println("   Supabase URL: ${config.supabaseUrl.take(30)}...")
            println("   Server URL: ${config.serverUrl}")
            println("   Supabase Key: ${if (config.supabaseKey.isNotEmpty()) "***configured***" else "not set"}")
        } catch (e: Exception) {
            println("⚠️ Could not log configuration: ${e.message}")
        }
    }
}