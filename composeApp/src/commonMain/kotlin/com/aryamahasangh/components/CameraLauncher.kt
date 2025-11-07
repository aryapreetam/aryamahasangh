package com.aryamahasangh.components

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

/**
 * Result of camera permission request
 */
sealed class CameraPermissionResult {
  data class Success(val file: PlatformFile) : CameraPermissionResult()
  object Denied : CameraPermissionResult()
  object PermanentlyDenied : CameraPermissionResult()
  object Cancelled : CameraPermissionResult()
}

/**
 * Platform-specific camera launcher
 * - On Android/iOS: Uses FileKit's camera picker
 * - On Desktop/Web: No-op (camera not supported)
 */
interface CameraLauncher {
  fun launch()
  fun openSettings() // Open app settings to enable camera permission
}

/**
 * Remember a camera picker launcher
 * Platform-specific implementation:
 * - Android/iOS: Real camera functionality
 * - Desktop/Web: No-op fallback
 */
@Composable
expect fun rememberPlatformCameraLauncher(
  onResult: (CameraPermissionResult) -> Unit
): CameraLauncher

/**
 * Check if camera is supported on current platform
 */
expect fun isCameraSupported(): Boolean

