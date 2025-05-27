package org.aryamahasangh.config

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Response

/**
 * Web implementation of SecretsLoader
 * Loads secrets from environment variables or embedded configuration
 */
class WebSecretsLoader : SecretsLoader {
    override suspend fun loadSecrets(): Map<String, String> {
        val secrets = mutableMapOf<String, String>()
        
        // Try to load from a config.json file (if available)
        try {
            val response: Response = window.fetch("./config.json").await()
            if (response.ok) {
                val configText = response.text().await()
                // Parse simple JSON-like config (basic implementation)
                parseJsonConfig(configText, secrets)
                println("✅ Loaded secrets from web config.json")
            }
        } catch (e: Exception) {
            println("⚠️ config.json not found or error loading: ${e.message}")
        }
        
        // Load from window environment variables (if set by build process)
        loadWindowEnvironmentVariables(secrets)
        
        // If no secrets loaded, use default development values
        if (secrets.isEmpty()) {
            loadDefaultValues(secrets)
        }
        
        return secrets
    }
    
    private fun parseJsonConfig(configText: String, secrets: MutableMap<String, String>) {
        // Basic JSON parsing for simple key-value pairs
        // This is a simplified parser - in production you might want to use a proper JSON library
        try {
            val lines = configText.lines()
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.contains(":") && !trimmed.startsWith("//") && !trimmed.startsWith("{") && !trimmed.startsWith("}")) {
                    val parts = trimmed.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim().removeSurrounding("\"")
                        val value = parts[1].trim().removeSuffix(",").removeSurrounding("\"")
                        secrets[key] = value
                    }
                }
            }
        } catch (e: Exception) {
            println("⚠️ Error parsing config.json: ${e.message}")
        }
    }
    
    private fun loadWindowEnvironmentVariables(secrets: MutableMap<String, String>) {
        // Check if environment variables are available on window object
        val env = js("window.env") as? dynamic
        
        if (env != null) {
            val envVars = listOf(
                "SUPABASE_URL" to "prod.supabase.url",
                "SUPABASE_KEY" to "prod.supabase.key", 
                "SERVER_URL" to "prod.server.url",
                "DEV_SUPABASE_URL" to "dev.supabase.url",
                "DEV_SUPABASE_KEY" to "dev.supabase.key",
                "DEV_SERVER_URL" to "dev.server.url",
                "ENVIRONMENT" to "environment"
            )
            
            envVars.forEach { (envKey, configKey) ->
                val value = env[envKey] as? String
                if (value != null) {
                    secrets[configKey] = value
                    println("✅ Loaded $configKey from window.env.$envKey")
                }
            }
        }
    }
    
    private fun loadDefaultValues(secrets: MutableMap<String, String>) {
        secrets.putAll(mapOf(
            "environment" to "dev",
            "dev.supabase.url" to "https://placeholder.supabase.co",
            "dev.supabase.key" to "placeholder-key",
            "dev.server.url" to "http://localhost:4000",
            "prod.supabase.url" to "https://placeholder.supabase.co",
            "prod.supabase.key" to "placeholder-key",
            "prod.server.url" to "https://your-production-server.com"
        ))
        println("⚠️ Using default development values for Web")
    }
}

actual object SecretsLoaderFactory {
    actual fun create(): SecretsLoader = WebSecretsLoader()
}

actual fun getPlatformEnvVar(key: String): String? {
    // In web environment, check window.env if available
    return try {
        val env = js("window.env") as? dynamic
        env?.get(key) as? String
    } catch (e: Exception) {
        null
    }
}