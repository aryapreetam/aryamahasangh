package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.aryamahasangh.OrganisationQuery
import org.aryamahasangh.OrganisationsQuery
import org.aryamahasangh.UpdateOrganisationLogoMutation
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
  fun updateOrganisationLogo(orgId: String, imageUrl: String): Flow<Result<Boolean>>
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

  override fun updateOrganisationLogo(
    orgId: String,
    imageUrl: String
  ): Flow<Result<Boolean>> {
    return flow {
      emit(Result.Loading)
      val result = safeCall {
        val response = apolloClient.mutation(
          UpdateOrganisationLogoMutation(orgId, imageUrl)
        ).execute()
        if (response.hasErrors()) {
          throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
        }
        response.data?.updateOrganisationLogo ?: false
      }
      emit(result)
    }
  }
}