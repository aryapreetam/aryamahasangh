package com.aryamahasangh.util

/**
 * WebAssembly implementation for getting version name
 * Tries to get version from global JavaScript variables injected during build
 */
actual fun getPlatformVersionName(): String {
  return try {
    // Try to get version from global JS variable set during build
    js("globalThis.APP_VERSION || '0.0.1'") as? String ?: "0.0.1"
  } catch (e: Exception) {
    "0.0.1" // Fallback version
  }
}

/**
 * WebAssembly implementation for getting version code
 */
actual fun getPlatformVersionCode(): Int {
  return try {
    // Try to get version code from global JS variable set during build
    val versionCode = js("globalThis.APP_VERSION_CODE || 1")
    when (versionCode) {
      is Int -> versionCode
      is Double -> versionCode.toInt()
      is String -> versionCode.toIntOrNull() ?: 1
      else -> 1
    }
  } catch (e: Exception) {
    1 // Fallback version code
  }
}

/**
 * WebAssembly implementation for getting environment
 */
actual fun getPlatformEnvironment(): String {
  return try {
    // Try to get environment from global JS variable set during build
    js("globalThis.APP_ENVIRONMENT || 'dev'") as? String ?: "dev"
  } catch (e: Exception) {
    "dev" // Fallback environment
  }
}
