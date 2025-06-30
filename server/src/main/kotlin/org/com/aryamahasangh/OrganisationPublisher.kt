package com.aryamahasangh

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object OrganisationPublisher {
  private val mutableOrganisationFlow = Channel<Organisation>()
  // Readonly
  val organisationFlow = mutableOrganisationFlow.receiveAsFlow()
  fun publishOrganisation(organisation: Organisation) {
    mutableOrganisationFlow.trySend(organisation)
  }
}