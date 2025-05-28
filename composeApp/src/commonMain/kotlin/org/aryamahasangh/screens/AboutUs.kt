package org.aryamahasangh.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.viewmodel.AboutUsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AboutUs(
  showDetailedAboutUs: () -> Unit,
  viewModel: AboutUsViewModel
) {
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
      ).run {
        when (this) {
          SnackbarResult.Dismissed -> {
            println("Dismissed")
          }
          SnackbarResult.ActionPerformed -> viewModel.loadOrganisationDetails("आर्य महासंघ")
        }
      }
    }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Failed to load about us information")
        Button(onClick = { viewModel.loadOrganisationDetails("आर्य महासंघ") }) {
          Text("Retry")
        }
      }
    }
    return
  }

  Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(resource = Res.drawable.mahasangh_logo_without_background),
        contentDescription = "logo आर्य महासंघ",
        modifier = Modifier.size(128.dp)
      )
    }
    Text(
      modifier =
        Modifier.clickable {
          showDetailedAboutUs()
        },
      text = "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भूमण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।"
    )
  }
}

@Preview
@Composable
fun AboutUsIntro() {
  // This is just a preview, so we don't need a real ViewModel
  // In a real app, we would inject the ViewModel
  // AboutUs(showDetailedAboutUs = {}, viewModel = AboutUsViewModel())
}
