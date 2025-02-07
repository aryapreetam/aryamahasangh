package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.aryamahasangh.components.OrgItem
import org.aryamahasangh.listOfOrganisations
import org.aryamahasangh.navigation.Screen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Orgs(navController: NavHostController, onNavigateToOrgDetails: (String) -> Unit) {
  Column(
    modifier = Modifier.padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)) {
    FlowRow(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      listOfOrganisations.take(10).forEach {
        OrgItem(it.name, it.description){
          onNavigateToOrgDetails(it.name)
          navController.navigate(Screen.OrgDetails(it.name))
        }
      }
    }
  }
}