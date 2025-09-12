package com.aryamahasangh.navigation

import LocalThemeIsDark
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import aryamahasangh.composeapp.generated.resources.*
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.auth.SessionManager
import com.aryamahasangh.components.LoginDialog
import com.aryamahasangh.util.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val LocalSnackbarHostState =
  compositionLocalOf<SnackbarHostState> { error("SnackbarHostState is not found") }

// CompositionLocal for custom back navigation handling
val LocalBackHandler = compositionLocalOf<(() -> Unit)?> { null }

// CompositionLocal for setting custom back handler
val LocalSetBackHandler = compositionLocalOf<((() -> Unit)?) -> Unit> { {} }

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
    DrawerOption("आर्य गतिविधियां", Res.drawable.local_activity, Screen.ActivitiesSection),
    DrawerOption("संलग्न आर्य संस्थाएं", Res.drawable.account_tree, Screen.OrgsSection),
    DrawerOption("हमसें जुडें", Res.drawable.handshake, Screen.JoinUs),
//    DrawerOption(
//      "आर्य गुरुकुल",
//      Res.drawable.school,
//      Screen.AryaGurukulSection
//    ),
//    DrawerOption(
//      "आर्या गुरुकुल",
//      Res.drawable.school,
//      Screen.AryaaGurukulSection
//    ),
    DrawerOption(
      "आर्य-आर्या निर्माण(सत्र)",
      Res.drawable.interactive_space,
      Screen.AryaNirmanSection
    ),
    DrawerOption("आर्य परिवार", Res.drawable.family, Screen.AryaPariwarSection),
    DrawerOption("आर्य समाज संगठन", Res.drawable.diversity_3, Screen.AryaSamajSection),
