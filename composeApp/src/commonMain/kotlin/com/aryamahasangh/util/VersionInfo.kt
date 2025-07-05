package com.aryamahasangh.util

import com.aryamahasangh.config.AppConfig

/**
 * Version information for the application
 */
object VersionInfo {
  /**
   * Get the current app version string (e.g., "1.0.6")
   */
  fun getVersionName(): String = AppConfig.versionName

  /**
   * Get the current app version code
   */
  fun getVersionCode(): Int = AppConfig.versionCode

  /**
   * Get the current environment (dev or prod)
   */
  fun getEnvironment(): String = AppConfig.environment

  /**
   * Get version with environment suffix (e.g., "1.0.6-dev" for dev, "1.0.6" for prod)
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
   * Get formatted version string with 'v' prefix and environment suffix (e.g., "v1.0.6-dev")
   */
  fun getFormattedVersion(): String = "v${getVersionWithEnvironment()}"
}
