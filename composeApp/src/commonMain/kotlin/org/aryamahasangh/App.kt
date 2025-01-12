package org.aryamahasangh

import AppTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import kotlinx.coroutines.launch
import org.aryamahasangh.components.Organisation
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.painterResource

@Composable
@Preview
fun App() {
  DevelopmentEntryPoint {
    AppTheme {
      Organisation(listOfSabha[11])
//      BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
//        val isLargeScreen = maxWidth > 800.dp
//        if (isLargeScreen) {
//          Box(modifier = Modifier.width(1024.dp)){
//            LargeScreens("Arya Mahasangh", drawerState)
//          }
//        } else {
//          SmallScreens("Arya Mahasangh", drawerState)
//        }
//      }
    }
  }
}

const val PLATFORM_WEB = "Web with Kotlin/Wasm"

@Composable
fun DrawerContent(drawerState: DrawerState) {
  val scope = rememberCoroutineScope()
  val drawerOptions = listOf("हमारे बारे मे", "गतिविधियां", "हमसें जुडें", "संलग्न संस्थाएं", "स्वाध्याय", "हमसे संपर्क करें")
  var selectedOption by remember { mutableStateOf(drawerOptions[0]) }
  Column(modifier = Modifier.width(240.dp).padding(16.dp).background(color = Color.LightGray)) {
    Text("आर्य महासंघ", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    drawerOptions.forEach { option ->
      Text(
        text = option,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .clickable {
            selectedOption = option
            scope.launch { drawerState.close() }
          }
      )
    }
  }
}

@Composable
fun LargeScreens(title: String, drawerState: DrawerState){
  PermanentNavigationDrawer(
    drawerContent = {
      PermanentDrawerSheet {
        DrawerContent(drawerState)
      }
    },
    content = {
      MainContent(title, drawerState, "")
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallScreens(title: String, drawerState: DrawerState) {
  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet {
        DrawerContent(drawerState)
      }
    },
    content = {
      MainContent(title, drawerState, "")
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(title: String, drawerState: DrawerState, selectedOption: String) {
  val scope = rememberCoroutineScope()
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("$title ॥ ओ३म् ॥")
          }
        },
        navigationIcon = {
          IconButton(onClick = { scope.launch { drawerState.open() } }) {
            Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
          }
        }
      )
    },
    content = { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentAlignment = Alignment.Center
      ) {
        Text("Selected Option: $selectedOption")
      }
    },
    bottomBar = {
      if (getPlatform().name.equals(PLATFORM_WEB) || getPlatform().name.startsWith("Java")) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("जय आर्य - जय आर्यावर्त")
            Text("© आर्य महासंघ")
          }
        }
      }
    }
  )
}

@Composable
fun AboutUs(){
  Column(
    Modifier.fillMaxSize(1.0f).padding(24.dp).verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "॥ ओ३म् ॥",
      letterSpacing = 0.sp,
      color = Color.Red,
      modifier = Modifier.padding(PaddingValues(0.dp, 0.dp, 0.dp, 24.dp))
    )
    Column(
      Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(Res.drawable.mahasangh_logo_without_background),
        contentDescription = "arya mahasangh",
        modifier = Modifier.width(250.dp).padding(16.dp)
      )
      Text(
        "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भूमण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।\n" +
            "\n" +
            "आचार्य हनुमत प्रसाद\n" +
            "अध्यक्ष, आर्य महासंघ",
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center
      )
    }
  }
}

