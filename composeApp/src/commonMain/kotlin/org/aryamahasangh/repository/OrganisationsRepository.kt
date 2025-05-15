package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.OrganisationsQuery
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.util.Result
import org.aryamahasangh.util.safeCall

/**
 * Repository for handling organisations-related operations
 */
interface OrganisationsRepository {
  /**
   * Get all organisations
   */
  fun getOrganisations(): Flow<Result<List<OrganisationsQuery.Organisation>>>

  /**
   * Get organisation details by name
   */
  fun getOrganisationByName(name: String): Flow<Result<OrganisationQuery.Organisation>>
}

/**
 * Implementation of OrganisationsRepository that uses Apollo GraphQL client
 */
class OrganisationsRepositoryImpl(private val apolloClient: ApolloClient) : OrganisationsRepository {

  override fun getOrganisations(): Flow<Result<List<OrganisationsQuery.Organisation>>> = flow {
    emit(Result.Loading)

    val result = safeCall {
      val response = apolloClient.query(OrganisationsQuery()).execute()
      if (response.hasErrors()) {
        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
      }
      response.data?.organisations ?: emptyList()
    }

    emit(result)
  }

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