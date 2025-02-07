package org.aryamahasangh.screens

import androidx.compose.runtime.Composable
import org.aryamahasangh.components.Organisation
import org.aryamahasangh.listOfOrganisations

@Composable
fun AboutUs() {
  Organisation(listOfOrganisations[11])
}