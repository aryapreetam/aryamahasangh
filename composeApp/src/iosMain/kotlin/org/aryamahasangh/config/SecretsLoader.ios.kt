package org.aryamahasangh.config

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringWithContentsOfFile
import platform.posix.getenv

/**
 * iOS implementation of SecretsLoader
 * Loads secrets from multiple sources in order of preference:
 * 1. Documents directory (for development)
 * 2. Bundle resources (for production)
 * 3. Environment variables (fallback)
 * 4. Default values (last resort)
 */
class IOSSecretsLoader : SecretsLoader {
  @OptIn(ExperimentalForeignApi::class)
  override suspend fun loadSecrets(): Map<String, String> {
      val secrets = mutableMapOf<String, String>()
      
      // Try to load from Documents directory first (preferred for development)
      if (tryLoadFromDocuments(secrets)) {
          println("✅ Loaded secrets from iOS Documents directory")
      } else {
          // Try to load from bundle resources as fallback
          if (tryLoadFromBundle(secrets)) {
              println("✅ Loaded secrets from iOS bundle")
          } else {
              println("⚠️ secrets.properties not found in iOS Documents or bundle")
          }
      }
      
      // Load environment variables as fallback/override
      loadEnvironmentVariables(secrets)
      
      // If no secrets loaded, use default development values
      if (secrets.isEmpty()) {
          loadDefaultValues(secrets)
      }
      
      return secrets
  }
  
  @OptIn(ExperimentalForeignApi::class)
  private fun tryLoadFromDocuments(secrets: MutableMap<String, String>): Boolean {
      return try {
          val documentsPath = NSSearchPathForDirectoriesInDomains(
              NSDocumentDirectory,
              NSUserDomainMask,
              true
          ).firstOrNull() as? String
          
          if (documentsPath != null) {
              val secretsPath = "$documentsPath/secrets.properties"
              val content = NSString.stringWithContentsOfFile(
                  path = secretsPath,
                  encoding = NSUTF8StringEncoding,
                  error = null
              )
              
              if (content != null) {
                  secrets.putAll(SecretsUtils.parseProperties(content))
                  return true
              }
          }
          false
      } catch (e: Exception) {
          println("⚠️ Error loading secrets from iOS Documents: ${e.message}")
          false
      }
  }
  
  @OptIn(ExperimentalForeignApi::class)
  private fun tryLoadFromBundle(secrets: MutableMap<String, String>): Boolean {
      return try {
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
                  return true
              }
          }
          false
      } catch (e: Exception) {
          println("⚠️ Error loading secrets from iOS bundle: ${e.message}")
          false
      }
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