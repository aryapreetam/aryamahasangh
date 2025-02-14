package org.aryamahasangh

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class OrgSubscriptionService : Subscription {
  @GraphQLDescription("Returns a random number every second")
  fun counter(limit: Int? = null): Flow<Int> = flow {
    var count = 0
    while (true) {
      count++
      if (limit != null) {
        if (count > limit) break
      }
      emit(Random.nextInt())
      delay(1000)
    }
  }

  @GraphQLDescription("Emits when a new organisation is created")
  fun onOrganisationCreated(): Flow<Organisation> = OrganisationPublisher.organisationFlow

  @GraphQLDescription("Emits when a new activity is created")
  fun onOrganisationalActivityCreated(): Flow<OrganisationalActivity> = OrganisationalActivityPublisher.organisationalActivityFlow
}

