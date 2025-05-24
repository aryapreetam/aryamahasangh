package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aryamahasangh.viewmodel.JoinUsViewModel

@Composable
fun AryaPariwarScreen(viewModel: JoinUsViewModel) {
  Column(Modifier.padding(8.dp)){
    Text("Arya Pariwar")
  }
}