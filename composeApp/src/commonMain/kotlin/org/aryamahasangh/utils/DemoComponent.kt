package org.aryamahasangh.utils

import androidx.compose.runtime.Composable
import org.aryamahasangh.OrganisationalActivityDetailQuery.*
import org.aryamahasangh.screens.JoinUsScreen
import org.aryamahasangh.screens.OrganisationalActivityForm
import org.aryamahasangh.type.ActivityType

@Composable
fun DemoComposable(){
  JoinUsComponent()
}

@Composable
fun JoinUsComponent(){
  JoinUsScreen()
}

@Composable
fun AddOrganisationInputForm(){
  //  val sampleOrganisation = Organisation(
//    name = "राष्ट्रीय आर्य निर्मात्री सभा",
//    description = "प्रत्येक बुद्धिमान व्यक्ति यह समझ सकता है कि मानवीय जीवन अति दुर्लभ है...",
//    logo = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image//nirmatri_sabha.webp",
//    keyPeople = listOf(
//      KeyPeople(
//        member = Member(
//          name = "आचार्य जितेन्द्र आर्य",
//          email = "",
//          phoneNumber = "9416201731",
//          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp",
//          educationalQualification = ""
//        ),
//        post = "अध्यक्ष",
//        priority = 1
//      )
//    )
//  )
//      OrganisationForm(organisation = sampleOrganisation)
}

@Composable
fun OrganisationActivityForm(){
  val sampleActivity = OrganisationalActivity(
    id = "eb179afc-aec7-4c87-8e51-9a9f192d4551",
    name = "Health Awareness Drive",
    description = "Campaign for spreading awareness about health and hygiene.",
    associatedOrganisation = listOf(
      "385933f4-2eca-46bb-bb31-50277d08d873",
      "36f4e51f-1517-4f1e-8395-d1d83b7c76ea"
    ),
    activityType = ActivityType.CAMPAIGN,
    place = "Bilāspur",
    startDateTime = "2025-02-20T15:38:26.337446",
    endDateTime = "2025-04-08T15:38:26.337446",
    mediaFiles = emptyList(),
    additionalInstructions = "Please follow COVID-19 protocols.",
    contactPeople = listOf(
      ContactPeople(
        member = Member(
          name = "Member 13",
          profileImage = "https://example.com/profile32.jpg",
          phoneNumber = "+12345678384"
        ),
        post = "Manager",
        priority = 4
      )
    )
  )
  OrganisationalActivityForm(activity = sampleActivity)
}
