package org.aryamahasangh.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aryamahasangh.OrganisationalActivitiesQuery.OrganisationalActivity
import org.aryamahasangh.type.ActivityType
import org.aryamahasangh.utils.formatShort
import org.jetbrains.compose.ui.tooling.preview.Preview

val activityTypeData = mapOf(
  ActivityType.COURSE to "कक्षा",
  ActivityType.EVENT to "कार्यक्रम",
  ActivityType.CAMPAIGN to "अभियान",
  ActivityType.SESSION to "सत्र"
)

@Preview
@Composable
fun ActivityListItemPreview() {
  val data = OrganisationalActivity(
    id = "3",
    name = "आर्य प्रशिक्षण सत्र",
    description = "महान संत परम्पराओं को जानने का बौद्धिक प्रकल्प",
    startDateTime = "2025-05-10T14:00:00",
    endDateTime = "2025-05-10T15:00:00",
    activityType = ActivityType.SESSION,
    place = "Community Hall, Bangalore"
  )
  ActivityListItem(data){}
}

@Composable
fun ActivityListItem(activity: OrganisationalActivity, handleOnClick: () -> Unit = {}) {

  val startDate = formatShort(activity.startDateTime)
  val endDate = formatShort(activity.endDateTime)

  ElevatedCard(
    modifier = Modifier.width(500.dp),
    shape = RoundedCornerShape(4.dp),
    onClick = handleOnClick
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(
        text = activity.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        modifier = Modifier.background(Color.LightGray).padding(vertical = 1.dp, horizontal = 8.dp),
        text = "${activityTypeData[activity.activityType]}", style = MaterialTheme.typography.bodyLarge)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = activity.description, style = MaterialTheme.typography.bodyMedium,
        maxLines = 1)
      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Date", tint = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "प्रारंभ: $startDate", style = MaterialTheme.typography.bodySmall,)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "समाप्ति: $endDate", style = MaterialTheme.typography.bodySmall)
      }
      Spacer(modifier = Modifier.height(4.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Place", tint = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "स्थान: ${activity.place}", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}