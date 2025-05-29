package org.aryamahasangh.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.error_profile_image
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import coil3.compose.AsyncImage
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.viewmodel.AboutUsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DetailedAboutUs(viewModel: AboutUsViewModel) {
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
        modifier = Modifier.size(150.dp)
      )
      Text("आर्य महासंघ", style = MaterialTheme.typography.headlineMedium)
    }
    Text(
      "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भूमण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।"
    )
    Row(modifier = Modifier.padding(top = 16.dp)) {
      AsyncImage(
        model = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_hanumat_prasad.webp",
        contentDescription = "profile image आचार्य हनुमत प्रसाद",
        contentScale = ContentScale.Crop,
        modifier = Modifier.clip(CircleShape).size(80.dp),
        placeholder =
          BrushPainter(
            Brush.linearGradient(
              listOf(
                Color(color = 0xFFFFFFFF),
                Color(color = 0xFFDDDDDD)
              )
            )
          ),
        fallback = painterResource(Res.drawable.error_profile_image),
        error = painterResource(Res.drawable.error_profile_image)
      )
      Column(modifier = Modifier.padding(12.dp, 8.dp)) {
        Text("आचार्य हनुमत प्रसाद", style = MaterialTheme.typography.bodyLarge)
        Text("अध्यक्ष, आर्य महासंघ")
      }
    }
  }
}

@Preview
@Composable
fun PreviewDetailedAboutUs() {
  // This is just a preview, so we don't need a real ViewModel
  // In a real app, we would inject the ViewModel
  // DetailedAboutUs(viewModel = AboutUsViewModel())
}
