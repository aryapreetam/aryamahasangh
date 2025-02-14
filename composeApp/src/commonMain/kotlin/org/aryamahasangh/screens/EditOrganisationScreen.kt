package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun EditOrganisation() {
  Column {

    OutlinedTextField(
      value = "",
      label = {"Name"},
      placeholder = {"Name"},
      onValueChange = { },
    )
    OutlinedTextField(
      value = "",
      label = {"Description"},
      placeholder = {"Description"},
      onValueChange = { },
    )

    Button(onClick = { }) {
      Text("Submit")
    }
  }
}