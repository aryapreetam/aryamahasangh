package com.aryamahasangh.components

import androidx.compose.runtime.Composable

private class WasmCameraLauncher : CameraLauncher {
  override fun launch() {
    // No-op: Camera not supported on Web/WasmJS
    print("Camera capture is not supported on Web platform")
  }

  override fun openSettings() {
    // No-op: Camera not supported on Web
  }
}

actual fun isCameraSupported(): Boolean = false

@Composable
actual fun rememberPlatformCameraLauncher(onResult: (CameraPermissionResult) -> Unit): CameraLauncher {
  return WasmCameraLauncher()
}
