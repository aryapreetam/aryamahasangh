package org.aryamahasangh.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.network.apolloClient
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AboutUs(showDetailedAboutUs: () -> Unit) {
  var organisation by remember { mutableStateOf<OrganisationQuery.Organisation?>(null) }

  LaunchedEffect(Unit) {
    val res = apolloClient.query(OrganisationQuery(name = "आर्य महासंघ")).execute()
    organisation = res.data?.organisation
  }

  Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ){
      Image(
        painter = painterResource(resource = Res.drawable.mahasangh_logo_without_background),
        contentDescription = "logo आर्य महासंघ",
        modifier = Modifier.size(128.dp)
      )
    }
    Text(
      modifier = Modifier.clickable {
        showDetailedAboutUs()
      },
      text = "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भूमण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।"
    )

  }
}

@Preview
@Composable
fun AboutUsIntro() {
  AboutUs(showDetailedAboutUs = {})
}


