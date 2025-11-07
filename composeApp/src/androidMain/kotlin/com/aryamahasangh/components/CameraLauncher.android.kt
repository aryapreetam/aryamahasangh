package com.aryamahasangh.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

private class AndroidCameraLauncher(
  private val launcher: io.github.vinceglb.filekit.dialogs.compose.PhotoResultLauncher,
  private val requestPermission: () -> Unit,
  private val isPermissionGranted: Boolean,
  private val shouldShowRationale: Boolean,
  private val openAppSettings: () -> Unit,
  private val onPermissionPermanentlyDenied: () -> Unit
) : CameraLauncher {
  override fun launch() {
    when {
      isPermissionGranted -> {
        // Permission already granted, launch camera directly
        launcher.launch()
      }
      shouldShowRationale -> {
        // Permission denied but we can show rationale and ask again
        requestPermission()
      }
      else -> {
        // Permission permanently denied or first request
        // Try to request - if it's first time, dialog will show
        // If permanently denied, onPermissionPermanentlyDenied will be called
        requestPermission()
      }
    }
  }

  override fun openSettings() {
    openAppSettings()
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberPlatformCameraLauncher(
  onResult: (CameraPermissionResult) -> Unit
): CameraLauncher {
  val context = LocalContext.current

  // Track whether we're waiting for permission to launch camera
  var shouldLaunchAfterPermission by remember { mutableStateOf(false) }
  var permissionDeniedOnce by remember { mutableStateOf(false) }

  val launcher = rememberCameraPickerLauncher { file ->
    if (file != null) {
      onResult(CameraPermissionResult.Success(file))
    } else {
      onResult(CameraPermissionResult.Cancelled)
    }
  }

  // Request camera permission
  val cameraPermissionState = rememberPermissionState(
    Manifest.permission.CAMERA
  ) { isGranted ->
    if (isGranted && shouldLaunchAfterPermission) {
      // Permission granted - automatically launch camera
      shouldLaunchAfterPermission = false
      permissionDeniedOnce = false
      launcher.launch()
    } else if (!isGranted) {
      // Permission denied
      shouldLaunchAfterPermission = false

      // We'll check if it's permanently denied in the next launch attempt
      // by checking shouldShowRationale at that point
      permissionDeniedOnce = true
      onResult(CameraPermissionResult.Denied)
    }
  }

  // Function to open app settings
  val openAppSettings = remember {
    {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
      }
      context.startActivity(intent)
    }
  }

  return remember(
    cameraPermissionState.status.isGranted,
    cameraPermissionState.status.shouldShowRationale,
    permissionDeniedOnce
  ) {
    AndroidCameraLauncher(
      launcher = launcher,
      requestPermission = {
        // Check if permission is permanently denied before requesting
        if (permissionDeniedOnce && !cameraPermissionState.status.shouldShowRationale && !cameraPermissionState.status.isGranted) {
          // Permission is permanently denied
          onResult(CameraPermissionResult.PermanentlyDenied)
        } else {
          // Can request permission
          shouldLaunchAfterPermission = true
          cameraPermissionState.launchPermissionRequest()
        }
      },
      isPermissionGranted = cameraPermissionState.status.isGranted,
      shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
      openAppSettings = openAppSettings,
      onPermissionPermanentlyDenied = {
        onResult(CameraPermissionResult.PermanentlyDenied)
      }
    )
  }
}

actual fun isCameraSupported(): Boolean = true

