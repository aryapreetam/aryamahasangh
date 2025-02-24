package org.aryamahasangh

import AppTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.launch
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
//       DemoComposable()
      AppDrawer()
    }
  }
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
  DrawerOption("हमसें जुडें", Res.drawable.handshake, Screen.JoinUs),
  DrawerOption("संलग्न संस्थाएं", Res.drawable.account_tree, Screen.Orgs),
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
    Row(modifier = Modifier.fillMaxSize(1f)) {
      VerticalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = 4.dp))
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

@Composable
@Preview
fun TopBarContentPreview() {
  val screenTitle = ""
  BoxWithConstraints {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
      Text(if(screenTitle.isNotEmpty()) screenTitle else "॥ ओ३म् ॥")
    }
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

  LaunchedEffect(selectedOption) {
    selectedOrgDetails("")
    selectedActivityDetails("")
    selectedVideoDetails("")
  }
  Scaffold(
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
    },
    bottomBar = {
//      if (getPlatform().name == PLATFORM_WEB || getPlatform().name.startsWith("Java")) {
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//          Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text("जय आर्य - जय आर्यावर्त")
//            Text("© आर्य महासंघ")
//          }
//        }
//      }
    }
  )
}
