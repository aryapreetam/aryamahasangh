@file:OptIn(ExperimentalLayoutApi::class)

package org.aryamahasangh.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import org.aryamahasangh.LearningsItemsQuery
import org.aryamahasangh.LearningsItemsQuery.LearningItem
import org.aryamahasangh.navigation.Screen
import org.aryamahasangh.network.apolloClient

@Composable
fun LearningScreen(navController: NavHostController, onNavigateToActivityDetails: (String) -> Unit) {
  val learnings = remember { mutableStateOf(emptyList<LearningItem>()) } // ✅ Correct
  LaunchedEffect(Unit) {
    val res = apolloClient.query(LearningsItemsQuery()).execute()
    learnings.value = res.data?.learningItems ?: emptyList()
  }
  Column(modifier = Modifier.padding(start =8.dp, end = 8.dp, bottom = 8.dp).fillMaxSize(1f).verticalScroll(rememberScrollState())) {
    Text("सत्यार्थ प्रकाश",
      style =  MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(vertical = 8.dp),
      fontWeight = FontWeight.Bold
    )
    BoxWithConstraints {
      val isSmallScreen = maxWidth <= 460.dp
      FlowRow(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        learnings.value.forEach { video ->
          val modifier = Modifier.clickable {
            onNavigateToActivityDetails(video.id)
            navController.navigate(Screen.VideoDetails(video.id))
          }
          Column(modifier = if(isSmallScreen) modifier.fillMaxWidth() else modifier.width(240.dp)) {
            AsyncImage(
              modifier = Modifier.aspectRatio(16f / 9f),
              model = video.thumbnailUrl,
              contentDescription = video.title,
            )
            Text(video.title, modifier = Modifier.padding(top = 4.dp),)
          }
        }
      }
    }
  }
}
