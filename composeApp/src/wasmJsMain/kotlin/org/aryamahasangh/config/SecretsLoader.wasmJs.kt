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
      
      println("🌐 Loading secrets for Web platform...")
      
      // Try to load from a config.json file (if available)
      var configLoaded = false
      try {
          println("🔍 Attempting to fetch ./config.json...")
          val response: Response = window.fetch("./config.json").await()
          println("📡 Fetch response status: ${response.status}")
          
          if (response.ok) {
              val configText: Any = response.text().await()
              val configStr = configText.toString()
              println("📄 Config file content length: ${configStr.length} characters")
              
              // Parse simple JSON-like config (basic implementation)
              parseJsonConfig(configStr, secrets)
              
              if (secrets.isNotEmpty()) {
                  println("✅ Successfully loaded ${secrets.size} secrets from web config.json")
                  secrets.forEach { (key, value) ->
                      val maskedValue = if (key.contains("key", ignoreCase = true)) "***" else value
                      println("   $key = $maskedValue")
                  }
                  configLoaded = true
              } else {
                  println("⚠️ Config file found but no secrets parsed")
              }
          } else {
              println("❌ Config fetch failed with status: ${response.status}")
          }
      } catch (e: Exception) {
          println("❌ config.json not found or error loading: ${e.message}")
          e.printStackTrace()
      }
      
      // Load from window environment variables (if set by build process)
      loadWindowEnvironmentVariables(secrets)
      
      // If no secrets loaded, use default development values
      if (secrets.isEmpty()) {
          println("⚠️ No secrets loaded from config.json or environment, using defaults")
          loadDefaultValues(secrets)
      } else if (configLoaded) {
          println("✅ Web secrets configuration loaded successfully")
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
      // For web platform, we rely on config.json or build-time configuration
      // Environment variables are not directly accessible in browser environment
      println("ℹ️ Web platform: Environment variables loaded via config.json or build configuration")
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
  // In web environment, environment variables are not directly accessible
  // They should be provided via config.json or build-time configuration
  return null
}