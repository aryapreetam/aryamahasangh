package org.aryamahasangh.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.error_profile_image
import coil3.compose.AsyncImage
import org.aryamahasangh.OrganisationalActivityDetailQuery
import org.aryamahasangh.OrganisationalActivityDetailQuery.ContactPeople
import org.aryamahasangh.OrganisationalActivityDetailQuery.OrganisationalActivity
import org.aryamahasangh.components.activityTypeData
import org.aryamahasangh.network.apolloClient
import org.aryamahasangh.type.ActivityType
import org.aryamahasangh.utils.format
import org.aryamahasangh.utils.formatDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ActivityDetailScreen(id: String) {
  val activity = remember { mutableStateOf<OrganisationalActivityDetailQuery.OrganisationalActivity?>(null) }
  LaunchedEffect(Unit) {
    val res = apolloClient.query(OrganisationalActivityDetailQuery(id)).execute()
    activity.value = res.data?.organisationalActivity
  }

  if(activity.value == null) {
    return
  }

  val data = activity.value!!

  ActivityDisplay(data)
}

@Preview
@Composable
fun ActivityDisplayPreview(){
  val activity = OrganisationalActivityDetailQuery.OrganisationalActivity(
    id = "",
    name =  "नियमित संध्या अनुष्ठान अभियान",
    description = "ईश के ज्ञान से लोक में जांच के आर्य कार्य आगे बढ़ाते रहें। नित्य है ना मिटे ना हटे ले चले प्रार्थना प्रेम से भाव लाते रहें।",
        associatedOrganisation =  listOf(
        "राष्ट्रीय आर्य निर्मात्री सभा"
        ),
        activityType = ActivityType.EVENT,
        place =  "रोहतक",
        startDateTime = "2025-02-25T09:04:42.006965",
        endDateTime = "2025-05-15T09:04:42.006965",
        mediaFiles = listOf(
        "https://images.pexels.com/photos/209831/pexels-photo-209831.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150",
        "https://images.pexels.com/photos/1402787/pexels-photo-1402787.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150"
        ),
        additionalInstructions = "शान्तिपाठ + जयघोष अभिवादन + प्रसाद वितरण।\nसभी आर्यसमाज पदाधिकारी अवश्य पहुंचें और संगठित स्वरूप को प्रकाशित करें !!",
        contactPeople = listOf(
          ContactPeople(
            member = OrganisationalActivityDetailQuery.Member(
              name = "आचार्य संजीव आर्य",
              profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
              phoneNumber = "9045353309"
            ),
            post = "अध्यक्ष",
            priority = 1
          )
        )
  )
  Surface(Modifier.background(Color.White)){
    ActivityDisplay(activity)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityDisplay(activity: OrganisationalActivity) {
  //Profile Image URLS
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // Name and Activity Type
    Column() {
      Text(
        text = activity.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        modifier = Modifier.background(Color.LightGray).padding(vertical = 4.dp, horizontal = 16.dp),
        text = "${activityTypeData[activity.activityType]}", style = MaterialTheme.typography.bodyLarge)
    }

    // Description
    Text(
      text = activity.description,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(vertical = 8.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Associated Organisations
    Text(
      text = "संबधित संस्थाएँ:",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    FlowRow(horizontalArrangement =  Arrangement.spacedBy(4.dp),
      verticalArrangement =  Arrangement.spacedBy(4.dp)) {
      activity.associatedOrganisation.forEach { associatedOrg ->
        AssistChip(
          onClick = { },
          label = { Text(associatedOrg) }
        )
      }
    }


    // Place
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 8.dp)
    ) {
      Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Place", tint = Color.Gray)
      Spacer(modifier = Modifier.width(4.dp))
      Text(text = "स्थान: ${activity.place}", style = MaterialTheme.typography.bodyMedium)
    }

    // Start and End Date/Time
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 4.dp)
    ) {
      Icon(imageVector = Icons.Default.DateRange, contentDescription = "Start Date", tint = Color.Gray)
      Spacer(modifier = Modifier.width(4.dp))
      Text(text = "प्रारंभ: ${formatDateTime(activity.startDateTime)}", style = MaterialTheme.typography.bodyMedium)
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      Icon(imageVector = Icons.Default.DateRange, contentDescription = "End Date", tint = Color.Gray)
      Spacer(modifier = Modifier.width(4.dp))
      Text(text = "समाप्ति: ${formatDateTime(activity.endDateTime)}", style = MaterialTheme.typography.bodyMedium)
    }

    // Media Files
    if (activity.mediaFiles.isNotEmpty()) {
      LazyRow(
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(activity.mediaFiles) { imageUrl ->
          AsyncImage(
            model = imageUrl,
            contentDescription = "Thumbnail ",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(150.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    // Contact People
    Text(
      text = "संपर्क सूत्र:",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )

    val sortedContactPeople = activity.contactPeople.sortedBy { it.priority }
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      sortedContactPeople.forEach { contactPerson ->
        ContactPersonItem(contactPerson = contactPerson)
      }
    }


    // Additional Instructions
    if (activity.additionalInstructions.isNotEmpty()) {
      Text(
        text = "अतिरिक्त निर्देश:",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
      )

      val instructions = activity.additionalInstructions.split("\n")
      Column {
        instructions.forEach { instruction ->
          Row(verticalAlignment = Alignment.Top) {
            Text(text = "• ", modifier = Modifier.padding(end = 4.dp))
            Text(text = instruction, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

@Preview
@Composable
fun ContactPersonPreview() {
  val contact = ContactPeople(
    member = OrganisationalActivityDetailQuery.Member(
      name = "आचार्य संजीव आर्य",
      profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
      phoneNumber = "9045353309"
    ),
    post = "अध्यक्ष",
    priority = 1
  )
  ContactPersonItem(contact)
}

@Composable
fun ContactPersonItem(contactPerson: ContactPeople) {
  Row(
    modifier = Modifier.heightIn(48.dp, 65.dp).padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Profile Image
    AsyncImage(
      model = contactPerson.member.profileImage,
      contentDescription = "Profile Image",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(50.dp)
        .clip(CircleShape),
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

    Spacer(modifier = Modifier.width(8.dp))

    Column {
        Text(
          text = contactPerson.member.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = contactPerson.post,
          style = MaterialTheme.typography.bodySmall
        )
    }
    VerticalDivider(
      modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 12.dp),
      thickness = 2.dp,
      color = Color.LightGray
    )
    IconButton(
      onClick = {  }
    ){
      Icon(
        imageVector = Icons.Default.Call,
        contentDescription = "Call", tint = Color.Gray
      )
    }
  }
}

//Helper Functions

fun formatDateTime(dateTimeString: Any): String {
  return format(dateTimeString)
}

//@Preview
//@Composable
//fun PreviewLeadershipWorkshopScreen() {
//  MaterialTheme { // Ensure MaterialTheme is provided in preview
//    LeadershipWorkshopScreen(data)
//  }
//}
