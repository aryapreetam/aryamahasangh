package org.aryamahasangh.util

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import org.aryamahasangh.navigation.Screen

@Composable
fun AndroidBackHandler(
  customBackHandler: (() -> Unit)?,
  currentDestination: String?,
  navController: NavHostController
) {
  val currentRoute = currentDestination?.substringAfterLast(".")

  // Only use BackHandler if we're NOT on AboutUs (to allow app exit on AboutUs)
  if (currentRoute != "AboutUs") {
    BackHandler {
      if (customBackHandler != null) {
        customBackHandler.invoke()
      } else {
        // Check if we're on a section start destination
        val isOnSectionStart =
          when (currentRoute) {
            "Activities" -> true
            "Orgs" -> true
            "Learning" -> true
            "BookOrderForm" -> true
            "AryaNirmanHome" -> true
            "AryaPariwarHome" -> true
            "AryaSamajHome" -> true
            "AryaGurukulCollege" -> true
            "AryaaGurukulCollege" -> true
            "JoinUs" -> true
            "AdminContainer" -> true
            else -> false
          }

        if (isOnSectionStart) {
          // Navigate to root start destination
          navController.navigate(Screen.AboutSection) {
            popUpTo(0) { inclusive = true }
          }
        } else {
          // Default back behavior for detail screens
          navController.navigateUp()
        }
      }
    }
  }
  // If currentRoute == "AboutUs", don't use BackHandler at all, allowing system to handle back press (app exit)
}
