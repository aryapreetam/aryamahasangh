package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling about us related operations
 */
interface AboutUsRepository {
  /**
   * Get organisation details by name
   */
  fun getOrganisationByName(name: String): Flow<Result<OrganisationQuery.Organisation>>
}

/**
 * Implementation of AboutUsRepository that uses Apollo GraphQL client
 */
class AboutUsRepositoryImpl(private val apolloClient: ApolloClient) : AboutUsRepository {

  override fun getOrganisationByName(name: String): Flow<Result<OrganisationQuery.Organisation>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(OrganisationQuery(name)).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.organisation ?: throw Exception("Organisation not found")
    }

    emit(result)
  }
}