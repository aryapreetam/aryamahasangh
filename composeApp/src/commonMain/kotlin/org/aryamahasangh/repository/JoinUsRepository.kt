package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.type.ActivityFilterInput
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall
import com.apollographql.apollo.api.Optional

/**
 * Repository for handling join us related operations
 */
interface JoinUsRepository {
  /**
   * Get filtered activities
   */
  fun getFilteredActivities(filter: ActivityFilterInput): Flow<Result<List<OrganisationalActivitiesQuery.OrganisationalActivity>>>
}

/**
 * Implementation of JoinUsRepository that uses Apollo GraphQL client
 */
class JoinUsRepositoryImpl(private val apolloClient: ApolloClient) : JoinUsRepository {

  override fun getFilteredActivities(filter: ActivityFilterInput): Flow<Result<List<OrganisationalActivitiesQuery.OrganisationalActivity>>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(
        OrganisationalActivitiesQuery(filter = Optional.present(filter))
      ).execute()
      
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.organisationalActivities ?: emptyList()
    }

    emit(result)
  }
}