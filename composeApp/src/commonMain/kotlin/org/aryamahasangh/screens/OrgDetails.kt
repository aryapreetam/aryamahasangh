package org.aryamahasangh.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.components.OrganisationDetail
import org.aryamahasangh.network.apolloClient

@Composable
fun OrgDetailScreen(name: String, navController: NavHostController){
  val organisation = remember { mutableStateOf<OrganisationQuery.Organisation?>(null) }
  LaunchedEffect(Unit) {
    val res = apolloClient.query(OrganisationQuery(name)).execute()
    val org = res.data?.organisation
    organisation.value = org
  }
  if(organisation.value == null){
    return
  }
  OrganisationDetail(organisation.value!!)
}
