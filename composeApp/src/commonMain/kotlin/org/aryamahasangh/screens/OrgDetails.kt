package org.aryamahasangh.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import org.aryamahasangh.components.Organisation
import org.aryamahasangh.listOfOrganisations

@Composable
fun OrgDetailScreen(name: String, navController: NavHostController){
  Organisation(listOfOrganisations.find { it.name == name }!!)
}
