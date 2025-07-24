package com.aryamahasangh

import AppTheme
import androidx.compose.foundation.layout.BoxWithConstraints
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
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.aryamahasangh.auth.SessionManager
import com.aryamahasangh.di.KoinInitializer
import com.aryamahasangh.examples.FormComponentsExample
import com.aryamahasangh.navigation.AppDrawer
import com.aryamahasangh.network.supabaseClient
import com.aryamahasangh.utils.WithTooltip
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.coil.coil3
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

  initializeCoilImageLoader()

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
      BoxWithConstraints {
        println("$maxWidth")
        FormComponentsExample()
      }

    }
    // ImagePickerExample()
    // ActivityFormImagePickerIntegration()
//    DragAndDropSample()
    // DNDWithCursor()
  }
}

@OptIn(SupabaseExperimental::class)
@Composable
private fun initializeCoilImageLoader() {
  setSingletonImageLoaderFactory {
    ImageLoader.Builder(it)
      .crossfade(true)
      .components {
        add(supabaseClient.coil3)
        add(KtorNetworkFetcherFactory())
      }.build()
  }
}

@Preview
@Composable
fun Test() {
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
        modifier =
          Modifier
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
