package com.aryamahasangh.components

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

private class IosCameraLauncher(
  private val launcher: io.github.vinceglb.filekit.dialogs.compose.PhotoResultLauncher
) : CameraLauncher {
  override fun launch() {
    launcher.launch()
  }

  override fun openSettings() {
    // iOS will handle opening Settings through system APIs if needed
    // For now, no-op as iOS usually handles this through system prompts
  }
}

@Composable
actual fun rememberPlatformCameraLauncher(
  onResult: (CameraPermissionResult) -> Unit
): CameraLauncher {
  val launcher = rememberCameraPickerLauncher { file ->
    if (file != null) {
      onResult(CameraPermissionResult.Success(file))
    } else {
      // iOS handles permission internally, if null it could be denied or cancelled
      onResult(CameraPermissionResult.Denied)
    }
  }
  return IosCameraLauncher(launcher)
}

actual fun isCameraSupported(): Boolean = true

