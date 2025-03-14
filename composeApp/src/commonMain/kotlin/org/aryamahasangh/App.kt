package org.aryamahasangh

import AppTheme
import LocalThemeIsDark
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import aryamahasangh.composeapp.generated.resources.*
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import dev.burnoo.compose.remembersetting.rememberStringSetting
import kotlinx.coroutines.launch
import org.aryamahasangh.components.LoginDialog
import org.aryamahasangh.navigation.RootNavGraph
import org.aryamahasangh.navigation.Screen
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App() {
  DevelopmentEntryPoint {
    AppTheme {
      // for quickly testing the components
       //DemoComposable()
      AppDrawer()
    }
  }
}

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("SnackbarHostState is not found") }
//CompositionLocal for Authentication State:
val LocalAuthState = compositionLocalOf { mutableStateOf(false) }

@Composable
fun rememberAuthState(): MutableState<Boolean> {
  val currentLocalAuthState = LocalAuthState.current
  return remember { currentLocalAuthState }
}

// Use rememberSetting to create setting:
object SettingKeys {
  const val isLoggedIn = "isLoggedIn"
  const val userEmail = "userEmail"
}


@Composable
fun AppDrawer(){
  val navController = rememberNavController()
  BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var (selectedOption, setValue) = remember { mutableStateOf(drawerOptions[0].title) }

    val isLargeScreen = maxWidth > 840.dp
    if (isLargeScreen) {
      Box(modifier = Modifier.width(1280.dp)){
        LargeScreens("", drawerState, selectedOption, setValue, navController)
      }
    } else {
      SmallScreens("", drawerState, selectedOption, setValue, navController)
    }
  }
}

data class DrawerOption(val title: String, val icon: DrawableResource, val route: Screen = Screen.AboutUs)
val drawerOptions = listOf(
  DrawerOption("हमारे बारे मे", Res.drawable.info, Screen.AboutUs),
  DrawerOption("गतिविधियां", Res.drawable.local_activity, Screen.Activities),
  DrawerOption("संलग्न संस्थाएं", Res.drawable.account_tree, Screen.Orgs),
  DrawerOption("हमसें जुडें", Res.drawable.handshake, Screen.JoinUs),
  DrawerOption("छात्रा प्रवेश", Res.drawable.local_library, Screen.AdmissionForm),
  DrawerOption("स्वाध्याय", Res.drawable.local_library, Screen.Learning),
)

@Composable
@Preview
fun DrawerContentPreview() {
  DrawerContent(rememberDrawerState(initialValue = DrawerValue.Open), drawerOptions[0].title, {}, navController1 = rememberNavController())
}

