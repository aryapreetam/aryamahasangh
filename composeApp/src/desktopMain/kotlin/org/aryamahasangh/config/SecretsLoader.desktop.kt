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
    val secretsFile = File("../secrets.properties")
    if (secretsFile.exists()) {
      try {
        val props = Properties()
        secretsFile.inputStream().use { props.load(it) }
        props.entries.forEach { (key, value) ->
          secrets[key.toString()] = value.toString()
        }
        println("✅ Loaded secrets from secrets.properties file")
      } catch (e: Exception) {
        println("⚠️ Error loading secrets.properties: ${e.message}")
      }
    } else {
      println("⚠️ secrets.properties file not found at: ${secretsFile.absolutePath}")
    }

    // Load environment variables as fallback/override
    loadEnvironmentVariables(secrets)

    return secrets
  }

  private fun loadEnvironmentVariables(secrets: MutableMap<String, String>) {
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