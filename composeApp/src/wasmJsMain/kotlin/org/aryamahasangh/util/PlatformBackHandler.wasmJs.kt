package org.aryamahasangh.util

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
actual fun PlatformBackHandler(
  customBackHandler: (() -> Unit)?,
  currentDestination: String?,
  navController: NavHostController
) {
  // Web back button handling could be implemented here if needed
  // For now, this is a no-op
}
