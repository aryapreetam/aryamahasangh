package org.aryamahasangh.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aryamahasangh.OrganisationalActivityDetailQuery
import org.aryamahasangh.network.apolloClient
import org.aryamahasangh.utils.format
import kotlin.random.Random

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

  LeadershipWorkshopScreen(data)
}

@Composable
fun PlaceholderImage(modifier: Modifier = Modifier) {
  Box(modifier = modifier.aspectRatio(1f).background(Color.LightGray)) // Square Placeholder
}

@Composable
fun ContactPerson(name: String, profileImage: String?, post: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Placeholder avatar - replace with actual image loading
    Box(modifier = Modifier.size(40.dp).background(Color.Gray))
    Spacer(modifier = Modifier.width(8.dp))
    Column{
      Text(text = name, style = MaterialTheme.typography.bodyLarge)
      Text(text = post, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
fun DetailItem(label: String, value: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(text = "$label: ", fontWeight = FontWeight.Bold)
    Text(text = value)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadershipWorkshopScreen(data: OrganisationalActivityDetailQuery.OrganisationalActivity) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp) // Add some general padding
  ) {

    Text(
      modifier = Modifier.padding(16.dp),
      text = data.name,
      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    )
    Text(
      modifier = Modifier.background(Color.Cyan).padding(8.dp),
      text = data.activityType.toString()
    )


    Text(
      text = data.description,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    Text(
      text = "Associated Organisations",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 16.dp)
    ) {
      data.associatedOrganisation.forEach {
        AssistChip(onClick = { /*TODO*/ }, label = { Text(it.toString().takeLast(Random.nextInt(5, 10))) })
      }
    }

    DetailItem(label = "Location", value = data.place)
    DetailItem(label = "Date & Time", value = "${format(data.startDateTime)} -  ${format(data.endDateTime)}")

    //Image Grid
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(top = 16.dp)
    )
    {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp))
      {
        PlaceholderImage(modifier = Modifier.width(150.dp).height(100.dp))
        PlaceholderImage(modifier = Modifier.width(150.dp).height(100.dp))
        PlaceholderImage(modifier = Modifier.width(150.dp).height(100.dp))
      }
    }


    Text(
      text = "Contact People",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )

    Column {
      data.contactPeople.forEach {
        ContactPerson(name = it.member.name, it.member.profileImage, it.post)
      }
    }

    Text(
      text = "Additional Instructions",
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )

    Text(
      text = data.additionalInstructions,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(bottom = 16.dp)
    )
  }
}

//@Preview
//@Composable
//fun PreviewLeadershipWorkshopScreen() {
//  MaterialTheme { // Ensure MaterialTheme is provided in preview
//    LeadershipWorkshopScreen(data)
//  }
//}
