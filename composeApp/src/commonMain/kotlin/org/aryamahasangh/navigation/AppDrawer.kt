package org.aryamahasangh.navigation

import LocalThemeIsDark
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import aryamahasangh.composeapp.generated.resources.*
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import dev.burnoo.compose.remembersetting.rememberStringSetting
import kotlinx.coroutines.launch
import org.aryamahasangh.components.LoginDialog
import org.aryamahasangh.util.PlatformBackHandler
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val LocalSnackbarHostState =
  compositionLocalOf<SnackbarHostState> { error("SnackbarHostState is not found") }

// CompositionLocal for custom back navigation handling
val LocalBackHandler = compositionLocalOf<(() -> Unit)?> { null }

// CompositionLocal for setting custom back handler
val LocalSetBackHandler = compositionLocalOf<(((() -> Unit)?) -> Unit)?> { null }

// CompositionLocal for Authentication State:
val LocalAuthState = compositionLocalOf { mutableStateOf(false) }

@Composable
fun rememberAuthState(): MutableState<Boolean> {
  val currentLocalAuthState = LocalAuthState.current
  return remember { currentLocalAuthState }
}

@Composable
fun AppDrawer() {
  BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    val isLargeScreen = maxWidth > 840.dp
    if (isLargeScreen) {
      Box(modifier = Modifier.width(1280.dp)) {
        LargeScreens(drawerState, navController)
      }
    } else {
      SmallScreens(drawerState, navController)
    }
  }
}

data class DrawerOption(val title: String, val icon: DrawableResource, val route: Screen = Screen.AboutSection)

val drawerOptions =
  listOf(
    DrawerOption("हमारे बारे मे", Res.drawable.info, Screen.AboutSection),
    DrawerOption("गतिविधियां", Res.drawable.local_activity, Screen.ActivitiesSection),
    DrawerOption("संलग्न संस्थाएं", Res.drawable.account_tree, Screen.OrgsSection),
    DrawerOption("हमसें जुडें", Res.drawable.handshake, Screen.JoinUs),
    DrawerOption("आर्य गुरुकुल", Res.drawable.school, Screen.AryaGurukulSection),
    DrawerOption("आर्या गुरुकुल", Res.drawable.school, Screen.AryaaGurukulSection),
    DrawerOption(
      "आर्य-आर्या निर्माण(सत्र)",
      Res.drawable.interactive_space,
      Screen.AryaNirmanSection
    ),
    DrawerOption("आर्य परिवार", Res.drawable.family, Screen.AryaPariwarSection),
    DrawerOption("आर्य समाज संगठन", Res.drawable.diversity_3, Screen.AryaSamajSection),
    DrawerOption(
      "आर्य क्षात्र शिविर पंजीकरण",
      Res.drawable.person,
      Screen.KshatraTrainingSection
    ),
    DrawerOption(
      "आर्य छात्रा शिविर पंजीकरण",
      Res.drawable.face_4,
      Screen.ChatraTrainingSection
    ),
    DrawerOption("आओ स्वाध्याय करें", Res.drawable.menu_book, Screen.Learning)
  )

