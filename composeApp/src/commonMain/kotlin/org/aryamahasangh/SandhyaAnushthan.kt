package org.aryamahasangh

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun SandhyaAnushthan(){
  Scaffold(
    topBar = {Text("Top Bar")},
    bottomBar = {Text("Bottom Bar ")},
  ) {
    SampleContent()
  }
}

@Composable
fun SampleContent() {
  Column(modifier = Modifier.padding(16.dp)) {
    var inputVal by remember { mutableStateOf("") }
    Text("Now it is fixed")
    Card(
      modifier = Modifier.safeContentPadding(),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Text("Simple card", modifier = Modifier.padding(16.dp))
    }
    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(1f),
      label = {Text("Label")},
      placeholder = {Text("Placeholder")},
      value = inputVal,
      onValueChange = {
        inputVal = it
      }
    )
  }
}