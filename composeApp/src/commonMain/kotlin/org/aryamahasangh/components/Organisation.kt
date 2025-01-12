package org.aryamahasangh.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.*
import coil3.compose.AsyncImage
import org.aryamahasangh.Sabha
import org.aryamahasangh.listOfSabha
import org.jetbrains.compose.resources.painterResource

fun drawableFromImageName(imageName: String) = when(imageName){
  "nirmatri_sabha" -> Res.drawable.nirmatri_sabha
  "ary_gurukul_mahavidyalaya" -> Res.drawable.ary_gurukul_mahavidyalaya
  "arya_gurukul_mahavidyalaya" -> Res.drawable.arya_gurukul_mahavidyalaya
  "chatra_sabha" -> Res.drawable.chatra_sabha
  "kshatriya_sabha" -> Res.drawable.kshatriya_sabha
  "sanrakshini_sabha" -> Res.drawable.sanrakshini_sabha
  "sanvardhini_sabha" -> Res.drawable.nirmatri_sabha
  "dalitoddharini_sabha" -> Res.drawable.dalitoddharini_sabha
  "arya_parishad" -> Res.drawable.arya_parishad
  "vanprasth_ayog" -> Res.drawable.vanprasth_ayog
  "sanchar_parishad" -> Res.drawable.sanchar_parishad
  else -> Res.drawable.mahasangh_logo_without_background
}

@Composable
@Preview
fun SabhaPreview(){
  Column(modifier = Modifier
    .verticalScroll(rememberScrollState())
  ) {
    Organisation(listOfSabha[11])
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Organisation(sabha: Sabha){
  val (name, logo, description, keyPeople, campaigns ) = sabha
  Column {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Image(painter = painterResource(resource = drawableFromImageName(logo)), contentDescription = "logo $logo", modifier = Modifier.size(150.dp))
      Text(name, style = MaterialTheme.typography.headlineMedium)
      Text(description)
    }

    if(keyPeople.isNotEmpty()){
      val sortedPeople = keyPeople.sortedBy { it.priority }
      Column() {
        Text("कार्यकारिणी/पदाधिकारी",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
        )
      }
      FlowRow {
        sortedPeople.forEach {
          Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
              model = it.member.profileImage ?: "",
              contentDescription = "profile image ${it.member.name}",
              contentScale = ContentScale.Crop,
              modifier = Modifier.clip(CircleShape).size(96.dp),
              placeholder = BrushPainter(
                Brush.linearGradient(
                  listOf(
                    Color(color = 0xFFFFFFFF),
                    Color(color = 0xFFDDDDDD),
                  )
                )
              ),
            )
            Column(modifier = Modifier.padding(16.dp, 16.dp)) {
              Text(it.member.name, style = MaterialTheme.typography.bodyLarge)
              Text(it.post)
            }
          }
        }
      }
    }
    if(campaigns.isNotEmpty()){

    }
  }
}