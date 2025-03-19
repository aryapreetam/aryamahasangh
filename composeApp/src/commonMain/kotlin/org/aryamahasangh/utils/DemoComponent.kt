package org.aryamahasangh.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.aryamahasangh.OrganisationalActivityDetailQuery.ContactPeople
import org.aryamahasangh.screens.JoinUsScreen
import kotlin.random.Random

@Composable
fun DemoComposable(){
  //OrganisationActivityForm()
  //RegistrationForm()
  //JoinUsComponent()
}

@Composable
fun JoinUsComponent(){
  JoinUsScreen()
}

//@Composable
//fun AddOrganisationInputForm(){
//    val sampleOrganisation = Organisation(
//    name = "राष्ट्रीय आर्य निर्मात्री सभा",
//    description = "प्रत्येक बुद्धिमान व्यक्ति यह समझ सकता है कि मानवीय जीवन अति दुर्लभ है...",
//    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//nirmatri_sabha.webp",
//    keyPeople = listOf(
//      KeyPeople(
//        member = Member(
//          name = "आचार्य जितेन्द्र आर्य",
//          email = "",
//          phoneNumber = "9416201731",
//          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp",
//          educationalQualification = ""
//        ),
//        post = "अध्यक्ष",
//        priority = 1
//      )
//    )
//  )
//  OrganisationForm(organisation = sampleOrganisation)
//}

//@Composable
//fun OrganisationActivityForm(){
//  val sampleActivity = OrganisationalActivity(
//    id = "eb179afc-aec7-4c87-8e51-9a9f192d4551",
//    name = "Health Awareness Drive",
//    description = "Campaign for spreading awareness about health and hygiene.",
//    associatedOrganisation = listOf(
//      "385933f4-2eca-46bb-bb31-50277d08d873",
//      "36f4e51f-1517-4f1e-8395-d1d83b7c76ea"
//    ),
//    activityType = ActivityType.CAMPAIGN,
//    place = "Bilāspur",
//    startDateTime = "2025-02-20T15:38:26.337446",
//    endDateTime = "2025-04-08T15:38:26.337446",
//    mediaFiles = emptyList(),
//    additionalInstructions = "Please follow COVID-19 protocols.",
//    contactPeople = listOf(
//      ContactPeople(
//        member = Member(
//          name = "Member 13",
//          profileImage = "https://example.com/profile32.jpg",
//          phoneNumber = "+12345678384"
//        ),
//        post = "Manager",
//        priority = 4
//      )
//    )
//  )
//}

val profileImagesList = listOf(
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_mahesh.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/dr_mahesh_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_pravesh_ji.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_varchaspati.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/anil_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_ashvani.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_indra.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_suman.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/acharya_indra.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_loknath.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_vedprakash.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/arya_shivnarayan.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/anil_arya.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_hanumat_prasad.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_satish.webp",
"https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/upachary_jasbir_arya.webp",
)



//Data Classes

