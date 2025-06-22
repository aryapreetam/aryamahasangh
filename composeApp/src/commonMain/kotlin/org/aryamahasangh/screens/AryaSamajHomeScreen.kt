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
import org.aryamahasangh.features.admin.data.AryaSamajViewModel

@Composable
fun AryaSamajHomeScreen(viewModel: AryaSamajViewModel? = null) {
  Column(Modifier.padding(8.dp)) {
    listOf("आर्ष कर्मविधि").forEach { name ->
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
        listOf("प्रातःकालीन मंत्र", "उपासना", "संध्या", "अग्निहोत्र", "भोजन मन्त्र", "शयन मंत्र", "वैदिक राष्ट्रीय प्रार्थना", "स्वराज्य यज्ञ", "नवसंवत्सर यज्ञ", "वासंती नवसस्येष्टि (होली)", "शारदीय नवसस्येष्टि (दीपावली)").forEach {
          OutlinedCard(
            onClick = {}
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
