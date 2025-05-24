package org.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.baseline_groups
import aryamahasangh.composeapp.generated.resources.error_profile_image
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import coil3.compose.AsyncImage
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.SettingKeys
import org.aryamahasangh.network.bucket
import org.aryamahasangh.screens.EditImageButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

fun drawableFromImageName(imageName: String) = when(imageName){
//  "sanchar_parishad" -> Res.drawable.sanchar_parishad
  else -> Res.drawable.mahasangh_logo_without_background
}

@Composable
@Preview
fun SabhaPreview(){
  Column(modifier = Modifier
    .verticalScroll(rememberScrollState())
  ) {
    //OrganisationDetail(listOfOrganisations[11])
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrganisationDetail(organisation: OrganisationQuery.Organisation, updateOrganisationLogo: (String, String, String) -> Unit){
  val (id, name, logo, description, keyPeople ) = organisation
  var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
  Column(modifier = Modifier.fillMaxSize().padding(8.dp)
    .verticalScroll(rememberScrollState())) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
      ){
        if(isLoggedIn){
          Row(modifier = Modifier.padding()){
            val scope = rememberCoroutineScope()
            val launcher = rememberFilePickerLauncher(
              type = PickerType.Image,
              mode = PickerMode.Single,
              title = "Select logo",
            ) { file ->
              if(file != null) {
                scope.launch {
                  try {
                    val uploadResponse = bucket.upload(
                      path = "${name}_logo_${Clock.System.now().epochSeconds}.jpg",
                      data = file.readBytes()
                    )
                    val imageUrl = bucket.publicUrl(uploadResponse.path)
                    updateOrganisationLogo(id, name, imageUrl)
                  }catch (e: Exception){
                    println("error uploading files: $e")
                  }
                }
              }
            }

            AsyncImage(
              model = logo,
              contentDescription = "logo for $name",
              contentScale = ContentScale.Fit,
              modifier = Modifier.size(150.dp),
              placeholder = BrushPainter(
                Brush.linearGradient(
                  listOf(
                    Color(color = 0xFFFFFFFF),
                    Color(color = 0xFFDDDDDD),
                  )
                )
              ),
              fallback = painterResource(Res.drawable.baseline_groups),
              error = painterResource(Res.drawable.baseline_groups)
            )
            EditImageButton {
              launcher.launch()
            }
          }
        }else{
          AsyncImage(
            model = logo,
            contentDescription = "logo for $name",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(150.dp),
            placeholder = BrushPainter(
              Brush.linearGradient(
                listOf(
                  Color(color = 0xFFFFFFFF),
                  Color(color = 0xFFDDDDDD),
                )
              )
            ),
            fallback = painterResource(Res.drawable.baseline_groups),
            error = painterResource(Res.drawable.baseline_groups)
          )
        }
        Text(name, style = MaterialTheme.typography.headlineMedium)
      }
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
              modifier = Modifier.clip(CircleShape).size(80.dp),
              placeholder = BrushPainter(
                Brush.linearGradient(
                  listOf(
                    Color(color = 0xFFFFFFFF),
                    Color(color = 0xFFDDDDDD),
                  )
                )
              ),
              fallback = painterResource(Res.drawable.error_profile_image),
              error = painterResource(Res.drawable.error_profile_image)
            )
            Column(modifier = Modifier.padding(12.dp, 8.dp)) {
              Text(it.member.name, style = MaterialTheme.typography.bodyLarge)
              Text(it.post)
            }
          }
        }
      }
    }
  }
}