package com.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.aryamahasangh.features.organisations.OrganisationDetail
import com.aryamahasangh.nhost.OrganisationByNameQuery
import com.aryamahasangh.nhost.OrganisationNamesQuery
import com.aryamahasangh.util.Result
import com.aryamahasangh.util.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Data class representing organization name and id
 */
data class OrganisationName(
  val id: String,
  val name: String
)

/**
 * Repository for handling about us related operations
 */
interface AboutUsRepository {
  /**
   * Get organisation details by name
   */
  fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>>

  /**
   * Get all organisation names
   */
  fun getOrganisationNames(): Flow<Result<List<OrganisationName>>>
}

/**
 * Implementation of AboutUsRepository that uses Apollo GraphQL client
 */
class AboutUsRepositoryImpl(private val apolloClient: ApolloClient) : AboutUsRepository {
  override fun getOrganisationByName(name: String): Flow<Result<OrganisationDetail>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response =
            apolloClient.query(OrganisationByNameQuery(name)).execute()

          if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
            throw Exception(errorMessage)
          }
          val org = response.data?.organisation?.first()!!
          OrganisationDetail(
            id = org.id,
            name = org.name,
            description = org.description,
            logo = org.logo
          )
        }
      emit(result)
    }

  override fun getOrganisationNames(): Flow<Result<List<OrganisationName>>> =
    flow {
      emit(Result.Loading)

      val result =
        safeCall {
          val response = apolloClient.query(OrganisationNamesQuery()).execute()

          if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Unknown error occurred"
            throw Exception(errorMessage)
          }
          response.data?.organisation?.map { edge ->
            OrganisationName(
              id = edge.id,
              name = edge.name
            )
          } ?: emptyList()
        }

      emit(result)
    }
}
