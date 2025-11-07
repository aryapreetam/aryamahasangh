package com.aryamahasangh.components

import androidx.compose.runtime.Composable

private class DesktopCameraLauncher : CameraLauncher {
  override fun launch() {
    // No-op: Camera not supported on Desktop
    println("Camera capture is not supported on Desktop platform")
  }

  override fun openSettings() {
    // No-op: Camera not supported on Desktop
  }
}

@Composable
actual fun rememberPlatformCameraLauncher(
  onResult: (CameraPermissionResult) -> Unit
): CameraLauncher {
  return DesktopCameraLauncher()
}

actual fun isCameraSupported(): Boolean = false

