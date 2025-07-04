package com.aryamahasangh.util

/**
 * iOS implementation for getting version name
 * For iOS, we use hardcoded version that matches gradle.properties
 * This is a simple approach to avoid iOS build complexity
 */
actual fun getPlatformVersionName(): String {
  return "1.0.0" // This should match appVersion in gradle.properties
}

/**
 * iOS implementation for getting version code
 * Calculated from version 0.0.1 -> 1
 */
actual fun getPlatformVersionCode(): Int {
  return 10000 // Version code for 1.0.0
}

/**
 * iOS implementation for getting environment
 * For iOS, we use "dev" as default environment
 */
actual fun getPlatformEnvironment(): String {
  return "dev" // Default environment for iOS
}
