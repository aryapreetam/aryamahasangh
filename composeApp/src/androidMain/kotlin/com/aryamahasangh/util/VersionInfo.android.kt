package com.aryamahasangh.util

/**
 * Android implementation for getting version name from BuildConfig
 */
actual fun getPlatformVersionName(): String {
  return try {
    // Use reflection to get BuildConfig since it might not be available at compile time
    val buildConfigClass = Class.forName("com.aryamahasangh.BuildConfig")
    val versionNameField = buildConfigClass.getField("VERSION_NAME")
    versionNameField.get(null) as String
  } catch (e: Exception) {
    "0.0.1" // Fallback version
  }
}

/**
 * Android implementation for getting version code from BuildConfig
 */
actual fun getPlatformVersionCode(): Int {
  return try {
    // Use reflection to get BuildConfig since it might not be available at compile time
    val buildConfigClass = Class.forName("com.aryamahasangh.BuildConfig")
    val versionCodeField = buildConfigClass.getField("VERSION_CODE")
    versionCodeField.get(null) as Int
  } catch (e: Exception) {
    1 // Fallback version code
  }
}

/**
 * Android implementation for getting environment from BuildConfig
 */
actual fun getPlatformEnvironment(): String {
  return try {
    // Use reflection to get BuildConfig since it might not be available at compile time
    val buildConfigClass = Class.forName("com.aryamahasangh.BuildConfig")
    val environmentField = buildConfigClass.getField("ENVIRONMENT")
    environmentField.get(null) as String
  } catch (e: Exception) {
    "dev" // Fallback environment
  }
}
