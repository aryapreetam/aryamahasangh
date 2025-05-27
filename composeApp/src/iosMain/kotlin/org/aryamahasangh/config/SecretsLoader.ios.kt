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
          println("🔍 Searching for secrets.properties in iOS bundle...")
          
          // Try different possible locations and names
          val possiblePaths = listOf(
              bundle.pathForResource("secrets", "properties"),
              bundle.pathForResource("secrets.properties", null),
              bundle.pathForResource("secrets", null),
              bundle.pathForResource("config", "json"),
              bundle.pathForResource("config.json", null)
          )
          
          for ((index, path) in possiblePaths.withIndex()) {
              println("🔍 Trying path ${index + 1}: $path")
              if (path != null) {
                  println("✅ Found file at: $path")
                  val content = NSString.stringWithContentsOfFile(
                      path = path,
                      encoding = NSUTF8StringEncoding,
                      error = null
                  )
                  
                  if (content != null) {
                      val contentStr = content.toString()
                      if (contentStr.isNotBlank()) {
                          // Determine file type and parse accordingly
                          if (path.contains(".json")) {
                              // Parse JSON config
                              parseJsonConfig(contentStr, secrets)
                          } else {
                              // Parse properties file
                              secrets.putAll(SecretsUtils.parseProperties(content))
                          }
                          println("✅ Successfully loaded secrets from iOS bundle: $path")
                          println("🔍 Loaded ${secrets.size} properties")
                          return true
                      } else {
                          println("⚠️ File found but content is empty: $path")
                      }
                  } else {
                      println("❌ Could not read content from bundle file at path: $path")
                  }
              } else {
                  println("❌ Path ${index + 1} not found")
              }
          }
          
          // Debug: List bundle path
          val bundlePath = bundle.bundlePath
          println("🔍 Bundle path: $bundlePath")
          
          println("❌ secrets.properties not found in iOS bundle")
          false
      } catch (e: Exception) {
          println("❌ Error loading from iOS bundle: ${e.message}")
          false
      }
  }
  
  private fun parseJsonConfig(jsonContent: String, secrets: MutableMap<String, String>) {
      try {
          // Simple JSON parsing for our specific format
          val lines = jsonContent.lines()
          for (line in lines) {
              val trimmed = line.trim()
              if (trimmed.contains(":") && !trimmed.startsWith("{") && !trimmed.startsWith("}")) {
                  val parts = trimmed.split(":", limit = 2)
                  if (parts.size == 2) {
                      val key = parts[0].trim().removeSurrounding("\"")
                      val value = parts[1].trim().removeSurrounding("\"").removeSuffix(",")
                      if (key.isNotEmpty() && value.isNotEmpty()) {
                          secrets[key] = value
                      }
                  }
              }
          }
          println("🔍 Parsed ${secrets.size} properties from JSON config")
      } catch (e: Exception) {
          println("❌ Error parsing JSON config: ${e.message}")
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