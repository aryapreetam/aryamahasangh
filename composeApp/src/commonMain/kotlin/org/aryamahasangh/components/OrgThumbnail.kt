package org.aryamahasangh.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

@Composable
@Preview
fun OrgThumbnailPreview(){
  OrgThumbnail("राष्ट्रीय आर्य संवर्धिनी सभा", "sanvardhini_sabha", {})
}

@Composable
fun OrgThumbnail(name: String, imageUrl: String, navigateToOrgDetails: () -> Unit) {
  Column(
    modifier = Modifier.clickable {
      navigateToOrgDetails()
    },
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally) {
    Image(
      painter = painterResource(drawableFromImageName(imageUrl)),
      contentDescription = "logo for $name",
      modifier = Modifier.size(180.dp),
      contentScale = ContentScale.Crop
    )
    Text(name)
  }
}