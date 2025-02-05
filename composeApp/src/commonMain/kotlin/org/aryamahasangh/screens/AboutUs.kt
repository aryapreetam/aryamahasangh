package org.aryamahasangh.screens

import androidx.compose.runtime.Composable
import org.aryamahasangh.components.Organisation
import org.aryamahasangh.listOfSabha

@Composable
fun AboutUs() {
  Organisation(listOfSabha[11])
}