@Composable
fun DrawerContent(
  drawerState: DrawerState,
  navController: NavHostController
) {
  val scope = rememberCoroutineScope()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination by remember {
    derivedStateOf { backStackEntry?.destination?.route }
  }
  var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)

  Column(
    modifier = Modifier.width(250.dp).padding(8.dp).fillMaxHeight()
  ) {
    // Main content in scrollable column
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
    ) {
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
          modifier = Modifier.padding(top = 8.dp)
        ) {
          Text(
            "आर्य महासंघ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text("कृण्वन्तो विश्वमार्यम", style = MaterialTheme.typography.bodyMedium)
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      drawerOptions.forEach { option ->
        NavigationDrawerItem(
          label = {
            Text(option.title, style = MaterialTheme.typography.bodyLarge)
          },
          selected =
            checkIfSelected(
              currentDestination,
              option.route.toString()
            ),
          onClick = {
            navController.navigate(option.route) {
              // Clear back stack to root start destination when navigating to section start destinations
              val isStartDestination = when (option.route) {
                Screen.AboutSection -> true
                Screen.ActivitiesSection -> true
                Screen.OrgsSection -> true
                Screen.LearningSection -> true
                Screen.BookSection -> true
                Screen.AryaNirmanSection -> true
                Screen.AryaPariwarSection -> true
                Screen.AryaSamajSection -> true
                Screen.AryaGurukulSection -> true
                Screen.AryaaGurukulSection -> true
                Screen.JoinUs -> true
                else -> false
              }

              if (isStartDestination) {
                // For section start destinations, clear back stack completely to root
                popUpTo(Screen.AboutSection) {
                  inclusive = true  // Include AboutSection in the pop
                  saveState = false // Don't save state to ensure clean back stack
                }
              } else {
                // For other destinations, use current behavior
                popUpTo(navController.graph.startDestDisplayName) {
                  saveState = true
                }
              }
              launchSingleTop = true
              restoreState = false // Don't restore state for clean navigation
              scope.launch {
                drawerState.close()
              }
            }
          },
          icon = {
            Icon(
              painter = painterResource(option.icon),
              contentDescription = option.title
            )
          }
        )
        if (option.title == "हमसें जुडें") {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Text(
            "आर्ष विद्या",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            style = MaterialTheme.typography.titleMedium
          )
        } else if (option.title == "आर्या गुरुकुल") {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Text(
            "संगठन कार्य ",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            style = MaterialTheme.typography.titleMedium
          )
        } else if (option.title == "आर्य छात्रा शिविर पंजीकरण") {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Text(
            "स्वाध्याय ",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            style = MaterialTheme.typography.titleMedium
          )
        }
      }
    }

    if(isLoggedIn) {
      // Admin option at the bottom
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      NavigationDrawerItem(
        label = {
          Text("व्यवस्थापकीय", style = MaterialTheme.typography.bodyLarge)
        },
        selected = checkIfSelected(currentDestination, Screen.AdminContainer.toString()),
        onClick = {
          navController.navigate(Screen.AdminContainer) {
            // For admin section, clear back stack completely to root
            popUpTo(Screen.AboutSection) {
              inclusive = true
              saveState = false
            }
            launchSingleTop = true
            restoreState = false
            scope.launch {
              drawerState.close()
            }
          }
        },
        icon = {
          Icon(
            painter = painterResource(Res.drawable.account_circle),
            contentDescription = "व्यवस्थापकीय"
          )
        }
      )
    }
  }
}

private fun checkIfSelected(
  currentDestination: String?,
  currentDrawerItem: String
): Boolean {
  return if ((currentDestination?.contains("AboutUs") == true || currentDestination?.contains("AboutUsDetails") == true) && currentDrawerItem == Screen.AboutSection.toString()) {
    true
  } else if ((
      currentDestination?.contains("Activities") == true ||
        currentDestination?.contains("ActivityDetails") == true ||
        currentDestination?.contains("EditActivity") == true
      ) && currentDrawerItem == Screen.ActivitiesSection.toString()) {
    true
  } else if ((currentDestination?.contains("Orgs") == true || currentDestination?.contains("OrgDetails") == true) && currentDrawerItem == Screen.OrgsSection.toString()) {
    true
  } else if ((currentDestination?.contains("Learning") == true || currentDestination?.contains("VideoDetails") == true) && currentDrawerItem == Screen.LearningSection.toString()) {
    true
  } else if ((currentDestination?.contains("BookOrderForm") == true || currentDestination?.contains("BookOrderDetails") == true) && currentDrawerItem == Screen.BookSection.toString()) {
    true
  } else if ((currentDestination?.contains("AryaNirmanHome") == true || currentDestination?.contains("AryaNirmanRegistrationForm") == true) && currentDrawerItem == Screen.AryaNirmanSection.toString()) {
    true
  } else if ((currentDestination?.contains("AryaPariwarHome") == true) && currentDrawerItem == Screen.AryaPariwarSection.toString()) {
    true
  } else if ((currentDestination?.contains("AryaSamajHome") == true) && currentDrawerItem == Screen.AryaSamajSection.toString()) {
    true
  } else if ((currentDestination?.contains("KshatraTrainingHome") == true) && currentDrawerItem == Screen.KshatraTrainingSection.toString()) {
    true
  } else if ((currentDestination?.contains("ChatraTrainingHome") == true) && currentDrawerItem == Screen.ChatraTrainingSection.toString()) {
    true
  } else if ((currentDestination?.contains("AryaGurukulCollege") == true) && currentDrawerItem == Screen.AryaGurukulSection.toString()) {
    true
  } else if ((currentDestination?.contains("AryaaGurukulCollege") == true || currentDestination?.contains("AdmissionForm") == true) && currentDrawerItem == Screen.AryaaGurukulSection.toString()) {
    true
  } else if ((currentDestination?.contains("AdminContainer") == true || currentDestination?.contains("MemberDetail") == true) && currentDrawerItem == Screen.AdminSection.toString()) {
    true
  } else if (currentDestination?.contains(currentDrawerItem) == true) {
    true
  } else {
    false
  }
}

