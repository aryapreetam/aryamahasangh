package com.aryamahasangh.util

/**
 * iOS implementation for getting version name
 * For iOS, we'll use a fallback version since we can't easily access build configuration
 */
actual fun getPlatformVersionName(): String {
  return "0.0.1" // Fallback version for iOS
}

/**
 * iOS implementation for getting version code
 */
actual fun getPlatformVersionCode(): Int {
  return 1 // Fallback version code for iOS
}

/**
 * iOS implementation for getting environment
 * For iOS, we'll use "dev" as fallback for now
 */
actual fun getPlatformEnvironment(): String {
  return "dev" // Fallback environment for iOS
}
