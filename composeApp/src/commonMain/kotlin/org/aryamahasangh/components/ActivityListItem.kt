package org.aryamahasangh.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import org.aryamahasangh.OrganisationalActivitiesQuery.OrganisationalActivity
import org.aryamahasangh.SettingKeys
import org.aryamahasangh.type.ActivityType
import org.aryamahasangh.utils.formatShort

val activityTypeData = mapOf(
  ActivityType.COURSE to "कक्षा",
  ActivityType.EVENT to "कार्यक्रम",
  ActivityType.CAMPAIGN to "अभियान",
  ActivityType.SESSION to "सत्र"
)

//@Preview
//@Composable
//fun ActivityListItemPreview() {
//  val data = OrganisationalActivity(
//    id = "3",
//    name = "आर्य प्रशिक्षण सत्र",
//    description = "महान संत परम्पराओं को जानने का बौद्धिक प्रकल्प",
//    startDateTime = "2025-05-10T14:00:00",
//    endDateTime = "2025-05-10T15:00:00",
//    activityType = ActivityType.SESSION,
//    district = "Bangalore"
//  )
//  ActivityListItem(data){}
//}

@Composable
fun ActivityListItem(
  activity: OrganisationalActivity,
  handleOnClick: () -> Unit = {},
  handleDeleteActivity: () -> Unit = {}
) {

  val startDate = formatShort(activity.startDateTime)
  val endDate = formatShort(activity.endDateTime)
  var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
  var showConfirmDialog by remember { mutableStateOf(false) }

  ElevatedCard(
    modifier = Modifier.width(500.dp),
    shape = RoundedCornerShape(4.dp),
    onClick = handleOnClick
  ) {
    Box(
      modifier = Modifier.padding(8.dp).fillMaxWidth()
    ){
      Column() {
        Text(
          text = activity.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          modifier = Modifier.background(MaterialTheme.colorScheme.outlineVariant).padding(vertical = 1.dp, horizontal = 8.dp),
          text = "${activityTypeData[activity.activityType]}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = activity.shortDescription, style = MaterialTheme.typography.bodyMedium,
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
          Text(text = "स्थान: ${activity.district}", style = MaterialTheme.typography.bodySmall)
        }
      }
      if(isLoggedIn){
        Box(
          modifier = Modifier.size(36.dp).clickable {
            showConfirmDialog = true
          }.align(alignment = Alignment.TopEnd),
          contentAlignment = Alignment.Center
        ){
          Icon(
            Icons.Default.Delete,
            contentDescription = "delete activity",
            Modifier.size(24.dp)
          )
        }
      }
    }
    if (showConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showConfirmDialog = false },
        title = { Text("Delete Activity") },
        text = { Text("Are you sure you want delete this activity?") },
        confirmButton = {
          TextButton(onClick = {
            showConfirmDialog = false
            handleDeleteActivity()
          }) {
            Text("Yes")
          }
        },
        dismissButton = {
          TextButton(onClick = { showConfirmDialog = false }) {
            Text("No")
          }
        }
      )
    }
  }
}