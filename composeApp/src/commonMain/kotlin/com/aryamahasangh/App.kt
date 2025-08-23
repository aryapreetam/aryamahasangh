package com.aryamahasangh

import AppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.family_add
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.aryamahasangh.auth.SessionManager
import com.aryamahasangh.di.KoinInitializer
import com.aryamahasangh.navigation.AppDrawer
import com.aryamahasangh.network.supabaseClient
import com.aryamahasangh.utils.WithTooltip
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.coil.coil3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

  //initializeCoilImageLoader()

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
        val viewModel = CounterViewModel()
        CounterScreen(viewModel = viewModel)
//        FormComponentsExample()
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

class CounterViewModel {
  private val _count = MutableStateFlow(0)
  val count: StateFlow<Int> = _count.asStateFlow()

  fun increment() {
    _count.value += 1
  }
}
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
  val count by viewModel.count.collectAsState()

  Column(
    modifier = Modifier.padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = count.toString(),
      fontSize = 24.sp,
      modifier = Modifier.testTag("counterText")
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
      onClick = { viewModel.increment() },
      modifier = Modifier.testTag("incrementButton")
    ) {
      Text("Increment")
    }
  }
}

