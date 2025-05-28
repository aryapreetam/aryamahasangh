package org.aryamahasangh.config

import android.annotation.SuppressLint
import android.content.Context
import java.io.IOException

/**
 * Android implementation of SecretsLoader
 * Loads secrets from assets/secrets.properties or environment variables
 */
class AndroidSecretsLoader(private val context: Context) : SecretsLoader {
  override suspend fun loadSecrets(): Map<String, String> {
    val secrets = mutableMapOf<String, String>()

    // Try to load from assets/secrets.properties
    try {
      val inputStream = context.assets.open("secrets.properties")
      val content = inputStream.bufferedReader().use { it.readText() }
      secrets.putAll(SecretsUtils.parseProperties(content))
      println("✅ Loaded secrets from Android assets")
    } catch (e: IOException) {
      println("⚠️ secrets.properties not found in Android assets: ${e.message}")
    } catch (e: Exception) {
      println("⚠️ Error loading secrets from Android assets: ${e.message}")
    }

    // Load environment variables as fallback/override
    loadEnvironmentVariables(secrets)

    // If no secrets loaded, use default development values
    if (secrets.isEmpty()) {
      loadDefaultValues(secrets)
    }

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

  private fun loadDefaultValues(secrets: MutableMap<String, String>) {
    secrets.putAll(
      mapOf(
        "environment" to "dev",
        "dev.supabase.url" to "https://placeholder.supabase.co",
        "dev.supabase.key" to "placeholder-key",
        "dev.server.url" to "http://10.0.2.2:4000", // Android emulator localhost
        "prod.supabase.url" to "https://placeholder.supabase.co",
        "prod.supabase.key" to "placeholder-key",
        "prod.server.url" to "https://your-production-server.com"
      )
    )
    println("⚠️ Using default development values for Android")
  }
}

// Global context holder for Android
@SuppressLint("StaticFieldLeak")
object AndroidContextHolder {
  private var _context: Context? = null

  fun init(context: Context) {
    _context = context.applicationContext
  }

  val context: Context
    get() = _context ?: throw IllegalStateException("AndroidContextHolder not initialized")
}

actual object SecretsLoaderFactory {
  actual fun create(): SecretsLoader = AndroidSecretsLoader(AndroidContextHolder.context)
}

actual fun getPlatformEnvVar(key: String): String? = System.getenv(key)
