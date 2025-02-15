package org.aryamahasangh.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.aryamahasangh.navigation.Screens.PostDetail

@Serializable
sealed class Screens() {
  @Serializable
  data object Home : Screens()
  @Serializable
  data object Posts : Screens()
  @Serializable
  data class PostDetail(val postId: String) : Screens()
  @Serializable
  data object ContactUs : Screens()
}

@Composable
fun Appa(navController: NavHostController) {
  NavHost(navController, startDestination = Screens.Home) {
    composable<Screens.Home> { HomeScreen(navController) }
    composable<Screens.Posts> { PostsScreen(navController) }
    composable<PostDetail> {
      val id = it.toRoute<PostDetail>().postId
      PostDetailScreen(navController, id)
    }
    composable<Screens.ContactUs> { ContactUsScreen(navController) }
  }
}

@Composable
fun HomeScreen(navController: NavController) {
  Text("Home Screen")
}

@Composable
fun ContactUsScreen(navController: NavController) {
  Text("Contact us Screen")
}


@Composable
fun PostsScreen(navController: NavController) {
  Column {
    Text("Posts Screen")
    for(i in 1..5) {
      Button(onClick = { navController.navigate(PostDetail("$i")) }) {
        Text("Go to Post $i Detail")
      }
    }
  }
}

@Composable
fun PostDetailScreen(navController: NavController, postId: String) {
  Column {
    Text("Post Detail Screen for Post $postId")
    Button(onClick = { navController.popBackStack() }) {
      Text("Back to Posts")
    }
  }
}


@Composable
fun DrawerContent(navController: NavController) {
  Column {
    DrawerItem("Home", Screens.Home, navController)
    DrawerItem("Posts", Screens.Posts, navController)
    DrawerItem("Contact Us", Screens.ContactUs, navController)
  }
}

@Composable
fun DrawerItem(text: String, route: Screens, navController: NavController) {
  TextButton(onClick = {
    navController.navigate(route) {
      popUpTo(route)
      launchSingleTop = true
    }
  }) {
    Text(text = text)
  }
}

@Composable
fun MainScreen() {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val navController = rememberNavController()
  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet {
        DrawerContent(navController)
      }
    }
  ) {
    Appa(navController)
  }
}

