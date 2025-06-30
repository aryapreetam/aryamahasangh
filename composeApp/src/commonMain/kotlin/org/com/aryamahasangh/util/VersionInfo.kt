package com.aryamahasangh.util

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
   * Get the current environment (dev or prod)
   */
  fun getEnvironment(): String = getPlatformEnvironment()

  /**
   * Get version with environment suffix (e.g., "0.0.1-dev" for dev, "0.0.1" for prod)
   */
  fun getVersionWithEnvironment(): String {
    val baseVersion = getVersionName()
    val environment = getEnvironment()
    return if (environment == "dev") {
      "$baseVersion-dev"
    } else {
      baseVersion
    }
  }

  /**
   * Get formatted version string with 'v' prefix and environment suffix (e.g., "v0.0.1-dev")
   */
  fun getFormattedVersion(): String = "v${getVersionWithEnvironment()}"
}

/**
 * Platform-specific implementation for getting version name
 */
expect fun getPlatformVersionName(): String

/**
 * Platform-specific implementation for getting version code
 */
expect fun getPlatformVersionCode(): Int

/**
 * Platform-specific implementation for getting environment
 */
expect fun getPlatformEnvironment(): String
