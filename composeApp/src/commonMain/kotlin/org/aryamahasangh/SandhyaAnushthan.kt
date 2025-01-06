package org.aryamahasangh

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun SandhyaAnushthan(){
  Scaffold(
    topBar = {Text("Top Bar")},
    bottomBar = {Text("Bottom Bar ")}
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      var inputVal by remember { mutableStateOf("") }
      Text("Now it is fixed")
      OutlinedButton(
        onClick = {}
      ){
        Text("Preetam")
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
}