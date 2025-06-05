package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GurukulCollegeHomeScreen(navigateToAdmissionForm: () -> Unit) {
  Column(Modifier.padding(8.dp)) {
    listOf("आर्य गुरुकुल महाविद्यालय").forEach { name ->
      Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(vertical = 8.dp)
      )
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        listOf("वर्तमान कार्य", "गुरुकुल प्रवेश", "आगामी बैठक", "कक्षाएं").forEach {
          OutlinedCard(
            onClick = {
              if (it == "गुरुकुल प्रवेश") {
                // navigateToAdmissionForm()
              }
            }
          ) {
            Text(
              text = it,
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp)
            )
          }
        }
      }
    }
  }
}
