@file:OptIn(ExperimentalLayoutApi::class)

package com.aryamahasangh.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.navigation.Screen
import com.aryamahasangh.utils.TBD
import com.aryamahasangh.viewmodel.LearningViewModel
import org.koin.compose.koinInject

@Composable
fun LearningScreen(
  navController: NavHostController,
  onNavigateToActivityDetails: (String) -> Unit,
  viewModel: LearningViewModel = koinInject()
) {
  // FIXME
  if (true) {
    Column(Modifier.padding(8.dp)) {
      listOf("आर्ष कर्मविधि").forEach { name ->
        Text(
          text = name,
          style = MaterialTheme.typography.headlineSmall,
          modifier = Modifier.padding(vertical = 8.dp)
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = 16.dp)
        ) {
          listOf("प्रातःकालीन मंत्र", "उपासना", "संध्या", "अग्निहोत्र", "भोजन मन्त्र", "शयन मंत्र", "वैदिक राष्ट्रीय प्रार्थना", "स्वराज्य यज्ञ", "नवसंवत्सर यज्ञ", "वासंती नवसस्येष्टि (होली)", "शारदीय नवसस्येष्टि (दीपावली)").forEach {
            OutlinedCard(
              onClick = {}
            ) {
              Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp)
              )
            }
          }
        }
      }
    }
    return
  }

  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current

  // Collect UI state from ViewModel
  val uiState by viewModel.uiState.collectAsState()

  // Handle loading state
  if (uiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      LinearProgressIndicator()
    }
    return
  }

  // Handle error state
  uiState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Retry"
      )
    }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Failed to load learning items")
        Button(onClick = { viewModel.loadLearningItems() }) {
          Text("Retry")
        }
      }
    }
    return
  }

  // Handle empty state
  if (uiState.learningItems.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("No learning items available")
    }
    return
  }

  Column(
    modifier =
      Modifier.padding(
        start = 8.dp,
        end = 8.dp,
        bottom = 8.dp
      ).fillMaxSize(1f).verticalScroll(rememberScrollState())
  ) {
    Text(
      "सत्यार्थ प्रकाश",
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.padding(vertical = 8.dp),
      fontWeight = FontWeight.Bold
    )
    BoxWithConstraints {
      val isSmallScreen = maxWidth <= 460.dp
      FlowRow(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        uiState.learningItems.forEach { video ->
          val modifier =
            Modifier.clickable {
              onNavigateToActivityDetails(video.id)
              navController.navigate(Screen.VideoDetails(video.id))
            }
          Column(modifier = if (isSmallScreen) modifier.fillMaxWidth() else modifier.width(240.dp)) {
            AsyncImage(
              modifier = Modifier.aspectRatio(16f / 9f),
              model = video.thumbnailUrl,
              contentDescription = video.title
            )
            Text(video.title, modifier = Modifier.padding(top = 4.dp))
          }
        }
      }
    }
  }
}
