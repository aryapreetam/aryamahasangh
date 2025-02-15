package org.aryamahasangh.screens

import androidx.compose.runtime.*
import org.aryamahasangh.OnOrganisationCreatedSubscription
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.components.OrganisationDetail
import org.aryamahasangh.network.apolloClient

@Composable
fun AboutUs() {
  val organisation = remember { mutableStateOf<OrganisationQuery.Organisation?>(null) }
//  val organisationAddedFlow = remember { apolloClient.subscription(OnOrganisationCreatedSubscription()).toFlow() }
//  val organisationAddedResponse = organisationAddedFlow.collectAsState(initial = null)
//
//  if(organisationAddedResponse == null) return
//  LaunchedEffect(organisationAddedResponse.value) {
//    if(organisationAddedResponse.value == null) return@LaunchedEffect
//    val message = when(organisationAddedResponse.value!!.data?.onOrganisationCreated?.name){
//      null -> null
//      else -> {
//        println("new org: ${organisationAddedResponse.value!!.data?.onOrganisationCreated?.name}")
//        organisationAddedResponse.value!!.data?.onOrganisationCreated?.name
//      }
//    }
//    println("$message")
//  }

  LaunchedEffect(Unit) {
    val res = apolloClient.query(OrganisationQuery("आर्य महासंघ")).execute()
    val org = res.data?.organisation
    organisation.value = org
  }
  if(organisation.value == null){
    return
  }
  OrganisationDetail(organisation.value!!)
}