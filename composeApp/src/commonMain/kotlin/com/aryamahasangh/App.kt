package com.aryamahasangh

import AppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aryamahasangh.auth.SessionManager
import com.aryamahasangh.di.KoinInitializer
import com.aryamahasangh.navigation.AppDrawer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//// CompositionLocal for Authentication State
val LocalIsAuthenticated = compositionLocalOf { false }
//
//@Composable
//fun App() {
//
//  //initializeCoilImageLoader()
//
//  val demo = false // Set to true to show form components example
//  if (!demo) {
//    // Initialize Koin eagerly before composition to ensure proper initialization order
//    // Wrapped in remember to ensure it only runs once
//    remember {
//      try {
//        KoinInitializer.init()
//        println("Koin initialized successfully")
//        true
//      } catch (e: Exception) {
//        println("Error initializing Koin: ${e.message}")
//        e.printStackTrace()
//        false
//      }
//    }
//
//    // Initialize session management when app starts
//    LaunchedEffect(Unit) {
//      try {
//        SessionManager.initialize()
//        println("SessionManager initialized successfully")
//      } catch (e: Exception) {
//        println("Error initializing SessionManager: ${e.message}")
//        e.printStackTrace()
//        // Non-fatal: user can still use app, just won't have restored session
//      }
//    }
//
//    // Observe authentication state
//    val isAuthenticated by SessionManager.isAuthenticated.collectAsState(initial = false)
//
//    AppTheme {
//      // Provide authentication state to the entire app
//      CompositionLocalProvider(LocalIsAuthenticated provides isAuthenticated) {
//        // Always show AppDrawer - login is optional
//        AppDrawer()
//      }
//    }
//  } else {
//    AppTheme {
//      BoxWithConstraints {
//        println("$maxWidth")
//        //val viewModel = CounterViewModel()
//        //CounterScreen(viewModel = viewModel)
////        FormComponentsExample()
//        FlowRow{
//          OngoingIcon(Modifier.size(48.dp))
//          Spacer(modifier = Modifier.width(16.dp))
////          UpcomingIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          OpenForRegistrationIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          RegistrationAvailableIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          OpenGateIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          OpenRegistrationIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          StarburstIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          JoinNowIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          OngoingIcon1(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          OngoingIcon2(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          AlivePulsingOngoingIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          //LiveNowIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          GlowOngoingIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          WaveOngoingIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          BreathingOngoingIcon(Modifier.size(48.dp))
////          Spacer(modifier = Modifier.width(16.dp))
////          OrbitingOngoingIcon(Modifier.size(48.dp))
////          PeacefulOngoingIconsRow()
////          ElegantOrbitingOngoingIcon()
////          RippleHaloOngoingIcon()
////          OrbitingOngoingIconEnhanced()
////          EccentricOrbitingIcon()
//        }
//      }
//    }
//    // ImagePickerExample()
//    // ActivityFormImagePickerIntegration()
////    DragAndDropSample()
//    // DNDWithCursor()
//  }
//}
//
//@OptIn(SupabaseExperimental::class)
//@Composable
//private fun initializeCoilImageLoader() {
//  setSingletonImageLoaderFactory {
//    ImageLoader.Builder(it)
//      .crossfade(true)
//      .components {
//        add(supabaseClient.coil3)
//        add(KtorNetworkFetcherFactory())
//      }.build()
//  }
//}
//
//class CounterViewModel {
//  private val _count = MutableStateFlow(0)
//  val count: StateFlow<Int> = _count.asStateFlow()
//
//  fun increment() {
//    _count.value += 1
//  }
//}
//@Composable
//fun CounterScreen(viewModel: CounterViewModel) {
//  val count by viewModel.count.collectAsState()
//
//  Column(
//    modifier = Modifier.padding(16.dp),
//    horizontalAlignment = Alignment.CenterHorizontally
//  ) {
//    Text(
//      text = count.toString(),
//      fontSize = 24.sp,
//      modifier = Modifier.testTag("counterText")
//    )
//    Spacer(modifier = Modifier.height(8.dp))
//    Button(
//      onClick = { viewModel.increment() },
//      modifier = Modifier.testTag("incrementButton")
//    ) {
//      Text("Increment")
//    }
//  }
//}


@Composable
fun App() {
  // Track initialization state
  var initializationComplete by remember { mutableStateOf(false) }
  var initializationError by remember { mutableStateOf<String?>(null) }

  // STEP 1: Initialize Sentry (safe, no dependencies)
//  LaunchedEffect(Unit) {
//    try {
//      initializeSentry()
//      println("✅ Sentry initialized successfully")
//    } catch (e: Exception) {
//      println("⚠️  Error initializing Sentry: ${e.message}")
//      // Non-fatal: app can continue without crash reporting
//    }
//  }

  // STEP 2: Initialize Koin FIRST in a coroutine (async, prevents iOS deadlock)
  // CRITICAL: Must use LaunchedEffect (async) not remember (sync)
  // Synchronous initialization during composition causes iOS Keychain deadlock
  var koinInitialized by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    try {
      // CRITICAL: Must run on Main thread because Koin modules access AppConfig (iOS Keychain)
      // Accessing Keychain from background thread during startup causes C++ exception on iOS
      KoinInitializer.init()
      println("✅ Koin initialized successfully")
      println("✅ SupabaseClient initialized via Koin module")
      koinInitialized = true
    } catch (e: Exception) {
      println("❌ Error initializing Koin: ${e.message}")
      e.printStackTrace()
      initializationError = "Koin initialization failed: ${e.message}"
      // Allow degraded mode
      initializationComplete = true
    }
  }

  // STEP 3: Initialize SessionManager ONLY AFTER Koin is ready
  // This ensures supabaseClient is already initialized
  LaunchedEffect(koinInitialized) {
    if (koinInitialized) {
      try {
        // SessionManager accesses supabaseClient which may trigger Keychain access
        // Keep on Main thread for safety
        SessionManager.initialize()
        println("✅ SessionManager initialized successfully")
        initializationComplete = true
      } catch (e: Exception) {
        println("⚠️  Error initializing SessionManager: ${e.message}")
        e.printStackTrace()
        // Non-fatal: user can still use app, just won't have restored session
        initializationComplete = true // Still allow app to continue
      }
    }
  }

  // STEP 4: Show loading or error state during initialization
  if (!initializationComplete) {
    AppTheme {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          if (initializationError != null) {
            Text(
              "⚠️ Initialization Error",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              initializationError ?: "",
              style = MaterialTheme.typography.bodyMedium
            )
          } else {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Initializing...")
          }
        }
      }
    }
    return  // Don't proceed until initialization is complete
  }

  // STEP 5: Observe authentication state (ONLY after supabaseClient is initialized)
  val isAuthenticated by SessionManager.isAuthenticated.collectAsState(initial = false)

  AppTheme {
    // Provide authentication state to the entire app
    CompositionLocalProvider(LocalIsAuthenticated provides isAuthenticated) {
      // Always show AppDrawer - login is optional
      AppDrawer()
    }
  }
}