//@Composable
//fun ActivityDisplay(activity: OrganisationalActivity) {
//  //Organisation Mapping
//  val organisationList = listOf(
//    "राष्ट्रीय आर्य निर्मात्री सभा",
//    "राष्ट्रीय आर्य क्षत्रिय सभा",
//    "राष्ट्रीय आर्य संरक्षिणी सभा",
//    "राष्ट्रीय आर्य संवर्धिनी सभा",
//    "राष्ट्रीय आर्य दलितोद्धारिणी सभा",
//    "आर्य गुरुकुल महाविद्यालय",
//    "आर्या गुरुकुल महाविद्यालय",
//    "आर्या परिषद्",
//    "वानप्रस्थ आयोग",
//    "राष्ट्रीय आर्य छात्र सभा",
//    "राष्ट्रीय आर्य संचार परिषद",
//    "आर्य महासंघ"
//  )
//
//  //Profile Image URLS
//  val profileImageList = profileImagesList
//
//  // Mock Nature-Themed Thumbnail URLs (Replace with actual URLs)
//  val natureImageUrls = remember {
//    listOf(
//      "https://images.pexels.com/photos/209831/pexels-photo-209831.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150",
//      "https://images.pexels.com/photos/106415/pexels-photo-106415.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150",
//      "https://images.pexels.com/photos/1624496/pexels-photo-1624496.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150",
//      "https://images.pexels.com/photos/1402787/pexels-photo-1402787.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150"
//    ).shuffled()
//  }
//  val randomImage = remember {
//    natureImageUrls.take(Random.nextInt(1, natureImageUrls.size)).toMutableList()
//  }
//
//  Column(
//    modifier = Modifier
//      .fillMaxSize()
//      .padding(16.dp)
//      .verticalScroll(rememberScrollState())
//  ) {
//    // Name and Activity Type
//    Column() {
//      Text(
//        text = activity.name,
//        style = MaterialTheme.typography.headlineSmall,
//        fontWeight = FontWeight.Bold
//      )
//      Spacer(modifier = Modifier.width(8.dp))
//      AssistChip(
//        onClick = { },
//        label = { Text(activity.activityType.toString()) }
//      )
//    }
//
//    // Description
//    Text(
//      text = activity.description,
//      style = MaterialTheme.typography.bodyLarge,
//      modifier = Modifier.padding(vertical = 8.dp)
//    )
//
//    // Associated Organisations
//    Text(
//      text = "Associated Organisations:",
//      style = MaterialTheme.typography.titleMedium,
//      fontWeight = FontWeight.Bold,
//      modifier = Modifier.padding(top = 16.dp)
//    )
//
//    val associatedOrgNames = activity.associatedOrganisation.mapNotNull { orgId ->
//      val index = activity.associatedOrganisation.indexOf(orgId)
//      organisationList.getOrNull(index) ?: "Unknown Organisation"
//    }
//
//    Text(text = associatedOrgNames.joinToString(", "))
//
//
//    // Place
//    Row(
//      verticalAlignment = Alignment.CenterVertically,
//      modifier = Modifier.padding(top = 8.dp)
//    ) {
//      Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Place", tint = Color.Gray)
//      Spacer(modifier = Modifier.width(4.dp))
//      Text(text = "Place: ${activity.place}", style = MaterialTheme.typography.bodyMedium)
//    }
//
//    // Start and End Date/Time
//    Row(
//      verticalAlignment = Alignment.CenterVertically,
//      modifier = Modifier.padding(top = 4.dp)
//    ) {
//      Icon(imageVector = Icons.Default.DateRange, contentDescription = "Start Date", tint = Color.Gray)
//      Spacer(modifier = Modifier.width(4.dp))
//      Text(text = "Start: ${formatDateTime(activity.startDateTime)}", style = MaterialTheme.typography.bodyMedium)
//    }
//
//    Row(
//      verticalAlignment = Alignment.CenterVertically,
//      modifier = Modifier.padding(bottom = 8.dp)
//    ) {
//      Icon(imageVector = Icons.Default.DateRange, contentDescription = "End Date", tint = Color.Gray)
//      Spacer(modifier = Modifier.width(4.dp))
//      Text(text = "End: ${formatDateTime(activity.endDateTime)}", style = MaterialTheme.typography.bodyMedium)
//    }
//
//    // Media Files
//    if (randomImage.isNotEmpty()) {
//      Text(
//        text = "Media Files",
//        style = MaterialTheme.typography.titleMedium,
//        fontWeight = FontWeight.Bold,
//        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
//      )
//      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//        items(randomImage) { imageUrl ->
//          AsyncImage(
//            model = imageUrl,
//            contentDescription = "Thumbnail ",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.size(150.dp)
//          )
//        }
//      }
//    }
//
//    // Contact People
//    Text(
//      text = "Contact People",
//      style = MaterialTheme.typography.titleMedium,
//      fontWeight = FontWeight.Bold,
//      modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
//    )
//
//    val sortedContactPeople = activity.contactPeople.sortedBy { it.priority }
//    sortedContactPeople.forEach { contactPerson ->
//      ContactPersonItem(contactPerson = contactPerson, profileImageList = profileImageList)
//    }
//
//    // Additional Instructions
//    if (activity.additionalInstructions.isNotEmpty()) {
//      Text(
//        text = "Additional Instructions",
//        style = MaterialTheme.typography.titleMedium,
//        fontWeight = FontWeight.Bold,
//        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
//      )
//
//      val instructions = activity.additionalInstructions.split("\n")
//      Column {
//        instructions.forEach { instruction ->
//          Row(verticalAlignment = Alignment.Top) {
//            Text(text = "• ", modifier = Modifier.padding(end = 4.dp))
//            Text(text = instruction, style = MaterialTheme.typography.bodyMedium)
//          }
//        }
//      }
//    }
//  }
//}

@Composable
fun ContactPersonItem(contactPerson: ContactPeople, profileImageList: List<String>) {
  val randomProfileImage = remember {
    profileImageList.getOrNull(Random.nextInt(0, profileImageList.size)) ?: ""
  }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 8.dp)
  ) {
    // Profile Image
    AsyncImage(
      model = randomProfileImage,
      contentDescription = "Profile Image",
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(50.dp)
        .clip(CircleShape)
    )

    Spacer(modifier = Modifier.width(8.dp))

    Column {
      // Name (Highlighted) and Call Icon
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = contactPerson.member.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = {
          val phoneNumber = contactPerson.member.phoneNumber
          try {
//            val encodedPhoneNumber = URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8.toString())
//            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
//              data = android.net.Uri.parse("tel:$encodedPhoneNumber")
//            }
//            context.startActivity(intent)

          } catch (e: Exception) {
            println("exception while dialing $e")
          }
        }) {
          Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = Color.Blue)
        }
      }

      // Phone Number
      Text(
        text = "Phone: ${contactPerson.member.phoneNumber}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 2.dp)
      )

      // Post
      Text(
        text = "Post: ${contactPerson.post}",
        style = MaterialTheme.typography.bodySmall
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
//fun PreviewActivityDisplay() {
//  val sampleActivity = OrganisationalActivity(
//    id = "327fdbe7-474f-4135-8f4f-2c107303a474",
//    name = "Health Awareness Drive",
//    description = "An event to develop leadership skills.",
//    associatedOrganisation = listOf(
//      "15ba9fd9-8b4d-460e-9c37-0d7baacf88a8",
//      "17ed7341-ae40-4ce8-97a5-456af73e35ec"
//    ),
//    activityType = ActivityType.EVENT,
//    place = "Ludhiāna",
//    startDateTime = "2025-02-27T12:47:01.629816",
//    endDateTime = "2025-06-11T12:47:01.629816",
//    mediaFiles = emptyList(),
//    additionalInstructions = "Please follow COVID-19 protocols.\nWear masks.\nMaintain social distancing.",
//    contactPeople = listOf(
//      ContactPeople(
//        member = Member(
//          name = "Member 22",
//          profileImage = "https://example.com/profile48.jpg",
//          phoneNumber = "+12345678199"
//        ),
//        post = "Organizer",
//        priority = 4
//      ),
//      ContactPeople(
//        member = Member(
//          name = "Member 55",
//          profileImage = "https://example.com/profile10.jpg",
//          phoneNumber = "+12345678492"
//        ),
//        post = "Manager",
//        priority = 2
//      )
//    )
//  )
//  ActivityDisplay(activity = sampleActivity)
//}