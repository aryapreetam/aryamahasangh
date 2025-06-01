package org.aryamahasangh.util

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
actual fun PlatformBackHandler(
  customBackHandler: (() -> Unit)?,
  currentDestination: String?,
  navController: NavHostController
) {
  // iOS doesn't have a system back button, so this is a no-op
}
