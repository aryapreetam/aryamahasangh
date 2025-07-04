package com.aryamahasangh.util

import java.util.Properties

/**
 * Desktop implementation for getting version name
 * Uses system properties set by Gradle or falls back to reading from resources
 */
actual fun getPlatformVersionName(): String {
  return try {
    // First try system properties (set by Gradle during build)
    System.getProperty("app.version.name")?.takeIf { it.isNotEmpty() }
      ?: try {
        // Fallback: try to read from resources
        val resourceStream = object {}.javaClass.getResourceAsStream("/version.properties")
        if (resourceStream != null) {
          val properties = java.util.Properties()
          properties.load(resourceStream)
          properties.getProperty("version.name", "0.0.1")
        } else {
          "0.0.1"
        }
      } catch (e: Exception) {
        "0.0.1"
      }
  } catch (e: Exception) {
    "0.0.1" // Fallback version
  }
}

/**
 * Desktop implementation for getting version code
 */
actual fun getPlatformVersionCode(): Int {
  return try {
    // First try system properties (set by Gradle during build)
    System.getProperty("app.version.code")?.toIntOrNull()
      ?: try {
        // Fallback: try to read from resources
        val resourceStream = object {}.javaClass.getResourceAsStream("/version.properties")
        if (resourceStream != null) {
          val properties = java.util.Properties()
          properties.load(resourceStream)
          properties.getProperty("version.code", "1").toIntOrNull() ?: 1
        } else {
          1
        }
      } catch (e: Exception) {
        1
      }
  } catch (e: Exception) {
    1 // Fallback version code
  }
}

/**
 * Desktop implementation for getting environment
 */
actual fun getPlatformEnvironment(): String {
  return try {
    // First try system properties (set by Gradle during build)
    System.getProperty("app.environment")?.takeIf { it.isNotEmpty() }
      ?: try {
        // Fallback: try to read from resources
        val resourceStream = object {}.javaClass.getResourceAsStream("/version.properties")
        if (resourceStream != null) {
          val properties = java.util.Properties()
          properties.load(resourceStream)
          properties.getProperty("environment", "dev")
        } else {
          "dev"
        }
      } catch (e: Exception) {
        "dev"
      }
  } catch (e: Exception) {
    "dev" // Fallback environment
  }
}
