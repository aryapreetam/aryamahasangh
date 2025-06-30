package com.aryamahasangh.util

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
expect fun PlatformBackHandler(
  customBackHandler: (() -> Unit)?,
  currentDestination: String?,
  navController: NavHostController
)
