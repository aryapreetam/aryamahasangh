package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.aryamahasangh.OrganisationsQuery
import org.aryamahasangh.components.OrgItem
import org.aryamahasangh.navigation.Screen
import org.aryamahasangh.network.apolloClient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Orgs(navController: NavHostController, onNavigateToOrgDetails: (String) -> Unit) {

  val organisations = remember { mutableStateOf<List<OrganisationsQuery.Organisation>>(listOf()) }
  LaunchedEffect(Unit) {
    val res = apolloClient.query(OrganisationsQuery()).execute()
    val orgs = res.data?.organisations
    organisations.value = orgs ?: emptyList()
  }

  Column(
    modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)) {
    FlowRow(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      organisations.value.forEach {
        OrgItem(it.name, it.description){
          onNavigateToOrgDetails(it.name)
          navController.navigate(Screen.OrgDetails(it.name))
        }
      }
    }
  }
}