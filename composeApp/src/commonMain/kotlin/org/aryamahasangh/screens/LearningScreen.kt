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
import coil3.compose.AsyncImage
import org.aryamahasangh.LearningsQuery
import org.aryamahasangh.LearningsQuery.Learning
import org.aryamahasangh.network.apolloClient

@Composable
fun LearningScreen() {
  val learnings = remember { mutableStateOf(emptyList<Learning>()) } // ✅ Correct
  LaunchedEffect(Unit) {
    val res = apolloClient.query(LearningsQuery()).execute()
    learnings.value = res.data?.learning!!
  }
  Column(modifier = Modifier.padding(start =16.dp, end = 16.dp, bottom = 16.dp).fillMaxSize(1f).verticalScroll(rememberScrollState())) {
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
          val modifier = Modifier.clickable {  }
          Column(modifier = if(isSmallScreen) modifier.fillMaxWidth() else modifier.width(240.dp)) {
            AsyncImage(
              modifier = Modifier.aspectRatio(16f / 9f),
              model = video.thumbnailUrl,
              contentDescription = video.description,
            )
            Text(video.title, modifier = Modifier.padding(top = 4.dp),)
          }
        }
      }
    }
  }
}