//    DrawerOption(
//      "आर्य क्षात्र शिविर पंजीकरण",
//      Res.drawable.person,
//      Screen.KshatraTrainingSection
//    ),
//    DrawerOption(
//      "आर्य छात्रा शिविर पंजीकरण",
//      Res.drawable.face_4,
//      Screen.ChatraTrainingSection
//    ),
//    DrawerOption("आओ स्वाध्याय करें", Res.drawable.menu_book, Screen.Learning)
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
  val isLoggedIn = LocalIsAuthenticated.current

  Column(
    modifier =
      Modifier
        .width(250.dp)
        .padding(8.dp)
        .fillMaxHeight()
  ) {
    // Main content in scrollable column
    Column(
      modifier =
        Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth().clickable {
            // Navigate to AboutUs page
            navController.navigate(Screen.AboutSection) {
              // Clear back stack to root when navigating to AboutSection
              popUpTo(Screen.AboutSection) {
                inclusive = true
                saveState = false
              }
              launchSingleTop = true
              restoreState = false
            }
            scope.launch {
              drawerState.close()
            }
          },
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
          Text("कृण्वन्तो विश्वमार्यम्", style = MaterialTheme.typography.bodyMedium)
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
              val isStartDestination =
                when (option.route) {
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
                  Screen.KshatraTrainingSection -> true
                  Screen.ChatraTrainingSection -> true
                  else -> false
                }

              if (isStartDestination) {
                // For section start destinations, clear back stack completely to root
                popUpTo(Screen.AboutSection) {
                  inclusive = true // Include AboutSection in the pop
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
//        if (option.title == "हमसें जुडें") {
//          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
//          Text(
//            "आर्य गुरुकुल महाविद्यालय",
//            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
//            style = MaterialTheme.typography.titleMedium
//          )
//        } else
          if (option.title == "हमसें जुडें") {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Text(
            "सांगठनिक कार्य",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            style = MaterialTheme.typography.titleMedium
          )
        }
//          else if (option.title == "आर्य समाज संगठन") {
//          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
//          Text(
//            "स्वाध्याय ",
//            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
//            style = MaterialTheme.typography.titleMedium
//          )
//        }
      }
    }

    if (isLoggedIn) {
      // Admin option at the bottom
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      NavigationDrawerItem(
        label = {
          Text("व्यवस्थापकीय", style = MaterialTheme.typography.bodyLarge)
        },
        selected =
          checkIfSelected(
            currentDestination,
            Screen.AdminSection.toString()
          ),
        onClick = {
          navController.navigate(Screen.AdminContainer(0)) {
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
  } else if (
    (
      currentDestination?.contains("Activities") == true ||
        currentDestination?.contains("ActivityDetails") == true ||
        currentDestination?.contains("EditActivity") == true ||
        currentDestination?.contains("CreateActivity") == true
    ) &&
    currentDrawerItem == Screen.ActivitiesSection.toString()
  ) {
    true
  } else if (
    (
      currentDestination?.contains("Orgs") == true ||
        currentDestination?.contains("OrgDetails") == true
    ) &&
    currentDrawerItem == Screen.OrgsSection.toString()
  ) {
    true
  } else if (
    (
      currentDestination?.contains("Learning") == true ||
        currentDestination?.contains("VideoDetails") == true
    ) &&
    currentDrawerItem == Screen.LearningSection.toString()
  ) {
    true
  } else if (
    (
      currentDestination?.contains("BookOrderForm") == true ||
        currentDestination?.contains("BookOrderDetails") == true
    ) &&
    currentDrawerItem == Screen.BookSection.toString()
  ) {
    true
  } else if (
    (
      currentDestination?.contains("AryaNirmanHome") == true ||
        currentDestination?.contains("AryaNirmanRegistrationForm") == true
    ) &&
    currentDrawerItem == Screen.AryaNirmanSection.toString()
  ) {
    true
  } else if (
    currentDestination?.contains("AryaPariwarHome") == true &&
    currentDrawerItem == Screen.AryaPariwarSection.toString()
  ) {
    true
  } else if (
    currentDestination?.contains("AryaSamajHome") == true &&
    currentDrawerItem == Screen.AryaSamajSection.toString()
  ) {
    true
  } else if (
    currentDestination?.contains("KshatraTrainingHome") == true &&
    currentDrawerItem == Screen.KshatraTrainingSection.toString()
  ) {
    true
  } else if (
    currentDestination?.contains("ChatraTrainingHome") == true &&
    currentDrawerItem == Screen.ChatraTrainingSection.toString()
  ) {
    true
  } else if (
    currentDestination?.contains("AryaGurukulCollege") == true &&
    currentDrawerItem == Screen.AryaGurukulSection.toString()
  ) {
    true
  } else if (
    (
      currentDestination?.contains("AryaaGurukulCollege") == true ||
        currentDestination?.contains("AdmissionForm") == true
    ) &&
    currentDrawerItem == Screen.AryaaGurukulSection.toString()
  ) {
    true
  } else if (
    (
      currentDestination?.contains("AdminContainer") == true ||
        currentDestination?.contains("MemberDetail") == true ||
        currentDestination?.contains("AddMemberForm") == true ||
        currentDestination?.contains("EditMemberForm") == true ||
        currentDestination?.contains("AddAryaSamajForm") == true ||
        currentDestination?.contains("AryaSamajDetail") == true ||
        currentDestination?.contains("CreateFamilyForm") == true ||
        currentDestination?.contains("EditFamilyForm") == true ||
        currentDestination?.contains("FamilyDetail") == true
    ) &&
    currentDrawerItem == Screen.AdminSection.toString()
  ) {
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

  val isLoggedIn = LocalIsAuthenticated.current
  var showLoginDialog by remember { mutableStateOf(false) }
  var showLogoutDialog by remember { mutableStateOf(false) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
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

  // Global message observation
  val globalMessage by GlobalMessageManager.currentMessage.collectAsState()
  LaunchedEffect(globalMessage) {
    globalMessage?.let { message ->
      val duration = when (message.duration) {
        GlobalMessageDuration.SHORT -> SnackbarDuration.Short
        GlobalMessageDuration.LONG -> SnackbarDuration.Short
        GlobalMessageDuration.INDEFINITE -> SnackbarDuration.Indefinite
      }

      snackbarHostState.showSnackbar(
        message = message.message,
        actionLabel = null,
        duration = duration
      )

      // Clear the message after showing
      GlobalMessageManager.clearMessage()
    }
  }

  Scaffold(
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
          // Determine message type for icon
          // Find the GlobalMessage in currentMessage to match string or propagate meta
          // (We use the last shown message; best effort for matching)
          val globalMsg = globalMessage
          val isError = globalMsg?.type == GlobalMessageType.ERROR
          val isSuccess = globalMsg?.type == GlobalMessageType.SUCCESS
          val icon = when {
            isSuccess -> Icons.Filled.CheckCircle
            isError -> Icons.Filled.Error
            else -> Icons.Filled.Info
          }
          val iconTint = when {
            isError -> MaterialTheme.colorScheme.error
            isSuccess -> Color(0xFF00C853) // Bright, vibrant green for celebration
            else -> MaterialTheme.colorScheme.primary
          }

          // Center snackbar and constrain width to content
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Surface(
              shadowElevation = 6.dp,
              shape = MaterialTheme.shapes.extraLarge,
              tonalElevation = 2.dp,
              color = MaterialTheme.colorScheme.inverseSurface,
              modifier = Modifier
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
              ) {
                Icon(
                  icon, contentDescription = null,
                  tint = iconTint,
                  modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp)
                )
                Text(
                  data.visuals.message,
                  color = MaterialTheme.colorScheme.inverseOnSurface,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = { snackbarHostState.currentSnackbarData?.dismiss() },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.inverseOnSurface
                  )
                }
              }
            }
          }
        }
      )
    },
    topBar = {
      TopAppBar(
        title = {
          Row(
            modifier = Modifier.fillMaxWidth().basicMarquee().padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center
          ) {
            // Show परीक्षणार्थ for non-production environments
            val environment = VersionInfo.getEnvironment()
            if (environment != "prod") {
              Text(
                "परीक्षणार्थ",
                color = Color.Red.copy(alpha = 0.6f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        },
        navigationIcon = {
          val currentScreen = currentDestination?.substringAfterLast(".")
          val shouldShowBack =
            shouldShowBackButtonFor(currentScreen)
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
            }, modifier = Modifier.semantics { contentDescription = "nav_back" }.testTag("nav_back")) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "nav_back")
            }
          } else {
            IconButton(
              onClick = { scope.launch { drawerState.open() } },
              modifier = Modifier.semantics { contentDescription = "nav_hamburger" }.testTag("nav_hamburger")
            ) {
              Icon(Icons.Default.Menu, contentDescription = "nav_hamburger")
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
                onClick = {
                  showLogoutDialog = true
                }
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
                onClick = {
                  showLoginDialog = true
                }
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
                    text = VersionInfo.getFormattedVersion(),
                    style = MaterialTheme.typography.labelSmall
                  )
                },
                onClick = { showOverflowMenu = false }
              )
              DropdownMenuItem(
                text = { Text("Privacy Policy") },
                onClick = {
                  showOverflowMenu = false
                  showPrivacyDialog = true
                }
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
          LocalBackHandler provides customBackHandler, // Provide the mutable state for the back handler
          LocalSetBackHandler provides { handler -> setCustomBackHandler(handler) } // Provide the setter for the back handler
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
        // Authentication state is automatically updated by SessionManager
        showLoginDialog = false
        scope.launch {
          snackbarHostState.showSnackbar(message = "Login successful")
        }
      }
    )
  }

  // Logout Dialog
  if (showLogoutDialog) {
    var logoutAttempts by remember { mutableStateOf(0) }
    var showRetryOption by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = {
        showLogoutDialog = false
        logoutAttempts = 0
        showRetryOption = false
      },
      title = { Text("Logout") },
      text = {
        Column {
          Text("Are you sure you want to logout?")
          if (showRetryOption) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              "कनेक्शन की समस्या के कारण लॉगआउट पूरा नहीं हो सका। पुनः प्रयत्न करें?",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            scope.launch {
              logoutAttempts++

              // Use SessionManager to sign out
              val result = SessionManager.signOut()

              result.fold(
                onSuccess = {
                  showLogoutDialog = false
                  logoutAttempts = 0
                  showRetryOption = false
                  snackbarHostState.showSnackbar("सफलतापूर्वक लॉगआउट हो गया")
                },
                onFailure = { error ->
                  println("Logout attempt $logoutAttempts failed: ${error.message}")

                  if (logoutAttempts >= 2) {
                    // After 2 attempts, do local logout and close dialog
                    showLogoutDialog = false
                    logoutAttempts = 0
                    showRetryOption = false
                    // Local session was already cleared in signOut fallback
                    snackbarHostState.showSnackbar(
                      "लॉगआउट हो गया (ऑफलाइन मोड)",
                      duration = SnackbarDuration.Long
                    )
                  } else {
                    // Show retry option after first failure
                    showRetryOption = true
                  }
                }
              )
            }
          }
        ) {
          Text(
            if (showRetryOption) "पुनः प्रयत्न करें" else "Yes"
          )
        }
      },
      dismissButton = {
        Row {
          if (showRetryOption) {
            TextButton(
              onClick = {
                // User chooses to logout locally only
                showLogoutDialog = false
                logoutAttempts = 0
                showRetryOption = false
                scope.launch {
                  snackbarHostState.showSnackbar(
                    "लॉगआउट हो गया (ऑफलाइन मोड)",
                    duration = SnackbarDuration.Long
                  )
                }
              }
            ) {
              Text("ऑफलाइन लॉगआउट")
            }
            Spacer(modifier = Modifier.width(8.dp))
          }
          TextButton(
            onClick = {
              showLogoutDialog = false
              logoutAttempts = 0
              showRetryOption = false
            }
          ) {
            Text(if (showRetryOption) "Cancel" else "No")
          }
        }
      }
    )
  }

  // Privacy Policy Dialog
  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy Policy") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
          Text("Our Privacy Policy:")
          Spacer(modifier = Modifier.height(8.dp))
          Text("• This application is purely for educational purpose.")
          Spacer(modifier = Modifier.height(8.dp))
          Text("• This application does not collect any user data of any kind.")
          Spacer(modifier = Modifier.height(4.dp))
          Text("• The intended audience is anyone 16+ years old.")
          Spacer(modifier = Modifier.height(4.dp))
          Text("• We fully respect user privacy.")
          Spacer(modifier = Modifier.height(8.dp))
          Text("Contact Information:")
          Spacer(modifier = Modifier.height(4.dp))
          Text("Email: mahasangharya@gmail.com")
          Spacer(modifier = Modifier.height(4.dp))
          Text("Website: https://aryamahasangh.com")
        }
      },
      confirmButton = {
        TextButton(
          onClick = { showPrivacyDialog = false }
        ) {
          Text("OK")
        }
      }
    )
  }
}

private fun shouldShowBackButtonFor(currentScreen: String?): Boolean = listOf(
  "AboutUsDetails",
  "ActivityDetails",
  "EditActivity",
  "CreateActivity",
  "OrgDetails",
  "NewOrganisationForm",
  "VideoDetails",
  "AdmissionForm",
  "BookOrderDetails",
  "AryaNirmanRegistrationForm",
  "MemberDetail",
  "AddMemberForm",
  "EditMemberForm",
  "AddAryaSamajForm",
  "EditAryaSamajForm",
  "AryaSamajDetail",
  "CreateFamilyForm",
  "EditFamilyForm",
  "FamilyDetail"
).any {
  currentScreen?.startsWith(it) == true
}
