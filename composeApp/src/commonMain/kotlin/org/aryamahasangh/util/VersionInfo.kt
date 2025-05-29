package org.aryamahasangh.util

/**
 * Version information for the application
 */
object VersionInfo {
  /**
   * Get the current app version string (e.g., "0.0.1")
   */
  fun getVersionName(): String = getPlatformVersionName()

  /**
   * Get the current app version code
   */
  fun getVersionCode(): Int = getPlatformVersionCode()

  /**
   * Get formatted version string with 'v' prefix (e.g., "v0.0.1")
   */
  fun getFormattedVersion(): String = "v${getVersionName()}"
}

/**
 * Platform-specific implementation for getting version name
 */
expect fun getPlatformVersionName(): String

/**
 * Platform-specific implementation for getting version code
 */
expect fun getPlatformVersionCode(): Int
