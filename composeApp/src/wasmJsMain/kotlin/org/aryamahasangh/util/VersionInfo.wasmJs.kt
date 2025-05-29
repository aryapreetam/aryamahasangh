package org.aryamahasangh.util

/**
 * WebAssembly implementation for getting version name
 * For web, we'll try to get version from global JS variable or use fallback
 */
actual fun getPlatformVersionName(): String = "0.0.1"

/**
 * WebAssembly implementation for getting version code
 */
actual fun getPlatformVersionCode(): Int = 1
