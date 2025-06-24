package org.aryamahasangh

import AppTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.family_add
import org.aryamahasangh.auth.SessionManager
import org.aryamahasangh.di.KoinInitializer
import org.aryamahasangh.examples.FormComponentsExample
import org.aryamahasangh.navigation.AppDrawer
import org.aryamahasangh.utils.WithTooltip
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview

// CompositionLocal for Authentication State
val LocalIsAuthenticated = compositionLocalOf { false }

// Initialize Koin for dependency injection
private val initKoin by lazy {
  KoinInitializer.init()
  true // Return a value to satisfy the lazy property
}


@Composable
fun App() {
  val demo = false // Set to true to show form components example
  if (!demo) {

    // Ensure Koin is initialized
    initKoin

    // Initialize session management when app starts
    LaunchedEffect(Unit) {
      SessionManager.initialize()
    }

    // Observe authentication state
    val isAuthenticated by SessionManager.isAuthenticated.collectAsState(initial = false)

    AppTheme {
      // Provide authentication state to the entire app
      CompositionLocalProvider(LocalIsAuthenticated provides isAuthenticated) {
        // Always show AppDrawer - login is optional
        AppDrawer()
      }
    }
  } else {
    AppTheme {
      // Show the form components example
      FormComponentsExample()
    }
    //ImagePickerExample()
    //ActivityFormImagePickerIntegration()
//    DragAndDropSample()
    //DNDWithCursor()
  }
}

@Preview
@Composable
fun Test(){
  Column {
    Icon(
      modifier = Modifier.size(96.dp),
      imageVector = vectorResource(Res.drawable.family_add),
      contentDescription = "fdf",
      tint = MaterialTheme.colorScheme.primary
    )
    WithTooltip(tooltip = "नया परिवार जोड़ें") {
      IconButton(
        onClick = {},
        modifier = Modifier
          .size(48.dp) // or your desired size
          .clip(RectangleShape)
      ) {
        Icon(
          imageVector = vectorResource(Res.drawable.family_add),
          contentDescription = "नया परिवार जोड़ें"
        )
      }
    }
  }
}
