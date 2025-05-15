package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.YoutubeVideoPlayer
import org.aryamahasangh.viewmodel.LearningViewModel

@Composable
fun VideoDetailsScreen(learningItemId: String, viewModel: LearningViewModel) {
  val scope = rememberCoroutineScope()
  val snackbarHostState = LocalSnackbarHostState.current
  
  // Load learning item details
  LaunchedEffect(learningItemId) {
    viewModel.loadLearningItemDetail(learningItemId)
  }
  
  // Collect UI state from ViewModel
  val uiState by viewModel.videoPlayerUiState.collectAsState()
  
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
        Text("Failed to load video details")
        Button(onClick = { viewModel.loadLearningItemDetail(learningItemId) }) {
          Text("Retry")
        }
      }
    }
    return
  }
  
  // Handle null learning item
  if (uiState.learningItem == null) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("Video not found")
    }
    return
  }
  
  val detail = uiState.learningItem!!
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    YoutubeVideoPlayer(detail.videoId)
    Text(
      text = detail.title,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text("ईश्वर के नाम की व्याख्या भाग 1: " +
        "प्रस्तुत श्रव्यकणिका में आचार्य जी ने सत्यार्थ प्रकाश के प्रथम समुल्लास में वर्णित ईश्वर के नामों, जैसे वरुण, अर्यमा, इंद्र, वृहस्पति इत्यादि की व्याख्या अत्यंत सरल शब्दों में की है। \n" +
        "वैसे तो प्रथम समुल्लास सत्यार्थ प्रकाश में सबसे कठिन माना जाता है पर आचार्य जी ने इसे विविध उदहारण देकर रुचिपूर्वक बनाया है।")
  }
}