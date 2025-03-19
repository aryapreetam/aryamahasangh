package org.aryamahasangh.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.OrganisationQuery.KeyPeople
import org.aryamahasangh.OrganisationQuery.Member
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
fun OrganisationForm(organisation: OrganisationQuery.Organisation) {

  var nameState = remember { mutableStateOf(TextFieldValue(organisation.name)) }
  var descriptionState = remember { mutableStateOf(TextFieldValue(organisation.description)) }
  var logoImageState = remember { mutableStateOf<ImageBitmap?>(null) } // State for the logo image

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {

    Text(
      text = "Organisation Details",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    OutlinedTextField(
      value = nameState.value,
      onValueChange = { nameState.value = it },
      label = { Text("Name") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )

    OutlinedTextField(
      value = descriptionState.value,
      onValueChange = { descriptionState.value = it },
      label = { Text("Description") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      minLines = 3
    )

    // Logo Image Upload
    Text(
      text = "Organisation Logo",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = CenterVertically) {
      if (logoImageState.value != null) {
        Image(
          bitmap = logoImageState.value!!,
          contentDescription = "Uploaded Logo",
          modifier = Modifier
            .height(100.dp)
            .fillMaxWidth()
            .padding(top = 8.dp),
          contentScale = ContentScale.Fit
        )
      } else {
        Box(
          modifier = Modifier
            .size(100.dp)
            .fillMaxWidth()
            .background(Color.LightGray),
          contentAlignment = Alignment.Center
        ) {
          Text("Organisation \n Logo")
        }
      }
      ImageUploadComponent(
        onImageSelected = { imageBitmap ->
          logoImageState.value = imageBitmap
        }
      )
    }


    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Add Key Person",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    KeyPersonInputForm()

    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = { /*TODO: Save data*/ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
      Text("Save")
    }
  }
}

@Composable
fun ImageUploadComponent(onImageSelected: (ImageBitmap) -> Unit) {
  val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

  // Placeholder for image selection logic (platform-specific)
  Button(onClick = {
    // Replace this with actual image selection code for each platform
    // (e.g., using Intent on Android, FileChooser on Desktop, etc.)
    // After selecting the image, load it into an ImageBitmap and
    // update the imageBitmap state:
    //
    // Example (Android):
    // val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    // val launcher = (context as? ActivityResultRegistryOwner)?.activityResultRegistry?.register(
    //     "image_picker",
    //     ActivityResultContracts.StartActivityForResult()
    // ) { result ->
    //     if (result.resultCode == Activity.RESULT_OK) {
    //         val uri = result.data?.data
    //         uri?.let {
    //             val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
    //             imageBitmap.value = bitmap.asImageBitmap()
    //             imageBitmap.value?.let { it1 -> onImageSelected(it1) }
    //         }
    //     }
    // }
    // launcher?.launch(intent)

    // For now, using a default image
    //val defaultImage = ImageBitmap.imageResource(id = androidx.core.R.drawable.ic_call_answer)  // Replace with your default image resource
    //imageBitmap.value = defaultImage
    //onImageSelected(defaultImage)
  }) {
    Text("Upload Image")
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyPersonInputForm() {
  var memberName by remember { mutableStateOf(TextFieldValue("")) }
  var memberEmail by remember { mutableStateOf(TextFieldValue("")) }
  var memberPhone by remember { mutableStateOf(TextFieldValue("")) }
  var memberProfileImageState = remember { mutableStateOf<ImageBitmap?>(null) }
  var postState by remember { mutableStateOf(TextFieldValue("")) }
  var priorityState by remember { mutableStateOf(1) }

  // Dropdown state
  var expanded by remember { mutableStateOf(false) }
  val priorityOptions = (1..5).toList()
  val selectedPriorityText = remember { mutableStateOf(priorityState.toString()) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {

      OutlinedTextField(
        value = memberName,
        onValueChange = { memberName = it },
        label = { Text("Member Name") },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      )

      OutlinedTextField(
        value = memberEmail,
        onValueChange = { memberEmail = it },
        label = { Text("Member Email") },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      )

      OutlinedTextField(
        value = memberPhone,
        onValueChange = { memberPhone = it },
        label = { Text("Member Phone") },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      )
      Text(
        text = "Member Profile Image",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Normal,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = CenterVertically) {
// Profile Image Upload



        if (memberProfileImageState.value != null) {
          Image(
            bitmap = memberProfileImageState.value!!,
            contentDescription = "Uploaded Profile Image",
            modifier = Modifier
              .height(50.dp)
              .width(50.dp)
              .padding(top = 8.dp)
              .clip(CircleShape),
            contentScale = ContentScale.Crop  // Use Crop for profile images
          )
        } else {
          Box(
            modifier = Modifier
              .size(100.dp)
              .background(Color.Gray)
              .clip(CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("Profile \nPhoto", color = Color.White)
          }
        }
        ImageUploadComponent(
          onImageSelected = { imageBitmap ->
            memberProfileImageState.value = imageBitmap
          }
        )
      }


      OutlinedTextField(
        value = postState,
        onValueChange = { postState = it },
        label = { Text("Post") },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      )

      ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        OutlinedTextField(
          readOnly = true,
          value = selectedPriorityText.value,
          onValueChange = { },
          label = { Text("Priority") },
          trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(
              expanded = expanded
            )
          },
          modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false }
        ) {
          priorityOptions.forEach { priority ->
            DropdownMenuItem(
              text = { Text(text = priority.toString()) },
              onClick = {
                priorityState = priority
                selectedPriorityText.value = priority.toString()
                expanded = false
              }
            )
          }
        }
      }
    }
  }
}


@Preview
@Composable
fun PreviewOrganisationForm() {
  val sampleOrganisation = OrganisationQuery.Organisation(
    id = "sdfdsf",
    name = "राष्ट्रीय आर्य निर्मात्री सभा",
    description = "प्रत्येक बुद्धिमान व्यक्ति यह समझ सकता है कि मानवीय जीवन अति दुर्लभ है...",
    logo = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image//nirmatri_sabha.webp",
    keyPeople = listOf(
      KeyPeople(
        member = Member(
          name = "आचार्य जितेन्द्र आर्य",
          email = "",
          phoneNumber = "9416201731",
          profileImage = "https://placeholder-staging-supabase.co/storage/v1/object/public/profile_image/achary_jitendra.webp",
          educationalQualification = ""
        ),
        post = "अध्यक्ष",
        priority = 1
      )
    )
  )

  MaterialTheme {
    OrganisationForm(organisation = sampleOrganisation)
  }
}