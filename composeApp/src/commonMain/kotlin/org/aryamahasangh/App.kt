package org.aryamahasangh

import AppTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.aryamahasangh.components.OrgThumbnail
import org.aryamahasangh.components.Organisation
import org.aryamahasangh.navigation.RootNavGraph
import org.aryamahasangh.navigation.Screen
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource


@Composable
@Preview
fun App() {
  DevelopmentEntryPoint {
    AppTheme {
      val navController = rememberNavController()
      BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        var (selectedOption, setValue) = remember { mutableStateOf(drawerOptions[0].title) }

        println("selected: ${navController.currentDestination?.label}")

        val isLargeScreen = maxWidth > 800.dp
        if (isLargeScreen) {
          Box(modifier = Modifier.width(1024.dp)){
            LargeScreens("", drawerState, selectedOption, setValue, navController)
          }
        } else {
          SmallScreens("", drawerState, selectedOption, setValue, navController)
        }
      }
    }
  }
}

const val PLATFORM_WEB = "Web with Kotlin/Wasm"
data class DrawerOption(val title: String, val icon: DrawableResource, val route: Screen = Screen.AboutUs)
val drawerOptions = listOf(
  DrawerOption("हमारे बारे मे", Res.drawable.info, Screen.AboutUs),
  DrawerOption("गतिविधियां", Res.drawable.local_activity, Screen.Activities),
  DrawerOption("हमसें जुडें", Res.drawable.handshake, Screen.JoinUs),
  DrawerOption("संलग्न संस्थाएं", Res.drawable.account_tree, Screen.Orgs),
  DrawerOption("स्वाध्याय", Res.drawable.local_library, Screen.Learning),
  DrawerOption("हमसे संपर्क करें", Res.drawable.contact_page, Screen.ContactUs),
)


@Composable
fun AboutUs() {
  Organisation(listOfSabha[11])
}

@Composable
fun Activities(){
  Text("Activities")
}


@Composable
@Preview
fun JoinUsScreen() {
  Text("नमस्ते जी,\n" +
      "आप निर्मात्री सभा द्वारा आयोजित दो दिवसीय लघु गुरुकुल पाठ्यक्रम पूर्ण कर आर्य महासंघ से जुड़ सकते है। \n" +
      "\n" +
      "निचे आप अपना क्षेत्र चुनकर आपके क्षेत्रों में आयोजित होने वाले सत्रों के विवरण देख सकते है। ")
}

@Composable
fun LearningScreen() {
  Text("यह पृष्ठ निर्माणाधीन है")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Orgs(navController: NavHostController, onNavigateToOrgDetails: (String) -> Unit) {
  Column(
    modifier = Modifier.padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
      Text(
        text = "आर्य महासंघ का वर्तमान स्वरुप",
        fontWeight = FontWeight.Bold
      )
    }
    FlowRow(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      listOfSabha.take(10).forEach {
        OrgThumbnail(it.name, it.logo){
          onNavigateToOrgDetails(it.name)
          navController.navigate(Screen.OrgDetails(it.name))
        }
      }
    }
  }
}

@Composable
fun OrgDetailScreen(name: String, navController: NavHostController){
  Organisation(listOfSabha.find { it.name == name }!!)
}

@Composable
fun ContactUs() {
  Text(" नमस्ते जी,\n" +
      "आप हमें aaryamahasangh@gmail.com ईमेल पर संपर्क कर सकते है। ")
}

@Composable
@Preview
fun DrawerContentPreview() {
  //DrawerContent(rememberDrawerState(initialValue = DrawerValue.Open), drawerOptions[0].title, {}, navController1)
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
  println("route: $route")
  return ""
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
  LaunchedEffect(selectedOption) {
    selectedOrgDetails("")
  }
  Scaffold(
    topBar = {
      val currentRoute = navController1.currentDestination?.route
      val screenTitle = getScreenTitle(currentRoute)
      TopAppBar(
        title = {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(if(screenTitle.isNotEmpty()) screenTitle else "॥ ओ३म् ॥")
          }
        },
        navigationIcon = {
          if(orgDetails.isNotEmpty()) {
            IconButton(onClick = {
              selectedOrgDetails("")
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
          .verticalScroll(rememberScrollState())
          .padding(paddingValues)
      ) {
        println("Selected Option: $selectedOption")
        //Organisation(listOfSabha[11])
        RootNavGraph(
          navController = navController1,
          onNavigateToOrgDetails = { orgId ->
            selectedOrgDetails(orgId)
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
