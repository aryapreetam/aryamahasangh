package org.aryamahasangh.util

/**
 * WebAssembly implementation for getting version name
 * For web, we'll try to get version from global JS variable or use fallback
 */
actual fun getPlatformVersionName(): String {
  return try {
    // Try to get version from global JS variable if set during build
    js("typeof window !== 'undefined' && window.APP_VERSION") as? String
      ?: "0.0.1" // Fallback version for web
  } catch (e: Exception) {
    "0.0.1" // Fallback version for web
  }
}

/**
 * WebAssembly implementation for getting version code
 */
actual fun getPlatformVersionCode(): Int {
  return try {
    // Try to get version code from global JS variable if set during build
    (js("typeof window !== 'undefined' && window.APP_VERSION_CODE") as? Int)
      ?: 1 // Fallback version code for web
  } catch (e: Exception) {
    1 // Fallback version code for web
  }
}
