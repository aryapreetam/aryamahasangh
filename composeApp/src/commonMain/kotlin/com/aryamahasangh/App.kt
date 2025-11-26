package com.aryamahasangh

import AppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.aryamahasangh.auth.SessionManager
import com.aryamahasangh.components.OngoingIcon
import com.aryamahasangh.di.KoinInitializer
import com.aryamahasangh.navigation.AppDrawer
import com.aryamahasangh.network.supabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.coil.coil3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        //val viewModel = CounterViewModel()
        //CounterScreen(viewModel = viewModel)
//        FormComponentsExample()
        FlowRow{
          OngoingIcon(Modifier.size(48.dp))
          Spacer(modifier = Modifier.width(16.dp))
//          UpcomingIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          OpenForRegistrationIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          RegistrationAvailableIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          OpenGateIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          OpenRegistrationIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          StarburstIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          JoinNowIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          OngoingIcon1(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          OngoingIcon2(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          AlivePulsingOngoingIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          //LiveNowIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          GlowOngoingIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          WaveOngoingIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          BreathingOngoingIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
//          OrbitingOngoingIcon(Modifier.size(48.dp))
//          PeacefulOngoingIconsRow()
//          ElegantOrbitingOngoingIcon()
//          RippleHaloOngoingIcon()
//          OrbitingOngoingIconEnhanced()
//          EccentricOrbitingIcon()
        }
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

//@Preview
//@Composable
//fun Test() {
//  Column {
//    Icon(
//      modifier = Modifier.size(96.dp),
//      imageVector = vectorResource(Res.drawable.family_add),
//      contentDescription = "fdf",
//      tint = MaterialTheme.colorScheme.primary
//    )
//    WithTooltip(tooltip = "नया परिवार जोड़ें") {
//      IconButton(
//        onClick = {},
//        modifier =
//          Modifier
//            .size(48.dp) // or your desired size
//            .clip(RectangleShape)
//      ) {
//        Icon(
//          imageVector = vectorResource(Res.drawable.family_add),
//          contentDescription = "नया परिवार जोड़ें"
//        )
//      }
//    }
//  }
//}

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

