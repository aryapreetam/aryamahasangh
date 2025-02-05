package org.aryamahasangh.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import org.aryamahasangh.components.Organisation
import org.aryamahasangh.listOfSabha

@Composable
fun OrgDetailScreen(name: String, navController: NavHostController){
  Organisation(listOfSabha.find { it.name == name }!!)
}
