package org.aryamahasangh.util

/**
 * Desktop implementation for getting version name
 * For desktop, we'll use a fallback version or try to read from resources
 */
actual fun getPlatformVersionName(): String {
  return try {
    // Try to read version from resources if available
    val resourceStream = object {}.javaClass.getResourceAsStream("/version.properties")
    if (resourceStream != null) {
      val properties = java.util.Properties()
      properties.load(resourceStream)
      properties.getProperty("version.name", "0.0.1")
    } else {
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
    // Try to read version code from resources if available
    val resourceStream = object {}.javaClass.getResourceAsStream("/version.properties")
    if (resourceStream != null) {
      val properties = java.util.Properties()
      properties.load(resourceStream)
      properties.getProperty("version.code", "1").toIntOrNull() ?: 1
    } else {
      1
    }
  } catch (e: Exception) {
    1 // Fallback version code
  }
}
