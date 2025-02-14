package org.aryamahasangh

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object OrganisationalActivityPublisher {
  private val mutableOrganisationalActivityFlow = Channel<OrganisationalActivity>()
  // Readonly
  val organisationalActivityFlow = mutableOrganisationalActivityFlow.receiveAsFlow()
  fun publishConference(organisationalActivity: OrganisationalActivity) {
    mutableOrganisationalActivityFlow.trySend(organisationalActivity)
  }
}