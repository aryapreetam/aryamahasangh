package org.aryamahasangh

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun App() {
  MaterialTheme {
    Column(
      Modifier.fillMaxSize(1.0f).padding(24.dp).verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text("Hello ${getPlatform().name}")
//      Text(
//        text = "|| ओ३म् ||",
//        letterSpacing = 0.sp,
//        fontWeight = FontWeight.ExtraBold,
//        fontFamily = ManFamily(),
//        color = Color.Red,
//        modifier = Modifier.padding(PaddingValues(0.dp,0.dp,0.dp, 24.dp))
//      )
//      Button(
//        onClick = {},
//      ){
//        Text(text = "|| ओ३म् ||", fontFamily = ManFamily())
//      }
//      Column(
//        Modifier.fillMaxWidth(),
//        horizontalAlignment = Alignment.CenterHorizontally
//      ) {
//        Image(
//          painter = painterResource(Res.drawable.mahasangh_logo_without_background),
//          contentDescription = "arya mahasangh",
//          modifier = Modifier.width(250.dp).padding(16.dp)
//        )
//        Text(
//          "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भूमण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।\n" +
//              "\n" +
//              "आचार्य हनुमत प्रसाद\n" +
//              "अध्यक्ष, आर्य महासंघ",
//          fontWeight = FontWeight.SemiBold,
//          letterSpacing = 0.sp,
//          fontFamily = ManFamily(),
//          textAlign = TextAlign.Center
//        )
//      }
    }
  }
}

//@OptIn(ExperimentalResourceApi::class)
//@Composable
//fun ManFamily() = FontFamily(Font(Res.font.noto_sans_variable))
