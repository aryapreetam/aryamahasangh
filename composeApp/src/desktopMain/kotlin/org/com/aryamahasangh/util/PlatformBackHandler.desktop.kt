package com.aryamahasangh.util

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
actual fun PlatformBackHandler(
  customBackHandler: (() -> Unit)?,
  currentDestination: String?,
  navController: NavHostController
) {
  // Desktop doesn't typically use back button navigation, so this is a no-op
}
