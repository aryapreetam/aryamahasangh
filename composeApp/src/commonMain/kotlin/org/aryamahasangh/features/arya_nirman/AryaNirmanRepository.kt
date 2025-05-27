package org.aryamahasangh.features.arya_nirman

import com.apollographql.apollo.ApolloClient
import org.aryamahasangh.UpcomingSatrActivitiesQuery

interface AryaNirmanRepository {
  fun getUpcomingActivities()
  fun registerForActivity()
}

class AryaNirmanRepositoryImpl(private val apolloClient: ApolloClient) : AryaNirmanRepository {
  override fun getUpcomingActivities() {
    apolloClient.query(UpcomingSatrActivitiesQuery())
  }

  override fun registerForActivity() {
    TODO("Not yet implemented")
  }
}