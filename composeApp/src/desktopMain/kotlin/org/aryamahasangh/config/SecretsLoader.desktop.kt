package org.aryamahasangh.config

import java.io.File
import java.util.*

/**
 * Desktop implementation of SecretsLoader
 * Loads secrets from secrets.properties file or environment variables
 */
class DesktopSecretsLoader : SecretsLoader {
  override suspend fun loadSecrets(): Map<String, String> {
    val secrets = mutableMapOf<String, String>()
    // First, try to load from secrets.properties file
    // Try multiple possible locations
    val possiblePaths =
      listOf(
        "secrets.properties", // Current directory
        "../secrets.properties", // Parent directory
        "../../secrets.properties" // Grandparent directory
      )

    var secretsFile: File? = null
    for (path in possiblePaths) {
      val file = File(path)
      if (file.exists()) {
        secretsFile = file
        break
      }
    }

    if (secretsFile != null) {
      try {
        val props = Properties()
        secretsFile.inputStream().use { props.load(it) }
        props.entries.forEach { (key, value) ->
          secrets[key.toString()] = value.toString()
        }
        println("✅ Loaded secrets from secrets.properties file: ${secretsFile.absolutePath}")
      } catch (e: Exception) {
        println("⚠️ Error loading secrets.properties: ${e.message}")
      }
    } else {
      println("⚠️ secrets.properties file not found in any of these locations:")
      possiblePaths.forEach { path ->
        println("   - ${File(path).absolutePath}")
      }
    }

    // Load environment variables as fallback/override
    loadEnvironmentVariables(secrets)

    return secrets
  }

  private fun loadEnvironmentVariables(secrets: MutableMap<String, String>) {
    val envVars =
      listOf(
        "SUPABASE_URL" to "prod.supabase.url",
        "SUPABASE_KEY" to "prod.supabase.key",
        "SERVER_URL" to "prod.server.url",
        "DEV_SUPABASE_URL" to "dev.supabase.url",
        "DEV_SUPABASE_KEY" to "dev.supabase.key",
        "DEV_SERVER_URL" to "dev.server.url",
        "ENVIRONMENT" to "environment"
      )

    envVars.forEach { (envKey, configKey) ->
      System.getenv(envKey)?.let { value ->
        secrets[configKey] = value
        println("✅ Loaded $configKey from environment variable $envKey")
      }
    }
  }
}

actual object SecretsLoaderFactory {
  actual fun create(): SecretsLoader = DesktopSecretsLoader()
}

actual fun getPlatformEnvVar(key: String): String? = System.getenv(key)
