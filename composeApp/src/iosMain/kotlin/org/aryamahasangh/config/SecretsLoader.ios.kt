package org.aryamahasangh.config

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.posix.getenv

/**
 * iOS implementation of SecretsLoader
 * Loads secrets from bundle resources or environment variables
 */
class IOSSecretsLoader : SecretsLoader {
  @OptIn(ExperimentalForeignApi::class)
  override suspend fun loadSecrets(): Map<String, String> {
      val secrets = mutableMapOf<String, String>()
      
      // Try to load from bundle resources
      try {
          val bundle = NSBundle.mainBundle
          val path = bundle.pathForResource("secrets", "properties")
          
          if (path != null) {
              val content = NSString.stringWithContentsOfFile(
                  path = path,
                  encoding = NSUTF8StringEncoding,
                  error = null
              )
              
              if (content != null) {
                  secrets.putAll(SecretsUtils.parseProperties(content))
                  println("✅ Loaded secrets from iOS bundle")
              }
          } else {
              println("⚠️ secrets.properties not found in iOS bundle")
          }
      } catch (e: Exception) {
          println("⚠️ Error loading secrets from iOS bundle: ${e.message}")
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
          getPlatformEnvVar(envKey)?.let { value ->
              secrets[configKey] = value
              println("✅ Loaded $configKey from environment variable $envKey")
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
      println("⚠️ Using default development values for iOS")
  }
}

actual object SecretsLoaderFactory {
  actual fun create(): SecretsLoader = IOSSecretsLoader()
}

@OptIn(ExperimentalForeignApi::class)
actual fun getPlatformEnvVar(key: String): String? {
  return getenv(key)?.toKString()
}