@Composable
fun DrawerContent(
  drawerState: DrawerState,
  selectedOption: String,
  setValue: (String) -> Unit,
  navController1: NavHostController
) {
  val scope = rememberCoroutineScope()
  Column(modifier = Modifier.width(250.dp).padding(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Image(
        painter = painterResource(Res.drawable.mahasangh_logo_without_background),
        contentDescription = "arya mahasangh",
        modifier = Modifier.width(64.dp)
      )
      Column(
        modifier = Modifier.padding(top = 8.dp)) {
        Text("आर्य महासंघ",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold)
        Text("कृण्वन्तो विश्वमार्यम", style = MaterialTheme.typography.bodyMedium)
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    drawerOptions.forEach { option ->
      NavigationDrawerItem(
        label = {
          Text(option.title, style = MaterialTheme.typography.bodyLarge)
        },
        selected = selectedOption == option.title,
        onClick = {
          setValue(option.title)
          scope.launch { drawerState.close() }
          navController1.navigate(option.route){
            popUpTo(option.route)
            launchSingleTop = true
          }
        },
        icon = {
          Icon(
            painter = painterResource(option.icon),
            contentDescription = option.title,
          )
        },
      )
      if(option.title == "हमसें जुडें"){
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("गुरुकुल", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}

@Composable
fun LargeScreens(
  title: String,
  drawerState: DrawerState,
  selectedOption: String,
  setValue: (String) -> Unit,
  navController1: NavHostController
){
  PermanentNavigationDrawer(
    drawerContent = {
      PermanentDrawerSheet {
        DrawerContent(drawerState, selectedOption, setValue, navController1)
      }
    }
  ){
    Row(modifier = Modifier.fillMaxSize(1f).background(MaterialTheme.colorScheme.surfaceDim)) {
      VerticalDivider(
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = 4.dp)
      )
      MainContent(title, drawerState, selectedOption, setValue, navController1)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallScreens(
  title: String,
  drawerState: DrawerState,
  selectedOption: String,
  setValue: (String) -> Unit,
  navController1: NavHostController
) {
  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet {
        DrawerContent(drawerState, selectedOption, setValue, navController1)
      }
    },
    content = {
      MainContent(title, drawerState, selectedOption, setValue, navController1)
    }
  )
}

fun getScreenTitle(route: String?): String{
  val routeString = route?.substring(route.lastIndexOf(".") + 1)
  println("routeString: $routeString")
  return when(routeString) {
    "AboutUs" -> "हमारे बारे मे"
    "Activities" -> "गतिविधियां"
    "JoinUs" -> "हमसें जुडें"
    "Orgs" -> "आर्य महासंघ का वर्तमान स्वरुप"
    "OrgDetails/{name}" -> ""
    "Learning" -> "स्वाध्याय"
    "ContactUs" -> "हमसे संपर्क करें"
    else -> ""
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
  title: String,
  drawerState: DrawerState,
  selectedOption: String,
  setValue: (String) -> Unit,
  navController1: NavHostController
) {
  val scope = rememberCoroutineScope()
  val (orgDetails, selectedOrgDetails) = remember { mutableStateOf("") }
  val (activityDetails, selectedActivityDetails) = remember { mutableStateOf("") }
  val (videoDetails, selectedVideoDetails) = remember { mutableStateOf("") }

  val authState = rememberAuthState() // Get auth state from CompositionLocal
  var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
  var userEmail by rememberStringSetting(SettingKeys.userEmail, "")

  var showLoginDialog by remember { mutableStateOf(false) }
  var showLogoutDialog by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(selectedOption) {
    selectedOrgDetails("")
    selectedActivityDetails("")
    selectedVideoDetails("")
  }
  Scaffold(
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    topBar = {
      val currentRoute = navController1.currentDestination?.route
      val screenTitle = getScreenTitle(currentRoute)
      println("screentitle: $screenTitle")
      TopAppBar(
        title = {
          Row(modifier = Modifier.fillMaxWidth().basicMarquee().padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center) {
            Text(if(screenTitle.isNotEmpty()) screenTitle else "॥ ओ३म् ॥")
          }
        },
        navigationIcon = {
          if(orgDetails.isNotEmpty() || activityDetails.isNotEmpty() || videoDetails.isNotEmpty()) {
            IconButton(onClick = {
              selectedOrgDetails("")
              selectedActivityDetails("")
              selectedVideoDetails("")
              navController1.navigateUp()
            }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back Arrow")
            }
          }else {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
              Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
            }
          }
        },
        actions = {
          var isDark by LocalThemeIsDark.current
          TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(if(isDark) "Toggle to light" else "Toggle to dark") } },
            state = rememberTooltipState()
          ){
            IconButton(
              onClick = { isDark = !isDark }
            ) {
              Icon(
                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle brightness"
              )
            }
          }

          if(isLoggedIn){
            TooltipBox(
              positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
              tooltip = { PlainTooltip { Text("Logout") } },
              state = rememberTooltipState()
            ) {
              IconButton(
                onClick = { showLogoutDialog = true }
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.Logout,
                  contentDescription = "logout"
                )
              }
            }
          }else{
            TooltipBox(
              positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
              tooltip = { PlainTooltip { Text("Login") } },
              state = rememberTooltipState()
            ) {
              IconButton(
                onClick = { showLoginDialog = true }
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.Login,
                  contentDescription = "login"
                )
              }
            }
          }
        }
      )
    },
    content = { paddingValues ->
      Column (
        modifier = Modifier
          .fillMaxSize(1.0f)
          .padding(paddingValues)
      ) {
        println("Selected Option: $selectedOption")
        CompositionLocalProvider(
          LocalSnackbarHostState provides snackbarHostState,
          LocalAuthState provides authState
        ){
          RootNavGraph(
            navController = navController1,
            onNavigateToOrgDetails = { orgId ->
              selectedOrgDetails(orgId)
            },
            onNavigateToActivityDetails = { id ->
              selectedActivityDetails(id)
            },
            onNavigateToVideoDetails = { id ->
              selectedVideoDetails(id)
            }
          )
        }
      }
    },
    bottomBar = {

    }
  )
  // Login Dialog
  if (showLoginDialog) {
    LoginDialog(
      onDismiss = { showLoginDialog = false },
      onLoginSuccess = {
        isLoggedIn = true
        authState.value = true // Update auth state
        showLoginDialog = false
        scope.launch {
          snackbarHostState.showSnackbar(message = "Login successful")
        }
      },
    )
  }

  // Logout Dialog
  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Logout") },
      text = { Text("Are you sure you want to logout?") },
      confirmButton = {
        TextButton(onClick = {
          isLoggedIn = false
          authState.value = false
          showLogoutDialog = false
          scope.launch {
            snackbarHostState.showSnackbar("Logout successful")
          }
        }) {
          Text("Yes")
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("No")
        }
      }
    )
  }
}
