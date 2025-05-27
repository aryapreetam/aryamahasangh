package org.aryamahasangh.config

/**
 * Common interface for loading secrets across all platforms
 */
interface SecretsLoader {
  /**
   * Load secrets from platform-specific sources
   * @return Map of key-value pairs from secrets configuration
   */
  suspend fun loadSecrets(): Map<String, String>
}

/**
 * Platform-specific secrets loader factory
 */
expect object SecretsLoaderFactory {
  fun create(): SecretsLoader
}

/**
 * Utility functions for secrets management
 */
object SecretsUtils {
  /**
   * Parse properties format string into a map
   */
  fun parseProperties(content: String): Map<String, String> {
      return content.lines()
          .filter { line -> 
              line.isNotBlank() && 
              !line.trimStart().startsWith("#") && 
              line.contains("=")
          }
          .associate { line ->
              val parts = line.split("=", limit = 2)
              parts[0].trim() to parts[1].trim()
          }
  }

  /**
   * Get environment variable with fallback
   */
  fun getEnvVar(key: String, default: String = ""): String {
      return try {
          // This will be implemented platform-specifically
          getPlatformEnvVar(key) ?: default
      } catch (e: Exception) {
          default
      }
  }
}

/**
 * Platform-specific environment variable access
 */
expect fun getPlatformEnvVar(key: String): String?