@Composable
fun LargeScreens(
  drawerState: DrawerState,
  navController: NavHostController
) {
  PermanentNavigationDrawer(
    drawerContent = {
      PermanentDrawerSheet {
        DrawerContent(drawerState, navController)
      }
    }
  ) {
    Row(modifier = Modifier.fillMaxSize(1f).background(MaterialTheme.colorScheme.surfaceDim)) {
      VerticalDivider(
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = 4.dp)
      )
      MainContent(drawerState, navController)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallScreens(
  drawerState: DrawerState,
  navController: NavHostController
) {
  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet {
        DrawerContent(drawerState, navController)
      }
    },
    content = {
      MainContent(drawerState, navController)
    }
  )
}

fun getScreenTitle(route: String?): String {
  val routeString = route?.substring(route.lastIndexOf(".") + 1)
  println("routeString: $routeString")
  return when (routeString) {
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
  drawerState: DrawerState,
  navController: NavHostController
) {
  val scope = rememberCoroutineScope()

  val authState = rememberAuthState() // Get auth state from CompositionLocal
  var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
  var userEmail by rememberStringSetting(SettingKeys.userEmail, "")
  var showLoginDialog by remember { mutableStateOf(false) }
  var showLogoutDialog by remember { mutableStateOf(false) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination by remember {
    derivedStateOf { backStackEntry?.destination?.route }
  }

  // State for custom back handler
  var customBackHandler by remember { mutableStateOf<(() -> Unit)?>(null) }

  // Function to set the custom back handler
  fun setCustomBackHandler(handler: (() -> Unit)?) {
    customBackHandler = handler
  }

  LaunchedEffect(currentDestination) {
    println("currentRoute: $currentDestination")
    customBackHandler = null // Clear any custom back handler when destination changes
  }

  Scaffold(
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    topBar = {
      TopAppBar(
        title = {
          Row(
            modifier = Modifier.fillMaxWidth().basicMarquee().padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center
          ) {
            // Text(if(screenTitle.isNotEmpty()) screenTitle else "॥ ओ३म् ॥")
          }
        },
        navigationIcon = {
          val currentScreen = currentDestination?.substringAfterLast(".")
          val shouldShowBack =
            listOf(
              "AboutUsDetails",
              "ActivityDetails",
              "EditActivity",
              "OrgDetails",
              "VideoDetails",
              "AdmissionForm",
              "BookOrderDetails",
              "AryaNirmanRegistrationForm",
              "MemberDetail"
            ).any {
              currentScreen?.startsWith(it) == true
            }
          if (shouldShowBack) {
            val backHandler = LocalBackHandler.current
            IconButton(onClick = {
              if (customBackHandler != null) {
                customBackHandler!!.invoke()
              } else if (backHandler != null) {
                backHandler()
              } else {
                navController.navigateUp()
              }
            }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back Arrow")
            }
          } else {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
              Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
            }
          }
        },
        actions = {
          var isDark by LocalThemeIsDark.current
          TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(if (isDark) "Toggle to light" else "Toggle to dark") } },
            state = rememberTooltipState()
          ) {
            IconButton(
              onClick = { isDark = !isDark }
            ) {
              Icon(
                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle brightness"
              )
            }
          }

          if (isLoggedIn) {
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
          } else {
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

          // Overflow menu with version info
          IconButton(
            onClick = { showOverflowMenu = true }
          ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
          }

          if (showOverflowMenu) {
            DropdownMenu(
              expanded = showOverflowMenu,
              onDismissRequest = { showOverflowMenu = false }
            ) {
              DropdownMenuItem(
                text = {
                  Text(
                    text = org.aryamahasangh.util.VersionInfo.getFormattedVersion(),
                    style = MaterialTheme.typography.labelSmall
                  )
                },
                onClick = { showOverflowMenu = false }
              )
            }
          }
        }
      )
    },
    content = { paddingValues ->
      Column(
        modifier =
          Modifier
            .fillMaxSize(1.0f)
            .padding(paddingValues)
      ) {
        CompositionLocalProvider(
          LocalSnackbarHostState provides snackbarHostState,
          LocalAuthState provides authState,
          LocalBackHandler provides customBackHandler, // Provide the mutable state for the back handler
          LocalSetBackHandler provides { setCustomBackHandler(it) } // Provide the setter for the back handler
        ) {
          // Platform-specific back button handling
          PlatformBackHandler(
            customBackHandler = customBackHandler,
            currentDestination = currentDestination,
            navController = navController
          )

          RootNavGraph(navController)
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
      }
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

object SettingKeys {
  const val isLoggedIn = "isLoggedIn"
  const val userEmail = "userEmail"
}
